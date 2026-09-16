# Exact Source → Target Migration Matrix

| Source | Target | Method | Reason | Status |
|---|---|---|---|---|
| `.github/workflows/nexusnova-autopilot.yml` | `.github/workflows/veytrix-autopilot.yml` | Adapted standalone workflow | Reusable orchestration; target checkout isolated; NexusNova hardcoding removed | MIGRATED + IMPROVED |
| `.github/workflows/nexusnova-autopilot-self-test.yml` | `.github/workflows/veytrix-autopilot-self-test.yml` | Adapted self-test | Reusable deterministic recovery probe | MIGRATED |
| `autopilot-e2e-test/trigger.txt` | `autopilot-e2e-test/trigger.txt` | Adapted fixture text | Keeps self-test fixture while removing product-specific naming | MIGRATED |
| `NexusNovaAndroid/autopilot/src/main/java/com/nexusnova/autopilot/MainActivity.java` | `android/autopilot/src/main/java/com/veytrix/autopilot/MainActivity.java` | Adapted client | Preserves token encryption, GitHub API, run tracking, artifacts, history, voice, templates and theme; target repo becomes input | PLANNED IN TARGET |
| `NexusNovaAndroid/autopilot/src/main/AndroidManifest.xml` | `android/autopilot/src/main/AndroidManifest.xml` | Adapted manifest | Veytrix package/app identity | PLANNED IN TARGET |
| `NexusNovaAndroid/autopilot/src/main/res/values/styles.xml` | `android/autopilot/src/main/res/values/styles.xml` | Adapted theme | Veytrix resource identity | PLANNED IN TARGET |
| `NexusNovaAndroid/autopilot/build.gradle.kts` | `android/autopilot/build.gradle.kts` | Adapted module build | Remove NexusNova package/signing coupling | PLANNED IN TARGET |
| `NexusNovaAndroid/autopilot/proguard-rules.pro` | `android/autopilot/proguard-rules.pro` | Migrated statement | Same current requirement | PLANNED IN TARGET |
| `NexusNovaAndroid/settings.gradle.kts` | `android/settings.gradle.kts` | Recreated standalone root | Source file is NexusNova parent integration, not reusable core | INTENTIONALLY REPLACED |
| `NexusNovaAndroid/build.gradle.kts` | `android/build.gradle.kts` | Recreated minimal root plugin setup | Avoid importing unrelated Kotlin/Google services plugins | INTENTIONALLY REPLACED |
| `.github/workflows/build-nexusnova-autopilot-apk.yml` | `.github/workflows/build-android.yml` | Recreated safe build workflow | Source workflow is bound to NexusNova signing secrets/package and must not be copied directly | INTENTIONALLY REPLACED / SECURITY BOUNDARY |
| `NexusNovaAndroid/gradle.properties` | `android/gradle.properties` | Minimal equivalent | Only Android/Gradle properties required by the standalone app | INTENTIONALLY REPLACED |

## Mapping rule

No directory was bulk-copied. Each target file has a documented source path, adaptation method, and verification plan. Source remains unchanged and is still the reference implementation until the live verification stage passes.
