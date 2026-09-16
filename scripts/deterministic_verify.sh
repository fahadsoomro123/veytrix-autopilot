#!/usr/bin/env bash
set -euo pipefail

repo_root="${1:-.}"
cd "$repo_root"

echo '=== VEYTRIX DETERMINISTIC VERIFICATION ==='

if [[ -n "${AUTOPILOT_VERIFY_COMMAND:-}" ]]; then
  echo "verification-command=explicit"
  timeout "${AUTOPILOT_COMMAND_TIMEOUT_SECONDS:-900}" bash -lc "$AUTOPILOT_VERIFY_COMMAND"
elif [[ -x ./gradlew ]]; then
  echo 'verification-command=gradle'
  timeout "${AUTOPILOT_COMMAND_TIMEOUT_SECONDS:-900}" ./gradlew test
elif [[ -f package.json ]]; then
  echo 'verification-command=node'
  node -e 'const p=require("./package.json"); if (!p.scripts || !p.scripts.test) process.exit(10);'
  timeout "${AUTOPILOT_COMMAND_TIMEOUT_SECONDS:-900}" npm test -- --if-present
elif [[ -f pyproject.toml || -f requirements.txt ]]; then
  echo 'verification-command=python-compile'
  timeout "${AUTOPILOT_COMMAND_TIMEOUT_SECONDS:-900}" python -m compileall -q .
elif [[ -f pom.xml ]]; then
  echo 'verification-command=maven'
  timeout "${AUTOPILOT_COMMAND_TIMEOUT_SECONDS:-900}" mvn -B test
else
  echo 'verification-command=none'
  echo 'No deterministic build/test command could be proven from repository structure.'
  exit 20
fi

echo '=== DETERMINISTIC VERIFICATION PASSED ==='
