import assert from 'node:assert/strict';
import { FreeAiMesh } from './FreeAiMesh.mjs';

const TARGET_LANES = 3_000_000;
const FAILURE_DEPTH = 100_000;

const lanes = Array.from({ length: TARGET_LANES }, (_, i) => ({
  id: `free-lane-${i + 1}`,
  provider: ['puter', 'qwen', 'glm', 'kimi', 'mistral'][i % 5],
}));

let calls = 0;
const adapter = {
  async listModels() {
    return lanes;
  },
  async chat(modelId) {
    calls += 1;
    const laneNumber = Number(modelId.replace('free-lane-', ''));

    if (laneNumber <= FAILURE_DEPTH) {
      const err = new Error(`synthetic provider rate-limit on ${modelId}`);
      err.retryable = true;
      throw err;
    }

    return { text: `SUCCESS from ${modelId}` };
  },
};

const mesh = new FreeAiMesh({
  adapter,
  maxAttempts: FAILURE_DEPTH + 1,
  cooldownMs: 1,
  maxDiscovered: TARGET_LANES,
});

const discovered = await mesh.discover();
assert.equal(discovered.length, TARGET_LANES);

const result = await mesh.ask('repair an Android build failure');

assert.equal(result.model, `free-lane-${FAILURE_DEPTH + 1}`);
assert.equal(result.text, `SUCCESS from ${result.model}`);
assert.equal(result.attempts.length, FAILURE_DEPTH + 1);
assert.equal(calls, FAILURE_DEPTH + 1);
assert.equal(result.attempts.filter(a => a.status === 'failed').length, FAILURE_DEPTH);
assert.equal(result.attempts.at(-1).status, 'success');

const second = await mesh.ask('reason about the build failure');
assert.equal(second.model, `free-lane-${FAILURE_DEPTH + 1}`);
assert.equal(second.attempts.length, 1);

console.log('FREE_AI_MESH_3M_HARDCORE_CANARY=PASS');
console.log(`discovered_lanes=${discovered.length}`);
console.log(`consecutive_failures=${FAILURE_DEPTH}`);
console.log(`successful_fallback=${result.model}`);
console.log(`second_request_preferred=${second.model}`);
