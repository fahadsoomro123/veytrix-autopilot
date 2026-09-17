#!/usr/bin/env bash
set -euo pipefail

root="${1:-.}"
root="$(cd "$root" && pwd)"
module="$root/android/autopilot"

fail() {
  echo "VEYTRIX_ANDROID_CONTRACT_FAILED: $*" >&2
  exit 1
}

[[ -d "$module" ]] || fail "android/autopilot module is missing"
[[ -f "$module/build.gradle.kts" ]] || fail "android/autopilot/build.gradle.kts is missing"
[[ -f "$module/src/main/AndroidManifest.xml" ]] || fail "AndroidManifest.xml is missing"

build_gradle="$module/build.gradle.kts"
manifest="$module/src/main/AndroidManifest.xml"

# These are immutable identity invariants for the current VEYTRIX Android line.
grep -Fq 'namespace = "com.veytrix.autopilot"' "$build_gradle" || fail "namespace changed"
grep -Fq 'applicationId = "com.veytrix.autopilot"' "$build_gradle" || fail "applicationId changed"
grep -Eq 'versionCode[[:space:]]*=[[:space:]]*[0-9]+' "$build_gradle" || fail "versionCode missing"
grep -Eq 'versionName[[:space:]]*=[[:space:]]*"[^"]+"' "$build_gradle" || fail "versionName missing"

test -s "$manifest" || fail "manifest is empty"
grep -Fq 'android:name=".MainActivity"' "$manifest" || fail "launcher activity changed"
grep -Fq 'android:exported="true"' "$manifest" || fail "launcher activity export contract changed"
grep -Fq 'android:usesCleartextTraffic="false"' "$manifest" || fail "cleartext traffic contract changed"

# The approved preview is locked to its exact Git blob identity.
preview="$module/src/main/assets/veytrix_preview.html"
if [[ -f "$preview" ]]; then
  expected='4c256667400e0f0599f2c9322ee482a0bea8eb39'
  actual="$(git -C "$root" hash-object "$preview")"
  [[ "$actual" == "$expected" ]] || fail "approved preview Git blob hash mismatch: $actual"
fi

# The approved immutable preview may contain mock repository names. Exclude only
# that fixture from identity scanning; implementation sources must stay VEYTRIX-only.
if grep -RniE 'nexusnova|com\.nexusnova\.' "$module/src" --exclude='veytrix_preview.html' 2>/dev/null; then
  fail "historical NexusNova identity detected in Android implementation"
fi

if grep -niE 'nexusnova|com\.nexusnova\.' "$build_gradle" "$manifest" 2>/dev/null; then
  fail "historical NexusNova identity detected in Android build identity"
fi

echo 'VEYTRIX Android contract: PASS'
