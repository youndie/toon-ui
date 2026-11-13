---
id: B-84
title: "The list item wraps its title and cuts its sub before the trailing"
status: open
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
