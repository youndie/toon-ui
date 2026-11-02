---
id: B-40
title: "Tooltip: a speech balloon that inflates from its tail"
status: wip
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
