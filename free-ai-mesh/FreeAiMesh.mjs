const DEFAULT_MAX_DISCOVERED = 3_000_000;

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
    this.maxDiscovered = Math.max(1, Math.min(maxDiscovered, DEFAULT_MAX_DISCOVERED));
    this.maxAttempts = Math.max(1, Math.min(maxAttempts, this.maxDiscovered));
    this.cooldownMs = Math.max(1, cooldownMs);
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

  scoreHealth(modelId, index = 0) {
    const h = this.health.get(modelId) || { successes: 0, failures: 0, cooldownUntil: 0 };
    if (h.cooldownUntil > Date.now()) return Number.NEGATIVE_INFINITY;
    return (h.successes * 5) - (h.failures * 10) - index * 0.000001;
  }

  *candidateSequence() {
    const now = Date.now();
    const preferred = [];

    // Only sort lanes we have actually used successfully. In a 3M-lane mesh,
    // this keeps routing proportional to the active health set instead of
    // sorting millions of discovered entries on every request.
    for (const [id, health] of this.health.entries()) {
      if (health.successes > 0 && health.cooldownUntil <= now) {
        preferred.push({ id, score: (health.successes * 5) - (health.failures * 10) });
      }
    }
    preferred.sort((a, b) => b.score - a.score);

    const preferredIds = new Set();
    for (const item of preferred) {
      preferredIds.add(item.id);
      const model = this.models.find(candidate => candidate.id === item.id);
      if (model) yield model;
    }

    // Then walk the discovered pool without a full-array sort. Failed/cooldown
    // lanes are skipped until their cooldown expires.
    for (let index = 0; index < this.models.length; index += 1) {
      const model = this.models[index];
      if (preferredIds.has(model.id)) continue;
      const health = this.health.get(model.id);
      if (health && health.cooldownUntil > now) continue;
      yield model;
    }
  }

  route() {
    return [...this.candidateSequence()];
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

    const attempts = [];
    let attempted = 0;

    for (const model of this.candidateSequence()) {
      if (attempted >= this.maxAttempts) break;
      attempted += 1;

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

    const error = new Error(`Free AI mesh exhausted after ${attempted} attempts`);
    error.attempts = attempts;
    throw error;
  }
}
