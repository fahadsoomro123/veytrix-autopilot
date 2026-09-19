#!/usr/bin/env bash
set -euo pipefail

root="${1:-.}"
root="$(cd "$root" && pwd)"
main="$root/android/autopilot/src/main/java/com/veytrix/autopilot/MainActivity.java"
home="$root/android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixHomeView.java"
tokens="$root/android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixDesignTokens.java"
core="$root/android/autopilot/src/main/java/com/veytrix/autopilot/FlagshipCoreView.java"

fail() {
  echo "VEYTRIX_PHASE2_HOME_CONTRACT_FAILED: $*" >&2
  exit 1
}

for file in "$main" "$home" "$tokens" "$core"; do
  [[ -s "$file" ]] || fail "required Phase 2 source is missing: $file"
done

grep -Fq 'new VeytrixHomeView(' "$main" ||
  fail "MainActivity is not wired to the Phase 2 Home surface"

if grep -nE '\b(class|instanceof) HomeView\b' "$main"; then
  fail "legacy HomeView remains in MainActivity"
fi

if grep -nE 'ScrollView|HorizontalScrollView' "$home"; then
  fail "Phase 2 Home must not depend on page scrolling"
fi

grep -Fq 'client.startMission(' "$home" ||
  fail "Home execution button is not wired to the real autopilot client"

grep -Fq 'client.verify' "$main" ||
  fail "secure GitHub verification is not reachable from the Home connection flow"

grep -Fq 'TOUCH_TARGET_DP = 48' "$tokens" ||
  fail "48dp touch-target contract is missing"

grep -Fq 'GRID_DP = 8' "$tokens" ||
  fail "8dp spacing rhythm contract is missing"

for token in PEARL WHITE SILVER CHAMPAGNE VIOLET PURPLE MAGENTA PINK ROSE CRIMSON; do
  grep -Fq "public static final int $token" "$tokens" ||
    fail "required flagship token is missing: $token"
done

if grep -nE 'Color\.rgb|Color\.argb' "$core" | grep -Ev 'Color\.argb\(a, Color\.red'; then
  fail "autonomous core contains a raw color outside the centralized token system"
fi

grep -Fq 'WindowCompat.setDecorFitsSystemWindows(window, false)' "$main" ||
  fail "edge-to-edge window contract is missing"

grep -Fq 'WindowInsetsCompat.Type.systemBars()' "$main" ||
  fail "system bar inset handling is missing"

echo 'VEYTRIX Phase 2 Home contract: PASS'
