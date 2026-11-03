---
id: B-54
title: "A chip makes room for its tick smoothly"
status: open
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
  equal to the selected width from 180 ms; the select strip's departed cells are listed in
  `ToonDepartures` and their goldens re-recorded.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonChip.kt`.
