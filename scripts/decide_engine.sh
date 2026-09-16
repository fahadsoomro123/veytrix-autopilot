#!/usr/bin/env bash
set -euo pipefail

requested="${1:-auto}"
deterministic_outcome="${2:-failure}"
mission="${3:-}"
ai_available="${4:-false}"

selected=""
reason=""

is_verification_only_mission() {
  printf '%s\n' "$mission" | grep -Eqi '^[[:space:]]*(verify|verification|run[[:space:]]+tests?|test|check[[:space:]]+tests?|validate)[[:space:]]*(the[[:space:]]+)?(repository|repo|project|build|tests?)?[[:space:]]*$'
}

case "$requested" in
  github-free)
    if [[ "$deterministic_outcome" == "success" ]] && is_verification_only_mission; then
      selected="github-free"
      reason="deterministic-capability-sufficient-explicit"
    else
      selected="github-free"
      reason="explicit-github-free"
    fi
    ;;
  ai)
    if [[ "$ai_available" != "true" ]]; then
      echo "error=ai-requested-but-openai-key-unavailable"
      exit 2
    fi
    selected="ai"
    reason="explicit-ai"
    ;;
  auto)
    # A passing deterministic verification is only sufficient when the mission
    # itself is a verification-only mission. Passing tests cannot prove that an
    # arbitrary implementation request was completed.
    if [[ "$deterministic_outcome" == "success" ]] && is_verification_only_mission; then
      selected="github-free"
      reason="deterministic-capability-sufficient"
    elif [[ "$ai_available" == "true" ]]; then
      selected="ai"
      if [[ "$deterministic_outcome" == "success" ]]; then
        reason="deterministic-verification-passed-but-mission-requires-reasoning"
      else
        reason="deterministic-verification-insufficient"
      fi
    else
      selected="github-free"
      reason="ai-unavailable-deterministic-only"
    fi
    ;;
  *)
    echo "error=invalid-engine:$requested"
    exit 3
    ;;
esac

printf 'selected=%s\n' "$selected"
printf 'reason=%s\n' "$reason"
printf 'deterministic_outcome=%s\n' "$deterministic_outcome"
printf 'ai_available=%s\n' "$ai_available"
