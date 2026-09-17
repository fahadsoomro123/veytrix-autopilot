#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

cat > "$TMP/transient.log" <<'EOF'
HTTP 503 while downloading dependency
EOF
cat > "$TMP/build.log" <<'EOF'
Execution failed: compilation failed
EOF
cat > "$TMP/credential.log" <<'EOF'
Authentication failed: signing key not available
EOF
cat > "$TMP/signing-secret-empty.log" <<'EOF'
test -n "${VEYTRIX_KEYSTORE_BASE64:-}"
test -n "${VEYTRIX_KEYSTORE_PASSWORD:-}"
test -n "${VEYTRIX_KEY_ALIAS:-}"
test -n "${VEYTRIX_KEY_PASSWORD:-}"
EOF

expect_class() {
  local file="$1" expected="$2"
  local actual
  actual="$(bash "$ROOT/scripts/classify_failure.sh" "$file" | awk -F= '$1=="classification"{print $2}')"
  [[ "$actual" == "$expected" ]] || { echo "expected $expected, got $actual"; exit 1; }
}

expect_class "$TMP/transient.log" transient
expect_class "$TMP/build.log" code-or-build
expect_class "$TMP/credential.log" credential-or-permission
expect_class "$TMP/signing-secret-empty.log" credential-or-permission

fingerprint="$(bash "$ROOT/scripts/failure_fingerprint.sh" "$TMP/transient.log" 'Veytrix Control Plane Self-Test' 'deadbeef' | awk -F= '$1=="fingerprint"{print $2}')"
[[ "$fingerprint" =~ ^[0-9a-f]{64}$ ]] || { echo "invalid fingerprint: $fingerprint"; exit 1; }

echo 'failure-intelligence-tests=passed'
