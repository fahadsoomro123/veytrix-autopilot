#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
decider="$root/decide_engine.sh"
assert_decision() { local expected_selected="$1" expected_reason="$2"; shift 2; local output; output="$(bash "$decider" "$@")"; grep -Fxq "selected=$expected_selected" <<<"$output"; grep -Fxq "reason=$expected_reason" <<<"$output"; }
assert_decision github-free deterministic-capability-sufficient auto success "verify repository" true
assert_decision ai deterministic-verification-passed-but-mission-requires-reasoning auto success "implement the requested feature" true
assert_decision ai deterministic-verification-insufficient auto failure "implement the requested feature" true
assert_decision github-free ai-unavailable-deterministic-only auto failure "implement the requested feature" false
assert_decision github-free deterministic-capability-sufficient-explicit github-free success "verify repository" true
assert_decision github-free explicit-github-free github-free failure "anything" true
assert_decision ai explicit-ai ai success "anything" true
if bash "$decider" ai success "anything" false >/tmp/veytrix-ai-missing-provider.out 2>&1; then exit 1; fi
grep -Fxq 'error=ai-requested-but-no-ai-provider-available' /tmp/veytrix-ai-missing-provider.out
echo '=== ENGINE DECISION SELF-TEST PASSED ==='
