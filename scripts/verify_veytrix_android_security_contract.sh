#!/usr/bin/env bash
set -euo pipefail

root="${1:-.}"
root="$(cd "$root" && pwd)"
secure_store="$root/android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixSecureStore.java"
client="$root/android/autopilot/src/main/java/com/veytrix/autopilot/VeytrixAutopilotClient.java"

fail() {
  echo "VEYTRIX_ANDROID_SECURITY_CONTRACT_FAILED: $*" >&2
  exit 1
}

[[ -s "$secure_store" ]] || fail "secure store source is missing"
[[ -s "$client" ]] || fail "autopilot client source is missing"

grep -Fq 'AndroidKeyStore' "$secure_store" ||
  fail "GitHub token encryption key must be held by Android Keystore"

grep -Fq 'AES/GCM/NoPadding' "$secure_store" ||
  fail "GitHub token encryption must use AES-GCM"

grep -Fq 'Cipher.ENCRYPT_MODE' "$secure_store" ||
  fail "secure store encryption path is missing"

grep -Fq 'Cipher.DECRYPT_MODE' "$secure_store" ||
  fail "secure store decryption path is missing"

grep -Fq 'api.github.com' "$client" ||
  fail "GitHub API host restriction is missing"

grep -Fq 'https://' "$client" ||
  fail "GitHub API transport must use HTTPS"

grep -Fq 'setInstanceFollowRedirects(false)' "$client" ||
  fail "GitHub API client must refuse automatic redirects"

grep -Fq 'Authorization", "Bearer " + token' "$client" ||
  fail "GitHub API authorization boundary is missing"

if grep -RInE 'OPENAI_API_KEY|VEYTRIX_GITHUB_TOKEN|GITHUB_TOKEN'     "$root/android/autopilot/src/main"     --include='*.java' --include='*.kt' --include='*.xml'; then
  fail "CI/provider secrets must never appear in Android production source"
fi

if grep -RInE 'Log\.(d|i|w|e|v).*token|System\.out\.print.*token|System\.err\.print.*token'     "$root/android/autopilot/src/main"     --include='*.java' --include='*.kt'; then
  fail "token material must not be sent to Android logs"
fi

echo 'VEYTRIX Android security contract: PASS'
