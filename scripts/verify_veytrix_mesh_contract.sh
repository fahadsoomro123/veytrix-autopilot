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

if grep -Eqi 'child_process.*exec[^F]|execSync\([^)]*\$|spawn\([^)]*shell[[:space:]]*:[[:space:]]*true' "$agent"; then
  echo 'Unsafe arbitrary command execution leaked into mesh agent.' >&2
  exit 1
fi

if grep -Eqi 'git (commit|push)|gh[[:space:]]+.*(push|pr)' "$agent"; then
  echo 'Mesh agent must not commit or push.' >&2
  exit 1
fi

if grep -Eqi 'OPENAI_API_KEY|VEYTRIX_GITHUB_TOKEN|VEYTRIX_KEYSTORE' "$agent"; then
  echo 'Mesh agent must not consume primary or signing credentials.' >&2
  exit 1
fi

node --check "$core"
node --check "$agent"

echo 'VEYTRIX 3M GITHUB MESH CONTRACT: PASS'
