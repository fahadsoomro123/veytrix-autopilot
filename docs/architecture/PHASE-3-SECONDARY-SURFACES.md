# VEYTRIX Phase 3 — Secondary Surfaces

Branch: `implementation/phase3-secondary-surfaces`

Phase 2 has a verified Android build and debug APK artifact. Phase 3 continues from that exact head and replaces the remaining presentation-only secondary surfaces without touching the protected 3M Mesh, OTA, or signing work.

## Repository reality

The legacy `MainActivity` still contained fake profile/completed metrics and a synthetic run-detail surface. The drawer also routed Help & Support to a toast. Voice was a visual-only placeholder.

## Phase 3 implementation

- **Profile:** only real credential/target state; no usage metrics.
- **Completed:** filters the real workflow history returned by GitHub to successful completed runs.
- **Run Details:** uses the selected real run and real GitHub artifact metadata.
- **Voice Command:** uses Android `SpeechRecognizer` when the device provides it and requests microphone permission at runtime; captured text is passed to the real mission composer and is never auto-executed.
- **Help & Support:** provides actual connection/control guidance and routes to real actions.
- **Drawer / More:** secondary destinations route to these native surfaces.

## Data integrity

No synthetic completion percentages, file counts, duration claims, workflow stages, or fabricated output are introduced.

## Out of scope

3M Free AI Mesh production integration, OTA runtime update/rollback, release signing changes, and broad primary-tab redesign remain separate phases.

## Lock condition

Phase 3 is only lockable after the contract gate, Gradle checks, debug APK build, APK identity verification, and phone review all pass.
