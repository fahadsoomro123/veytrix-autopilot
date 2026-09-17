// Intentionally broken quick benchmark fixture. The AI must repair this file only.
export function mergeHeaders(base = {}, override = {}) {
  // BUG: JavaScript object spread is case-sensitive and does not implement
  // HTTP-style header merging/removal semantics.
  return { ...base, ...override };
}
