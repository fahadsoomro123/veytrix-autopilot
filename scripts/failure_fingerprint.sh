#!/usr/bin/env bash
set -euo pipefail

log="${1:?sanitized log required}"
workflow="${2:-unknown-workflow}"
sha="${3:-unknown-sha}"

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
classification="$(bash "$script_dir/classify_failure.sh" "$log" | awk -F= '$1=="classification"{print $2}')"
reason="$(bash "$script_dir/classify_failure.sh" "$log" | awk -F= '$1=="reason"{print $2}')"

# Normalize volatile run IDs, timestamps and hashes so repeated instances of a
# failure converge on one fingerprint. Credential/configuration blockers use the
# deterministic reason code as their semantic identity, preventing harmless log
# wording changes from creating a fresh repair chain.
normalized="$(sed -E \
  -e 's/[0-9]{4}-[0-9]{2}-[0-9]{2}T[^ ]+/<TIME>/g' \
  -e 's/[0-9]{10,}/<NUM>/g' \
  -e 's/[0-9a-f]{40}/<SHA>/g' \
  "$log" | tail -n 250)"

if [[ "$classification" == "credential-or-permission" ]]; then
  signature_payload="${workflow}|${classification}|${reason}"
else
  signature_payload="${workflow}|${classification}|${reason}|${normalized}"
fi

signature="$(printf '%s\n' "$signature_payload" | sha256sum | awk '{print $1}')"
printf 'classification=%s\nfingerprint=%s\nworkflow=%s\nsha=%s\nreason=%s\n' "$classification" "$signature" "$workflow" "$sha" "$reason"
