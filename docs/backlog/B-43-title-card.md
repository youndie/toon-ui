---
id: B-43
title: "Title card: splash and empty state, rings and a plaque"
status: done
priority: P2
size: S
stage: stage-2-components
epic: feature-v1-components
blocked_by: [B-12]
---

# B-43 — Title card: splash and empty state, rings and a plaque

A v1 component (brief §«Инвентарь»). It arrived with the second snapshot of the design (research
§1.1): designed in `reference/design-system/ToonTitleCard.dc.html`, drawn by `titleCard` and `splashAt` in the engine, specified on the
`toon-ui v1 Components` page.

- **What to build.** Port the renderer and its curve: concentric rings flow inward, the plaque pops in with overshoot and two bursts of stars, and the text arrives once it lands; `variant` is splash or empty.
- Built on the shared pieces like every MVP component (B-04, B-07…B-10, B-12). Props follow the dc
  file's `data-props`, and the reduce-motion word comes from the v1 page (D8).
- The renderer runs in Node, so its goldens come from B-03's harness like the MVP ones.

- AC: the v1 page's cells and frame strips within the floor in three themes; reduce motion as
  specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonTitleCard.dc.html`, `reference/design-system/toon-ui v1 Components.dc.html`, `reference/design-system/toon-engine.js`.

## Findings (2026-11-02)

- **Done as decided**, the engine's `titleCard` and `splashAt` with the kit's `C.titlecard`, as
  `ToonTitleCard`. Props follow `data-props`: `variant` (splash or empty), `title` and `sub` (null is
  the variant's own words; an empty `sub` is none, as the kit's `??` reads it), `width` (360),
  `height` (520), `onClick`. The card clips, as the kit's box does. It plays its entrance when it
  appears and again on a click. A screen reader hears a heading with the title and the line.
- **The rings are ellipses in two arcs** (`A r r·0.96`), a path no component had: `ToonSegments.Ellipse`,
  its numbers in the order the engine writes them, each to a tenth as `f` does.
- **The kit rests where its rings stop, not where they start:** once the 900 ms entrance has played,
  the kit draws `splashAt(10⁶)`, its rings 16 dp along their 34-dp period, so at 900 ms they jump
  from 2 to 16. The port does the same.
- **Frames:** the page's splash and empty state and its five enter frames, in three themes, are the
  engine's (21 cells, worst coordinate 0.050 dp), and so are six probes: a card with no line, and
  the kit's own empty state 300 ms in, its stars where the splash's are. `splashAt` within 4·10⁻⁶.
  Seventeen mutants caught, two of them by the probes added for them.
- **Pixels:** every cell under the Divider's ink floor.
- **Reduce motion** is the v1 page's `fade 120`: the card at rest fades in, without the pop, the
  flow or the stars.
- **B-34, looked at first:** the design project still serves `toon-engine.js?v=10` and
  `toon-kit.js?v=11`, so there was nothing to re-import.
