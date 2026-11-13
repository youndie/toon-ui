---
id: B-83
title: "The card fits a long time, title and meta"
status: done
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

## Findings

- The design's times measure 29 to 43 dp on desktop ("20:00" in Figtree 700 at 15 is the widest),
  so a 10 dp pad keeps the design's 64 dp sticker for all of them. "12 ноября, 09:17" measures 115 and
  gets a 135 dp sticker. The share cap is 45 % of the card (161 dp at 358), so the title keeps at
  least about half the card.
- The cut is a property of the frame's text: `ToonLabel.maxWidth`, which the renderer lays out on one
  line with `TextOverflow.Ellipsis`. Every place that copies a label carries it: `placed`, the card's
  turned and scaled words, the hover lift, and the dialog's test frames. Other components can now
  cut a text the same way (the list item, B-84).
- Held by `ToonCardTextsTest`:
  - the sticker width over every time 0–500 dp;
  - the widths the design's cards give their words;
  - the real card at 358 dp with a 60-character title, a long Russian meta and two long times. It reads
    the sticker's paper as grown, and no title, meta or time ink between the words and the sticker.
- The design's cards keep their cells and goldens: `check` and `viddikVerify` green with nothing
  re-recorded.
