#!/usr/bin/env bash
set -euo pipefail

ROOT=${1:-.}
cd "$ROOT"

changed="$(git diff --name-only --diff-filter=ACMR)"
changed_cached="$(git diff --cached --name-only --diff-filter=ACMR)"
untracked="$(git ls-files --others --exclude-standard)"
all_changed="$(printf '%s\n%s\n%s\n' "$changed" "$changed_cached" "$untracked" | sed '/^$/d' | sort -u)"

while IFS= read -r file; do
  [[ -z "$file" ]] && continue
  case "$file" in
    .github/*|free-ai-mesh/*)
      echo "Mesh patch policy rejects protected path: $file" >&2
      exit 1
      ;;
    *VeytrixSecureStore*|*VeytrixAutopilotClient*|*local.properties|*gradle.properties|*.jks|*.keystore|*.p12|*.pem)
      echo "Mesh patch policy rejects protected/sensitive path: $file" >&2
      exit 1
      ;;
  esac
done <<< "$all_changed"

patch="$(git diff --no-ext-diff --unified=0 -- . ':(exclude).github' ':(exclude)free-ai-mesh')"
patch_cached="$(git diff --cached --no-ext-diff --unified=0 -- . ':(exclude).github' ':(exclude)free-ai-mesh')"
untracked_content="$(while IFS= read -r file; do
  [[ -z "$file" ]] && continue
  case "$file" in
    .github/*|free-ai-mesh/*) continue ;;
  esac
  if [[ -f "$file" ]]; then
    cat "$file"
    printf '\n'
  fi
done <<< "$untracked")"
combined="$patch
$patch_cached
$untracked_content"

if grep -Eqi '(-----BEGIN [A-Z ]*PRIVATE KEY-----|ghp_[A-Za-z0-9_]{20,}|github_pat_[A-Za-z0-9_]{20,}|OPENAI_API_KEY[[:space:]]*[:=]|PUTER_AUTH_TOKEN[[:space:]]*[:=]|VEYTRIX_GITHUB_TOKEN[[:space:]]*[:=]|VEYTRIX_KEYSTORE_|AIza[A-Za-z0-9_-]{20,})' <<< "$combined"; then
  echo 'Mesh patch policy detected possible secret material.' >&2
  exit 1
fi

if grep -Eqi '(usesCleartextTraffic[[:space:]]*=[[:space:]]*["'"']true|setHostnameVerifier|ALLOW_ALL_HOSTNAME_VERIFIER|TrustAll|trustAll|WebView|http://)' <<< "$combined"; then
  echo 'Mesh patch policy detected a transport/security weakening signature.' >&2
  exit 1
fi

git diff --check
echo 'VEYTRIX MESH PATCH POLICY: PASS'
