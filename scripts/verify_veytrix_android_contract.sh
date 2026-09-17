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

# Immutable identity invariants for the current VEYTRIX native Android line.
grep -Fq 'namespace = "com.veytrix.autopilot"' "$build_gradle" || fail "namespace changed"
grep -Fq 'applicationId = "com.veytrix.autopilot"' "$build_gradle" || fail "applicationId changed"
grep -Eq 'versionCode[[:space:]]*=[[:space:]]*[0-9]+' "$build_gradle" || fail "versionCode missing"
grep -Eq 'versionName[[:space:]]*=[[:space:]]*"[^"]+"' "$build_gradle" || fail "versionName missing"

test -s "$manifest" || fail "manifest is empty"
grep -Fq 'android:name=".MainActivity"' "$manifest" || fail "launcher activity changed"
grep -Fq 'android:exported="true"' "$manifest" || fail "launcher activity export contract changed"
grep -Fq 'android:usesCleartextTraffic="false"' "$manifest" || fail "cleartext traffic contract changed"

# The launcher must be native Android code; legacy WebView hosting is forbidden.
main_activity="$module/src/main/java/com/veytrix/autopilot/MainActivity.java"
test -s "$main_activity" || fail "MainActivity.java is missing or empty"
if grep -nE 'android\.webkit\.(WebView|WebSettings|WebViewClient|WebChromeClient)|loadUrl\("file:///android_asset/' "$main_activity" 2>/dev/null; then
  fail "legacy WebView implementation detected in MainActivity"
fi

# The Android module must not carry the retired WebView preview asset.
legacy_preview="$module/src/main/assets/veytrix_preview.html"
if [[ -e "$legacy_preview" ]]; then
  fail "legacy WebView preview asset still exists"
fi

# Implementation sources must stay VEYTRIX-only.
if grep -RniE 'nexusnova|com\.nexusnova\.' "$module/src" 2>/dev/null; then
  fail "historical NexusNova identity detected in Android implementation"
fi

if grep -niE 'nexusnova|com\.nexusnova\.' "$build_gradle" "$manifest" 2>/dev/null; then
  fail "historical NexusNova identity detected in Android build identity"
fi

echo 'VEYTRIX native Android contract: PASS'
