#!/usr/bin/env bash
set -euo pipefail

log="${1:?sanitized log required}"
workflow="${2:-unknown-workflow}"
sha="${3:-unknown-sha}"

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
classification="$(bash "$script_dir/classify_failure.sh" "$log" | awk -F= '$1=="classification"{print $2}')"

# Normalize volatile run IDs, timestamps and obvious addresses so repeated instances
# of the same root failure converge on one fingerprint.
normalized="$(sed -E \
  -e 's/[0-9]{10,}/<NUM>/g' \
  -e 's/[0-9]{4}-[0-9]{2}-[0-9]{2}T[^ ]+/<TIME>/g' \
  -e 's/[0-9a-f]{40}/<SHA>/g' \
  "$log" | tail -n 250)"

signature="$(printf '%s\n%s\n%s\n' "$workflow" "$classification" "$normalized" | sha256sum | awk '{print $1}')"
printf 'classification=%s\nfingerprint=%s\nworkflow=%s\nsha=%s\n' "$classification" "$signature" "$workflow" "$sha"
