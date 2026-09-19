#!/usr/bin/env bash
set -euo pipefail

ROOT=${1:-.}

files=(
  "android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixUpdateClient.java"
  "android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixApkInstaller.java"
  "android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixInstallResultReceiver.java"
  "android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixUpdateView.java"
)

for file in "${files[@]}"; do
  test -f "$ROOT/$file"
done

manifest="$ROOT/android/autopilot/src/main/AndroidManifest.xml"
main="$ROOT/android/autopilot/src/main/java/com/veytrix/autopilot/MainActivity.java"
publish="$ROOT/.github/workflows/veytrix-ota-publish.yml"

require_text() {
  local pattern="$1"
  local file="$2"
  if ! grep -Fq -- "$pattern" "$file"; then
    echo "OTA contract missing: [$pattern] in $file" >&2
    exit 1
  fi
}

require_text 'REQUEST_INSTALL_PACKAGES' "$manifest"
require_text 'VeytrixInstallResultReceiver' "$manifest"
require_text 'PackageInstaller' "${files[1]}"
require_text 'setRequireUserAction' "${files[1]}"
require_text 'USER_ACTION_REQUIRED' "${files[1]}"
require_text 'getApkContentsSigners' "${files[1]}"
require_text 'getSigningCertificateHistory' "${files[1]}"
require_text 'SHA-256 verification failed' "${files[1]}"
require_text 'canRequestPackageInstalls' "${files[3]}"
require_text 'ACTION_MANAGE_UNKNOWN_APP_SOURCES' "${files[3]}"
require_text 'browser_download_url' "${files[0]}"
require_text 'digest' "${files[0]}"
require_text 'release-assets.githubusercontent.com' "${files[0]}"
require_text 'setInstanceFollowRedirects(false)' "${files[0]}"
require_text 'https://api.github.com/repos/' "${files[0]}"
require_text 'MAX_API_RESPONSE_CHARS' "${files[0]}"
require_text 'GitHub API response is too large' "${files[0]}"
require_text 'REPOSITORY' "${files[0]}"
require_text '/releases/latest' "${files[0]}"
require_text 'Release tag is not semantic-versioned' "${files[0]}"
require_text 'Release APK URL does not match the published release tag' "${files[0]}"
require_text 'sha256:[0-9a-f]{64}' "${files[0]}"
require_text 'ambiguous VEYTRIX APK assets' "${files[0]}"
require_text 'showUpdates' "$main"
require_text 'Updates' "$main"
require_text 'permissions:' "$publish"
require_text 'contents: write' "$publish"
require_text 'gh release create' "$publish"
require_text 'Verify published OTA release' "$publish"
require_text 'gh api "repos/${GITHUB_REPOSITORY}/releases/tags/${RELEASE_TAG}"' "$publish"
require_text 'target_commitish' "$publish"
require_text 'asset_digest' "$publish"
require_text 'sha256:$EXPECTED_SHA256' "$publish"
require_text 'update.json' "$publish"
require_text 'VEYTRIX_KEYSTORE_BASE64' "$publish"
require_text 'build_commit="$(git rev-parse HEAD)"' "$publish"
require_text 'jq -n' "$publish"
require_text 'tag_version="${RELEASE_TAG#v}"' "$publish"
require_text 'tag_base="${tag_version%%[-+]*}"' "$publish"
require_text 'version_base="${version_name%%[-+]*}"' "$publish"
require_text 'Release tag base version must match the APK versionName' "$publish"
require_text '--target "$BUILD_COMMIT"' "$publish"
require_text 'already has a GitHub release; refusing to replace published OTA assets.' "$publish"
require_text 'already exists; refusing to reuse an immutable OTA tag.' "$publish"
if grep -Fq 'gh release upload' "$publish" || grep -Fq -- '--clobber' "$publish"; then
  echo 'OTA publisher must not replace assets under an existing release tag.' >&2
  exit 1
fi

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

bash -n "$ROOT/scripts/verify_veytrix_phase5_ota_contract.sh"
echo 'VEYTRIX Phase 5 OTA contract: PASS'