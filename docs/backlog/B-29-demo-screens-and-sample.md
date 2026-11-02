---
id: B-29
title: "Demo screens: sign in, the list, settings, day and night, and a sample app"
status: wip
priority: P2
size: M
stage: stage-3-screens
blocked_by: [B-13, B-14, B-19, B-21, B-27]
---

# B-29 — Demo screens: sign in, the list, settings, day and night, and a sample app

The Screens page assembles three phone screens from library components only, in all three themes
(Ink & Gouache day and night on the gouache tile, Bright Cel on a flat backdrop), plus one
1280 × 800 desktop layout. That is the check that the components compose.

- **The decision and its reason.** Rebuild the three screens as parity fixtures at 390×844, then as
  a desktop sample app that runs them live, with swipe-to-dismiss and the snackbar undo.
- The desktop layout is a fixture too: same components, wider props, no bottom bar.

- AC: nine phone fixtures and the desktop fixture within the floor; the sample app runs on desktop.
- Anchors: `reference/design-system/toon-ui Screens.dc.html`, `reference/design-system/toon-ui Kit.dc.html`.
- The screens stand on `ToonStage` (B-13), which draws the gouache tile in both Ink & Gouache themes
  and Bright Cel's flat backdrop; B-13's acceptance on the Screens fixtures is checked here.
