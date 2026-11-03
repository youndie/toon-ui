---
id: B-60
title: "No ink seam where the tooltip's tail meets the balloon"
status: done
priority: P2
size: S
stage: stage-2-components
epic: feature-components
---

# B-60 — No ink seam where the tooltip's tail meets the balloon

The owner, in the running kit (2026-11-03): "the Tooltip's triangle needs raising a little — on the
Cel theme the joint shows". In Bright Cel a hairline of ink ran across the top of the tail's paper,
between it and the balloon's paper.

- **The decision and its reason.** The tail's paper is carried up into the balloon: its two sides
  run on straight along their tangents from y −15.5 to −19, so it overlaps the balloon's paper in
  every theme by 3 dp or more. The engine's paper stops at −15.5, and the balloon's paper ends
  12 dp + 0.8 × the theme's bottom inset above the tip: −15.2 in Ink & Gouache (inset 4), but
  −15.68 in Bright Cel (inset 4.6), which leaves a 0.18 dp band of the balloon's ink showing between
  the two papers. Everything below −15.5, the only part of the tail that shows, is still the
  engine's. A departure (D13): the tooltip cells are listed in `ToonDepartures` (`Tooltip_`).
  `toonTooltipScene` keeps `asDesigned`, which draws the engine's paper, and the cells are still held
  to the engine's frames that way (`EngineFrames.cells(…, asDesigned = true)`), so every other layer
  stays checked.
- Rejected: moving the tail's top corners up to −16 as the owner put it, "raising the triangle".
  Moving the corners alone changes the slope of both sides, so the visible tail changes shape too,
  and a fixed −16 is still within the balloon paper's wobble (±0.8 dp in Ink & Gouache) of its edge.
- AC: drawn 8× in each theme with no text, the columns over the middle of the tail are paper from
  20 dp above the tip down to 10 dp above it; drawn as designed, every tooltip cell is the engine's.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonTooltip.kt`.

## Findings (2026-11-03)

- **Cause.** The engine's `tooltip` draws, in order, the shadow, the tail's ink, the balloon's ink,
  the balloon's paper (inset by 0.8 × the theme's insets, wobbled by 0.8 × 0.8 × its amplitude) and
  the tail's paper `M-4.6 -15.5 Q-1.5 -8 .9 -3.4 Q3 -8.5 6.6 -15.5 Z`. In Bright Cel the balloon's
  paper ends at −15.68 ± 0.4, above the tail paper's top at −15.5, and the balloon's ink shows
  through the gap. The Chrome reference `design/Tooltip_Default_BrightCel.png` has the same line: it
  is in the design.
- `ToonTooltipTest` "no ink crosses the joint…" reads the pixels across the joint in the three
  themes. Mutant: the tail's paper drawn as the engine's again — the test fails with
  `BrightCel: ink at x -1 dp, y -15.875 dp (Color(0.59, 0.56, 0.51 …))`.
- **Goldens.** All 24 tooltip cells re-recorded with `--force` (viddik's tolerance accepted the old
  Default golden, so a plain record rewrote only three) and looked at. The difference is at the joint
  only: in the eight Bright Cel cells the hairline is gone (one row of pixels, up to 124 levels); in
  the Ink & Gouache cells the two papers' coincident edges blend by 1 level, 6 and 13 in the 200 ms
  inflate frame. `Tooltip_Inflate_320ms_InkGouacheLight` and `Tooltip_Inflate_0ms_*` did not change.
