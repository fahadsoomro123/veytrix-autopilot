#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
decider="$root/decide_engine.sh"

assert_decision() {
  local expected_selected="$1"
  local expected_reason="$2"
  shift 2
  local output
  output="$(bash "$decider" "$@")"
  grep -Fxq "selected=$expected_selected" <<<"$output"
  grep -Fxq "reason=$expected_reason" <<<"$output"
}

assert_decision github-free deterministic-capability-sufficient auto success "verify repository" true
assert_decision ai deterministic-verification-passed-but-mission-requires-reasoning auto success "implement the requested feature" true
assert_decision ai deterministic-verification-insufficient auto failure "implement the requested feature" true
assert_decision github-free ai-unavailable-no-mesh-configured auto failure "implement the requested feature" false false
assert_decision mesh ai-unavailable-mesh-fallback auto failure "implement the requested feature" false true
assert_decision github-free deterministic-capability-sufficient-explicit github-free success "verify repository" true
assert_decision github-free explicit-github-free github-free failure "anything" true
assert_decision ai explicit-ai ai success "anything" true
assert_decision mesh explicit-mesh mesh failure "anything" false true

if bash "$decider" ai success "anything" false >/tmp/veytrix-ai-missing-key.out 2>&1; then
  echo 'Expected explicit AI without a key to fail.' >&2
  exit 1
fi
grep -Fxq 'error=ai-requested-but-openai-key-unavailable' /tmp/veytrix-ai-missing-key.out

echo '=== ENGINE DECISION SELF-TEST PASSED ==='

if bash "$decider" mesh failure "anything" false false >/tmp/veytrix-mesh-missing-token.out 2>&1; then
  echo 'Expected explicit mesh without a Puter token to fail.' >&2
  exit 1
fi
grep -Fxq 'error=mesh-requested-but-puter-token-unavailable' /tmp/veytrix-mesh-missing-token.out
