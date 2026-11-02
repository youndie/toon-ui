---
id: B-37
title: "Menu: a paper scroll that unrolls under its trigger"
status: done
priority: P2
size: S
stage: stage-2-components
epic: feature-v1-components
blocked_by: [B-10, B-11]
---

# B-37 — Menu: a paper scroll that unrolls under its trigger

A v1 component (brief §«Инвентарь»). It arrived with the second snapshot of the design (research
§1.1): designed in `reference/design-system/ToonMenu.dc.html`, drawn by `menu` and `menuUnroll` in the engine, specified on the
`toon-ui v1 Components` page.

- **What to build.** Port the renderer and its curve: the scroll drops and unrolls with ~20 % overshoot (spring ζ 0.42, ω 24) and rolls up in 160 ms; an item fires on release, before the roll finishes.
- Built on the shared pieces like every MVP component (B-04, B-07…B-10, B-12). Props follow the dc
  file's `data-props`, and the reduce-motion word comes from the v1 page (D8).
- The renderer runs in Node, so its goldens come from B-03's harness like the MVP ones.

- AC: the v1 page's cells and frame strips within the floor in three themes; reduce motion as
  specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonMenu.dc.html`, `reference/design-system/toon-ui v1 Components.dc.html`, `reference/design-system/toon-engine.js`.

## Findings (2026-11-02)

- **Done as decided**, the engine's `menu` and `menuUnroll` with the kit's `C.menu`. Props follow
  `data-props`: `items` (a `ToonMenuItem` each, label and icon), `open`, `selected`, `width` (200),
  `onSelect`. An item fires on release while the menu is open, never on press; the keyboard walks
  the items and chooses, and a screen reader has one action per item (service quirks).
- **Timing is the kit's, not one number:** the open it mounts with plays 600 ms and every change
  after it, either way, 400 ms; then the menu is at rest, unrolled exactly. The curve at those ends
  is 0.997 and 1.008, so the kit, and this port, settle with a step of 0.4 dp and 1.3 dp.
- **Frames:** the five v1 page cells, the five unroll and four roll-up frames, in three themes, are
  the engine's (42 cells, worst coordinate 0.050 dp), and so are nine probes in the paper's first
  20 dp, which no page frame lands in; `menuUnroll` within 6·10⁻⁷ both ways. Fourteen mutants
  caught, the first survivor (the body drawn from 20 dp instead of 2) by the probes added for it.
- **Pixels:** every cell under the Divider's ink floor; the menu is read with the shapes, not the
  text floor.
- **Reduce motion** is the v1 page's `fade 120` both ways: the scroll fades in unrolled and fades
  out without rolling up.
- **Targets:** the first item's reaches up to the scroll's top and the last one's down to its
  bottom, 58 and 69 dp; an item between them stays the design's 44 dp, which is B-50's question.
- **The menu does not place itself:** it is the scroll alone, laid out where the caller puts it,
  as the page's `menuScene` puts it 34 dp left of and 54 dp under the `more` button.
- **B-34, looked at first:** the design project still serves `toon-engine.js?v=10` and
  `toon-kit.js?v=11`, so there was nothing to re-import.
