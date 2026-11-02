---
id: B-21
title: "Card: tilt, drop-in with dust and cascade, press lean, swipe dismiss with a hole"
status: wip
priority: P1
size: L
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-12]
---

# B-21 — Card: tilt, drop-in with dust and cascade, press lean, swipe dismiss with a hole

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonCard.dc.html` and drawn by `card` in the
engine.

- **What to build.** Port `card`, `cardAppear` (with the 40 ms cascade), the press lean toward the finger, and `cardDismiss`: speed lines, then the hole FX on the spot, and the neighbours closing up. This is the scene B-05 measures.
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: cells and strips within the floor; a swipe past 80 dp dismisses and fires `card.gone`; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonCard.dc.html`, `reference/design-system/toon-engine.js`.
