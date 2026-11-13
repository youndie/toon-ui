---
id: B-84
title: "The list item wraps its title and cuts its sub before the trailing"
status: done
priority: P1
size: M
stage: stage-4-product
epic: feature-components
---

# B-84 — The list item wraps its title and cuts its sub before the trailing

Issues [#33](https://github.com/youndie/toon-ui/issues/33) and the list half of
[#36](https://github.com/youndie/toon-ui/issues/36). `ToonListItem` draws its title and sub as one
line each, at fixed places, with no width, in a 72 dp row. A pull request's title (40–80 characters)
or a board row's sub runs under the chevron and past the row's edge.

- **The decision and its reason.** The consumer asked for the title to wrap: their screen must not
  cut it.
  - The title wraps up to `titleMaxLines` (default 2) at the width left of the leading and the
    trailing. The row grows to the measured text, with 72 dp as the minimum, so a one-line row keeps
    the design's height and cell.
  - The sub stays one line and is cut with an ellipsis; `subMaxLines` lets a consumer open it.
  - The leading, trailing and the row's rule follow the measured height.
- Rejected: only cutting. The consumer's workaround (a `BasicText` under the row, with the row's
  rule moved) shows a cut title is not enough.
- Not covered: the selected state, which is B-87.

- AC: at 358 dp, an 80-character title draws on two lines inside the row, clear of the chevron, and
  the row's height follows it. A long sub ends in an ellipsis before the trailing. The design's rows
  keep their cells and goldens.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonListItem.kt`.

## Findings

- Built on B-83's `ToonLabel.maxWidth`, which now has `maxLines`: the renderer wraps a text with a
  width onto that many lines, the last ending in an ellipsis. Every copy of a label carries both.
- The room is from the words' start (56 dp) to 8 dp short of the control: the chevron at `w - 28`,
  the switch at `w - 60`, the check at `w - 48`, or the row's edge with none.
- The row grows by the distance from the first line's baseline to the last line's, measured in the
  composable for the title and the sub. The scene takes it through `ToonListItemLook`, whose
  defaults (one line, nothing added) are the design's row, so its cells and goldens do not move.
- A grown row centres its sticker and its control. With a sub, the title keeps its first baseline at
  33 and the sub moves down by the title's added lines; without one, the title's first baseline
  stays at 42.
- Held by `ToonListItemTextsTest`:
  - the room per trailing control, and the scene's widths, lines and baselines;
  - the design's rows at 72 dp, and a row with a two-line title at about 92 (bounds of the real
    composable);
  - no title or sub ink between the room's end and the control, with a chevron and with a check
    (pixels).
