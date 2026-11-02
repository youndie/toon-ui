---
id: B-15
title: "IconButton and Badge"
status: wip
priority: P1
size: S
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-11, B-12]
---

# B-15 — IconButton and Badge

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonIconButton.dc.html` and drawn by `iconBtn` and `badge` in the
engine.

- **What to build.** IconButton: the hand-drawn circle squashes like a button, and the icon hops on its own spring (`iconBounce`); a toggle adds the flash. Badge: the star-burst pop (`badgeAppear`), the count swap (`badgeCount`), the dot, and the overflow `99+`.
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: the MVP page's IconButton and Badge cells and strips within the floor; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonIconButton.dc.html`, `reference/design-system/toon-engine.js`.
