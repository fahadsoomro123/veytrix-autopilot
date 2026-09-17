#!/usr/bin/env bash
set -euo pipefail

log_file="${1:-/tmp/autopilot-failure.log}"
if [[ ! -f "$log_file" ]]; then
  echo "classification=unknown"
  echo "reason=log-file-missing"
  exit 0
fi

if grep -Eqi '(secret|credential|token|keystore|signing key).*(missing|not set|not found|invalid|expired|denied)|permission denied|resource not accessible|authentication failed|unauthorized|forbidden|test -n .*VEYTRIX_KEYSTORE_(BASE64|PASSWORD)|test -n .*VEYTRIX_KEY_ALIAS|test -n .*VEYTRIX_KEY_PASSWORD' "$log_file"; then
  echo "classification=credential-or-permission"
  echo "reason=external-credential-or-permission-blocker"
elif grep -Eqi '(billing|quota exceeded|payment required|insufficient quota|api key.*(invalid|revoked))' "$log_file"; then
  echo "classification=external-service"
  echo "reason=external-service-or-quota-blocker"
elif grep -Eqi '(rate limit|timed out|timeout|HTTP 503|HTTP 502|HTTP 504|\b503\b|\b502\b|\b504\b|connection reset|connection refused|temporary failure|network is unreachable|failed to download|could not resolve host|runner.*unavailable|service unavailable|no space left on device)' "$log_file"; then
  echo "classification=transient"
  echo "reason=known-transient-infrastructure-signature"
elif grep -Eqi '(compilation failed|compile.*error|test failed|assertion.*failed|syntax error|type error|lint.*error|module not found|cannot find symbol|execution failed|gradle.*failed|build failed|check.*failed)' "$log_file"; then
  echo "classification=code-or-build"
  echo "reason=source-or-build-failure-signature"
else
  echo "classification=unknown"
  echo "reason=no-safe-deterministic-signature"
fi
