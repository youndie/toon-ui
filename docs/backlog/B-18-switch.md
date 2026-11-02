---
id: B-18
title: "Switch: wind-up, smear, slap, colour wave"
status: done
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

## Findings (2026-11-02)

- **Done as decided**, the engine's `sw` and `swAnim` with the kit's `C.switch`, wave and echo
  included. Props: `checked`, `onCheckedChange` (`onChange`), `label`, `width` (0 fits the label),
  `enabled`; the dc file's demo label and `checked: true` are not kept as defaults.
- **The Core page draws an older `sw` and `swAnim`** in its own script, without the echo and the
  wave; the references are the engine's, which have them.
- **Frames:** the six Core page cells and six toggle frames in three themes are the engine's (36
  cells, worst coordinate 0.050 dp). The thumb squashes about its own pivot, so a frame layer now has
  an x and y scale about a point, and frames are compared as whole affine transforms.
- **The strip's values match the goldens at every millisecond**, both directions: `swAnim` within
  6·10⁻⁷, echo, wave, origin and track colour included. The curve goldens recorded only the keys of
  a curve's first moment, which left the echo and the wave out; they record every key now.
- **Pixels:** within 1.25 % of the cell, under the Divider's 1.54 %. Nine mutants caught.
- **Reduce motion** is the Core page's `instant`. A press flattens the thumb onto its bottom as the
  Pressed cell draws it, which the kit does not do.
