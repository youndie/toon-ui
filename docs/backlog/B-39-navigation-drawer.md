---
id: B-39
title: "NavigationDrawer: a door on three hinges that swings and shivers"
status: wip
priority: P2
size: M
stage: stage-2-components
epic: feature-v1-components
blocked_by: [B-10]
---

# B-39 — NavigationDrawer: a door on three hinges that swings and shivers

A v1 component (brief §«Инвентарь»). It arrived with the second snapshot of the design (research
§1.1): designed in `reference/design-system/ToonNavDrawer.dc.html`, drawn by `drawer`, `drawerOpen` and `drawerClose` in the engine, specified on the
`toon-ui v1 Components` page.

- **What to build.** Port the renderer and its curve: the door swings open with a perspective skew, overshoots and shivers ±0.7° on its hinges; labels fade in once it is flat. Perspective skew is not a plain affine scale, so how it is drawn in Compose is part of the item.
- Built on the shared pieces like every MVP component (B-04, B-07…B-10, B-12). Props follow the dc
  file's `data-props`, and the reduce-motion word comes from the v1 page (D8).
- The renderer runs in Node, so its goldens come from B-03's harness like the MVP ones.

- AC: the v1 page's cells and frame strips within the floor in three themes; reduce motion as
  specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonNavDrawer.dc.html`, `reference/design-system/toon-ui v1 Components.dc.html`, `reference/design-system/toon-engine.js`.
