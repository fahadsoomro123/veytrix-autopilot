#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ROUTER="$ROOT/scripts/ai_router.sh"

bash -n "$ROUTER"

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
printf 'repair this fixture\n' > "$tmp/prompt.txt"

# Static contract checks: provider secrets, bounded retries, and free-model
# OpenRouter fallback must remain present without requiring live API calls.
grep -q 'GEMINI_API_KEY' "$ROUTER"
grep -q 'GROQ_API_KEY' "$ROUTER"
grep -q 'CEREBRAS_API_KEY' "$ROUTER"
grep -q 'MISTRAL_API_KEY' "$ROUTER"
grep -q 'OPENROUTER_API_KEY' "$ROUTER"
grep -q 'run_with_retries' "$ROUTER"
grep -q 'openrouter/free' "$ROUTER"

echo 'AI router syntax and fallback contract checks passed.'
