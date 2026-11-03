---
id: B-46
title: "Card list: the 40 ms cascade and the close-up after a dismiss"
status: done
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

## Findings (2026-11-03)

- **Built.** `ToonCardList` (library's own composable, no design file: the Screens page lays the cards
  out 18 dp apart with `delay: j * 40`, and only the Core page's `cardStack` draws the close-up). Each
  card is a `ToonCard` over a state the list holds, so the list reads the thrown card's own clock and
  pulls the cards below up by `cardDismiss`'s `n` × 102 dp in a graphics layer, with no second clock to
  drift from it.
- **The cascade, against the engine.** A new probe group, `CardCascade` (`cardStack(T, t, 'appear')` at
  0–460 ms, 27 cells), holds `CardFrames.cascade`: worst coordinate 0.050 dp. The appear branch of
  `cardStack` is now in the page-text holds.
- **The live list, as pixels.** Against the frames drawn by the same rasteriser: the cascade at
  48–384 ms and the dismiss strip at 176–512 ms differ by 0.00 % of the inked pixels (floor 14.81 %).
  The card below sits at `204 + n × 102` dp within 0.01 dp on every frame and settles in the slot.
- **Not compared: the throw before 170 ms.** There the thrown card is the kit's, not the strip's
  (B-21): from the finger, no pull-back, the smear scaled about the centre. At 128 ms that is 10.8 %
  of the inked pixels — the card's, not the list's. Recorded in toon-core.md's quirks.
- **Timing.** `onDismiss` comes at `card.gone`, 200 ms in; the list keeps the slot to 520 ms (120 ms
  under reduce motion) even when the caller drops the item at once, as the Screens page does.
- **Reduce motion** (D8): `fade 120`; the cards below wait for the fade and then take the slot at once.
- `InkParityTest`'s ratio and floors moved into `parity/InkRatio.kt` so a test of a live component can
  read the same floors; the check itself is unchanged.
