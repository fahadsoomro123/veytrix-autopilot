// Intentionally broken benchmark fixture. The AI must repair this file only.
export class PlanError extends Error {}

export function buildExecutionPlan(inputJobs, options = {}) {
  const maxParallel = options.maxParallel ?? 2;
  const now = options.now ?? 0;

  if (!Number.isInteger(maxParallel) || maxParallel < 1) {
    throw new PlanError('maxParallel must be a positive integer');
  }
  if (!Array.isArray(inputJobs)) {
    throw new PlanError('jobs must be an array');
  }

  // BUGS are intentional: weak validation, wrong revision selection,
  // cancelled-dependency handling, incomplete readiness logic, and no cycle detection.
  const byId = new Map();
  for (const job of inputJobs) {
    if (!job || typeof job.id !== 'string' || !job.id) continue;
    if (job.cancelled) continue;
    if (!byId.has(job.id) || (job.revision ?? 0) < (byId.get(job.id).revision ?? 0)) {
      byId.set(job.id, { ...job, deps: [...(job.deps ?? [])] });
    }
  }

  const remaining = new Set(byId.keys());
  const completed = new Set();
  const plan = [];
  let tick = now;

  while (remaining.size) {
    const ready = [...remaining]
      .map(id => byId.get(id))
      .filter(job => job && (job.availableAt ?? 0) <= tick && job.deps.every(dep => completed.has(dep)))
      .sort((a, b) => (b.priority ?? 0) - (a.priority ?? 0) || a.id.localeCompare(b.id));

    const batch = ready.slice(0, maxParallel).map(job => job.id);
    if (batch.length) {
      plan.push(batch);
      for (const id of batch) {
        remaining.delete(id);
        completed.add(id);
      }
      continue;
    }

    const future = [...remaining]
      .map(id => byId.get(id)?.availableAt ?? 0)
      .filter(value => value > tick)
      .sort((a, b) => a - b)[0];
    if (future !== undefined) {
      tick = future;
      continue;
    }

    // BUG: this treats missing dependencies and cycles as if the queue were empty.
    break;
  }

  return plan;
}
