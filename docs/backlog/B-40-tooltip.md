---
id: B-40
title: "Tooltip: a speech balloon that inflates from its tail"
status: done
priority: P2
size: S
stage: stage-2-components
epic: feature-v1-components
blocked_by: [B-09]
---

# B-40 — Tooltip: a speech balloon that inflates from its tail

A v1 component (brief §«Инвентарь»). It arrived with the second snapshot of the design (research
§1.1): designed in `reference/design-system/ToonTooltip.dc.html`, drawn by `tooltip` and `tipInflate` in the engine, specified on the
`toon-ui v1 Components` page.

- **What to build.** Port the renderer and its curve: inflates from the tail tip to ~1.35 and wobbles (ζ 0.3, ω 30); the text appears past 0.85.
- Built on the shared pieces like every MVP component (B-04, B-07…B-10, B-12). Props follow the dc
  file's `data-props`, and the reduce-motion word comes from the v1 page (D8).
- The renderer runs in Node, so its goldens come from B-03's harness like the MVP ones.

- AC: the v1 page's cells and frame strips within the floor in three themes; reduce motion as
  specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonTooltip.dc.html`, `reference/design-system/toon-ui v1 Components.dc.html`, `reference/design-system/toon-engine.js`.

## Findings (2026-11-02)

- **Done as decided**, the engine's `tooltip` and `tipInflate` with the kit's `C.tooltip`. Props
  follow `data-props`: `text` and `show`. The box is the balloon's width, its text measured and 14 dp
  a side, and 46 dp tall, the tail's tip at its bottom middle; the balloon overshoots it while it
  inflates. A screen reader hears the text, as a polite live region, while it shows; it is no target.
- **Timing is the kit's:** a show or a hide plays 500 ms; the balloon is not drawn under 0.02, so a
  hide is gone at 118 ms.
- **The width is measured text.** The engine measures on a canvas, and without a DOM as
  length × 14 × 0.55 (research §1.2). The frame test gives the port Node's width, the fixtures
  Compose's own; the page's cell is Node's width, so in Chrome and in Compose the balloon sits left of
  the cell's middle, alike.
- **Frames:** the page's three cells and five inflate frames, in three themes, are the engine's (24
  cells, worst coordinate 0.050 dp); `tipInflate` within 6·10⁻⁷ both ways. Fourteen mutants caught,
  the drawing threshold by an assertion added for it.
- **Pixels:** every cell under the Divider's ink floor, but closest of all the shapes: 14.46 % at
  320 ms in Bright Cel against 14.81 %. The label is most of a small balloon's ink, and text is what
  the two rasterisers draw least alike; it is held to the shapes' floor all the same.
- **Reduce motion** is the v1 page's `fade 120` both ways: the balloon fades at its size.
- **B-34, looked at first:** the design project still serves `toon-engine.js?v=10` and
  `toon-kit.js?v=11`, so there was nothing to re-import.
