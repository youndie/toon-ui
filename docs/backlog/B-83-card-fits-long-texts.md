---
id: B-83
title: "The card fits a long time, title and meta"
status: open
priority: P1
size: M
stage: stage-4-product
epic: feature-components
---

# B-83 — The card fits a long time, title and meta

Issues [#32](https://github.com/youndie/toon-ui/issues/32) and the card half of
[#36](https://github.com/youndie/toon-ui/issues/36). On `ToonCard`, three texts run off the card:

- the time badge has a fixed width, so "12 ноября, 09:17" runs past both edges of the badge;
- the meta line runs past the card's right edge at 358 dp;
- a long title is drawn as one line at a fixed place.

- **The decision and its reason.** Measure all three against the room they have.
  - The badge grows with its text, with today's width as the minimum, so the design's "09:17" keeps
    its cell.
  - Title and meta are cut with an ellipsis at the width left of the badge and the sticker.
  - The card stays 84 dp tall. A card is a fixed sticker in the design's list and its swipe and
    close-up animations (B-66, B-67) are built on that height. A consumer needing the whole sentence
    uses a list item (B-84).
- Rejected: wrapping the card's title. The card's height would vary, and every curve of the list's
  dismiss and close-up would have to measure it.
- Not covered: the card list's spacing.

- AC: at 358 dp, a 40-character title and meta end in an ellipsis before the badge. A long time sits
  inside its badge. The design's cards keep their cells and goldens.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonCard.kt`.
