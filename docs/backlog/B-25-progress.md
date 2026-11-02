---
id: B-25
title: "Progress: the inchworm and the spinning blob"
status: open
priority: P1
size: S
stage: stage-2-components
epic: feature-components
blocked_by: [B-09]
---

# B-25 — Progress: the inchworm and the spinning blob

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonProgress.dc.html` and drawn by `linear` and `circular` in the
engine.

- **What to build.** Port `linear` with `worm`, its arch, and the static 33 % under reduce motion; port `circular`, the three-lobed blob. Determinate values too.
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: cells and loop frames within the floor; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonProgress.dc.html`, `reference/design-system/toon-engine.js`.
