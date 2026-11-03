---
id: B-54
title: "A chip makes room for its tick smoothly"
status: done
priority: P2
size: S
stage: stage-2-components
epic: feature-components
---

# B-54 — A chip makes room for its tick smoothly

When a filter chip is selected its tick appears and the chip grows by 24 dp in one frame; on
deselect it shrinks in one (owner, 2026-11-03: "the size changes abruptly, it should stretch
smoothly"). The engine does the same (`chip`'s width is decided by `sel`), so this departs from the
design (research D13).

- **The decision and its reason.** The tick's room grows with the paint's sweep, the select's own
  ease-out over 180 ms, and shrinks the same way on deselect, so the chip's box, and the row it
  sits in, move with the animation instead of ahead of it. Under reduce motion (`instant`) it jumps.
- AC: the chip's width at every frame of the select is between the two widths, monotonic, and
  equal to the selected width from 180 ms.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonChip.kt`.

## Findings (2026-11-03)

- `ToonChipLook.lead`, 0 to 1, is how far a filter chip's tick room is open. The select gives it the
  sweep's own curve (`1 − (1 − t/180)²`); a deselect closes it as `(1 − t/180)²`; the composable's
  box is `chipWidth(variant, lead, label)`, so the row moves with the chip. The tick fades in with
  its room. Reduce motion stays `instant`.
- **No cell departed.** The page's select strip fixes `scale` and `swipe` per frame and leaves the
  room at its default, open, so every fixture, engine frame and golden still draws the design's
  frame. The departure is in the clock alone (`toonChipFrameAt`), and `ToonChipTest` holds it: the
  width is monotonic, within the two widths, at the selected width from 180 ms, and less than half
  open at 30 ms; the deselect mirrors it.
