---
id: B-29
title: "Demo screens: sign in, the list, settings, day and night, and a sample app"
status: done
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

## Findings (2026-11-02)

- **The references are the page itself.** `scripts/design-references.mjs` opens `toon-ui
  Screens.dc.html` in Chrome, where the design's runtime (`support.js`, the kit over the engine)
  mounts the components, with `prefers-reduced-motion` so the kit puts every animation at its last
  frame and stops the idle loops, and cuts out each `[data-screen-label]`. A second render matches
  byte for byte.
- **The fixtures** (`ScreensFixtures.kt`) build the nine phone screens and the 1280 × 800 desktop
  layout from the library's components at the page's places, on `ToonStage`, without the entrances
  (the same last frame). By area they are within 1.54–3.11 % of the page, nearly all of it the
  page's text set by CoreText against Skia; in ink 9.96 % at worst, under the Divider's floor
  (14.81 %), so they are held to it. Two of the page's CSS habits are modelled (service quirks).
- **B-13's acceptance** holds here: both Ink & Gouache rows stand on the gouache tile, Bright Cel on
  its flat backdrop.
- **The sample app** (`sample`, `./gradlew :sample:run`) runs the three screens live: sign in,
  dismiss and undo, the settings' theme switch, sign out; `SampleAppTest` drives that path, and it
  was started on this mac. The library gains `ToonTypeSpec.textStyle` for a screen's own text.
- **Mutants:** a button moved 6 dp fails three goldens, a wrong radio one; dropping Undo, a wrong
  destination or removing the wrong card fails the app's test.
- **A wrong claim corrected:** B-35's findings said viddik's default verification missed a backing
  face's scale. The harness counted failures with `grep -o`, which cannot see a `<failure>` on the
  line after its test case; re-run, the goldens fail. Corrected in B-35.
