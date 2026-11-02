---
id: B-42
title: "Pull-to-refresh: a rubber band that flings the spinner into place"
status: wip
priority: P2
size: M
stage: stage-2-components
epic: feature-v1-components
blocked_by: [B-25]
---

# B-42 — Pull-to-refresh: a rubber band that flings the spinner into place

A v1 component (brief §«Инвентарь»). It arrived with the second snapshot of the design (research
§1.1): designed in `reference/design-system/ToonPullToRefresh.dc.html`, drawn by `pull` and `pullRelease` in the engine, specified on the
`toon-ui v1 Components` page.

- **What to build.** Port the renderer and its curve: the band stretches with the finger at 0.7× and thins; past 40 dp the release snaps it back and flings the blob up into the spinning progress blob (B-25). The caller owns `refreshing`.
- Built on the shared pieces like every MVP component (B-04, B-07…B-10, B-12). Props follow the dc
  file's `data-props`, and the reduce-motion word comes from the v1 page (D8).
- The renderer runs in Node, so its goldens come from B-03's harness like the MVP ones.

- AC: the v1 page's cells and frame strips within the floor in three themes; reduce motion as
  specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonPullToRefresh.dc.html`, `reference/design-system/toon-ui v1 Components.dc.html`, `reference/design-system/toon-engine.js`.
