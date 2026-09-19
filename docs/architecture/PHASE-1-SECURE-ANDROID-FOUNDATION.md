# VEYTRIX Phase 1 — Secure Android Foundation

Status: \`implementation/phase1-secure-android-foundation\`

## Scope

This phase establishes the secure foundation for the real native Android VEYTRIX app. It does not redesign the flagship UI and does not replace protected Smart Autopilot or Free AI Mesh systems.

## Trust boundary

\`\`\`
Android UI
   |
   | validated mission / repository / branch / engine
   v
VeytrixAutopilotClient
   |
   | HTTPS
   v
GitHub API
   |
   | workflow_dispatch
   v
VEYTRIX control-plane
   |
   +--> deterministic verification (NO AI SECRET)
   |
   +--> AI execution (OPENAI_API_KEY only at this step)
   |
   +--> final deterministic verification (NO AI SECRET)
   |
   '--> target push via ephemeral Authorization header
\`\`\`

## Security invariants

1. Android does not embed a GitHub token.
2. The GitHub token is encrypted using an AES-GCM key held by Android Keystore.
3. API requests are restricted to \`api.github.com\` and redirects are disabled.
4. Mission, repository, branch, engine and verification depth are validated before dispatch.
5. Generic target verification does not receive \`OPENAI_API_KEY\`.
6. AI receives its key only at the Codex action boundary.
7. Target git credentials are not written to the git remote URL.
8. The workflow \`GITHUB_TOKEN\` is read-only.
9. Target publication uses an ephemeral HTTP Authorization header.
10. Verification depth is bounded to 2.

## Protected systems

- deterministic failure classification and fingerprinting
- deterministic engine decision logic
- autonomous repair controller
- 3M Free AI Mesh research/proof branch
- native-only Android contract

## Deferred

- flagship UI reconstruction
- data-driven mission/activity/result screens
- Mesh adapter integration into Android
- target CI recovery orchestration
- signed OTA client/update discovery
- device/instrumentation review

## Verification

\`\`\`bash
bash scripts/verify_veytrix_control_plane_contract.sh .
\`\`\`
