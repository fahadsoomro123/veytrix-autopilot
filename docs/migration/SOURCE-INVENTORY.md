# Smart Autopilot Source Inventory

Source: `fahadsoomro123/nexusnova-app` (`main`)
Target: `fahadsoomro123/veytrix-autopilot`

The inventory below is based on the source tree, Autopilot commit history, dependency inspection, and direct file reads. No source file was deleted or modified by this migration.

| Source path / component | Purpose | Classification | Reusable? | Target mapping | Verification |
|---|---|---|---|---|---|
| `.github/workflows/nexusnova-autopilot.yml` | Mission dispatch, failed-workflow recovery, engine selection, bounded repair/verification | A | Yes | `.github/workflows/veytrix-autopilot.yml` | YAML/static review + self-test + live run required |
| `.github/workflows/nexusnova-autopilot-self-test.yml` | Deterministic transient-failure/recovery probe | A | Yes | `.github/workflows/veytrix-autopilot-self-test.yml` | Forced first-attempt failure + bounded rerun |
| `autopilot-e2e-test/trigger.txt` | Self-test fixture proving expected repository path | A | Yes | `autopilot-e2e-test/trigger.txt` | File existence/non-empty check |
| `NexusNovaAndroid/autopilot/src/main/java/com/nexusnova/autopilot/MainActivity.java` | Mobile control plane: token protection, GitHub API, dispatch, polling, artifacts, history, templates, voice, theme | A | Yes | `android/autopilot/src/main/java/com/veytrix/autopilot/MainActivity.java` | Android compile + runtime/manual API verification |
| `NexusNovaAndroid/autopilot/src/main/AndroidManifest.xml` | Android launcher/network/security manifest | A | Yes | `android/autopilot/src/main/AndroidManifest.xml` | Android manifest/compile validation |
| `NexusNovaAndroid/autopilot/src/main/res/values/styles.xml` | Standalone Autopilot theme | A | Yes | `android/autopilot/src/main/res/values/styles.xml` | Android compile |
| `NexusNovaAndroid/autopilot/build.gradle.kts` | Autopilot Android application module build config | A | Yes | `android/autopilot/build.gradle.kts` | Gradle configuration/compile |
| `NexusNovaAndroid/autopilot/proguard-rules.pro` | Release shrinker rules; currently only a documented no-custom-rules statement | A | Yes | `android/autopilot/proguard-rules.pro` | Gradle release configuration |
| `NexusNovaAndroid/settings.gradle.kts` | Parent NexusNova Android project includes `:autopilot` | B | Partially | Not copied; replaced by Veytrix standalone Android settings | Root build validation |
| `NexusNovaAndroid/build.gradle.kts` | NexusNova parent plugin versions | B | No | Not copied; only required Android plugin configuration recreated for Veytrix | Root build validation |
| `.github/workflows/build-nexusnova-autopilot-apk.yml` | Signed NexusNova Autopilot APK build using NexusNova keystore secrets and package identity | E | Partially | Not copied directly; replaced by generic `build-android.yml` with Veytrix-safe signing boundary | CI build + artifact checks |
| `NexusNovaAndroid/gradle.properties` | Parent Android/Gradle environment defaults | B | No | Not copied; Veytrix equivalent contains only required project properties | Gradle configuration |
| Source Autopilot commit history | Evolution/provenance of the system, including free engine, AI engine, bounded repair, self-tests, mobile client, security hardening | A | Evidence only | `docs/migration/` reports | Commit-by-commit audit |

## Source evolution evidence

Autopilot was introduced in commit `5225f8c77d64840615aa1efebcfa186be73117da`, expanded with the free/AI engine in `8d722ee48c861f517af1e4b49650cd4d28c5b781`, added mobile control capabilities through the `00e465...` / `061de...` / `538ee...` family, added signed mobile packaging in `6e101...`, and was hardened with deterministic self-healing tests and bounded verification before the latest broad `workflow_run` behavior at `7a030...`.

## Important exclusions

The broader NexusNova Android app, token/mining/wallet/product UI, Firebase/business logic, and unrelated security/build workflows are outside Smart Autopilot scope and are not migrated. They remain intact in the source repository.
