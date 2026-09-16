# VEYTRIX — Flagship Preview Research Record

## Scope

This branch is **PREVIEW ONLY**. No production Android implementation, backend, AI router, workflow, APK, or integration changes are included.

The research pass deliberately mixed shipped products, platform guidance, award programs, mobile UX references, agent products, DevOps tooling, and prototyping systems. The accessible research environment did not permit truthful inspection of 1009 distinct shipped apps, so the count is not fabricated.

### Approximate directly inspected reference set

**20+ high-quality product/design references and source pages**, including:

- Apple Design Awards 2025 and 2026 award material
- Google Material 3 Expressive research and Android guidance
- GitHub Mobile
- Cursor agent/mobile experience
- Linear Mobile redesign
- Raycast command/action model
- Figma mobile/prototyping/variables
- Notion mobile
- Things 3
- GitLab Mobile DevOps
- Vercel dashboard concepts
- Mobbin real-product reference library
- Superhuman mobile command navigation
- Framer interactive 3D glass reference
- Figma Mobile UI Kit
- Figma mobile app
- GitLab DevOps/build/signing guidance
- additional official product and interaction references surfaced during research

Secondary reference coverage also included real-product UI libraries and design publications. The number above refers to source/product references actually inspected, not a claim of 1009 app teardowns.

## Patterns extracted

### Command / control
- Make one dominant action obvious; keep supporting telemetry subordinate.
- Treat search/command as a first-class interaction, not a buried settings feature.
- Use contextual actions rather than permanently exposing every possible command.

### Agentic work
- Mobile is strongest as a control/review surface: select target, launch work, follow state, inspect diffs/evidence, approve delivery.
- Separate intent, plan, execution, recovery and verification so the system feels autonomous without becoming opaque.

### Navigation
- Bottom navigation works when the core destinations are few and high-frequency.
- Contextual top controls and command surfaces can reduce navigation depth.
- One-handed access and large touch targets matter more than desktop-style density.

### Status / verification
- Status should be explicit: READY, ACTIVE, WAITING, FAILED, RECOVERING, VERIFIED.
- Verification needs an evidence model, not just a green checkmark.
- Timelines are useful for traceability while detailed logs should remain a secondary surface.

### Visual language
- Dark interfaces work best when contrast is structured rather than uniformly bright.
- Accent color should communicate state/action, not paint every surface.
- Selective translucency creates hierarchy and depth; blanket glassmorphism reduces readability.
- Strong typography, spacing and containment can guide attention faster than decoration.

### Spatial / 3D
- Apple award material demonstrates that 3D is strongest when placement, size, controls and function are considered together.
- Depth, lighting and motion are useful when they encode state or hierarchy.
- VEYTRIX therefore uses one dimensional autonomous-engine centerpiece plus restrained artifact/depth cues rather than random sci-fi objects.

### Motion
- Motion should explain state transitions, progress, repair and verification.
- Spring-like movement and subtle morphing can make state changes legible without turning the interface into a game.

## VEYTRIX synthesis

**Position:** Autonomous Engineering Control + AI-native operating system + premium mobile command center.

**Base:** obsidian / graphite / deep blue-black.

**Accent:** electric violet with restrained cyan secondary signal.

**Semantic colors:** green for verified/healthy, amber for attention/recovery, red for destructive/failure, neutral blue-white for information.

**Material:** opaque command surfaces first; selective translucent elevated layers; soft edge highlights; restrained bloom; dimensional centerpiece only where it explains autonomy/state.

**Typography:** compact technical labels + strong display hierarchy + readable body copy. Avoid dashboard microtext.

**Interaction model:** command → mission → live state → evidence → artifact → delivery gate.

## Preview information architecture

The preview currently includes **17 meaningful screens**:

1. Command Center
2. New Mission
3. Mission Configuration
4. Live Autopilot
5. Execution Timeline
6. Failure / Recovery Detail
7. Repositories
8. Repository Detail
9. Artifacts
10. Artifact Detail
11. Verification
12. Mission History
13. Mission Detail
14. AI Engine / Router
15. Settings
16. Security
17. System / About

## Live interaction coverage

The isolated HTML prototype uses local mock data and supports:

- navigation between all major surfaces
- New Mission → Live Autopilot
- Mission Configuration → Live Autopilot
- mission state visualization
- execution timeline inspection
- failure/recovery inspection
- repository → repository detail
- artifact → artifact detail
- history → mission detail
- AI routing visualization
- security/settings/about surfaces
- mock delivery gate and command feedback
- responsive layouts for 390×844 and narrow 320px-class widths

## QA intent

The preview is deliberately built as a single-purpose-per-screen mobile control system rather than an endless dashboard. No production Android code was changed by this preview branch.

## Research source notes

Apple's 2025 Design Awards highlight innovation, interaction, accessibility, strong visuals and meaningful 3D/spatial interaction; the 2026 awards continue the spatial/immersive direction. Google Material 3 Expressive emphasizes color, shape, size, motion and containment, with research across 46 studies and more than 18,000 participants. GitHub Mobile emphasizes review, monitoring and agent collaboration. Cursor's current mobile experience explicitly centers directing agents, observing work, reviewing changes and merging rather than editing code. Linear's mobile redesign uses a custom frosted material and bottom toolbar. Raycast's command model elevates search/action as a primary surface. Figma's current prototyping system supports variables, conditional logic and responsive prototype playback.
