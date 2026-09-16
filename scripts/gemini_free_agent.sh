#!/usr/bin/env bash
set -euo pipefail

# Veytrix free AI provider adapter.
# Uses Google's Gemini CLI with GEMINI_API_KEY. The API has a documented free tier.
# This adapter intentionally keeps the model provider outside the Veytrix decision engine.

if [[ -z "${GEMINI_API_KEY:-}" ]]; then
  echo "ERROR: GEMINI_API_KEY is not configured." >&2
  exit 2
fi

command -v node >/dev/null 2>&1 || { echo "ERROR: Node.js is required." >&2; exit 3; }

if ! command -v gemini >/dev/null 2>&1; then
  echo "Installing Gemini CLI..." >&2
  npm install -g @google/gemini-cli
fi

MODEL="${GEMINI_MODEL:-gemini-3.8-flash}"
PROMPT_FILE="${1:-}"

if [[ -z "$PROMPT_FILE" || ! -f "$PROMPT_FILE" ]]; then
  echo "Usage: $0 <prompt-file>" >&2
  exit 4
fi

# Headless, workspace-editing agent. The caller is responsible for sandboxing,
# bounded retries, verification, and deciding whether AI is necessary.
GEMINI_API_KEY="$GEMINI_API_KEY" \
GEMINI_CLI_TRUST_WORKSPACE=true \
gemini \
  --model "$MODEL" \
  --yolo \
  --output-format json \
  -p "$(cat "$PROMPT_FILE")"
