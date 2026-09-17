# Forensic source move — NexusNova Autopilot predecessor → VEYTRIX

Date: 2026-09-17
Source repository: `fahadsoomro123/nexusnova-app`
Target repository: `fahadsoomro123/veytrix-autopilot`
Target working branch: `implementation/preview-to-android-pass-1`

## 1. Previous project identity confirmed

The VEYTRIX predecessor was not discovered from the current `VEYTRIX` name alone. The historical identity is explicitly recorded in the NexusNova history as **NexusNova Autopilot**.

Evidence: commit `5225f8c77d64840615aa1efebcfa186be73117da` is titled `feat: add NexusNova autonomous CI repair autopilot` and adds `.github/workflows/nexusnova-autopilot.yml`.

The Android predecessor also used the identity `com.nexusnova.autopilot` and the workflow `nexusnova-autopilot.yml`.

## 2. Exact main-branch predecessor scope

The complete autopilot development range is `1f16fe1b63348cadbac7bc551ad97ec9e97d33d9` → `7a0309265e9e85739e35cd662ee2f2ee895ecc97` (26 commits). The repository comparison shows exactly 11 touched paths:

| Source path | Source evidence | VEYTRIX disposition |
|---|---|---|
| `.github/workflows/nexusnova-autopilot.yml` | autopilot history; current blob SHA `f17f1449b83eee96038fd3d4cf1390d621e4b37a` | Adapted to `.github/workflows/veytrix-autopilot.yml`; current implementation exists on target branch |
| `.github/workflows/nexusnova-autopilot-self-test.yml` | current blob SHA `218940c0707b027bdd923262ec4b52e03965737c` | Adapted to `.github/workflows/veytrix-autopilot-self-test.yml` |
| `.autopilot-live-test` | current blob SHA `c90f05a84db89ab9efde3ec8af064b857e90afe5` | Disposable verification marker; not carried forward |
| `autopilot-e2e-test/trigger.txt` | autopilot history | Adapted and retained as `autopilot-e2e-test/trigger.txt` |
| `NexusNovaAndroid/autopilot/build.gradle.kts` | current blob SHA `a57e275cd14fdb662606962ff4b399b6327f4f0f` | Adapted to `android/autopilot/build.gradle.kts` |
| `NexusNovaAndroid/autopilot/proguard-rules.pro` | current blob SHA `be44c6fb73babce02ac63e6fe1a16a5a1dfb4f44` | Adapted to `android/autopilot/proguard-rules.pro` |
| `NexusNovaAndroid/autopilot/src/main/AndroidManifest.xml` | current blob SHA `3018f0b93614001db5be9788f8c3a414fcc6b0d1` | Adapted to `android/autopilot/src/main/AndroidManifest.xml` |
| `NexusNovaAndroid/autopilot/src/main/java/com/nexusnova/autopilot/MainActivity.java` | current blob SHA `1cd9a6a644bbb4104be20cf66fd404acea7ecf64` | Historical functional client was adapted on `migration/forensic-smart-autopilot`; active implementation branch intentionally replaced the host with the approved exact-preview WebView shell |
| `NexusNovaAndroid/autopilot/src/main/res/values/styles.xml` | current tree evidence | Adapted to `android/autopilot/src/main/res/values/styles.xml` |
| `NexusNovaAndroid/settings.gradle.kts` | current blob SHA `6e8b3d6421d5cf1d03f72448b70fb731d7d13e3c` | Standalone VEYTRIX Android settings recreated; NexusNova parent integration not copied |
| `.github/workflows/build-nexusnova-autopilot-apk.yml` | current blob SHA `0a28dfe0e5d1f8f326de88a121723f998aac8240` | Replaced by `.github/workflows/build-android.yml`; VEYTRIX signing secrets/package identity are separate |

## 3. Branch-only predecessor scope

The NexusNova branch `autopilot/smoke-test` is two commits ahead of its merge base and adds exactly two autopilot-specific paths:

- `.github/workflows/nexusnova-autopilot-smoke.yml` (SHA `68b880a2f4deebf2bcca9064e95e6578e6d958bf`)
- `autopilot-smoke/trigger.txt` (SHA `fb26b94e42f76f17ab5affcb3e87a133de9c9bb5`)

Its deterministic transient-recovery behavior is superseded by the maintained VEYTRIX self-test workflow and `autopilot-e2e-test/trigger.txt`. The source branch itself is not deleted by this move because the available GitHub connection exposes branch-ref updates but not a branch-delete operation.

The other autopilot-named NexusNova branch, `feat/autopilot-free-first-flagship`, has no unique changes relative to the current NexusNova main; it is behind main.

## 4. Destination state verified before source cleanup

The target branch already contains the standalone VEYTRIX Android module, exact-preview packaging, deterministic engine scripts, recovery helpers, and migration evidence. The target Android package identity is `com.veytrix.autopilot`.

Target evidence checked:

- `android/autopilot/build.gradle.kts` SHA `11ead83a2f09de1c05daf1d155f5960f4ad2c6fc`
- `android/autopilot/src/main/AndroidManifest.xml` SHA `32650dda1a115549abcb8b4ae1aa819b4dca2cd8`
- `android/autopilot/src/main/java/com/veytrix/autopilot/MainActivity.java` SHA `af86c0af53a0c68e1579a92d8f8d50283ba0f06f`
- `android/autopilot/src/main/assets/veytrix_preview.html` present and populated from the approved preview SHA `4c256667400e0f0599f2c9322ee482a0bea8eb39`
- `scripts/` contains the deterministic inspection, verification, engine decision, failure classification, target CI recovery, Gemini-free, and artifact verification helpers

## 5. Safety boundary

No NexusNova business/product code is part of this move. The old Android parent settings/build files and signing workflow are not bulk-copied because they are coupled to the NexusNova application and secrets. Only the confirmed Autopilot predecessor surface is in scope.

No signing secret, keystore, API credential, token, or private credential is copied into VEYTRIX.

## 6. Known follow-up

The active implementation-branch `.github/workflows/veytrix-autopilot.yml` still contains a historical NexusNova default target/reference inherited from an intermediate migration pass. This is a separate correctness cleanup and must be repaired before treating the workflow configuration itself as fully VEYTRIX-pure. The current approved Android preview shell is not overwritten as part of source cleanup.
