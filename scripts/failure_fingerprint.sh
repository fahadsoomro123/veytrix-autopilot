#!/usr/bin/env bash
set -euo pipefail

log="${1:?sanitized log required}"
workflow="${2:-unknown-workflow}"
sha="${3:-unknown-sha}"

classification=unknown
if grep -Eqi '(rate limit|timed out|timeout|HTTP 503|HTTP 502|HTTP 504|connection reset|connection refused|temporary failure|network is unreachable|failed to download|could not resolve host|runner.*unavailable|service unavailable|no space left on device)' "$log"; then
  classification=transient
elif grep -Eqi '(compilation failed|compile.*error|test failed|assertion.*failed|syntax error|type error|lint.*error|module not found|cannot find symbol|execution failed|gradle.*failed|build failed)' "$log"; then
  classification=code-or-build
fi

# Normalize volatile run IDs, timestamps and obvious addresses so repeated instances
# of the same root failure converge on one fingerprint.
normalized="$(sed -E \
  -e 's/[0-9]{10,}/<NUM>/g' \
  -e 's/[0-9]{4}-[0-9]{2}-[0-9]{2}T[^ ]+/<TIME>/g' \
  -e 's/[0-9a-f]{40}/<SHA>/g' \
  "$log" | tail -n 250)"

signature="$(printf '%s\n%s\n%s\n' "$workflow" "$classification" "$normalized" | sha256sum | awk '{print $1}')"
printf 'classification=%s\nfingerprint=%s\nworkflow=%s\nsha=%s\n' "$classification" "$signature" "$workflow" "$sha"
