import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { FreeAiMesh } from './FreeAiMesh.mjs';

const TARGET = path.resolve(process.env.VEYTRIX_TARGET_DIR || process.cwd());
const CORE = path.resolve(process.env.VEYTRIX_CORE_DIR || path.join(TARGET, '.veytrix-core'));
const PUTER_TOKEN = process.env.PUTER_AUTH_TOKEN || '';
const MAX_TOOL_ROUNDS = 24;
const MAX_FILE_BYTES = 120 * 1024;
const MAX_READ_BYTES = 60 * 1024;

function rel(p) {
  return path.relative(TARGET, p).split(path.sep).join('/');
}

function safeRelative(input) {
  if (typeof input !== 'string' || !input.trim()) {
    throw new Error('path is required');
  }
  const normalized = path.posix.normalize(input.replaceAll('\\', '/'));
  if (
    normalized.startsWith('../') ||
    normalized === '..' ||
    normalized.startsWith('/') ||
    normalized.includes('/../') ||
    normalized.includes('\\0')
  ) {
    throw new Error('path must stay inside the target repository');
  }
  return normalized;
}

function forbiddenPath(p) {
  const lower = p.toLowerCase();
  if (
    lower.startsWith('.git/') ||
    lower.startsWith('.github/') ||
    lower.startsWith('free-ai-mesh/') ||
    lower === '.git' ||
    lower.includes('.env') ||
    lower.includes('secret') ||
    lower.includes('credential') ||
    lower.includes('keystore') ||
    lower.includes('local.properties') ||
    lower.includes('gradle.properties') ||
    lower.endsWith('.jks') ||
    lower.endsWith('.keystore') ||
    lower.endsWith('.p12') ||
    lower.endsWith('.pem') ||
    lower.includes('veytrixsecurestore') ||
    lower.includes('veytrixautopilotclient')
  ) return true;
  return false;
}

function readablePath(p) {
  if (forbiddenPath(p)) throw new Error('protected or sensitive path is not available');
  if (!(p === 'README.md' || p.startsWith('android/') || p.startsWith('scripts/') || p.startsWith('docs/'))) {
    throw new Error('agent access is limited to repository source, scripts, docs, and README');
  }
}

function assertContained(full, allowMissingLeaf = false) {
  const targetRoot = fs.realpathSync(TARGET);
  const candidate = allowMissingLeaf
    ? path.resolve(full)
    : fs.realpathSync(full);
  const candidateForCheck = allowMissingLeaf
    ? fs.realpathSync(path.dirname(candidate))
    : candidate;
  if (candidateForCheck !== targetRoot && !candidateForCheck.startsWith(targetRoot + path.sep)) {
    throw new Error('path resolves outside the target repository');
  }
  return candidate;
}

function assertTextContent(content) {
  if (typeof content !== 'string') throw new Error('content must be text');
  if (Buffer.byteLength(content, 'utf8') > MAX_FILE_BYTES) {
    throw new Error('content exceeds the bounded file-size limit');
  }
  const secretPatterns = [
    /-----BEGIN [A-Z ]*PRIVATE KEY-----/,
    /(?:ghp_|github_pat_|gho_)[A-Za-z0-9_]+/i,
    /(?:sk-[A-Za-z0-9]{20,}|AIza[A-Za-z0-9_-]{20,})/,
    /(?:OPENAI_API_KEY|PUTER_AUTH_TOKEN|HF_TOKEN)\s*[:=]/,
    /Bearer\s+[A-Za-z0-9._-]{24,}/i
  ];
  for (const pattern of secretPatterns) {
    if (pattern.test(content)) throw new Error('write rejected: possible secret material detected');
  }
}

function sha256(content) {
  return crypto.createHash('sha256').update(content, 'utf8').digest('hex');
}

function walk(dir, depth = 0, out = []) {
  if (depth > 6 || out.length >= 500) return out;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name))) {
    if (entry.name === '.git' || entry.name === '.github' || entry.name === 'node_modules' || entry.name === 'build' || entry.name === '.gradle') continue;
    if (entry.isSymbolicLink()) continue;
    const full = path.join(dir, entry.name);
    const rp = rel(full);
    if (entry.isDirectory()) walk(full, depth + 1, out);
    else if (!forbiddenPath(rp)) out.push(rp);
    if (out.length >= 500) break;
  }
  return out;
}

function readFileTool(args) {
  const p = safeRelative(args.path);
  readablePath(p);
  const full = path.join(TARGET, p);
  if (!fs.existsSync(full) || !fs.statSync(full).isFile()) throw new Error('file not found');
  assertContained(full);
  const content = fs.readFileSync(full, 'utf8');
  if (Buffer.byteLength(content, 'utf8') > MAX_READ_BYTES) {
    throw new Error('file is larger than the bounded read limit; narrow the request or use search_text');
  }
  const start = Math.max(1, Number(args.start_line || 1));
  const end = Math.max(start, Number(args.end_line || 240));
  const lines = content.split('\n');
  const selected = lines.slice(start - 1, Math.min(end, start + 239));
  return { path: p, sha256: sha256(content), start_line: start, end_line: Math.min(end, lines.length), content: selected.map((line, i) => `${start + i}: ${line}`).join('\n') };
}

function searchTool(args) {
  const query = String(args.query || '');
  if (!query || query.length > 200) throw new Error('query must be 1-200 characters');
  const root = args.path ? safeRelative(args.path) : '';
  if (root) readablePath(root);
  const base = root ? path.join(TARGET, root) : TARGET;
  if (!fs.existsSync(base)) throw new Error('search path not found');
  assertContained(base);
  const candidates = fs.statSync(base).isFile() ? [base] : walk(base);
  const results = [];
  for (const candidate of candidates) {
    if (results.length >= 80) break;
    const full = path.join(TARGET, candidate);
    if (!fs.existsSync(full) || !fs.statSync(full).isFile()) continue;
    let content;
    try {
      if (fs.statSync(full).size > MAX_READ_BYTES) continue;
      content = fs.readFileSync(full, 'utf8');
    } catch {
      continue;
    }
    const lines = content.split('\n');
    for (let i = 0; i < lines.length; i += 1) {
      if (lines[i].toLowerCase().includes(query.toLowerCase())) {
        results.push(`${candidate}:${i + 1}: ${lines[i].slice(0, 300)}`);
        if (results.length >= 80) break;
      }
    }
  }
  return { query, results };
}

function writeFileTool(args) {
  const p = safeRelative(args.path);
  readablePath(p);
  assertTextContent(args.content);
  const full = path.join(TARGET, p);
  const exists = fs.existsSync(full);
  if (exists) assertContained(full);
  else assertContained(full, true);
  const current = exists ? fs.readFileSync(full, 'utf8') : '';
  const expected = String(args.expected_sha256 || '');
  if (exists && sha256(current) !== expected) {
    throw new Error('write rejected: expected_sha256 does not match the current file');
  }
  if (!exists && expected !== '') {
    throw new Error('write rejected: new file requires an empty expected_sha256');
  }
  if (exists && current === args.content) {
    return { path: p, changed: false, sha256: sha256(current) };
  }
  fs.mkdirSync(path.dirname(full), { recursive: true });
  fs.writeFileSync(full, args.content, 'utf8');
  return { path: p, changed: true, sha256: sha256(args.content) };
}

function runVerification(check) {
  const run = (cmd, args, cwd = TARGET) => execFileSync(cmd, args, { cwd, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'], maxBuffer: 2 * 1024 * 1024 }).trim();
  switch (check) {
    case 'diff_check':
      return { check, output: run('git', ['diff', '--check']) };
    case 'generic_verify': {
      const verifier = path.join(CORE, 'scripts', 'deterministic_verify.sh');
      if (!fs.existsSync(verifier)) return { check, output: 'generic verifier unavailable' };
      return { check, output: run('bash', [verifier, TARGET]) };
    }
    case 'android_check':
      return { check, output: run('gradle', ['--no-daemon', '--project-dir', 'android', ':autopilot:check']) };
    case 'android_debug_apk':
      return { check, output: run('gradle', ['--no-daemon', '--project-dir', 'android', ':autopilot:assembleDebug']) };
    case 'veytrix_android_contract': {
      const verifier = path.join(CORE, 'scripts', 'verify_veytrix_android_contract.sh');
      if (!fs.existsSync(TARGET + '/android/autopilot') || !fs.existsSync(verifier)) return { check, output: 'Veytrix Android contract not applicable' };
      return { check, output: run('bash', [verifier, TARGET]) };
    }
    case 'relevant': {
      const output = [];
      output.push(run('git', ['diff', '--check']));
      const isAndroid = fs.existsSync(path.join(TARGET, 'android', 'settings.gradle.kts'));
      if (isAndroid) output.push(run('gradle', ['--no-daemon', '--project-dir', 'android', ':autopilot:check']));
      if (fs.existsSync(path.join(TARGET, 'android', 'autopilot'))) {
        const verifier = path.join(CORE, 'scripts', 'verify_veytrix_android_contract.sh');
        if (fs.existsSync(verifier)) output.push(run('bash', [verifier, TARGET]));
        output.push(run('gradle', ['--no-daemon', '--project-dir', 'android', ':autopilot:assembleDebug']));
      }
      const policy = path.join(CORE, 'scripts', 'verify_veytrix_mesh_patch_policy.sh');
      if (fs.existsSync(policy)) output.push(run('bash', [policy, TARGET]));
      return { check, output: output.join('\n') };
    }
    default:
      throw new Error('unsupported verification check');
  }
}

const toolSchemas = [
  {
    type: 'function',
    function: {
      name: 'list_files',
      description: 'List bounded repository source files. Protected/sensitive areas are omitted.',
      parameters: { type: 'object', properties: {}, additionalProperties: false }
    }
  },
  {
    type: 'function',
    function: {
      name: 'read_file',
      description: 'Read a bounded text file by safe repository-relative path.',
      parameters: {
        type: 'object',
        properties: {
          path: { type: 'string' },
          start_line: { type: 'integer', minimum: 1 },
          end_line: { type: 'integer', minimum: 1 }
        },
        required: ['path'],
        additionalProperties: false
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'search_text',
      description: 'Search source text without exposing protected files.',
      parameters: {
        type: 'object',
        properties: {
          query: { type: 'string' },
          path: { type: 'string' }
        },
        required: ['query'],
        additionalProperties: false
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'write_file',
      description: 'Create or replace one bounded UTF-8 source file. Never use for secrets or protected paths. Existing files require the exact current SHA-256 returned by read_file.',
      parameters: {
        type: 'object',
        properties: {
          path: { type: 'string' },
          content: { type: 'string' },
          expected_sha256: { type: 'string' }
        },
        required: ['path', 'content', 'expected_sha256'],
        additionalProperties: false
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'run_verification',
      description: 'Run only a fixed allowlisted verification command.',
      parameters: {
        type: 'object',
        properties: {
          check: {
            type: 'string',
            enum: ['diff_check', 'generic_verify', 'android_check', 'android_debug_apk', 'veytrix_android_contract', 'relevant']
          }
        },
        required: ['check'],
        additionalProperties: false
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'git_diff',
      description: 'Return the bounded current diff for review before finishing.',
      parameters: { type: 'object', properties: {}, additionalProperties: false }
    }
  }
];

function gitDiff() {
  return execFileSync('git', ['diff', '--no-ext-diff', '--unified=3', '--', ':(exclude).github', ':(exclude)free-ai-mesh'], {
    cwd: TARGET,
    encoding: 'utf8',
    maxBuffer: 4 * 1024 * 1024
  }).slice(-120000);
}

async function puterModels() {
  const response = await fetch('https://api.puter.com/puterai/chat/models/details', {
    headers: PUTER_TOKEN ? { Authorization: `Bearer ${PUTER_TOKEN}` } : {},
    signal: AbortSignal.timeout(20000)
  });
  if (!response.ok) throw new Error(`Puter model discovery HTTP ${response.status}`);
  const payload = await response.json();
  const list = Array.isArray(payload) ? payload : (payload?.models || payload?.data || []);
  return list
    .filter(model => model && typeof model.id === 'string' && model.id.trim())
    .filter(model => {
      const cost = model.cost || {};
      const zero = (Number(cost.input) === 0 && Number(cost.output) === 0);
      return model.free === true || /:free$/i.test(model.id) || zero;
    })
    .map(model => ({ id: model.id, provider: model.provider || 'puter', name: model.name || model.id }));
}

function redact(text) {
  let value = String(text ?? '');
  if (PUTER_TOKEN) value = value.split(PUTER_TOKEN).join('[REDACTED]');
  return value.replace(/(ghp_|github_pat_|sk-|AIza)[A-Za-z0-9_-]{8,}/g, '$1[REDACTED]');
}

async function main() {
  const mission = process.env.AUTOPILOT_MISSION || process.argv.slice(2).join(' ').trim();
  if (!mission || mission.length > 4000) throw new Error('AUTOPILOT_MISSION must be 1-4000 characters');
  if (!PUTER_TOKEN) throw new Error('PUTER_AUTH_TOKEN is not configured');

  const adapter = {
    async listModels() {
      return await puterModels();
    },
    async chat(modelId, { task }) {
      const messages = [
        {
          role: 'system',
          content: [
            'You are the VEYTRIX Mesh Fallback Code Agent.',
            'You are a secondary executor used only when the primary AI is unavailable or has failed.',
            'Inspect before editing. Make the smallest production-quality change that fulfills the mission.',
            'Use only the supplied tools. Never invent files, APIs, test results, or repository state.',
            'Never touch .github workflows, free-ai-mesh, secrets, credentials, signing material, secure credential storage, or protected authentication clients.',
            'Never weaken or remove tests, verification, security controls, transport restrictions, or package identity.',
            'Do not commit or push.',
            'Before every existing-file write, read that file and pass its exact SHA-256.',
            'After meaningful edits, run relevant verification. Do not claim completion unless verification passes.',
            'The final answer must summarize what was actually changed and verified.'
          ].join('\n')
        },
        { role: 'user', content: `MISSION:\n${task}\n\nTARGET ROOT:\nRepository working tree is the current directory.\n\nBegin by inspecting the repository.` }
      ];

      const executeTool = async (name, args) => {
        if (name === 'list_files') return { files: walk(TARGET) };
        if (name === 'read_file') return readFileTool(args);
        if (name === 'search_text') return searchTool(args);
        if (name === 'write_file') return writeFileTool(args);
        if (name === 'run_verification') return runVerification(args.check);
        if (name === 'git_diff') return { diff: gitDiff() };
        throw new Error(`unsupported tool: ${name}`);
      };

      for (let round = 0; round < MAX_TOOL_ROUNDS; round += 1) {
        const response = await fetch('https://api.puter.com/puterai/openai/v1/chat/completions', {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${PUTER_TOKEN}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            model: modelId,
            messages,
            tools: toolSchemas,
            tool_choice: 'auto',
            temperature: 0.1,
            max_tokens: 6000,
            normalize: true
          }),
          signal: AbortSignal.timeout(120000)
        });

        if (!response.ok) {
          const body = redact(await response.text()).slice(0, 800);
          const error = new Error(`Puter chat HTTP ${response.status}: ${body}`);
          error.retryable = [408, 429, 500, 502, 503, 504].includes(response.status);
          throw error;
        }

        const payload = await response.json();
        const message = payload?.choices?.[0]?.message;
        if (!message) throw new Error('Puter chat returned no assistant message');

        if (Array.isArray(message.tool_calls) && message.tool_calls.length > 0) {
          messages.push({
            role: 'assistant',
            content: message.content ?? null,
            tool_calls: message.tool_calls
          });

          for (const toolCall of message.tool_calls) {
            const name = toolCall?.function?.name;
            let args;
            try {
              args = JSON.parse(toolCall?.function?.arguments || '{}');
            } catch {
              args = {};
            }

            let result;
            try {
              result = await executeTool(name, args);
            } catch (error) {
              result = { error: redact(error?.message || error) };
            }
            messages.push({
              role: 'tool',
              tool_call_id: toolCall.id,
              content: JSON.stringify(result).slice(0, 30000)
            });
          }
          continue;
        }

        const text = redact(
          message.content ||
          payload?.choices?.[0]?.text ||
          payload?.output_text ||
          ''
        ).trim();

        if (!text) throw new Error('empty assistant result');

        // Hard final gate: an agent attempt is considered a successful lane
        // only when the worktree passes deterministic verification.
        const verification = runVerification('relevant');
        return {
          text,
          verification,
          model: modelId
        };
      }

      const error = new Error(`agent tool-round ceiling reached for ${modelId}`);
      error.retryable = true;
      throw error;
    }
  };

  const mesh = new FreeAiMesh({
    adapter,
    maxAttempts: Number(process.env.MESH_MAX_ATTEMPTS || 6),
    cooldownMs: Number(process.env.MESH_COOLDOWN_MS || 15000),
    maxDiscovered: 3_000_000
  });

  const result = await mesh.ask(mission);
  console.log(JSON.stringify({
    mesh: '3M Free AI Mesh',
    discovered_lanes: mesh.models.length,
    selected_model: result.model,
    attempts: result.attempts,
    verification: result.verification || null,
    final: result.text
  }, null, 2));
}

main().catch(error => {
  console.error(redact(error?.stack || error?.message || error));
  process.exit(1);
});
