---
id: B-73
title: "ToonSplitHandle: the grip between a sidebar and the content"
status: wip
priority: P2
size: S
stage: stage-5-desktop
epic: feature-components
blocked_by: [B-68, B-69]
---

# B-73 — ToonSplitHandle: the grip between a sidebar and the content

`ToonSplitHandle.dc.html` (engine: `splitGrip`). 16 dp wide; states rest | hover | drag | focus;
`onDragStart`, `onDrag(dxFromStart)`, `onDragEnd`; arrow keys move it by 8.

- A separator to accessibility, with a column-resize pointer.
- AC: the four states match their renderer; drag and keys report the offsets above under a test.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonSplitHandle.kt`.
