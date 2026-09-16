# Veytrix Smart Autopilot

Standalone, deterministic-first engineering control plane migrated from the Smart Autopilot implementation in `fahadsoomro123/nexusnova-app`.

## Core contract

MISSION → PREFLIGHT → INSPECT → DETERMINISTIC EXECUTION → BUILD/TEST → FAILURE ANALYSIS → SAFE RECOVERY → VERIFY → AI ESCALATION WHEN REQUIRED → REPAIR → RETEST → REVERIFY → ARTIFACT VALIDATION → DELIVERY / CONCRETE BLOCKER.

AI is an escalation layer, not the default executor.

## Repository boundary

Veytrix checks out its own core separately from the target repository. The target repository is selected through `target_repository` and `branch` inputs. NexusNova product/business code is not part of the Veytrix core.

## Security

Use a fine-grained GitHub token as the `VEYTRIX_GITHUB_TOKEN` Actions secret for target-repository access. Store `OPENAI_API_KEY` only as an Actions secret when AI escalation is enabled. Never commit tokens, signing keys, keystores, passwords or credentials.

The Android control app encrypts its GitHub token with the Android Keystore + AES-GCM and never stores the token in source.

## Verification

The repository contains deterministic inspection, failure classification, verification, artifact integrity checking, a self-healing E2E probe, and an Android build workflow. See `docs/migration/VERIFICATION.md` for the live verification record and remaining checks.

## Migration provenance

The complete source inventory, dependency map, source → target matrix, security review, and improvement record live under `docs/migration/`.

Source migration is intentionally non-destructive: the NexusNova implementation remains intact until the migration is proven complete.
