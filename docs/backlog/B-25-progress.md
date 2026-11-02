---
id: B-25
title: "Progress: the inchworm and the spinning blob"
status: done
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

## Findings (2026-11-02)

- **Done as decided**, the engine's `linear`, `worm` and `circular` with the kit's `C.progress`.
  Props: `kind` (linear, circular), `value` (−1: not known, as `data-props`), `width` (280). A screen
  reader hears the value, or that it is indeterminate.
- **The frame grew** one circular SVG arc, the determinate ring. The worm's eye (`h.01`) showed that
  the frame comparison read a number written from its point as a whole one; it reads it right now,
  and no earlier frame had such a number.
- **A Float trap:** `sin π` is −8.7·10⁻⁸ in Float and +1.2·10⁻¹⁶ in the engine's doubles, so the
  arch's `pow(1.3)` at the head was NaN; the sine is clamped at 0, which is what the engine computes.
- **Frames:** the five MVP page cells and the four worm and four spin loop frames, in three themes,
  are the engine's (39 cells, worst coordinate 0.050 dp); `worm` within 6·10⁻⁷ over its loop. Eleven
  mutants caught; the reduced share only once the frame was chosen by a pure function and tested.
- **Pixels:** within 2.87 % of the cell — a 14 dp tube in a 260 × 34 cell, where its edges weigh —
  and 6.46 % in ink at worst, under the Divider's floor (14.81 %).
- **Reduce motion** is the MVP page's: a static bar at 33 %, a static blob.
