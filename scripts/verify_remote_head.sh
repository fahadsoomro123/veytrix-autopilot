#!/usr/bin/env bash
set -euo pipefail

repo="${1:?repository required}"
branch="${2:?branch required}"
expected="${3:?expected head required}"
token="${GH_TOKEN:?GH_TOKEN required}"

actual="$(gh api "repos/${repo}/git/ref/heads/${branch}" --jq '.object.sha')"
if [[ "$actual" != "$expected" ]]; then
  echo "Remote branch moved while repair was running."
  echo "expected=$expected"
  echo "actual=$actual"
  exit 20
fi

echo "Remote HEAD verified: $actual"
