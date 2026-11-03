---
id: B-58
title: "Both halves of the slider's rope move with a drag"
status: done
priority: P2
size: S
stage: stage-2-components
epic: feature-components
---

# B-58 — Both halves of the slider's rope move with a drag

The owner, in the running kit (2026-11-03): "the slider's left part stands still while the rope is
dragged, and the right part pulls fine". The engine's `rope` hatches every rope every 6 dp from its
start: the empty half starts at the ball, so its marks travel with a drag, and the filled half
starts at its peg, so its marks stand still and new ones appear at the ball.

- **The decision and its reason.** Both halves are hatched from the ball outwards, the filled one
  backwards: 4 dp from the ball, every 6, stopping 2 dp short of the peg, the engine's spacing
  mirrored. A drag now carries both halves' marks with the ball. A departure (D13): every slider cell
  whose filled half differs is listed in `ToonDepartures` (`Slider_`). `toonSliderScene` keeps
  `asDesigned`, which hatches as the engine does, and the cells are still held to the engine's
  frames that way (`EngineFrames.cells(…, asDesigned = true)`), so everything but the hatching stays
  checked.
- Rejected: hatching both halves from their pegs — the empty half would stand still instead.
- AC: at any value both halves' nearest marks stand 4, 10, 16, 22, 28 dp from the ball; drawn as
  designed, every slider cell is the engine's.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonSlider.kt`.

## Findings (2026-11-03)

- `ToonSliderTest` reads the hatch layers at 0.3, 0.31, 0.5 and 0.77: the five marks nearest the
  ball on each half stand at the same offsets. Mutant: the filled half hatched from its peg again —
  its marks stand at −5.6, −11.6 … at 0.3, and the as-designed engine comparison fails by 2 dp.
- **Goldens.** Fifteen cells re-recorded and looked at: Default, Dragging, Focused, Full and
  Disabled in the three themes; the filled half's marks shift by up to 6 dp, nothing else changes.
  The eighteen grab-strip cells did not change: at 0.5 on a 200 dp rope the ball stands 86 dp from
  the first mark, a whole number of steps, so both rules put the marks in the same places.
  `InkParityTest` skips the slider cells now, by the registry.
