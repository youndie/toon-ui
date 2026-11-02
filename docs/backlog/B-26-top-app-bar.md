---
id: B-26
title: "TopAppBar: a sign on two nails that sways"
status: open
priority: P1
size: S
stage: stage-2-components
epic: feature-components
blocked_by: [B-10]
---

# B-26 — TopAppBar: a sign on two nails that sways

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonTopAppBar.dc.html` and drawn by `topBar` in the
engine.

- **What to build.** Port `topBar` and the pendulum `sway`, kicked by a scroll fling or a scrolled flip. Small and center variants, and actions from the icon set.
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: cells and the sway strip within the floor; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonTopAppBar.dc.html`, `reference/design-system/toon-engine.js`.
