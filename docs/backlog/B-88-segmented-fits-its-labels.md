---
id: B-88
title: "The segmented control fits a label wider than its slot"
status: done
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

## Findings

- The issue's labels, measured on desktop at 800: «Все» 26, «Красные» 63, «С PR» 33, «Ждут меня» 78.
  At 328 dp they need 296 with 12 dp padding each side, so they fit at full size once the segments
  follow them. Each segment then gets 8 dp of the spare.
- **Changed from the item's decision.** Past the floor the item said "the widest is cut". A first
  version scaled every segment to the width, and a contact sheet showed «Все» squeezed to «В…» by one
  long neighbour. The segments past the floor are now max–min fair: the short ones are whole, and only
  the long ones share the cap and are cut.
- The drop's switch between unequal segments runs each end from its old edge to its new one on the
  tabs' lead and trail springs. Its thinning is measured against the wider of the two segments. With
  equal segments the design's `segSwitch` runs as before.
- The labels are measured in the composable, so the design's labels ("Day", "Week", "Month") keep the
  equal layout. The scene's equal path keeps the design's arithmetic, so its cells and goldens do not
  move.
- Held by `ToonSegmentedLabelsTest`:
  - the equal layout for labels that fit;
  - widths that fill the control, hold their labels and keep short labels whole past the floor;
  - the drop's ends with no jump over 700 ms;
  - the issue's control at 328 dp: no ink between «Ждут меня» and the outline (pixels), and a tap 4 dp
    into the last segment choosing it.
