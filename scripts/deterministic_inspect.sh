#!/usr/bin/env bash
set -euo pipefail

root="${1:-.}"
cd "$root"

target_repo="${AUTOPILOT_TARGET_REPOSITORY:-${GITHUB_REPOSITORY:-unknown}}"
printf '%s\n' '=== VEYTRIX DETERMINISTIC PREFLIGHT ==='
printf 'target_repo=%s\n' "$target_repo"
printf 'repo_root=%s\n' "$(pwd)"
printf 'sha=%s\n' "${GITHUB_SHA:-unknown}"
printf 'ref=%s\n' "${GITHUB_REF_NAME:-unknown}"
printf 'actor=%s\n' "${GITHUB_ACTOR:-unknown}"

echo '--- working tree ---'
git status --short --branch

echo '--- project signals ---'
for f in package.json pnpm-lock.yaml yarn.lock package-lock.json gradlew gradle/wrapper/gradle-wrapper.properties build.gradle build.gradle.kts settings.gradle settings.gradle.kts pom.xml pyproject.toml requirements.txt Makefile; do
  if [[ -e "$f" ]]; then echo "present=$f"; fi
done

echo '--- workflow inventory ---'
if [[ -d .github/workflows ]]; then
  find .github/workflows -maxdepth 1 -type f -printf '%f\n' | sort
else
  echo 'no-workflows-directory'
fi

echo '--- deterministic capabilities ---'
if [[ -f package.json ]]; then echo 'node-project=yes'; fi
if [[ -f gradlew || -f gradle/wrapper/gradle-wrapper.properties || -f build.gradle || -f build.gradle.kts ]]; then echo 'gradle-project=yes'; fi
if [[ -f pom.xml ]]; then echo 'maven-project=yes'; fi
if [[ -f pyproject.toml || -f requirements.txt ]]; then echo 'python-project=yes'; fi
if [[ -f Makefile ]]; then echo 'make-project=yes'; fi
printf '%s\n' '=== PREFLIGHT COMPLETE ==='
