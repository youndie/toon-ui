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

- **The decision and its reason.** In the entrance the rings travel exactly one period with an
  ease-in-out, ending where they rest, so the last frame of the entrance is the resting frame. A
  `loop` parameter keeps them flowing, a period every 1.6 s with the same ease, for a splash screen
  that waits; reduce motion holds them still.
- AC: no jump between the last entrance frame and rest; the loop is continuous across periods.
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
