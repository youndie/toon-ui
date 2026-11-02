---
id: B-36
title: "Slider: a ball on a rope that squashes when grabbed"
status: done
priority: P2
size: S
stage: stage-2-components
epic: feature-v1-components
blocked_by: [B-10]
---

# B-36 — Slider: a ball on a rope that squashes when grabbed

A v1 component (brief §«Инвентарь»). It arrived with the second snapshot of the design (research
§1.1): designed in `reference/design-system/ToonSlider.dc.html`, drawn by `slider` and `sliderGrab` in the engine, specified on the
`toon-ui v1 Components` page.

- **What to build.** Port the renderer and its curve: the ball squashes under the finger and the rope sags 5 dp, then the ball springs back and the rope twangs once; `steps` snaps the value.
- Built on the shared pieces like every MVP component (B-04, B-07…B-10, B-12). Props follow the dc
  file's `data-props`, and the reduce-motion word comes from the v1 page (D8).
- The renderer runs in Node, so its goldens come from B-03's harness like the MVP ones.

- AC: the v1 page's cells and frame strips within the floor in three themes; reduce motion as
  specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonSlider.dc.html`, `reference/design-system/toon-ui v1 Components.dc.html`, `reference/design-system/toon-engine.js`.

## Findings (2026-11-02)

- **Done as decided**, the engine's `slider`, `rope` and `sliderGrab` with the kit's `C.slider`.
  Props: `value` (controlled, with `onValueChange`), `steps`, `width` (300), `enabled`. A press sets
  the value under it and a drag follows, snapped to the stops; the keyboard and a screen reader's
  `setProgress` set it too (service quirks).
- **The frame grew** separate strokes in one path (`ToonSegments.Pairs`), the rope's hatching.
- **Frames:** the five v1 page cells and the six grab frames, in three themes, are the engine's
  (33 cells, worst coordinate 0.050 dp); `sliderGrab` within 6·10⁻⁷ held and let go. Nine mutants
  caught.
- **Pixels:** within 1.40 % of the cell; 11.10 % in ink at worst, under the Divider's floor.
- **Reduce motion** is the v1 page's `instant`.
- **Found on the way:** the Bright Cel list and settings screen references are not byte-stable
  between renders (13 px by 2 levels; service quirks).
- **B-34, looked at first:** the design project still serves `toon-engine.js?v=10` and
  `toon-kit.js?v=11` with the same files, so there was nothing to re-import.
