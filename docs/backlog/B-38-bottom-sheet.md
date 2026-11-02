---
id: B-38
title: "BottomSheet: a sheet with a tab that slides up with overshoot"
status: open
priority: P2
size: M
stage: stage-2-components
epic: feature-v1-components
blocked_by: [B-10, B-22]
---

# B-38 — BottomSheet: a sheet with a tab that slides up with overshoot

A v1 component (brief §«Инвентарь»). It arrived with the second snapshot of the design (research
§1.1): designed in `reference/design-system/ToonBottomSheet.dc.html`, drawn by `sheet` and `sheetMove` in the engine, specified on the
`toon-ui v1 Components` page.

- **What to build.** Port the renderer and its curve: slides up on its tongue with ~16 % overshoot (spring ζ 0.5, ω 22), the scrim darkens over 200 ms, and it falls in 220 ms on close; it fills its parent, as the Dialog does.
- Built on the shared pieces like every MVP component (B-04, B-07…B-10, B-12). Props follow the dc
  file's `data-props`, and the reduce-motion word comes from the v1 page (D8).
- The renderer runs in Node, so its goldens come from B-03's harness like the MVP ones.

- AC: the v1 page's cells and frame strips within the floor in three themes; reduce motion as
  specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonBottomSheet.dc.html`, `reference/design-system/toon-ui v1 Components.dc.html`, `reference/design-system/toon-engine.js`.
