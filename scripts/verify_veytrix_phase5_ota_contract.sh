#!/usr/bin/env bash
set -euo pipefail

ROOT=${1:-.}

files=(
  "android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixUpdateClient.java"
  "android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixApkInstaller.java"
  "android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixInstallResultReceiver.java"
  "android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixUpdateView.java"
)

for file in "${files[@]}"; do test -f "$ROOT/$file"; done

manifest="$ROOT/android/autopilot/src/main/AndroidManifest.xml"
main="$ROOT/android/autopilot/src/main/java/com/veytrix/autopilot/MainActivity.java"

grep -Fq 'REQUEST_INSTALL_PACKAGES' "$manifest"
grep -Fq 'VeytrixInstallResultReceiver' "$manifest"
grep -Fq 'PackageInstaller' "${files[1]}"
grep -Fq 'getApkContentsSigners' "${files[1]}"
grep -Fq 'getSigningCertificateHistory' "${files[1]}"
grep -Fq 'SHA-256 verification failed' "${files[1]}"
grep -Fq 'canRequestPackageInstalls' "${files[3]}"
grep -Fq 'ACTION_MANAGE_UNKNOWN_APP_SOURCES' "${files[3]}"
grep -Fq 'browser_download_url' "${files[0]}"
grep -Fq 'digest' "${files[0]}"
grep -Fq 'release-assets.githubusercontent.com' "${files[0]}"
grep -Fq 'setInstanceFollowRedirects(false)' "${files[0]}"
grep -Fq 'https://api.github.com/repos/fahadsoomro123/veytrix-autopilot/releases/latest' "${files[0]}"
grep -Fq 'showUpdates' "$main"
grep -Fq 'Updates' "$main"

if grep -REn 'ScrollView|HorizontalScrollView' "${files[@]}"; then
  echo 'Phase 5 update surface must not depend on page scrolling.' >&2
  exit 1
fi

if grep -REn 'WebView|http://' "${files[@]}"; then
  echo 'OTA implementation contains forbidden WebView or cleartext HTTP.' >&2
  exit 1
fi

if grep -REn 'Color\.rgb\((55, 132, 255|8, 23, 42|12, 32, 55)|Color\.BLACK|Color\.BLUE|#3784FF' "${files[@]}"; then
  echo 'Legacy blue/black palette leaked into OTA surface.' >&2
  exit 1
fi

echo 'VEYTRIX Phase 5 OTA contract: PASS'