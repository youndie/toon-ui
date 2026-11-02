---
id: B-16
title: "FAB: regular and extended, lands with dust"
status: wip
priority: P1
size: S
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-12]
---

# B-16 — FAB: regular and extended, lands with dust

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonFab.dc.html` and drawn by `fab` in the
engine.

- **What to build.** Port `fab` and `fabAppear` (snap-hold 90 ms, fall, land, dust). The idle slot comes from the budget; the default `idle: true` of the dc file is kept, and still counted.
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: the FAB cells and land strip within the floor; reduce motion is `fade 120`; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonFab.dc.html`, `reference/design-system/toon-engine.js`.
