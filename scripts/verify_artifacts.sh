#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -eq 0 ]]; then
  echo 'usage: verify_artifacts.sh <artifact> [artifact...]' >&2
  exit 2
fi

for artifact in "$@"; do
  test -f "$artifact" || { echo "missing=$artifact"; exit 10; }
  test -s "$artifact" || { echo "empty=$artifact"; exit 11; }
  if [[ -f "${artifact}.sha256" ]]; then
    echo "checksum=$artifact"
    (cd "$(dirname "$artifact")" && sha256sum -c "$(basename "${artifact}.sha256")")
  else
    echo "checksum=not-provided:$artifact"
    sha256sum "$artifact"
  fi
done

echo 'ARTIFACT VERIFICATION PASSED'
