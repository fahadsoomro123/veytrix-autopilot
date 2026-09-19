# VEYTRIX Phase 4 — GitHub Resilience and 3M Mesh Fallback

## Goal

Make the protected 3M Free AI Mesh a real GitHub-side fallback executor for missions that cannot be completed by the primary AI path.

The Android app is not the mesh runtime. The mesh stays on the GitHub/autopilot control plane.

## Runtime path

1. Veytrix receives a mission.
2. Deterministic verification runs first.
3. A verification-only mission may finish without an AI executor.
4. Otherwise Veytrix prefers the primary OpenAI/Codex executor when configured.
5. If the primary executor is unavailable at decision time, Veytrix selects the Mesh when its Puter credential is configured.
6. If the primary executor starts and fails, Veytrix invokes the Mesh as a bounded fallback.
7. The Mesh discovers available free Puter model lanes, applies health/cooldown routing, and runs a constrained code-agent loop.
8. The fallback agent can inspect, search, read, write source files, and invoke only allowlisted verification commands.
9. A Mesh lane is treated as successful only after deterministic verification passes.
10. A second patch-safety policy runs before publication.
11. Only the outer workflow commits/pushes verified changes.

## Protected boundaries

The Mesh agent cannot read or write `.github/workflows`, the Mesh core itself, secrets, credentials, signing material, the Android secure credential store, or the authenticated GitHub client.

The Mesh agent has no arbitrary shell tool. Verification commands are fixed and allowlisted.

The Mesh agent never commits or pushes.

The outer workflow owns publication and uses the GitHub credential only in the final publish step.

## Free lane discovery

`FreeAiMesh` retains a maximum discovery bound of 3,000,000 logical model lanes, deduplicates lane IDs, tracks per-lane health, cools down failures, prefers proven healthy lanes, and then walks remaining eligible lanes.

The runtime adapter filters Puter model metadata to lanes marked free by a `:free` variant, an explicit `free` flag, or zero input/output cost metadata.

Puter's current public documentation describes `puter.ai.listModels()` as sourcing the same catalog as `/puterai/chat/models/details`, and documents the OpenAI-compatible chat endpoint used by the runtime adapter.

## Evidence model

The 3M claim is deliberately separated into three evidence classes:

- **3M logical mesh capacity:** the protected core accepts up to 3,000,000 discovered lanes.
- **3M synthetic failover:** the hardcore canary constructs 3,000,000 logical lanes and verifies survival through 100,000 consecutive failures.
- **Real provider availability:** the live provider catalog and inference probes remain environment-dependent. The system does not treat a synthetic lane count as proof that 3,000,000 live free inference endpoints exist.

## Required secret

GitHub Actions must have `PUTER_AUTH_TOKEN` configured before the Mesh can execute. The value is consumed only by the Mesh runtime step and is never written to repository files or logs.

## Lock gate

Phase 4 is not considered complete until:

- Mesh contract passes.
- Integration failover canary passes.
- Engine routing self-test passes.
- Android build remains green with the Mesh gate included.
- A real GitHub Actions run demonstrates the fallback path with configured `PUTER_AUTH_TOKEN`.
- No protected files or security controls are modified by the Mesh executor.
