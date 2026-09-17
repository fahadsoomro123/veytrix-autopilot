const DEFAULT_MAX_DISCOVERED = 100_000;

export class FreeAiMesh {
  constructor({
    adapter,
    maxAttempts = 10,
    cooldownMs = 60_000,
    maxDiscovered = DEFAULT_MAX_DISCOVERED,
  } = {}) {
    if (!adapter || typeof adapter.chat !== 'function') {
      throw new TypeError('FreeAiMesh requires an adapter.chat function');
    }

    this.adapter = adapter;
    this.maxAttempts = Math.max(1, Math.min(maxAttempts, maxDiscovered));
    this.cooldownMs = Math.max(1, cooldownMs);
    this.maxDiscovered = Math.max(1, Math.min(maxDiscovered, DEFAULT_MAX_DISCOVERED));
    this.health = new Map();
    this.models = [];
  }

  async discover() {
    const models = typeof this.adapter.listModels === 'function'
      ? await this.adapter.listModels()
      : [];

    const seen = new Set();
    this.models = models
      .filter(model => model && typeof model.id === 'string' && model.id.trim())
      .filter(model => {
        if (seen.has(model.id)) return false;
        seen.add(model.id);
        return true;
      })
      .slice(0, this.maxDiscovered);

    if (this.models.length === 0) throw new Error('No Free AI lanes discovered');
    return this.models;
  }

  score(model, index) {
    const h = this.health.get(model.id) || { successes: 0, failures: 0, cooldownUntil: 0 };
    if (h.cooldownUntil > Date.now()) return Number.NEGATIVE_INFINITY;
    return (h.successes * 5) - (h.failures * 10) - index * 0.000001;
  }

  route() {
    return [...this.models]
      .map((model, index) => ({ model, score: this.score(model, index) }))
      .filter(x => Number.isFinite(x.score))
      .sort((a, b) => b.score - a.score)
      .map(x => x.model);
  }

  recordSuccess(id) {
    const h = this.health.get(id) || { successes: 0, failures: 0, cooldownUntil: 0 };
    h.successes += 1;
    h.cooldownUntil = 0;
    this.health.set(id, h);
  }

  recordFailure(id, retryable = true) {
    const h = this.health.get(id) || { successes: 0, failures: 0, cooldownUntil: 0 };
    h.failures += 1;
    h.cooldownUntil = Date.now() + (retryable ? this.cooldownMs : this.cooldownMs * 4);
    this.health.set(id, h);
  }

  async ask(task) {
    if (!this.models.length) await this.discover();

    const candidates = this.route();
    const attempts = [];

    for (const model of candidates.slice(0, this.maxAttempts)) {
      try {
        const started = Date.now();
        const result = await this.adapter.chat(model.id, { task });
        const text = String(
          result?.text ?? result?.message?.content ?? result?.content ?? '',
        ).trim();

        if (!text) throw new Error('empty response');

        this.recordSuccess(model.id);
        attempts.push({
          model: model.id,
          status: 'success',
          latencyMs: Date.now() - started,
        });
        return { text, model: model.id, attempts };
      } catch (error) {
        this.recordFailure(model.id, error?.retryable !== false);
        attempts.push({
          model: model.id,
          status: 'failed',
          error: String(error?.message ?? error),
        });
      }
    }

    const error = new Error(`All ${candidates.length} eligible Free AI lanes exhausted`);
    error.attempts = attempts;
    throw error;
  }
}
