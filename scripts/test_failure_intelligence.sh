#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

cat > "$TMP/transient.log" <<'EOF'
2026-09-17T10:00:00Z HTTP 503 while downloading dependency run 12345678901
EOF
cat > "$TMP/transient-rerun.log" <<'EOF'
2026-09-17T10:05:00Z HTTP 503 while downloading dependency run 98765432109
EOF
cat > "$TMP/build.log" <<'EOF'
Execution failed: compilation failed
EOF
cat > "$TMP/credential.log" <<'EOF'
Authentication failed: signing key not available
EOF
cat > "$TMP/credit-exhausted.log" <<'EOF'
stream disconnected before completion: You have no credits remaining. Add credits to continue using the API.
EOF
cat > "$TMP/signing-secret-empty.log" <<'EOF'
VEYTRIX_KEYSTORE_BASE64:
VEYTRIX_KEYSTORE_PASSWORD:
VEYTRIX_KEY_ALIAS:
VEYTRIX_KEY_PASSWORD:
EOF
cat > "$TMP/signing-secret-empty-variant.log" <<'EOF'
release job failed because VEYTRIX_KEYSTORE_BASE64 is empty and VEYTRIX_KEY_ALIAS was not provided
EOF

expect_class() {
  local file="$1" expected="$2"
  local actual
  actual="$(bash "$ROOT/scripts/classify_failure.sh" "$file" | awk -F= '$1=="classification"{print $2}')"
  [[ "$actual" == "$expected" ]] || { echo "expected $expected, got $actual"; exit 1; }
}

expect_reason() {
  local file="$1" expected="$2"
  local actual
  actual="$(bash "$ROOT/scripts/classify_failure.sh" "$file" | awk -F= '$1=="reason"{print $2}')"
  [[ "$actual" == "$expected" ]] || { echo "expected reason $expected, got $actual"; exit 1; }
}

fingerprint_for() {
  bash "$ROOT/scripts/failure_fingerprint.sh" "$1" 'Veytrix Control Plane Self-Test' 'deadbeef' | awk -F= '$1=="fingerprint"{print $2}'
}

expect_class "$TMP/transient.log" transient
expect_class "$TMP/build.log" code-or-build
expect_class "$TMP/credential.log" credential-or-permission
expect_class "$TMP/credit-exhausted.log" external-service
expect_class "$TMP/signing-secret-empty.log" credential-or-permission
expect_class "$TMP/signing-secret-empty-variant.log" credential-or-permission

expect_reason "$TMP/transient.log" known-transient-infrastructure-signature
expect_reason "$TMP/build.log" source-or-build-failure-signature
expect_reason "$TMP/credential.log" external-credential-or-permission-blocker
expect_reason "$TMP/credit-exhausted.log" external-service-or-quota-blocker
expect_reason "$TMP/signing-secret-empty.log" veytrix-signing-credential-signal
expect_reason "$TMP/signing-secret-empty-variant.log" veytrix-signing-credential-signal

fingerprint="$(fingerprint_for "$TMP/transient.log")"
[[ "$fingerprint" =~ ^[0-9a-f]{64}$ ]] || { echo "invalid fingerprint: $fingerprint"; exit 1; }

credential_fp="$(fingerprint_for "$TMP/signing-secret-empty.log")"
credential_variant_fp="$(fingerprint_for "$TMP/signing-secret-empty-variant.log")"
[[ "$credential_fp" == "$credential_variant_fp" ]] || {
  echo "credential fingerprint changed across equivalent wording"
  exit 1
}

transient_fp="$(fingerprint_for "$TMP/transient.log")"
transient_rerun_fp="$(fingerprint_for "$TMP/transient-rerun.log")"
[[ "$transient_fp" == "$transient_rerun_fp" ]] || {
  echo "transient fingerprint changed across volatile run/timestamp values"
  exit 1
}

echo 'failure-intelligence-tests=passed'
