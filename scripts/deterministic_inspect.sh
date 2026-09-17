#!/usr/bin/env bash
set -euo pipefail

root="${1:-.}"
cd "$root"

echo '=== Veytrix deterministic preflight ==='
printf 'repo=%s\n' "$(git remote get-url origin 2>/dev/null || true)"
printf 'head=%s\n' "$(git rev-parse HEAD)"

for path in android app preview .github/workflows scripts; do
  if [[ -e "$path" ]]; then
    echo "present=$path"
  fi
done

if [[ -f android/app/build.gradle || -f android/app/build.gradle.kts ]]; then
  echo 'android-project=true'
fi
if [[ -f preview/index.html ]]; then
  echo 'approved-preview-present=true'
fi
if grep -Rqs 'com\.veytrix\.autopilot' android 2>/dev/null; then
  echo 'application-id=com.veytrix.autopilot'
fi

echo 'workflows:'
find .github/workflows -maxdepth 1 -type f -print 2>/dev/null | sort || true
