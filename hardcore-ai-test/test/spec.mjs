import assert from 'node:assert/strict';
import { parseKeyValuePairs } from '../src/scheduler.mjs';

// 1. Basic key=value parsing.
assert.deepEqual(
  parseKeyValuePairs('name=Fahad, mode=quick'),
  { name: 'Fahad', mode: 'quick' },
);

// 2. Whitespace around segments, keys, and values is trimmed.
assert.deepEqual(
  parseKeyValuePairs('  name = Fahad  ,  mode = deep  '),
  { name: 'Fahad', mode: 'deep' },
);

// 3. Duplicate keys use the last value.
assert.deepEqual(
  parseKeyValuePairs('mode=quick,mode=deep'),
  { mode: 'deep' },
);

// 4. Values may contain '='; split only on the first '='.
assert.deepEqual(
  parseKeyValuePairs('token=a=b=c, name=Veytrix'),
  { token: 'a=b=c', name: 'Veytrix' },
);

// 5. Empty segments are ignored and a missing value becomes an empty string.
assert.deepEqual(
  parseKeyValuePairs('a=1,,b=,   ,c=3'),
  { a: '1', b: '', c: '3' },
);

// 6. A segment without '=' still creates a key with an empty value.
assert.deepEqual(
  parseKeyValuePairs('flag,mode=quick'),
  { flag: '', mode: 'quick' },
);

// 7. Blank keys are ignored.
assert.deepEqual(
  parseKeyValuePairs(' =bad, ok=yes'),
  { ok: 'yes' },
);

// 8. Non-string input returns a fresh empty object.
assert.deepEqual(parseKeyValuePairs(null), {});
assert.deepEqual(parseKeyValuePairs(undefined), {});

console.log('BASIC_SPEC=PASS');
