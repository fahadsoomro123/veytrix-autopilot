# Smart Autopilot Dependency Map

## Runtime dependency graph

```text
Veytrix workflow
  ├─ workflow_dispatch
  │    ├─ target_repository
  │    ├─ branch
  │    ├─ mission
  │    └─ engine
  ├─ deterministic_inspect.sh
  ├─ deterministic_verify.sh
  ├─ classify_failure.sh
  ├─ target_ci_recovery.sh
  └─ AI escalation (optional)
       └─ openai/codex-action@v1

Android control plane
  ├─ GitHub REST API
  ├─ Android Keystore / AES-GCM token storage
  ├─ Veytrix workflow_dispatch
  ├─ workflow-run polling
  └─ artifact lookup

Build layer
  ├─ Android Gradle Plugin 8.13.2
  ├─ Gradle 8.13
  ├─ Java 17
  └─ AndroidX core/appcompat
```

## NexusNova-only dependency boundary

The source implementation's parent Android root (`NexusNovaAndroid/settings.gradle.kts`, parent build plugins, project properties, and signed-release workflow) is coupled to the NexusNova application and/or signing environment. Those dependencies are not imported into the Veytrix core.

The target checkout is deliberately isolated from `.veytrix-core` so the same Veytrix workflow can operate against other repositories without moving their product/business code into Veytrix.
