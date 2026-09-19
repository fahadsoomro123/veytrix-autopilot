#!/usr/bin/env bash
set -euo pipefail

root="${1:-.}"
root="$(cd "$root" && pwd)"
main="$root/android/autopilot/src/main/java/com/veytrix/autopilot/MainActivity.java"
activity="$root/android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixActivityView.java"
results="$root/android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixResultsView.java"
control="$root/android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixControlView.java"
client="$root/android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixAutopilotClient.java"

fail() {
  echo "VEYTRIX_PHASE2_PRIMARY_CONTRACT_FAILED: $*" >&2
  exit 1
}

for file in "$main" "$activity" "$results" "$control" "$client"; do
  [[ -s "$file" ]] || fail "required primary screen source is missing: $file"
done

grep -Fq 'new VeytrixHomeView(' "$main" || fail "Home is not migrated"
grep -Fq 'new VeytrixActivityView(' "$main" || fail "Activity is not migrated"
grep -Fq 'new VeytrixResultsView(' "$main" || fail "Results is not migrated"
grep -Fq 'new VeytrixControlView(' "$main" || fail "Control is not migrated"

for file in "$activity" "$results" "$control"; do
  if grep -nE 'ScrollView|HorizontalScrollView' "$file"; then
    fail "primary surface contains a scrolling container: $file"
  fi
done

grep -Fq 'fetchRecentRuns(' "$activity" || fail "Activity is not backed by real run history"
grep -Fq 'fetchArtifacts(' "$results" || fail "Results is not backed by real artifacts"
grep -Fq 'getEngine()' "$control" || fail "Control does not read persisted execution engine"
grep -Fq 'getVerificationDepth()' "$control" || fail "Control does not read persisted verification depth"
grep -Fq 'setEngine(' "$control" || fail "Control cannot persist engine changes"
grep -Fq 'setVerificationDepth(' "$control" || fail "Control cannot persist verification depth"

if grep -nE 'E-commerce API|Modern UI Components|Database Optimization|api-server\.js|"128"|"98%"'     "$activity" "$results" "$control"; then
  fail "known legacy demo content leaked into a Phase 2 primary surface"
fi

echo 'VEYTRIX Phase 2 primary screen contract: PASS'
