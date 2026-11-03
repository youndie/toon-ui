---
id: B-65
title: "A list item's check plays the checkbox's toggle"
status: done
priority: P2
size: S
stage: stage-2-components
epic: feature-components
---

# B-65 — A list item's check plays the checkbox's toggle

The owner, in the running kit (2026-11-03): "the checkbox in the list item is not animated". Frame by
frame, the kit's "Share plans" row beside a standalone `ToonCheckbox`: the checkbox erases its tick
from the tip and shivers on an uncheck, and squashes, draws the tick on and flicks three specks on a
check; the row's check is off, or on, from the first frame.

- **The cause.** The design's own: the engine's `listItem` draws `check(T, {on, disabled})` with no
  `chkAnim`, and the kit's `C.listitem` hands the switch its `swAnim` but the check nothing. The port
  followed it, drawing `ToonCheckboxLook(on)` at every frame. The row's clock already recorded the
  toggle's age for the switch.
- **The decision and its reason.** The row draws its check through `toonCheckboxFrameAt` at the
  toggle's age, unpressed — the standalone checkbox's frames, its 300 ms and its reduce-motion word
  (`fade 120` for a check, `instant` for an uncheck) included. A departure from the design's
  behaviour (D13), but not from any cell: every list item cell is a row at rest, which draws as
  before, so `ToonDepartures` lists nothing and no golden is re-recorded; `ToonListItemTest` still
  holds every cell to the engine's frame.
- Rejected: pressing the check as the checkbox does under a press (its squash to 0.86). The row
  already sinks and bows its rule under a press; squashing the check as well would press twice.
- AC: over the 20 frames after a click, a check row draws at least 8 different pictures checking and
  4 unchecking, with the row's press left out; its check look is the checkbox's at every age below
  300 ms, and fades under reduce motion.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonListItem.kt`,
  `toon-core/src/desktopTest/kotlin/io/github/youndie/toon/component/ToonListItemTest.kt`.

## Findings (2026-11-03)

- **Test.** `ToonListItemTest` "a check row plays the checkbox's check and uncheck" clicks the row
  through semantics, so it does not sink, and counts the different pictures over 20 frames: 18 for
  a check, 11 for an uncheck. "A check row draws the checkbox's frames" compares the look with
  `toonCheckboxFrameAt` at 0–299 ms. Mutant — the scene drawing `ToonCheckboxLook(on)` again: "the
  check drew 1 different frames" (check 1, uncheck 1). The full old file is not a mutant here: the
  frame test reads `checkLook`, and it does not compile.
- `toonCheckboxFrameAt` holds the check's squash at 1 (`min` with the press), so the box does not
  overshoot past its size, in the row as on its own.
