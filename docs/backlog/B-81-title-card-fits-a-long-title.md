---
id: B-81
title: "The title card fits a title wider than its plaque"
status: done
priority: P1
size: S
stage: stage-4-product
epic: feature-components
---

# B-81 — The title card fits a title wider than its plaque

Issue [#30](https://github.com/youndie/toon-ui/issues/30), from the first consumer (pyatiletka). `toonTitleCardKitScene`
gives the plaque a fixed size (`pw = 280 / 260`, `ph = 120 / 110`) whatever the title is. A title
wider than about 280 dp at the display size runs past both edges of the plaque, e.g. "ПЯТИЛЕТКА"
in the Cyrillic backing face. A consumer cannot change it: the plaque size is not a parameter.

- **The decision and its reason.** Grow the plaque to the measured title plus the design's side
  margin, with today's 280/260 as the minimum. That is how a title card is lettered: the plaque
  holds the words. The design's own titles fit the minimum, so every cell and golden stays where it
  is. Past the card's width, the title scales down to fit; it never runs off the card.
- Rejected: only scaling the title down. A nine-letter word would come out smaller than the
  design's display size while the card has room.
- Rejected: a plaque-width parameter. It moves the measuring onto every consumer.
- Not covered: wrapping a title onto two lines.

- AC: `ToonTitleCard(title = "ПЯТИЛЕТКА")` and a 40-letter Latin title draw inside the plaque at
  the default width and at 280 dp (pixels of the title's ink inside the plaque's bounds, by a test).
  The design's titles keep their cells and goldens.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonTitleCard.kt`.

## Findings

- The design's words, measured on desktop at the design's sizes: "Saturday" 216 at 44, "Nothing yet"
  206 at 34, the splash sub 225 and the empty sub 173 at 14. So the side margin is 24 dp, not the
  stars' 27.5: at 28, "Nothing yet" would have widened the design's 260 plaque. The corner stars sit
  above and below the words' rows, so the smaller margin does not put words under a star.
- The sub line gets the same fit as the title. The issue names only the title, but a long sub ran off
  the plaque the same way.
- Held by `ToonTitleCardTest`:
  - `the plaque grows to hold long words, never past the card, …` sweeps the fit over word widths 0 to
    1,200 and cards 240 to 600 wide, and checks the kit scene sets the smaller sizes;
  - `a title wider than the design's plaque is drawn inside the plaque` runs the real composable with
    "ПЯТИЛЕТКА" at 360 and 280 and a 40-letter Latin title, and reads the pixels: the plaque's paper
    run, and no more ink before the paper on a title row than on the row under it. The text is the
    outline's colour, so the test cannot tell them apart by colour.
- The cells and goldens of the design's cards did not move: `check` and `viddikVerify` are green with
  no re-recording.
