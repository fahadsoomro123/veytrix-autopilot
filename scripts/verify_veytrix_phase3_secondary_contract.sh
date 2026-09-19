#!/usr/bin/env bash
set -euo pipefail

ROOT=${1:-.}

files=(
  "android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixProfileView.java"
  "android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixCompletedView.java"
  "android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixRunDetailsView.java"
  "android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixSupportView.java"
  "android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixVoiceView.java"
)

for file in "${files[@]}"; do
  test -f "$ROOT/$file"
done

if grep -REn 'ScrollView|HorizontalScrollView' "${files[@]}"; then
  echo "Phase 3 secondary surfaces must not depend on page scrolling." >&2
  exit 1
fi

if grep -REn '128.*Missions|24.*Projects|98%.*Success|2\.4x.*Faster|12.*Files|3.*Tests|2\.4m.*Total Time|100%.*Verified|AI mission has been successfully completed|Initializing project structure|Creating API endpoints' "${files[@]}"; then
  echo "Synthetic legacy metrics/output remain in a migrated secondary surface." >&2
  exit 1
fi

if grep -REn 'Color\.rgb\((55, 132, 255|8, 23, 42|12, 32, 55)|Color\.BLACK|Color\.BLUE|#3784FF' "${files[@]}"; then
  echo "Legacy blue/black palette leaked into Phase 3 surfaces." >&2
  exit 1
fi

grep -Fq 'android.permission.RECORD_AUDIO' "$ROOT/android/autopilot/src/main/AndroidManifest.xml"
grep -Fq 'SpeechRecognizer' "$ROOT/android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixVoiceView.java"
grep -Fq 'requestPermissions' "$ROOT/android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixVoiceView.java"
grep -Fq 'openRunDetails' "$ROOT/android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixActivityView.java"
grep -Fq 'showProfile' "$ROOT/android/autopilot/src/main/java/com/veytrix/autopilot/MainActivity.java"
grep -Fq 'showVoice' "$ROOT/android/autopilot/src/main/java/com/veytrix/autopilot/MainActivity.java"
grep -Fq 'showCompleted' "$ROOT/android/autopilot/src/main/java/com/veytrix/autopilot/MainActivity.java"
grep -Fq 'showSupport' "$ROOT/android/autopilot/src/main/java/com/veytrix/autopilot/MainActivity.java"
grep -Fq 'showRunDetails' "$ROOT/android/autopilot/src/main/java/com/veytrix/autopilot/MainActivity.java"

echo "VEYTRIX Phase 3 secondary surface contract: PASS"
