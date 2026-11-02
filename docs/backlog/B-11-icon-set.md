---
id: B-11
title: "Icons: 25 centerlines, variable-width outline, Regular and Bold, ToonIcon"
status: done
priority: P1
size: M
stage: stage-1-materials
blocked_by: [B-06, B-09]
---

# B-11 — Icons: 25 centerlines, variable-width outline, Regular and Bold, ToonIcon

The engine stores each icon as one centerline and computes the ink weight from it
(`iconOutline`, `iconWidth`). The sketch's port drops the 6 % swell term (research §1.5).

- **The decision and its reason.** Port `iconOutline` and the full `iconWidth`, sample with
  `PathMeasure` every 0.3 dp, cache per (icon, size, weight). The 25 centerlines (17 in the first snapshot, 8 added for v1 and settings) are generated from
  `ICONS`, never typed. `ToonIcon(icon, size, weight, filled, color)` takes the `ToonIcon.dc.html`
  props.
- Icons need the DOM in the engine, so their references come from B-06's Chrome renders, not from
  B-03's JSON.

- AC: all 25 icons in both weights within the floor against their references, in every theme.
- Anchors: `reference/design-system/toon-engine.js`, `reference/design-system/toon-ui Icons.dc.html`, `reference/design-system/ToonIcon.dc.html`.

## Findings (2026-11-02)

- **Done as decided**, with one change of method: `PathMeasure` measures each centerline enlarged
  1024 times. At the 24 dp grid Skia's flattening lost a percent of the length (the search lens:
  43.1 dp against 43.54), which dropped two samples against Chrome (research D11).
- **The numeric half comes from Chrome.** `scripts/design-references.mjs` also writes the engine's
  `iconOutline` for every glyph and weight; the port matches it with the same polygons and point
  counts, worst point 0.0149 dp, tolerance 0.02 (`IconOutlineGoldensTest`).
- **Pixels:** 348 cells — Regular, Bold and Filled at 48 dp, Regular and Bold at 24 dp, three themes,
  the Icons page's cells with the kit's overflow — all within the Divider floor of 1.54 %, worst
  0.51 % (More, Bold 48, night). The cell list is `scripts/icon-cells.mjs`, shared by the fixtures'
  generator and the references.
- **Mutants**, each caught by the numeric test and above the floor in pixels: the swell term dropped
  (the sketch's port) 0.12 dp, 5.39 %; measured unenlarged, a point-count failure, 1.98 %; Regular
  drawn with Bold's base 0.625 dp, 21.56 %.
- **Cache:** per (glyph, weight) in `remember`, the outline on the 24 dp grid scaled at draw time,
  as the engine caches per (name, base) and scales with a transform. Size is not in the key: a
  filled path scales without changing its ink, unlike a stroke.
- `ToonIcon` takes a `contentDescription` the design's props do not have (null = decorative); an
  icon that means something has to be able to say so.
