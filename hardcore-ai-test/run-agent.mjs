import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { FreeAiMesh } from '../../free-ai-mesh/FreeAiMesh.mjs';

const ROOT = path.dirname(fileURLToPath(import.meta.url));
const SOURCE = path.join(ROOT, 'src', 'scheduler.mjs');
const SPEC = path.join(ROOT, 'test', 'spec.mjs');
const REPORT_PATH = path.join(ROOT, 'report.json');
const HORDE_BASE = 'https://oai.aihorde.net/v1';
const ANON_KEY = '0000000000';
const MAX_MODELS = Number(process.env.QUICK_MAX_MODELS || 4);
const MODEL_TIMEOUT_MS = Number(process.env.QUICK_MODEL_TIMEOUT_MS || 45000);
const REPAIR_ROUNDS = 1;

const read = file => fs.readFileSync(file, 'utf8');

function extractFile(text) {
  const startMarker = '<FILE path="hardcore-ai-test/src/scheduler.mjs">';
  const endMarker = '</FILE>';
  const start = text.indexOf(startMarker);
  if (start >= 0) {
    const bodyStart = start + startMarker.length;
    const end = text.indexOf(endMarker, bodyStart);
    if (end >= 0) return text.slice(bodyStart, end).trim();
  }

  const fenceStart = text.indexOf('```');
  if (fenceStart >= 0) {
    const afterFence = text.indexOf('\n', fenceStart);
    const fenceEnd = text.indexOf('```', afterFence + 1);
    if (afterFence >= 0 && fenceEnd > afterFence) {
      const candidate = text.slice(afterFence + 1, fenceEnd).trim();
      if (candidate.includes('parseKeyValuePairs')) return candidate;
    }
  }

  const exportStart = text.indexOf('export function parseKeyValuePairs');
  if (exportStart >= 0) return text.slice(exportStart).trim();

  throw new Error('model response did not contain a parseable scheduler.mjs');
}

async function horde(pathname, init = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), MODEL_TIMEOUT_MS);
  try {
    const response = await fetch(`${HORDE_BASE}${pathname}`, {
      ...init,
      signal: controller.signal,
      headers: {
        Authorization: `Bearer ${ANON_KEY}`,
        'Content-Type': 'application/json',
        ...(init.headers || {}),
      },
    });
    const body = await response.text();
    if (!response.ok) throw new Error(`Horde HTTP ${response.status}: ${body.slice(0, 500)}`);
    return JSON.parse(body);
  } finally {
    clearTimeout(timer);
  }
}

async function discoverModels() {
  const data = await horde('/models');
  const ids = Array.isArray(data?.data) ? data.data.map(model => model?.id).filter(Boolean) : [];
  const preferred = ids.filter(id => /coder|code|qwen|deepseek|mistral|llama|glm|hermes|command|starcoder|7b|8b/i.test(id));
  const selected = [...new Set([...preferred, ...ids])].slice(0, MAX_MODELS);
  if (!selected.length) throw new Error('AI Horde exposed no text models through its OpenAI-compatible catalog');
  return selected;
}

async function chat(model, messages) {
  const data = await horde('/chat/completions', {
    method: 'POST',
    body: JSON.stringify({ model, messages, temperature: 0.1, max_tokens: 2200 }),
  });
  const text = data?.choices?.[0]?.message?.content;
  if (!text) throw new Error(`model ${model} returned an empty completion`);
  return text;
}

function runSpec(workRoot) {
  const run = spawnSync(process.execPath, [path.join(workRoot, 'test', 'spec.mjs')], {
    cwd: workRoot,
    encoding: 'utf8',
    timeout: 10000,
    maxBuffer: 1024 * 1024,
  });
  return {
    ok: run.status === 0,
    status: run.status,
    stdout: run.stdout || '',
    stderr: run.stderr || '',
    error: run.error ? String(run.error) : '',
  };
}

async function solveWithModel(model, task) {
  const workRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'veytrix-basic-ai-'));
  fs.mkdirSync(path.join(workRoot, 'src'), { recursive: true });
  fs.mkdirSync(path.join(workRoot, 'test'), { recursive: true });
  fs.copyFileSync(SOURCE, path.join(workRoot, 'src', 'scheduler.mjs'));
  fs.copyFileSync(SPEC, path.join(workRoot, 'test', 'spec.mjs'));

  const attempts = [];
  let current = read(path.join(workRoot, 'src', 'scheduler.mjs'));
  let feedback = '';

  for (let round = 1; round <= REPAIR_ROUNDS + 1; round += 1) {
    const prompt = `${task}\n\nCURRENT FILE:\n<FILE path="hardcore-ai-test/src/scheduler.mjs">\n${current}\n</FILE>\n\nREAD-ONLY TESTS:\n<TEST>\n${read(SPEC)}\n</TEST>\n\n${feedback}\n\nReturn ONLY the complete repaired file using exactly this wrapper:\n<FILE path="hardcore-ai-test/src/scheduler.mjs">\n...full file...\n</FILE>\nNo prose outside the wrapper.`;

    try {
      const response = await chat(model, [
        {
          role: 'system',
          content: 'You are a practical senior software engineer. Solve the small coding task directly. Do not modify tests or add dependencies. Return a complete replacement for the requested source file.',
        },
        { role: 'user', content: prompt },
      ]);
      const candidate = extractFile(response);
      fs.writeFileSync(path.join(workRoot, 'src', 'scheduler.mjs'), `${candidate}\n`);
      const result = runSpec(workRoot);
      attempts.push({ round, ok: result.ok, status: result.status, stdout: result.stdout.slice(0, 1500), stderr: result.stderr.slice(0, 2500) });
      if (result.ok) return { ok: true, model, attempts };
      current = candidate;
      feedback = `REPAIR FEEDBACK: The unmodified tests failed. Fix the implementation and return the complete file again. stdout=${result.stdout.slice(0, 1500)} stderr=${result.stderr.slice(0, 2800)}`;
    } catch (error) {
      attempts.push({ round, ok: false, error: String(error?.message ?? error).slice(0, 2200) });
      feedback = `HARNESS FEEDBACK: ${String(error?.message ?? error).slice(0, 2500)}. Return the complete file in the required wrapper and do not add dependencies.`;
    }
  }

  return { ok: false, model, attempts };
}

const task = `BASIC CODING BENCHMARK. Repair ONLY hardcore-ai-test/src/scheduler.mjs so every unmodified test passes. Implement parseKeyValuePairs(text) with these exact semantics: if text is not a string, return a fresh empty object; otherwise parse comma-separated segments; trim each segment; ignore empty segments; split each non-empty segment at the FIRST '=' only; trim the key and value; ignore segments whose trimmed key is empty; if '=' is missing, use an empty-string value; if a key appears more than once, the last value wins; preserve everything after the first '=' including additional '=' characters. Do not add dependencies, do not modify tests, and do not mutate any caller-owned input. Keep the implementation small and clean.`;

const modelResults = [];
const report = {
  benchmark: 'VEYTRIX-BASIC-DEPENDENCY-FREE-AI-CODING-PROOF',
  generatedAt: new Date().toISOString(),
  transport: 'AI Horde OpenAI-compatible proxy',
  anonymousLane: true,
  maxModels: MAX_MODELS,
  repairRoundsPerModel: REPAIR_ROUNDS + 1,
  models: modelResults,
};

try {
  const modelIds = await discoverModels();
  report.discovered_models = modelIds;

  const adapter = {
    listModels: async () => modelIds.map(id => ({ id })),
    chat: async (modelId, { task: routingTask }) => {
      const result = await solveWithModel(modelId, routingTask);
      modelResults.push(result);
      console.log(JSON.stringify(result));
      if (!result.ok) {
        const error = new Error(`model ${modelId} failed the basic coding benchmark`);
        error.retryable = true;
        throw error;
      }
      return { text: `MODEL_SOLVED=${modelId}` };
    },
  };

  const mesh = new FreeAiMesh({ adapter, maxAttempts: modelIds.length, cooldownMs: 1000, maxDiscovered: modelIds.length });
  const routed = await mesh.ask(task);
  report.mesh_result = { success: true, selected_model: routed.model, attempts: routed.attempts };
  report.success = true;
} catch (error) {
  report.success = false;
  report.error = String(error?.message ?? error);
  report.mesh_result = { success: false, attempts: error?.attempts || [] };
  console.error(report.error);
}

fs.writeFileSync(REPORT_PATH, `${JSON.stringify(report, null, 2)}\n`);
console.log(`BASIC_AI_PROOF=${report.success ? 'PASS' : 'FAIL'}`);
if (!report.success) process.exit(1);
