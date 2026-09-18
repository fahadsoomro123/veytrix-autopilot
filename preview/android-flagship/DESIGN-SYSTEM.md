# VEYTRIX Android Flagship Preview — Design System

## North star

**Premium hardware × AI intelligence × autonomous engineering × precision control.**

The app should feel like a high-end instrument, not a SaaS dashboard, crypto terminal, gaming HUD or neon cyberpunk interface.

## Viewport

- Primary reference: 390 × 844 dp-class portrait phone viewport.
- Desktop presentation is only a preview frame around the phone; the app itself remains portrait-first.
- Primary screens are composed to avoid whole-screen scrolling.
- Edge-to-edge is expected in production with system-bar insets handled explicitly.

## Navigation

### Top bar

- Compact VEYTRIX identity.
- Hamburger opens a mobile drawer; never a permanent desktop sidebar.
- Right-side state pill: READY / ACTIVE / MISSION / RECOVERY / VERIFIED.

### Bottom navigation

Four top-level destinations:

- Home — mission composer and current system state.
- Activity — mission history and recent state transitions.
- Results — verified outputs, artifacts and delivery outcomes.
- Control — account, preferences and system controls.

### Mobile drawer

Secondary destinations:

- Command home
- Activity & history
- Results & artifacts
- Settings
- Security
- System information

## Home composition

1. Smart Autopilot label.
2. Clear intent headline: what should VEYTRIX handle?
3. Dimensional autonomous core visualization.
4. Large multiline command composer.
5. Context + voice actions.
6. Prominent submit affordance.
7. Small suggested commands.
8. Compact target / engine context.

The prompt composer is the main control surface and must never be reduced to a generic single-line field.

## Autopilot state composition

State progression:

**Prompt received → Understanding → Planning → Executing → Verifying → Completed**

Each state must answer:

- What VEYTRIX is doing now.
- Why it is doing it.
- What comes next.
- Whether the current state is waiting, active, blocked or verified.

Raw technical logs stay behind a secondary detail action.

## Result composition

A completed mission shows:

- requested mission,
- target repository / branch context,
- execution outcome,
- verification status,
- compact evidence metrics,
- next action.

A completion state is never shown without a verification concept.

## Recovery composition

Failure is expressed as a bounded engineering state, not as a generic alert.

Show:

- reason class,
- whether AI was invoked,
- retry budget,
- execution boundary,
- the next safe action.

Unknown failures must not look equivalent to recognized transient recoveries.

## Material / color tokens

### Core

- Obsidian: `#070807`
- Graphite: `#10110F`
- Elevated surface: `#171815`
- Higher surface: `#1E1F1B`
- Hairline: `rgba(244,239,225,.11)`
- Strong divider: `rgba(244,239,225,.18)`
- Warm ivory: `#F1EEE6`
- Muted text: `#9D9A91`
- Deep muted text: `#74736E`

### Signature accent

Champagne / brushed metal:

- `#D9BD8C`
- `#A6875A`

### Semantic state colors

Use only for meaning, never as the overall theme:

- Verified / healthy: `#93B78A`
- Attention / recovery: `#D1AA73`
- Failure / destructive: `#C77C70`

No neon blue, cyan glow, rainbow gradient or full-screen color wash.

## Shape

- Phone frame: large 44–46 px corner radius.
- Primary composer: ~22 px radius.
- Cards: 15–18 px radius.
- Compact controls: 11–14 px radius.
- Pills: 999 px radius.

Shapes should feel engineered and consistent rather than bubbly or playful.

## Depth

Use three layers:

1. Base matte canvas.
2. Raised opaque control surfaces.
3. Rare translucent/highlighted overlays for state or drawer depth.

Avoid blanket glassmorphism. Do not put every module on a floating glass card.

## Typography

Use a system sans / Android-native type family in production. Visual hierarchy is compact but decisive:

- Eyebrow: 8–10 px, tracked uppercase.
- Primary headline: ~24 px, tight leading, strong weight.
- Body: 10–12 px with generous line-height.
- Control labels: 8–10 px.
- Dense telemetry: use sparingly; never make the core workflow microscopic.

## Motion

Motion communicates state:

- spring-like press response on tactile controls,
- subtle floating/orbiting motion for the autonomous core,
- cross-fade / 5–8 px vertical transition between screens,
- progressive timeline completion,
- bounded recovery feedback.

No constant particle fields, aggressive zooms, rotating HUDs or decorative animation that competes with the command.

## Touch

Primary controls must remain thumb-friendly and visually isolated. The submit control, hamburger, bottom navigation and recovery CTA are all dedicated touch targets with visible pressed states.

## Component inventory

- Android top bar
- status pill
- autonomous core visualization
- multiline prompt composer
- context chip
- voice control
- primary submit control
- suggestion pill
- target / engine mini-card
- mission list item
- status dot
- progress module
- execution timeline
- evidence / result module
- recovery module
- mobile drawer
- bottom navigation
- settings row
- toggle
- toast
- detail back control

## Implementation boundary

The preview is isolated. It must not modify:

- backend / orchestration logic,
- AI router behavior,
- APIs,
- data models,
- security controls,
- GitHub workflows,
- existing functional behavior.

Production implementation begins only after visual approval.
