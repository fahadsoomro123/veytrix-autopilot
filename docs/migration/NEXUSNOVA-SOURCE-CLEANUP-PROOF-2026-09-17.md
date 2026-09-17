# NexusNova Autopilot predecessor cleanup proof

Date: 2026-09-17

The historical predecessor identity was established as `NexusNova Autopilot` from NexusNova commit `5225f8c77d64840615aa1efebcfa186be73117da`.

## Source cleanup commits

- NexusNova `main`: `96e5f6dd6f3888b9d956a3b1185c98407a0a475c`
  - Parent: `aa30c1d5cc9d13fcc5432c1ca295052c37400300`
  - Removed the 11 confirmed Autopilot predecessor paths from the mainline and removed only `include(":autopilot")` from the shared NexusNova Android settings.
- NexusNova `autopilot/smoke-test`: `107c3079c12f3cd37c5e6d79fb3957c502c8b2f4`
  - Parent: `909a9aa91577a68ab642500a3717b5a127ab9515`
  - Removed the confirmed Autopilot/smoke predecessor paths from that stale branch and removed only its `:autopilot` parent-project include.

## Post-cleanup proof

- `.github/workflows/nexusnova-autopilot.yml` on NexusNova `main` returns HTTP 404.
- `.github/workflows/build-nexusnova-autopilot-apk.yml` on NexusNova `main` returns HTTP 404.
- `NexusNovaAndroid/autopilot/` on NexusNova `main` returns HTTP 404.
- NexusNova `main` `NexusNovaAndroid/settings.gradle.kts` now contains only `:app` and `:tracker` includes; the Autopilot include is gone.
- GitHub code search on NexusNova `main` for `NexusNova Autopilot` returns no indexed result.
- GitHub code search on NexusNova `main` for `com.nexusnova.autopilot` returns no indexed result.

## Not removed

The NexusNova product application, Firebase/web assets, travel/product workflows, and other unrelated NexusNova material were not touched by these cleanup commits.

The `feat/autopilot-free-first-flagship` branch was not rewritten because it has no unique changes relative to current NexusNova `main` and remains a historical branch reference. Branch deletion is not available through the active GitHub connection.

## Important implementation note

The active VEYTRIX implementation branch contains the exact-preview Android shell. The earlier VEYTRIX migration branch `migration/forensic-smart-autopilot` retains the adapted functional Android mission-runner implementation. The historical NexusNova client was therefore not blindly copied over the approved preview host.
