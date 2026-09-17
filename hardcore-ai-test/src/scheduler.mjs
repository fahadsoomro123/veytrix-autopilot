// Intentionally broken basic benchmark fixture. The AI must repair this file only.
export function parseKeyValuePairs(text) {
  // BUG: this naive split mishandles duplicate keys and values containing '='.
  const result = {};
  if (typeof text !== 'string') return result;
  for (const part of text.split(',')) {
    const trimmed = part.trim();
    if (!trimmed) continue;
    const [key = '', value = ''] = trimmed.split('=');
    if (key.trim()) result[key.trim()] = value.trim();
  }
  return result;
}
