# Smart Autopilot Decision Engine Gate

Date: 2026-09-16
Repository: `fahadsoomro123/veytrix-autopilot`
Branch: `migration/forensic-smart-autopilot`

## Purpose

The previous implementation selected `ai` in `auto` mode whenever `OPENAI_API_KEY` existed. That made API-key availability, rather than deterministic sufficiency, the effective decision criterion.

The corrected decision boundary is:

1. Run deterministic inspection/verification first.
2. If the requested mission is verification-only and deterministic verification succeeds, select `github-free` and do not invoke OpenAI.
3. If deterministic verification is insufficient or the mission requires source-level reasoning, select `ai` only when an API key is available.
4. Explicit `ai` without an API key is a hard error rather than a silent fallback.
5. Explicit `github-free` never claims an arbitrary implementation mission is complete.

## Implementation

- `scripts/decide_engine.sh` — deterministic-first decision function.
- `scripts/test_decide_engine.sh` — unit-style decision matrix.
- `.github/workflows/veytrix-autopilot.yml` — integrates the decision engine and records decision evidence in the job summary.
- `.github/workflows/veytrix-decision-engine-gate.yml` — runs the decision matrix and workflow YAML structure validation.

## Live evidence

Decision gate run: `35142485671`.

Result: **PASS**.

Evidence from the completed job:
- deterministic-first decision self-test: success
- YAML structure validation: success
- gate evidence summary: success
- no OpenAI call is made by the decision-engine self-test

The self-test covers:
- deterministic verification sufficient → `github-free`
- deterministic verification passes but mission requires reasoning → `ai`
- deterministic verification insufficient → `ai`
- AI unavailable → deterministic-only fallback
- explicit `ai` without a key → hard failure

## Remaining proof boundary

This gate proves the decision logic independently, but it does not constitute the final end-to-end NexusNova mission proof. The Veytrix manual workflow remains on the unmerged migration branch, and a real controlled cross-repository `workflow_dispatch` against NexusNova still needs to be exercised after the workflow is available from the default branch.

No OpenAI API key was printed or stored in this document.
