---
id: B-61
title: "No stripe across the menu's rolls"
status: done
priority: P2
size: S
stage: stage-2-components
epic: feature-components
---

# B-61 — No stripe across the menu's rolls

The owner, in the running kit (2026-11-03): "the dropdown menu has an unclear horizontal stripe
inside the art, at the top and at the bottom". Each of the scroll's two rolls is crossed by a thin,
pale line of ink along its whole length.

- **The decision and its reason.** The rolls are drawn without the line. The engine's `menu` creases
  each roll with `M8 y+1 H w-8`, a straight 1 dp stroke of ink at 35 % opacity, 1 dp under the roll's
  middle. The roll itself is wobbled and the line is not, and at 35 % it is neither ink nor paper: it
  reads as a smudge through the art, not as a fold. Without it a roll is its wobbled outline, its
  shadow and its paper, as the paper body below it is. A departure (D13): every cell that shows the
  scroll is listed in `ToonDepartures` — `Menu_Open`, the three `Menu_Item…` cells, `Menu_Unrolling`,
  the 60, 120, 200 and 360 ms unroll frames, the 0, 60 and 120 ms roll-up frames, and the `MenuProbe`
  cells; `Menu_Unroll_0ms` and `Menu_RollUp_160ms` show the trigger alone and stay the design's.
  `toonMenuScene` and `MenuFrames.strip` keep `asDesigned`, which draws the crease, and every cell is
  still held to the engine's frames that way (`EngineFrames.cells(…, asDesigned = true)`).
- Rejected: keeping the crease but drawing it crisp, in full ink. A dark line along the roll's middle
  splits the roll in two and reads as a second outline; the line has nothing to say that the roll's
  shape and shadow do not already say.
- AC: drawn 8× in each theme, each roll's paper is one colour from 3 dp above its middle to 3 dp
  under it, 20 dp in from either end; drawn as designed, every menu cell is the engine's.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonMenu.kt`.

## Findings (2026-11-03)

- **Cause.** The engine's `roll(y)` draws the roll's shadow, its ink, its paper (`surface2`) and then
  the crease, `this.L('M8 y+1 H w-8', 'none', { stroke: T.ink, sw: 1, op: .35 })`, at the top roll
  (y 7) and the bottom one (y 7 + h). It is in the design: the Chrome reference
  `design/Menu_Open_InkGouacheLight.png` has it as two pixel rows of `#C5B9A0` across the
  `#E6D9BC` roll, the 1 dp stroke straddling a pixel boundary at 1×.
- **Which stripe.** Rendered as the kit shows it, at 4× in the three themes, the crease is the only
  horizontal line inside the drawing, and it is at the top and at the bottom, as the owner said;
  without it the 4× render has none.
- `ToonMenuTest` "no stripe crosses the rolls…" reads the pixels across both rolls in the three
  themes. Mutant: the crease drawn again — the test fails with
  `InkGouacheLight, top roll: off its paper at x 20 dp, y 7.5 dp (Color(0.65, 0.6, 0.52 …))`.
- **Goldens.** 36 re-recorded (twelve cells × three themes) and looked at. In each, the two pixel
  rows of tint across each roll are gone (744 pixels, 17–58 levels; the Unroll 120 ms frame 372, its
  bottom roll being below the cell at the overshoot) and nothing else changes.
  `Menu_Unroll_0ms_*` and `Menu_RollUp_160ms_*` did not change.
