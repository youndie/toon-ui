---
id: B-63
title: "The text field's label flies out and back without a jump"
status: done
priority: P2
size: S
stage: stage-2-components
epic: feature-components
---

# B-63 — The text field's label flies out and back without a jump

The owner, in the running kit (2026-11-03): "the TextField's label jumps incorrectly during the
animation". Frame by frame, on an empty field: the first frame that draws the focus shows the label
already raised, the next one back inside the field, and only then does it fly up; on blur the first
frame shows it back at rest, the next one raised again, and then it falls. A 44 dp jump out and back,
each way.

- **The cause.** `ToonTextField` recorded a focus change in `LaunchedEffect(focused)`, which runs
  after the composition that first sees the new focus. That composition read the previous play's
  age — settled, hundreds of milliseconds — with the new focus, so `fldFocus` drew the new end
  state; the effect then started the play at age 0, from the old end. The kit sets the focus and
  starts its play in the same handler (`onFocus`/`onBlur` in `C.textfield`), so the design never
  draws such a frame.
- **The decision and its reason.** The field's clock takes the focus during composition
  (`ToonFieldClock.focusIs`): a change marks the play pending, and the composition that draws the
  new focus reads its age 0. The loop is restarted by the focus key, as before. Only live behaviour
  changes: no cell draws a focus change from a stale age, so nothing departs from the design and no
  golden is re-recorded.
- Rejected: keeping the effect and treating a focus that differs from the clock's last recorded one
  as age 0 in the frame function. It patches the reading instead of the record, and leaves two
  sources of "when did focus change".
- AC: on an empty field the label moves at most 20 dp between two 16 ms frames through a focus and
  a blur, and lifts by more than 30.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonTextField.kt`,
  `toon-core/src/desktopTest/kotlin/io/github/youndie/toon/component/ToonTextFieldTest.kt`.

## Findings (2026-11-03)

- **Test.** `ToonTextFieldTest` "the label flies out and back without standing a frame at the far
  end" renders the same focus-then-blur run twice, with the label and without; the pixels that
  differ are the label, and their rows' mean is where it stands. It reads every 16 ms frame: with
  the fix the steepest step is 12 dp (the spring at about 64 ms). Mutant — the effect restored:
  "the label jumped 44.4 px between frames 1 and 2", the series 51.0, 51.0, 6.6, 51.0, 45.7 … on
  focus and 6.6, 50.6, 6.6, 12.6 … on blur.
- **Not changed, the design's own:** the label's size goes from 16 to 13 sp with the lift, and its
  weight and colour switch at half the lift (500 muted to 700 text), as the engine's `field` draws
  them; the spring overshoots, carrying the label about 5 dp above its raised place before it
  settles. None of these is the reported jump.
- The kit's sample fields are filled, and a filled field keeps its label raised, so the jump shows
  only once the value is cleared.
