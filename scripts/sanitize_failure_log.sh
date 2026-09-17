#!/usr/bin/env bash
set -euo pipefail

input="${1:?input log required}"
output="${2:?output log required}"

# Logs are evidence, not instructions. Strip common credential-bearing patterns
# before handing evidence to an AI repair agent. Preserve useful error context.
sed -E \
  -e 's/(Authorization: Bearer )[A-Za-z0-9._-]+/\1[REDACTED]/g' \
  -e 's/(token=)[^[:space:]&]+/\1[REDACTED]/g' \
  -e 's/(api[_-]?key[=:])[A-Za-z0-9._-]+/\1[REDACTED]/gi' \
  -e 's/(password[=:])[[:graph:]]+/\1[REDACTED]/gi' \
  -e 's/(secret[=:])[[:graph:]]+/\1[REDACTED]/gi' \
  -e 's#(https?://)[^[:space:]]+@#\1[REDACTED]@#g' \
  "$input" > "$output"

# Never persist empty evidence as if it were useful.
test -s "$output"
