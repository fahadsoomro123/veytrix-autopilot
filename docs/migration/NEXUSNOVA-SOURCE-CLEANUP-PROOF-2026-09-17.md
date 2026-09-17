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
- NexusNova `feat/autopilot-free-first-flagship`: `3b975846fe4c23201169f38f17121a8585cb65b5`
  - Parent: `15605267e839cf6aa5886ad001c19ccd5cd942f2`
  - Removed the 8 confirmed Autopilot predecessor files present on that older branch and removed only its `include(":autopilot")` parent-project line.

## Historical source scope

The exact mainline Autopilot development range `1f16fe1b63348cadbac7bc551ad97ec9e97d33d9` → `7a0309265e9e85739e35cd662ee2f2ee895ecc97` changed exactly these 11 paths: the old Autopilot workflow, self-test, build workflow, E2E fixture, five Android Autopilot module files, the Android parent settings, and the disposable `.autopilot-live-test` marker.

The `autopilot/smoke-test` branch added the additional smoke-only workflow and trigger. The `feat/autopilot-free-first-flagship` branch had no unique commits relative to NexusNova `main`, but it retained the inherited Autopilot files and was therefore cleaned separately.

## Post-cleanup proof

NexusNova `main`:

- `.github/workflows/nexusnova-autopilot.yml` returns HTTP 404.
- `.github/workflows/build-nexusnova-autopilot-apk.yml` returns HTTP 404.
- `NexusNovaAndroid/autopilot/` returns HTTP 404.
- `NexusNovaAndroid/settings.gradle.kts` contains only `:app` and `:tracker` includes; the Autopilot include is gone.
- Code search for `NexusNova Autopilot` returns no indexed result.
- Code search for `com.nexusnova.autopilot` returns no indexed result.

NexusNova `feat/autopilot-free-first-flagship` was also verified before cleanup to contain the old Autopilot workflow/build workflow/module. After cleanup commit `3b975846fe4c23201169f38f17121a8585cb65b5`, the branch points to a tree with those files removed. The branch ref itself remains because the active GitHub connection does not expose a branch-delete operation.

## What was migrated vs. what was intentionally recreated

The confirmed predecessor capabilities already exist in the separate VEYTRIX repository rather than being blindly copied into the production preview implementation:

- `.github/workflows/nexusnova-autopilot.yml` → adapted VEYTRIX Autopilot orchestration
- `.github/workflows/nexusnova-autopilot-self-test.yml` → adapted VEYTRIX self-test
- `autopilot-e2e-test/trigger.txt` → adapted VEYTRIX fixture
- Old Android `MainActivity.java` → adapted functional Veytrix client on `migration/forensic-smart-autopilot`
- Old Android manifest/build/proguard/styles → adapted under `android/autopilot`
- NexusNova Android parent settings → standalone `android/settings.gradle.kts`
- NexusNova signing workflow → replaced by Veytrix `build-android.yml` with separate `VEYTRIX_*` signing secrets

The active implementation branch intentionally uses the approved exact-preview WebView shell, so the historical full Android client was not allowed to overwrite the approved UI host.

## Security boundary

No NexusNova business/product code was moved into VEYTRIX. No signing secret, keystore, API credential, token, or private credential was copied.

The NexusNova product application, Firebase/web assets, travel/product workflows, and unrelated CI material were not removed by these cleanup commits.

## VEYTRIX identity cleanup

The stale NexusNova repository/workflow references that were present in the active VEYTRIX implementation workflow were corrected in VEYTRIX commit `47d1c49a01bab4f0508d5a3413def25f8cdff2ff`. The duplicate legacy `veytrix-autopilot.yml` implementation-branch workflow was subsequently removed in commit `41a05315efc899d124ccbddb6b631c8656531ddc`, leaving the central `main` recovery controller as the recovery path. The Android contract verifier deliberately ignores the approved immutable preview fixture when checking for historical repository identity; the preview itself is locked by its approved Git blob hash.
