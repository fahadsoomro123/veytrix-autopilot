#!/usr/bin/env bash
set -euo pipefail

: "${GH_TOKEN:?GH_TOKEN is required}"
: "${TARGET_REPOSITORY:?TARGET_REPOSITORY is required}"
: "${TARGET_BRANCH:?TARGET_BRANCH is required}"

started_epoch="${STARTED_EPOCH:-0}"
max_reruns="${MAX_FREE_RERUNS:-2}"
page_size="${RUN_PAGE_SIZE:-30}"

api() {
  gh api "$@"
}

iso_to_epoch() {
  python - "$1" <<'PY'
import datetime, sys
s = sys.argv[1]
try:
    print(int(datetime.datetime.fromisoformat(s.replace('Z', '+00:00')).timestamp()))
except Exception:
    print(0)
PY
}

printf '%s\n' '=== VEYTRIX TARGET CI RECOVERY ==='
printf 'target=%s\nbranch=%s\n' "$TARGET_REPOSITORY" "$TARGET_BRANCH"

runs_json="$(api "repos/$TARGET_REPOSITORY/actions/runs?branch=$TARGET_BRANCH&per_page=$page_size")"
selected=''
selected_epoch=0

while IFS= read -r row; do
  run_id="$(jq -r '.id' <<<"$row")"
  status="$(jq -r '.status' <<<"$row")"
  conclusion="$(jq -r '.conclusion // \"\"' <<<"$row")"
  created_at="$(jq -r '.created_at' <<<"$row")"
  created_epoch="$(iso_to_epoch "$created_at")"
  [[ "$created_epoch" -ge "$started_epoch" ]] || continue

  selected="$run_id"
  selected_epoch="$created_epoch"
  selected_row="$row"
  break

done < <(jq -c '.workflow_runs[]' <<<"$runs_json")

if [[ -z "$selected" ]]; then
  echo 'target_ci_run=not-found'
  echo 'reason=No workflow run for the target branch was observed at or after the mission start boundary.'
  echo 'action=blocker'
  exit 20
fi

workflow_name="$(jq -r '.name // .workflow_name // "unknown"' <<<"$selected_row")"
run_attempt="$(jq -r '.run_attempt // 1' <<<"$selected_row")"
conclusion="$(jq -r '.conclusion // ""' <<<"$selected_row")"
status="$(jq -r '.status' <<<"$selected_row")"
head_sha="$(jq -r '.head_sha // ""' <<<"$selected_row")"

printf 'target_ci_run=%s\nworkflow=%s\nstatus=%s\nconclusion=%s\nattempt=%s\nsha=%s\n' "$selected" "$workflow_name" "$status" "$conclusion" "$run_attempt" "$head_sha"

gh run view "$selected" -R "$TARGET_REPOSITORY" --json name,status,conclusion,headSha,workflowName,runAttempt > /tmp/veytrix-target-run.json

if [[ "$status" != 'completed' ]]; then
  echo 'target_ci_state=running'
  echo 'action=wait-for-completion-or-next-control-plane-observation'
  exit 10
fi

if [[ "$conclusion" == 'success' ]]; then
  echo 'target_ci_state=verified-success'
  exit 0
fi

gh run view "$selected" -R "$TARGET_REPOSITORY" --log-failed > /tmp/veytrix-target-failure.log || true
bash "$(dirname "$0")/classify_failure.sh" /tmp/veytrix-target-failure.log | tee /tmp/veytrix-target-classification.txt
classification="$(awk -F= '$1=="classification" {print $2}' /tmp/veytrix-target-classification.txt)"

echo "target_failure_classification=$classification"

if [[ "$classification" == 'transient' ]]; then
  if [[ "$run_attempt" -ge "$max_reruns" ]]; then
    echo "action=blocker"
    echo "reason=Transient failure is recognized but rerun ceiling ($max_reruns) is reached."
    exit 21
  fi
  gh run rerun "$selected" -R "$TARGET_REPOSITORY" --failed
  echo 'action=rerun-failed-jobs'
  echo 'reason=recognized-transient-failure'
  exit 11
fi

echo 'action=ai-escalation-or-concrete-blocker'
echo 'reason=Failure is not a safely deterministic transient signature.'
exit 30
