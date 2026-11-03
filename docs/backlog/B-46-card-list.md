---
id: B-46
title: "Card list: the 40 ms cascade and the close-up after a dismiss"
status: wip
priority: P2
size: S
stage: stage-2-components
epic: feature-components
blocked_by: [B-21, B-45]
---

# B-46 — Card list: the 40 ms cascade and the close-up after a dismiss

B-21 built the card: one sheet that drops in `delay` ms late, and that a swipe throws away. What the
Core page's dismiss strip also shows belongs to a list of them: the card below "slides up over it
with overshoot" (`cardDismiss`'s `n`, a 102 dp step on `spr(t − 200, .55, 40)`), and in a list each
card starts 40 ms after the previous one. The page draws this as `cardStack`; the library has no
list that does it, and B-21's tests compose it only for the frames.

- **What to build.** A column of `ToonCard`s that passes each its cascade delay and, when one is
  dismissed, moves the cards below it up by the engine's `n` rather than jumping. How many cards land
  with dust is B-45's decision, which this item takes as given: the owner accepted the measured
  numbers (2026-11-02), so the cascade keeps its dust on every card.
- AC: the Core page's dismiss strip within the floor as a live list (the frames are B-21's
  `CardFrames.dismiss`); the cascade's delays are 40 ms apart; reduce motion as the page says.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonCard.kt`, `toon-core/src/desktopTest/kotlin/io/github/youndie/toon/parity/CardFrames.kt`.
