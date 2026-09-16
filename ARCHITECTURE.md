# Veytrix Smart Autopilot Architecture

## Operating principle

Veytrix executes deterministic work first. AI is an escalation layer used only when repository/contextual reasoning or source-code repair is genuinely required.

## Pipeline

MISSION → PREFLIGHT → INSPECT → CLASSIFY → DETERMINISTIC ACTIONS → BUILD/TEST → FAILURE ANALYSIS → SAFE RECOVERY → VERIFY → AI ESCALATION (when required) → REPAIR → RETEST → REVERIFY → ARTIFACT VALIDATION → DELIVERY / CONCRETE BLOCKER

## Layers

- **Core orchestration:** `.github/workflows/veytrix-autopilot.yml`
- **Deterministic inspection:** `scripts/deterministic_inspect.sh`
- **Failure classification:** `scripts/classify_failure.sh`
- **Deterministic verification:** `scripts/deterministic_verify.sh`
- **AI escalation:** `openai/codex-action@v1`, isolated to the target checkout
- **Repository adapter boundary:** workflow inputs `target_repository` + `branch`
- **Self-test:** `.github/workflows/veytrix-autopilot-self-test.yml`
- **Mobile control plane:** `android/autopilot`

## Safety invariants

1. No secrets are stored in source files.
2. Target code is checked out separately from the Veytrix core.
3. Retries are bounded.
4. Unknown failures are not blindly rerun.
5. AI does not commit or push; the outer orchestration performs bounded delivery.
6. Verification is required before a success claim.
7. NexusNova product/business code is not part of the Veytrix core.
