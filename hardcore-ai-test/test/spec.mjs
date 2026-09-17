import assert from 'node:assert/strict';
import { buildExecutionPlan, PlanError } from '../src/scheduler.mjs';

const j = (id, deps = [], extra = {}) => ({ id, revision: 1, priority: 0, deps, ...extra });

function expectPlan(jobs, expected, options) {
  assert.deepEqual(buildExecutionPlan(jobs, options), expected);
}

// 1. Basic dependency ordering.
expectPlan([
  j('compile'),
  j('test', ['compile']),
  j('package', ['test']),
], [['compile'], ['test'], ['package']]);

// 2. Higher priority wins among simultaneously-ready jobs.
expectPlan([
  j('a', [], { priority: 1 }),
  j('b', [], { priority: 9 }),
  j('c', [], { priority: 3 }),
], [['b', 'c'], ['a']], { maxParallel: 2 });

// 3. Deadline breaks priority ties; id breaks remaining ties.
expectPlan([
  j('z', [], { priority: 5, deadline: 30 }),
  j('a', [], { priority: 5, deadline: 10 }),
  j('m', [], { priority: 5, deadline: 10 }),
], [['a', 'm'], ['z']], { maxParallel: 2 });

// 4. Latest revision must survive, and duplicate tie uses last occurrence.
expectPlan([
  j('build', [], { revision: 1, priority: 1 }),
  j('build', [], { revision: 3, priority: 7 }),
  j('build', [], { revision: 2, priority: 99 }),
], [['build']]);

// 5. Cancelled jobs are removed, but their dependents must fail because a required dep is absent.
assert.throws(
  () => buildExecutionPlan([
    j('cancelled', [], { cancelled: true }),
    j('deploy', ['cancelled']),
  ]),
  /missing dependency.*cancelled/i,
);

// 6. Explicitly missing dependency must fail, not silently truncate the plan.
assert.throws(
  () => buildExecutionPlan([j('deploy', ['does-not-exist'])]),
  /missing dependency.*does-not-exist/i,
);

// 7. Cycle detection.
assert.throws(
  () => buildExecutionPlan([
    j('a', ['b']),
    j('b', ['c']),
    j('c', ['a']),
  ]),
  /cycle/i,
);

// 8. availableAt gates scheduling and logical time advances to the earliest future job.
expectPlan([
  j('now', [], { availableAt: 0 }),
  j('later', [], { availableAt: 5 }),
  j('after-later', ['later']),
], [['now'], ['later'], ['after-later']], { maxParallel: 1, now: 0 });

// 9. Dependencies in the same ready set are never co-batched with their parent.
expectPlan([
  j('a'),
  j('b', ['a']),
  j('c', ['a']),
], [['a'], ['b', 'c']], { maxParallel: 2 });

// 10. Input objects must not be mutated.
const original = [j('a', [], { deps: [] })];
const snapshot = structuredClone(original);
buildExecutionPlan(original);
assert.deepEqual(original, snapshot);

// 11. Invalid maxParallel.
assert.throws(() => buildExecutionPlan([], { maxParallel: 0 }), PlanError);
assert.throws(() => buildExecutionPlan([], { maxParallel: 1.5 }), PlanError);

// 12. Large deterministic DAG: every dependency appears in an earlier batch.
const graph = Array.from({ length: 60 }, (_, i) =>
  j(`job-${String(i).padStart(2, '0')}`, i === 0 ? [] : [`job-${String(i - 1).padStart(2, '0')}`], { priority: i % 4 }),
);
const result = buildExecutionPlan(graph, { maxParallel: 5 });
assert.equal(result.length, 60);
assert.deepEqual(result.flat(), graph.map(x => x.id));

console.log('HARDCORE_SPEC=PASS');
