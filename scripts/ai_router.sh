#!/usr/bin/env bash
set -euo pipefail

# Veytrix free-first AI provider router.
# Deterministic decision-making happens before this script. This script runs
# only after the orchestrator has established that source-level AI reasoning
# is required.
#
# Provider chain:
#   Gemini -> Groq -> Cerebras -> Mistral -> OpenRouter free model pool
# A provider failure is isolated so quota/429/503/network failures can fall
# through to the next configured provider.
#
# Supported secrets:
#   GEMINI_API_KEY, GROQ_API_KEY, CEREBRAS_API_KEY, MISTRAL_API_KEY,
#   OPENROUTER_API_KEY
#
# No paid OpenAI provider is selected by this router.

provider="${VEYTRIX_AI_PROVIDER:-auto}"
prompt_file="${1:-}"
workdir="${2:-.}"
max_attempts="${VEYTRIX_AI_PROVIDER_ATTEMPTS:-2}"

if [[ -z "$prompt_file" || ! -f "$prompt_file" ]]; then
  echo "Usage: $0 <prompt-file> [working-directory]" >&2
  exit 2
fi
[[ "$max_attempts" =~ ^[1-3]$ ]] || max_attempts=2

run_gemini() {
  GEMINI_MODEL="${GEMINI_MODEL:-gemini-3.8-flash}" \
    bash "$(dirname "$0")/gemini_free_agent.sh" "$prompt_file"
}

run_openai_compatible() {
  local api_key="$1" api_base="$2" model="$3"
  local prompt_abs
  prompt_abs="$(cd "$(dirname "$prompt_file")" && pwd)/$(basename "$prompt_file")"
  command -v aider >/dev/null 2>&1 || python -m pip install --quiet aider-chat
  export OPENAI_API_KEY="$api_key"
  export OPENAI_API_BASE="$api_base"
  cd "$workdir"
  aider --yes --no-auto-commits --no-gitignore --model "$model" --message-file "$prompt_abs"
}

run_groq() {
  run_openai_compatible "$GROQ_API_KEY" "https://api.groq.com/openai/v1" "${GROQ_MODEL:-openai/gpt-oss-120b}"
}

run_cerebras() {
  run_openai_compatible "$CEREBRAS_API_KEY" "https://api.cerebras.ai/v1" "${CEREBRAS_MODEL:-gpt-oss-120b}"
}

run_mistral() {
  run_openai_compatible "$MISTRAL_API_KEY" "https://api.mistral.ai/v1" "${MISTRAL_MODEL:-mistral-small-latest}"
}

run_openrouter() {
  run_openai_compatible "$OPENROUTER_API_KEY" "https://openrouter.ai/api/v1" "${OPENROUTER_MODEL:-openrouter/free}"
}

run_with_retries() {
  local name="$1"; shift
  local attempt rc
  for ((attempt=1; attempt<=max_attempts; attempt++)); do
    echo "Attempting AI provider: $name (attempt $attempt/$max_attempts)"
    set +e
    "$@"
    rc=$?
    set -e
    if [[ "$rc" -eq 0 ]]; then
      echo "AI provider succeeded: $name"
      return 0
    fi
    echo "Provider $name failed with exit $rc; continuing fallback chain." >&2
    [[ "$attempt" -lt "$max_attempts" ]] && sleep $((attempt * 3))
  done
  return 1
}

available=""
append_available() {
  [[ -n "$available" ]] && available="$available,"
  available="${available}$1"
}
[[ -n "${GEMINI_API_KEY:-}" ]] && append_available gemini-free
[[ -n "${GROQ_API_KEY:-}" ]] && append_available groq-free
[[ -n "${CEREBRAS_API_KEY:-}" ]] && append_available cerebras-free
[[ -n "${MISTRAL_API_KEY:-}" ]] && append_available mistral-free
[[ -n "${OPENROUTER_API_KEY:-}" ]] && append_available openrouter-free-pool
echo "Veytrix AI router candidates: ${available:-none}"

case "$provider" in
  gemini) [[ -n "${GEMINI_API_KEY:-}" ]] || { echo "GEMINI_API_KEY missing" >&2; exit 10; }; run_with_retries gemini-free run_gemini ;;
  groq) [[ -n "${GROQ_API_KEY:-}" ]] || { echo "GROQ_API_KEY missing" >&2; exit 10; }; run_with_retries groq-free run_groq ;;
  cerebras) [[ -n "${CEREBRAS_API_KEY:-}" ]] || { echo "CEREBRAS_API_KEY missing" >&2; exit 10; }; run_with_retries cerebras-free run_cerebras ;;
  mistral) [[ -n "${MISTRAL_API_KEY:-}" ]] || { echo "MISTRAL_API_KEY missing" >&2; exit 10; }; run_with_retries mistral-free run_mistral ;;
  openrouter) [[ -n "${OPENROUTER_API_KEY:-}" ]] || { echo "OPENROUTER_API_KEY missing" >&2; exit 10; }; run_with_retries openrouter-free-pool run_openrouter ;;
  auto)
    if [[ -n "${GEMINI_API_KEY:-}" ]] && run_with_retries gemini-free run_gemini; then exit 0; fi
    if [[ -n "${GROQ_API_KEY:-}" ]] && run_with_retries groq-free run_groq; then exit 0; fi
    if [[ -n "${CEREBRAS_API_KEY:-}" ]] && run_with_retries cerebras-free run_cerebras; then exit 0; fi
    if [[ -n "${MISTRAL_API_KEY:-}" ]] && run_with_retries mistral-free run_mistral; then exit 0; fi
    if [[ -n "${OPENROUTER_API_KEY:-}" ]] && run_with_retries openrouter-free-pool run_openrouter; then exit 0; fi
    echo "All configured free AI providers exhausted or unavailable." >&2
    exit 11
    ;;
  *) echo "Unknown provider: $provider" >&2; exit 3 ;;
esac
