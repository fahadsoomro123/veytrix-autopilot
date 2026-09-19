# VEYTRIX Phase 2 — Flagship Visual Architecture

Status: `implementation/phase2-flagship-visual-architecture`

## Phase objective

Replace the current presentation structure with a data-driven, native Android visual system without mixing the secure control-plane work with the final screen rewrite.

Phase 2 is an architecture and visual-system phase. It deliberately does **not** ship a half-finished redesign of every screen in one commit.

## Repository reality gate

The current Android surface is a native Views + Canvas implementation in `MainActivity.java` and `FlagshipCoreView.java`.

The current activity inventory observed in the source is:

### Primary navigation
1. Home
2. Activity
3. Results
4. Control

### Secondary / hamburger destinations
5. Profile
6. Voice
7. Settings
8. Help & Support
9. Completed

### Detail surface
10. Mission Details

The current source also contains legacy horizontal and vertical scrolling containers. Phase 2 does not preserve those as the final primary-screen interaction model.

## Problems explicitly carried into migration

- presentation and navigation are concentrated in one large activity
- mission/activity/result rows use hardcoded demonstration data
- current UI contains blue and dark/navy presentation tokens
- multiple screens depend on `ScrollView` / `HorizontalScrollView`
- operational state is represented by presentation strings rather than a shared mission-state contract
- bottom navigation and hamburger navigation are not backed by a shared screen/state model
- the autonomous core renderer is visually capable but uses the legacy blue/black palette

These are migration targets, not assumptions about the protected Smart Autopilot and Free AI Mesh subsystems.

## Phase 2 visual contract

### Surface language

Use a bright pearl/white/silver/champagne base with controlled violet/purple/magenta/pink/rose/crimson accents.

The final visual system must not introduce:
- blue
- black as a UI surface color
- dark mode
- dark grey
- orange
- orange gradients
- arbitrary rainbow effects

### Shape language

Avoid a generic card-wall.

Primary surfaces should read as:
- engineered rails
- sculpted control bands
- instrument surfaces
- layered sheets
- verification traces
- tactile action controls

Containers exist only when they establish hierarchy, grouping, focus, or state—not because a template expects a card.

### Density and viewport rule

The primary phone screens are designed as bounded compositions, not long documents.

Target validation matrix:
- 360 x 800
- 360 x 900
- 390 x 844
- 412 x 915

For compact phone widths, the screen composition must reorganize content rather than depend on vertical or horizontal scrolling.

### Touch and text rules

- Minimum interactive hit area: 48dp.
- Body text: 15sp target.
- Main titles: 22sp target.
- Section titles: 17sp target.
- Base spacing rhythm: 8dp.

These tokens are centralized in `VeytrixDesignTokens`.

## Data contract

The visual layer must render real mission state supplied by the control plane.

The allowed mission states are:

`IDLE → PLANNING → IMPLEMENTING → BUILDING → TESTING → FAILED → REPAIRING → RETESTING → VERIFYING → VERIFIED → DELIVERED`

The UI may not synthesize:
- fake percentages
- fake run counts
- fake “live” labels
- fake artifacts
- fake booking/tracking-style activity
- fake AI model telemetry

Missing operational data must be shown as an explicit unavailable/unknown state.

## Screen architecture

### Home

Purpose: launch and orient.

Required content:
- mission composer
- current connection state
- autonomous core visualization
- one clear execution action
- compact proof/status rail

No dashboard grid.

### Activity

Purpose: monitor actual runs.

Required content:
- real run state
- target repository + branch
- latest verification event
- bounded mission history preview

The primary surface must remain viewport-bounded. Detailed history can open a dedicated detail surface.

### Results

Purpose: inspect verified outputs.

Required content:
- only artifacts that actually exist
- artifact type/name
- cryptographic identity when available
- clear verification state

No invented download sizes or file counts.

### Control

Purpose: explicit operator controls.

Required content:
- mission execution mode
- verification depth
- notification behavior
- connection / credential status
- security-sensitive controls

Model names and fallback counts must come from real configuration, not presentation defaults.

### Hamburger secondary space

Secondary features stay out of the primary four-tab composition:
- Profile
- Voice
- Settings
- Help & Support
- Completed

The hamburger is the structural escape hatch for secondary features; it must not duplicate primary navigation.

## Component strategy

Phase 2 uses small native components with one responsibility:

- `VeytrixDesignTokens`
- mission state contract
- mission snapshot contract
- screen shell / inset handler
- sculpted action control
- status/proof rail
- autonomous core renderer
- artifact fingerprint row
- navigation controller

The existing `FlagshipCoreView` may be retained as a rendering engine, but its palette and layout are migrated to the new token system rather than copied into a new renderer.

## System bars and insets

The Android project targets SDK 35. The screen shell must therefore handle edge-to-edge system-bar insets explicitly and keep interactive content out of unsafe regions.

The shell will use the actual application window bounds for composition rather than assuming a single physical phone size.

## Migration order

1. Establish visual/data contracts.
2. Establish bounded screen shell and inset handling.
3. Rebuild Home.
4. Rebuild Activity.
5. Rebuild Results.
6. Rebuild Control.
7. Rebuild secondary hamburger destinations.
8. Migrate the autonomous core palette/rendering.
9. Add instrumentation screenshots at the required phone sizes.
10. Lock Phase 2 only after compile, device/instrumentation review, screenshot review, and regression checks.

## Protected systems

Phase 2 does not delete or replace:
- deterministic failure classification/fingerprinting
- deterministic engine selection
- autonomous repair controller
- 3M Free AI Mesh research/proof work
- native-only Android contract
- Phase 1 secure credential/control-plane code

## Out of scope for this phase

- 3M Mesh production integration
- OTA update discovery/install/rollback
- release signing changes
- autonomous repair-controller redesign
- remote target execution redesign
- destructive cleanup of historical branches
