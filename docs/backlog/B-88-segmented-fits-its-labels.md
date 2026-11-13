---
id: B-88
title: "The segmented control fits a label wider than its slot"
status: open
priority: P1
size: S
stage: stage-4-product
epic: feature-components
---

# B-88 — The segmented control fits a label wider than its slot

Issue [#42](https://github.com/youndie/toon-ui/issues/42). `toonSegmentedScene` splits the width into
equal slots and anchors each label at the slot's middle without measuring it. In
`ToonSegmented(listOf("Все", "Красные", "С PR", "Ждут меня"), width = 328.dp)`, "Ждут меня" runs
into the outer border.

- **The decision and its reason.** Size the slots by their labels when they do not fit equal slots.
  Each slot gets at least its label plus the design's padding; the rest of the width is shared
  equally. If even that does not fit, the labels scale down together to a floor of 0.8, then the
  widest is cut with an ellipsis.
  - Equal slots stay the default whenever the labels fit, so the design's controls keep their cells.
  - The thumb's travel follows the slot edges.
- Rejected: only an ellipsis. On a 360 dp phone four short Russian words would all lose letters
  while the narrow ones leave room.
- Not covered: wrapping a label onto two lines.

- AC: the call above at 328 dp draws every label inside its slot, by a test over the label bounds.
  The thumb moves to the measured slot. The design's segmented controls keep their cells and
  goldens.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonSegmented.kt`.
