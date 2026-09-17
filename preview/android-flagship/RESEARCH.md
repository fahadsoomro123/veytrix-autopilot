# VEYTRIX Android Flagship Preview — Research

## Product grounding

The repository describes VEYTRIX as a deterministic-first engineering control plane: mission intake, preflight, inspection, deterministic execution, build/test, failure analysis, safe recovery, verification, conditional AI escalation, repair/retest/reverification, artifact validation, and delivery or a concrete blocker. The Android control app is the mobile control plane. This preview therefore treats the phone as a mission-control/review surface rather than as a generic analytics dashboard.

The current Android implementation target is an Android application package using `com.veytrix.autopilot`, minSdk 26, targetSdk 35, with the existing first-pass implementation using a WebView-hosted preview asset. This design pass does not alter that production implementation.

## Current Android / flagship patterns reviewed

### Android platform

- Android's current guidance emphasizes edge-to-edge layouts, transparent/translucent system bars, correct window insets, and deliberate handling of cutouts and gesture areas.
- Android 16 continues the move toward enforced edge-to-edge behavior for apps targeting API 36.
- Material 3 / Material 3 Expressive extends the Android design system with richer theming, typography, motion, shape and interaction treatment; the current Compose guidance explicitly positions it as aligned with Android 16's visual direction.
- Bottom navigation is intended for a small set of top-level destinations (three to five), while drawers/navigation panels are more appropriate as secondary or larger-screen navigation.

### Agentic mobile products

- GitHub Mobile's 2025–2026 agent work makes agent sessions first-class on mobile: launch a task, observe real-time state, inspect session logs, stop a running session, review a pull request, and act on recovery/fixes.
- Cursor's current mobile product centers on directing agents, continuing prompts, observing state, reviewing artifacts/diffs and shipping from a phone; voice input and real-time status are also part of the mobile interaction model.
- These patterns support a VEYTRIX mobile model where the user supplies intent first, then mainly supervises state, evidence and outcomes.

### Command surfaces

- Raycast's interaction model treats search/command as the central interaction surface and consolidates contextual actions into an action bar.
- Linear's 2025 Android/iOS redesign added a stronger bottom toolbar and a more dimensional/frosted surface treatment, while keeping a create action prominent.

## Synthesis for VEYTRIX

1. **Prompt is the hero interaction.** The main screen opens around a large multiline mission composer rather than telemetry cards.
2. **Autonomy is visually staged.** The flow is represented as Understanding → Planning → Executing → Verifying → Completed so the user can see what the system is doing without reading raw logs.
3. **Evidence is a product surface.** Completion uses verification language and evidence framing instead of an unqualified green success badge.
4. **Primary navigation stays mobile-sized.** Four bottom destinations are used: Home, Activity, Results, Control. Secondary system destinations live in the hamburger drawer.
5. **Detail is intentionally secondary.** Detailed execution traces live in a mission-detail view; raw CI/log streams are not dumped into the main screen.
6. **3D encodes intelligence/state.** The central autonomous core uses restrained physical depth, rings, material highlights and motion to signal autonomy without becoming a gaming HUD.
7. **Material is selective.** Opaque/dimensional command surfaces dominate; translucency is restrained to preserve readability.
8. **Palette is deliberately non-generic.** Obsidian/graphite, warm ivory and champagne are the core visual language. No neon blue/cyan system is used; semantic colors appear only when state meaning requires them.
9. **Phone viewport is fixed-first.** On desktop, the preview is still presented inside a portrait Android phone composition; the preview container is not allowed to turn the UI into a desktop dashboard.

## Sources

- Android system bars: https://developer.android.com/design/ui/mobile/guides/foundations/system-bars
- Android edge-to-edge views: https://developer.android.com/develop/ui/views/layout/edge-to-edge
- Android Material 3 / Expressive: https://developer.android.com/develop/ui/compose/designsystems/material3
- Android Material 3 navigation guidance: https://developer.android.com/develop/ui/compose/designsystems/material3
- Android 16 behavior changes: https://developer.android.com/about/versions/16/behavior-changes-16
- GitHub Mobile agent sessions / logs: https://github.blog/changelog/2026-04-01-github-mobile-stay-in-flow-with-a-refreshed-copilot-tab-and-native-session-logs/
- GitHub Mobile live agent status: https://github.blog/changelog/2026-02-26-github-mobile-track-coding-agent-progress-in-real-time-with-live-notifications/
- Cursor Mobile: https://cursor.com/mobile
- Linear Mobile redesign: https://linear.app/changelog/2025-10-16-mobile-app-redesign
- Raycast interaction / command surfaces: https://www.raycast.com/blog/a-fresh-look-and-feel

## Preview conclusion

This preview is a deliberately original VEYTRIX system derived from the above interaction patterns. It does not copy any product's branding, assets or layout. The objective is a calm, premium, autonomous-engineering control experience that reads immediately as an Android app and keeps the mission composer, live state, recovery, verification and results at the center of the product.
