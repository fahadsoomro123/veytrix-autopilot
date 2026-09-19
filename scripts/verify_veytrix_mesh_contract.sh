#!/usr/bin/env bash
set -euo pipefail

ROOT=${1:-.}
mesh="$ROOT/free-ai-mesh"
agent="$mesh/mesh_autopilot.mjs"
core="$mesh/FreeAiMesh.mjs"

test -f "$core"
test -f "$ROOT/free-ai-mesh/free_ai_mesh_canary.mjs"
test -f "$ROOT/free-ai-mesh/veytrix_ai_router_canary.mjs"
test -f "$agent"
test -f "$ROOT/scripts/verify_veytrix_mesh_patch_policy.sh"

grep -Fq '3_000_000' "$core"
grep -Fq 'candidateSequence' "$core"
grep -Fq 'recordFailure' "$core"
grep -Fq 'health' "$core"

grep -Fq 'https://api.puter.com/puterai/chat/models/details' "$agent"
grep -Fq 'https://api.puter.com/puterai/openai/v1/chat/completions' "$agent"
grep -Fq 'PUTER_AUTH_TOKEN' "$agent"
grep -Fq 'tool_calls' "$agent"
grep -Fq 'write_file' "$agent"
grep -Fq 'run_verification' "$agent"
grep -Fq '.github/' "$agent"
grep -Fq 'free-ai-mesh/' "$agent"
grep -Fq 'veytrixsecurestore' "$agent"
grep -Fq 'veytrixautopilotclient' "$agent"

if grep -Eqi 'spawn\([^)]*shell[[:space:]]*:[[:space:]]*true|exec\([^)]*shell[[:space:]]*:[[:space:]]*true' "$agent"; then
  echo 'Unsafe shell execution leaked into mesh agent.' >&2
  exit 1
fi

if grep -Eq 'execFileSync\([^;]*\[.*git.*commit|execFileSync\([^;]*\[.*git.*push' "$agent"; then
  echo 'Mesh agent must not commit or push.' >&2
  exit 1
fi

if grep -Eq 'process\.env\.(OPENAI_API_KEY|VEYTRIX_GITHUB_TOKEN|VEYTRIX_KEYSTORE_BASE64|VEYTRIX_KEYSTORE_PASSWORD|VEYTRIX_KEY_ALIAS|VEYTRIX_KEY_PASSWORD)' "$agent"; then
  echo 'Mesh agent must not consume primary or signing credentials.' >&2
  exit 1
fi

bash -n "$ROOT/scripts/decide_engine.sh"
bash -n "$ROOT/scripts/test_decide_engine.sh"
bash -n "$ROOT/scripts/verify_veytrix_mesh_contract.sh"
bash -n "$ROOT/scripts/verify_veytrix_mesh_patch_policy.sh"
node --check "$core"
node --check "$agent"

echo 'VEYTRIX 3M GITHUB MESH CONTRACT: PASS'
