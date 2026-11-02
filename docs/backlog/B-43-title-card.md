---
id: B-43
title: "Title card: splash and empty state, rings and a plaque"
status: open
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
