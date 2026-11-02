---
id: B-22
title: "ListItem and Divider"
status: wip
priority: P1
size: S
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-09]
---

# B-22 — ListItem and Divider

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonListItem.dc.html` and drawn by `listItem` and `divider` in the
engine.

- **What to build.** ListItem: the sticker icon, the trailing chevron, switch or check, and the rule that bows like a plank when pressed (`listPress`). Divider: the tapering `inkLine` in s/m/l, inset or full.
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: cells and the press strip within the floor; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonListItem.dc.html`, `reference/design-system/toon-engine.js`.
