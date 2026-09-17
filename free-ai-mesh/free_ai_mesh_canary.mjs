import assert from 'node:assert/strict';
import { FreeAiMesh } from './FreeAiMesh.mjs';

const lanes = Array.from({ length: 25 }, (_, i) => ({
  id: `free-lane-${String(i + 1).padStart(2, '0')}`,
  provider: ['puter', 'qwen', 'glm', 'kimi', 'mistral'][i % 5],
}));

let calls = 0;
const adapter = {
  async listModels() {
    return lanes;
  },
  async chat(modelId) {
    calls += 1;
    const laneNumber = Number(modelId.slice(-2));

    // CONTROLLED OUTAGE: first 7 free lanes fail like quota/rate-limit events.
    if (laneNumber <= 7) {
      const err = new Error(`synthetic provider rate-limit on ${modelId}`);
      err.retryable = true;
      throw err;
    }

    return { text: `SUCCESS from ${modelId}` };
  },
};

const mesh = new FreeAiMesh({ adapter, maxAttempts: 10, cooldownMs: 1 });
const discovered = await mesh.discover();
assert.equal(discovered.length, 25, '25 free lanes must be available to the router');

const result = await mesh.ask('repair an Android build failure');

assert.equal(result.model, 'free-lane-08', 'router must fail over to lane 08 after seven outages');
assert.equal(result.text, 'SUCCESS from free-lane-08');
assert.equal(result.attempts.length, 8, 'router must prove the seven failures plus the successful fallback');
assert.equal(calls, 8);
assert.equal(result.attempts.filter(a => a.status === 'failed').length, 7);
assert.equal(result.attempts.at(-1).status, 'success');

// Second request: healthy lane 08 should now be preferred instead of restarting at lane 01.
const second = await mesh.ask('reason about the build failure');
assert.equal(second.model, 'free-lane-08', 'health scoring must prefer a recently successful lane');

console.log('FREE_AI_MESH_CANARY=PASS');
console.log(`discovered_lanes=${discovered.length}`);
console.log(`first_attempt_failures=7`);
console.log(`successful_fallback=${result.model}`);
console.log(`second_request_preferred=${second.model}`);
