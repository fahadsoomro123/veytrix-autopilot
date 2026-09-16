#!/usr/bin/env bash
set -euo pipefail

# Veytrix provider router.
# Deterministic decision-making happens before this script. This script only
# runs after the orchestrator has decided that source-level AI is required.
# Provider priority is explicit and health-aware:
#   1) Gemini free tier (GEMINI_API_KEY)
#   2) Groq free tier (GROQ_API_KEY)
#   3) OpenRouter free models (OPENROUTER_API_KEY)
#   4) Puter integration is reserved for a dedicated authenticated adapter.
# Paid OpenAI is deliberately not selected by this router.

provider="${VEYTRIX_AI_PROVIDER:-auto}"
prompt_file="${1:-}"
workdir="${2:-.}"

if [[ -z "$prompt_file" || ! -f "$prompt_file" ]]; then
  echo "Usage: $0 <prompt-file> [working-directory]" >&2
  exit 2
fi

run_gemini() {
  GEMINI_MODEL="${GEMINI_MODEL:-gemini-3.8-flash}" \
    bash "$(dirname "$0")/gemini_free_agent.sh" "$prompt_file"
}

run_openai_compatible() {
  local api_key="$1"
  local api_base="$2"
  local model="$3"
  command -v aider >/dev/null 2>&1 || python -m pip install --quiet aider-chat
  export OPENAI_API_KEY="$api_key"
  export OPENAI_API_BASE="$api_base"
  cd "$workdir"
  aider \
    --yes \
    --no-auto-commits \
    --no-gitignore \
    --model "$model" \
    --message-file "$OLDPWD/$prompt_file"
}

run_groq() {
  run_openai_compatible "$GROQ_API_KEY" "https://api.groq.com/openai/v1" "${GROQ_MODEL:-openai/gpt-oss-120b}"
}

run_openrouter() {
  run_openai_compatible "$OPENROUTER_API_KEY" "https://openrouter.ai/api/v1" "${OPENROUTER_MODEL:-openai/gpt-oss-120b:free}"
}

available=""
if [[ -n "${GEMINI_API_KEY:-}" ]]; then available="gemini"; fi
if [[ -n "${GROQ_API_KEY:-}" ]]; then [[ -n "$available" ]] && available="$available,"; available="${available}groq"; fi
if [[ -n "${OPENROUTER_API_KEY:-}" ]]; then [[ -n "$available" ]] && available="$available,"; available="${available}openrouter"; fi

echo "Veytrix AI router candidates: ${available:-none}"

case "$provider" in
  gemini) [[ -n "${GEMINI_API_KEY:-}" ]] || { echo "GEMINI_API_KEY missing" >&2; exit 10; }; run_gemini ;;
  groq) [[ -n "${GROQ_API_KEY:-}" ]] || { echo "GROQ_API_KEY missing" >&2; exit 10; }; run_groq ;;
  openrouter) [[ -n "${OPENROUTER_API_KEY:-}" ]] || { echo "OPENROUTER_API_KEY missing" >&2; exit 10; }; run_openrouter ;;
  auto)
    if [[ -n "${GEMINI_API_KEY:-}" ]]; then
      echo "Selected provider: gemini-free"
      run_gemini
    elif [[ -n "${GROQ_API_KEY:-}" ]]; then
      echo "Selected provider: groq-free"
      run_groq
    elif [[ -n "${OPENROUTER_API_KEY:-}" ]]; then
      echo "Selected provider: openrouter-free"
      run_openrouter
    else
      echo "No free AI provider is configured." >&2
      exit 11
    fi
    ;;
  *) echo "Unknown provider: $provider" >&2; exit 3 ;;
esac
