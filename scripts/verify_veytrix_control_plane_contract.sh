#!/usr/bin/env bash
set -euo pipefail

root="${1:-.}"
root="$(cd "$root" && pwd)"
workflow="$root/.github/workflows/veytrix-autopilot.yml"

fail() {
  echo "VEYTRIX_CONTROL_PLANE_CONTRACT_FAILED: $*" >&2
  exit 1
}

[[ -s "$workflow" ]] || fail "veytrix-autopilot.yml is missing or empty"

if grep -nE '^[[:space:]]+OPENAI_API_KEY:[[:space:]]*\${{ secrets\.OPENAI_API_KEY' "$workflow"; then
  fail "OPENAI_API_KEY is exposed at broad workflow/job scope"
fi

if grep -nE 'git remote set-url .*x-access-token:' "$workflow"; then
  fail "target git credential is persisted in .git/config"
fi

if grep -nE '\${{ inputs\.(target_repository|branch) }}' "$workflow" |
   grep -E 'git (push|remote)|bash -c|sh -c'; then
  fail "untrusted input is directly interpolated into a shell command"
fi

grep -Fq 'git check-ref-format --branch "${AUTOPILOT_TARGET_BRANCH}"' "$workflow" ||
  fail "branch validation boundary is missing"

grep -Fq 'repository: ${{ inputs.target_repository }}' "$workflow" ||
  fail "target repository checkout boundary is missing"

grep -Fq 'persist-credentials: false' "$workflow" ||
  fail "checkout credential persistence must remain disabled"

grep -Fq 'openai-api-key: ${{ secrets.OPENAI_API_KEY }}' "$workflow" ||
  fail "AI execution secret boundary is missing"

grep -Fq 'http.extraheader=AUTHORIZATION: bearer ${GH_TOKEN}' "$workflow" ||
  fail "target push must use an ephemeral authorization header"

grep -Fq 'contents: read' "$workflow" ||
  fail "control-plane GITHUB_TOKEN must stay read-only"

echo 'VEYTRIX control-plane security contract: PASS'
