---
id: B-67
title: "A swiped card springs back past its slot, then flies; its look and words follow"
status: done
priority: P2
size: S
stage: stage-2-components
epic: feature-components
---

# B-67 — A swiped card springs back past its slot, then flies; its look and words follow

The owner, on a video of B-66's throw (2026-11-09): pulled far enough, the card should come back to
its place and then run away, "even with a little wind-up the other way". The video's frames also
showed the card turn level and opaque in the one frame after the release, and its time slide off
its sticker: a dozen dp while dragged, half the card while smeared by the fling.

- **The decision and its reason.**
  - Let go past 80 dp, the card is the kit's drag spring (ω 30, ζ 0.55) let go from rest, until the
    far end of its first overshoot: 125 ms, and 12.6 % of the drag past the slot on the other side
    — the wind-up grows with the pull. There it stands still for an instant and is thrown from where
    it stands at the engine's fling (`cardDismiss` from 70 ms on), so the turn has no step in speed.
    The spring stands in for the engine's 70 ms wind-up; the rest of the throw — the lines, the
    hole, the close-up of the cards below, `onDismiss` — runs 55 ms later with it (`throwShift`).
  - On its way back the card keeps the drag's lift and turn, let go as it nears the turn
    (smoothstep over the 125 ms), so the first frame after the release is the last under the finger.
  - A card turned by a drag takes its words' places along (the words stay level), and a swiped card
    smeared by its fling scales their places with it: the time stays on its sticker.
  - A reader's Dismiss action throws the card from its slot with no drag (`throwAway`), the design's
    throw; it used to fake an 81 dp drag. Its frames, the `Card_Dismiss_*` cells and their goldens
    are unchanged: the words moved only on a drag or a swipe, which no cell draws.
- Supersedes B-66's first decision (the throw on from the finger); B-66's list without a hole and
  the plan screen stand.
- Rejected: a fixed-time ease back to the slot (B-66's predecessor, 70 ms) — a long pull crossed the
  row in four frames; the spring is what a short drag already plays when it springs back.
- AC: a swiped card never moves outward on its way back nor back in its throw, passes its slot by
  10–15 % of the drag, stands still at the turn, and is off the row 200 ms after it; the frame after
  the release equals the last dragged one; the time stays on its sticker; nothing closes up over the
  slot while the card is drawn.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonCard.kt`.

## Findings (2026-11-09)

- `ToonCardTest`: for drags of −150, 120 and −85 dp the throw starts at the finger, moves only
  toward and past the slot for 125 ms, then only away, under 15 dp a ms while drawn; the turn is
  12.6 % past the slot with no step in speed; off the row by 325 ms. The frame after the release
  matches the dragged one in every layer's opacity and placement and every word's place. The time's
  place under a −150 dp drag is the sticker's middle turned with the sheet. No close-up while the
  card is drawn.
- A video of the list (three swipes, rendered frame by frame) went to the owner before the merge.
