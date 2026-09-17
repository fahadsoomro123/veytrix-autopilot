#!/usr/bin/env bash
set -euo pipefail

root="${1:-.}"
cd "$root"

if [[ -n "${AUTOPILOT_VERIFY_COMMAND:-}" ]]; then
  echo "Running AUTOPILOT_VERIFY_COMMAND"
  bash -lc "$AUTOPILOT_VERIFY_COMMAND"
  exit 0
fi

if [[ -f gradlew ]]; then
  chmod +x gradlew
  ./gradlew test --no-daemon
  exit 0
fi

if [[ -f package.json ]]; then
  if command -v npm >/dev/null 2>&1; then
    npm test -- --runInBand
    exit 0
  fi
fi

if find . -maxdepth 2 -type f -name '*.py' -print -quit | grep -q .; then
  python3 -m compileall -q .
  exit 0
fi

if [[ -f pom.xml ]] && command -v mvn >/dev/null 2>&1; then
  mvn -q test
  exit 0
fi

echo 'No supported deterministic verification target found.' >&2
exit 20
