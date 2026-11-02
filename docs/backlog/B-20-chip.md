---
id: B-20
title: "Chip: filter, assist, input; inflate and paint-in; a 48 dp target"
status: done
priority: P1
size: S
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-12]
---

# B-20 — Chip: filter, assist, input; inflate and paint-in; a 48 dp target

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonChip.dc.html` and drawn by `chip` in the
engine.

- **What to build.** Port `chip` and `chipSelect`, and tilt from the fixed set. The layout box is 48 dp with the drawing at 36, 6 dp down, as the kit does since the second snapshot (research §1.6; the first kit took pointer input on a 36 dp box).
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: cells within the floor; a test asserts the 48 dp touch target; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonChip.dc.html`, `reference/design-system/toon-engine.js`.

## Findings (2026-11-02)

- **Done as decided**, the engine's `chip` and `chipSelect` with the kit's `C.chip`. Props: `label`,
  `variant`, `icon` (default `calendar`), `selected`, `tilt` (degrees, on the shape only),
  `enabled`, `onSelectedChange` (`onChange`), `onClick`. The dc file's demo label is not kept.
- **The touch target is 48 dp** round the 36 dp drawing, 6 dp down, and at least 48 wide
  (`ToonChipTest`).
- **Frames:** the seven MVP page cells and five select frames in three themes are the engine's (36
  cells, worst coordinate 0.050 dp); `chipSelect` within 6·10⁻⁷. Ten mutants caught.
- **Pixels:** 7–11 % of the cell, within the text floor in ink. The label sizes the chip: Chrome
  measures "Outdoors" at 64.72 dp (Figtree) and 63.49 (Fredoka), Compose at 65 and 64, and a
  different width moves every wobble point. With Chrome's widths put into the fixtures the worst
  cell went from 11.24 % to 6.54 %; the rest is the label's glyphs filling a small cell.
- **Reduce motion** is the MVP page's `instant`: a selected chip is painted at once.
