---
id: B-27
title: "NavigationBar: jumping icon, dust, paint-stroke indicator"
status: wip
priority: P1
size: S
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-12]
---

# B-27 — NavigationBar: jumping icon, dust, paint-stroke indicator

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonNavBar.dc.html` and drawn by `navBar` in the
engine.

- **What to build.** Port `navBar` and `navSelect`: the icon jumps 8 dp and lands with dust, and the paint stroke smears over. The badge dot sits on an item.
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: cells and the select strip within the floor; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonNavBar.dc.html`, `reference/design-system/toon-engine.js`.
