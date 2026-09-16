#!/usr/bin/env bash
set -euo pipefail

log_file="${1:-/tmp/autopilot-failure.log}"
if [[ ! -f "$log_file" ]]; then
  echo "classification=unknown"
  echo "reason=log-file-missing"
  exit 0
fi

if grep -Eqi '(rate limit|timed out|timeout|503|502|504|connection reset|connection refused|temporary failure|network is unreachable|failed to download|could not resolve host|runner.*unavailable|service unavailable|no space left on device)' "$log_file"; then
  echo "classification=transient"
  echo "reason=known-transient-infrastructure-signature"
elif grep -Eqi '(compilation failed|compile.*error|test failed|assertion.*failed|syntax error|type error|lint.*error|module not found|cannot find symbol|execution failed)' "$log_file"; then
  echo "classification=code-or-build"
  echo "reason=source-or-build-failure-signature"
else
  echo "classification=unknown"
  echo "reason=no-safe-deterministic-signature"
fi
