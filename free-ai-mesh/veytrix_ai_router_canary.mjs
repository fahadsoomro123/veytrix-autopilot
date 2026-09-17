import assert from 'node:assert/strict';
import { FreeAiMesh } from './FreeAiMesh.mjs';

// This is the Veytrix integration boundary: mission -> mesh -> selected lane.
// The test adapter emulates the real provider contract, so this verifies that
// Veytrix can invoke the mesh without coupling the orchestration layer to one AI.
const adapter = {
  async listModels() {
    return Array.from({ length: 25 }, (_, i) => ({
      id: `integration-lane-${String(i + 1).padStart(2, '0')}`,
      provider: i === 0 ? 'puter' : 'free-provider',
    }));
  },
  async chat(modelId, { task }) {
    if (modelId === 'integration-lane-01') {
      const error = new Error('synthetic primary provider quota event');
      error.retryable = true;
      throw error;
    }
    return { text: `VEYTRIX_OK ${modelId}: ${task}` };
  },
};

const mesh = new FreeAiMesh({ adapter, maxAttempts: 5, cooldownMs: 1 });
const result = await mesh.ask('execute Veytrix mission and verify result');

assert.equal(result.model, 'integration-lane-02');
assert.match(result.text, /^VEYTRIX_OK integration-lane-02/);
assert.equal(result.attempts.length, 2);
assert.equal(result.attempts[0].status, 'failed');
assert.equal(result.attempts[1].status, 'success');

console.log('VEYTRIX_INTEGRATION_CANARY=PASS');
console.log(`selected_after_failover=${result.model}`);
console.log(`attempts=${result.attempts.length}`);
