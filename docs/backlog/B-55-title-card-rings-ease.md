---
id: B-55
title: "Title card rings ease into place, and can loop"
status: done
priority: P2
size: S
stage: stage-2-components
epic: feature-components
---

# B-55 — Title card rings ease into place, and can loop

The title card's rings travel 0.04 dp a ms for the 900 ms entrance and then jump to where they rest
(`splashAt` at a long time after: 36 dp travelled against 16 at rest, modulo the 34 dp period). The
owner: "the background animates abruptly — make it smooth, maybe with the rings speeding up and
slowing down, and add a loop version so it shows" (2026-11-03). Departs from the engine (D13).

- **The decision and its reason.** In the entrance the rings travel two periods with an ease-in-out
  over 1400 ms, ending where they rest, so the entrance starts and ends on the resting picture. A
  `loop` parameter keeps them flowing, about a period every 1.6 s, the speed surging and the ring
  widths swelling in a turning wave (the owner, the same day: "interpolate the rings' width and
  speed, somehow psychedelically"), for a splash screen that waits; reduce motion holds them still.
- AC: no jump between the last entrance frame and rest; the drawn picture is continuous across
  periods, in the entrance and in the loop; the loop varies the ring widths and the speed.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonTitleCard.kt`.

## Findings (2026-11-03)

- The jump was 14 dp: the engine's rings travel 36 dp in the 900 ms entrance and rest at 16 dp of
  their 34 dp period (`splashAt` a long time after), so the last frame of the entrance stood at 2.
- `toonRingPhase` moves them exactly one period with a cubic ease-in-out, from a period behind rest
  to rest, so the entrance ends on the resting frame; with `loop` (new parameter of `ToonTitleCard`)
  they move one period every 1.6 s with the same ease, and only the phase modulo the period is drawn,
  so the loop has no seam and no growing number. Reduce motion holds them still. The kit shows a
  looping card beside the empty state.
- **No cell departed.** The page's enter strip is built from `splashAt` itself, which stays the
  engine's curve; only the live clock (`toonTitleCardFrameAt`) takes the new phase. `ToonTitleCardTest`
  holds it: no step over 0.2 dp a ms (the ease peaks at 0.11; the old jump was 14 dp), the entrance
  ends at rest, the loop moves and stays seamless for 6 s.

## Iteration 2 (2026-11-03): the colours jumped

The owner, in the running kit: "the title card animation (background) jerks". The first iteration
eased the phase but drew it modulo one period from the first colour, while the two colours make
the picture repeat every **two** periods: each time the phase wrapped, every ring swapped colour —
once in the entrance (at 567 ms) and every 1.6 s in the loop. The test measured the phase modulo
the period and could not see it.

- `toonRingRadii` lists the rings from the outside in with their colour, and a whole period
  travelled now swaps the colours, so the picture is a continuous function of the phase. For a
  phase in 0..34, every frame the engine draws, it is the engine's list unchanged: no cell departs.
- `toonRings` replaces `toonRingPhase`: the entrance moves two periods (68 dp) eased over 1400 ms,
  the plaque still landing at 900; the loop's speed is the average reached from rest plus a surge
  over 4.7 s (0.14 to 1.86 of the average, never backwards), and the widths bend in a wave, a
  crest every 3.7 rings turning in 2.9 s, from 0.41 to 1.59 of the period. Both come in from rest
  over 700 ms. With the wave on, one more ring is drawn outside so the edge stays covered.
- `ToonTitleCardTest` now reads the drawn picture: the colour at 700 distances from the centre,
  frame by frame over 12 s, flips at most 40 samples a millisecond (a swap flips hundreds); the
  entrance starts and ends on the resting picture; the loop never runs backwards, its speed varies
  more than 3×, its widths more than 2.5×, and the rings stay in order. Mutant: the colour taken
  from the reduced phase again fails at 567 ms.
