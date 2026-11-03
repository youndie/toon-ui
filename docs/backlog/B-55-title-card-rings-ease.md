---
id: B-55
title: "Title card rings ease into place, and can loop"
status: open
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
- AC: no jump between the last entrance frame and rest; the loop is continuous across periods;
  the entrance cells that changed are listed in `ToonDepartures` with their goldens re-recorded.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonTitleCard.kt`.
