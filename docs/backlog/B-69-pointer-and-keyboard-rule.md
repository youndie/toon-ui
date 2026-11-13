---
id: B-69
title: "One hover, focus and target rule for the whole kit"
status: done
priority: P1
size: L
stage: stage-5-desktop
epic: feature-components
blocked_by: [B-68]
---

# B-69 — One hover, focus and target rule for the whole kit

The kit draws no hover at all and its targets are sized for a finger. A desktop app needs both, and
the desktop design's `Pointer & Keyboard` page fixes one rule for every component.

- **The decision and its reason.** Apply the page's rule to every existing component, so a desktop
  app gets it without a flag per component:
  - **Hover:** a body with a hard shadow lifts −2, −2 while its shadow stays (4 → 6); a press drops the
    shadow to 0. A flat element gets the `surface2` backdrop instead of a lift. In 80 ms on the kit's
    press curve, out in 120 ms; reduce motion: instant. In a reading area the backdrop is always
    instant.
  - **Focus:** the kit's dashed ring on the `focus` token; a field's ink also thickens ×1.5.
  - **Targets:** at least 28 dp; icon button 32, control 40, list row 56, navigation row 44.
- Hover only on a pointer that hovers; touch keeps today's look. No action exists only on hover.
- Rejected: a hover parameter per component — the page is one rule, and per-component switches drift.

- AC: every component that is pressable shows the rule's hover, measured against the page's renderer
  output; focus is visible in all three themes; a test holds the target sizes.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/interaction/ToonHover.kt`,
  `toon-core/src/commonMain/kotlin/io/github/youndie/toon/interaction/ToonPress.kt`, `reference/desktop/`.

## Findings, 2026-11-13

- **Scope, read off the AC.** "Measured against the page's renderer output" can hold only what the
  page draws: section 01 draws the kit's filled and text button, icon button, chip, card, slider,
  menu and list row. Those, the outlined button and the FAB (the rule's "body with a hard shadow" in
  so many words) have the rule here. The eight pressables the page leaves out — checkbox, radio,
  switch, segmented, tabs, navigation bar, drawer, text field — need a look it does not give, and are
  B-79.
- **The mechanism** (research D14): `ToonHoverState` with `toonHoverable`, a mouse only, eased in
  80 ms and out 120 on the press curve from where it is, instant under reduce motion; `hoverLifted`,
  the extension's `hoverLift`; `toonPlateLayer`, its `plate`. Each component carries `hv` in its look
  and applies the rule at the end of its own scene, so its frame test sees it.
- **What each draws hovered**: button filled and outlined, icon button, FAB, chip, card — the whole
  body and its words 2 dp up and left, the shadow kept, the lift fading as a press drops the shadow
  (the extension's `liftBody` has that factor, its `hoverLift` none; at rest the two agree). Text
  button — a plate where its press blob goes, as the page's `under(tbtn(), 8, 5, 134, 42)`. Slider —
  the ball only, as the page's own row. List row and menu item — the selected shade, as the page draws
  them, each menu item on its own ramp.
- **The page's chip and card rows are placed at 0, 0**, and the engine's `hoverLift` then reads every
  ink layer as a shadow, keeping their outline in place under a lifted fill. The port reads the
  layer's own offset, the page's words; the goldens lift the bare renderer and place it after, as the
  kit's own chip and card cells are drawn.
- **Focus**: every pressable drew the kit's ring already, in all three themes (each component's
  `Focused` cells), except the card. It now takes keyboard focus (Enter or Space presses it at its
  middle) and draws the page's ring, 6 dp out (`Focus` cells, three themes). Hovered and focused at
  once, the ring lifts with the body and stays visible (`Hover_Button_Filled_Focused_*`).
- **Targets**: rule 4 leaves the kit's components their 48–52 dp, which `ToonAccessibilityTest` holds;
  the pointer sizes are `ToonPointerTarget`, held to the extension's `DESKTOP.pointer` with the hover
  timings (`ToonHoverTest`, from `manifest.json`).
- **A frame late.** The enter bumps the ramp's key; its loop starts on the next frame and moves on the
  one after, so a hover begins one to two frames after the pointer arrives. Nothing here needs it
  sooner; recorded so the reading-zone rule (instant, B-78) is built knowing it.
- The live slider and menu have departed (B-58, B-61), so their hover cells are listed with them in
  `ToonDepartures` and held as designed, as their own frame tests hold theirs.

- [x] Every pressable component the page draws shows the rule's hover, held to the page's renderer
  output: 42 cells in `desktop.json`'s `Hover`, worst coordinate within the engine's rounding.
- [x] Focus is visible in all three themes: the kit's `Focused` cells, and the card's new ring.
- [x] A test holds the target sizes: `ToonHoverTest` against the extension; the kit's 48 dp by
  `ToonAccessibilityTest`.
