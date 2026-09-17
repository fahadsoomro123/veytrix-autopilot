import assert from 'node:assert/strict';
import { mergeHeaders } from '../src/scheduler.mjs';

// 1. Override is case-insensitive and keeps the original base-key casing.
assert.deepEqual(
  mergeHeaders(
    { Authorization: 'Bearer old', 'Content-Type': 'application/json' },
    { authorization: 'Bearer new' },
  ),
  { Authorization: 'Bearer new', 'Content-Type': 'application/json' },
);

// 2. Null removes an existing header, case-insensitively.
assert.deepEqual(
  mergeHeaders({ Authorization: 'Bearer x', 'X-App': 'veytrix' }, { 'x-app': null }),
  { Authorization: 'Bearer x' },
);

// 3. Undefined also removes an existing header.
assert.deepEqual(
  mergeHeaders({ 'X-Test': 'yes' }, { 'x-test': undefined }),
  {},
);

// 4. New override headers are appended using their supplied key spelling.
assert.deepEqual(
  mergeHeaders({ A: '1', B: '2' }, { 'X-New': '3' }),
  { A: '1', B: '2', 'X-New': '3' },
);

// 5. An empty string is a legitimate value and must be retained.
assert.deepEqual(
  mergeHeaders({ A: '1' }, { a: '' }),
  { A: '' },
);

// 6. Inputs must not be mutated.
const base = { Authorization: 'old', A: '1' };
const override = { authorization: 'new', B: '2' };
const baseSnapshot = structuredClone(base);
const overrideSnapshot = structuredClone(override);
mergeHeaders(base, override);
assert.deepEqual(base, baseSnapshot);
assert.deepEqual(override, overrideSnapshot);

// 7. Matching uses ASCII case-insensitive header names.
assert.deepEqual(
  mergeHeaders({ 'X-CUSTOM-HEADER': 'old' }, { 'x-custom-header': 'new' }),
  { 'X-CUSTOM-HEADER': 'new' },
);

// 8. New values are preserved exactly; do not stringify them.
const value = { enabled: true };
const result = mergeHeaders({}, { 'X-Meta': value });
assert.equal(result['X-Meta'], value);

console.log('QUICK_SPEC=PASS');
