---
id: B-18
title: "Switch: wind-up, smear, slap, colour wave"
status: open
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-12]
---

# B-18 — Switch: wind-up, smear, slap, colour wave

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonSwitch.dc.html` and drawn by `sw` in the
engine.

- **What to build.** Port `sw` and `swAnim`: the 40 ms anticipation, the smear flight with its echo, the slap squash, and the track's colour wave from the impact edge. The sketch leaves the wave and echo as a comment.
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: the Switch strip's values match the goldens at every timestamp; cells within the floor; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonSwitch.dc.html`, `reference/design-system/toon-engine.js`.
