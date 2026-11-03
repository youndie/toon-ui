---
id: B-57
title: "A transition keeps both screens' state, and the new screen shows through the hole"
status: done
priority: P2
size: S
stage: stage-2-components
epic: feature-components
---

# B-57 — A transition keeps both screens' state, and the new screen shows through the hole

The owner, in the running kit (2026-11-03): "debug the transition too — the hole is crooked". Drawn
frame by frame at the kit's size (300 × 360, a title card on each screen), the hole transition had
three faults:

- the old screen's plaque popped in from nothing as the hole opened, and the new screen's plaque
  vanished and popped in again the moment the transition ended;
- the hole stopped short of the bottom of the box, so a strip of the old screen showed under a torn
  edge until the new screen faded over it;
- in the kit it burst from the page's default card, 82 dp from the top, where the kit draws nothing;
- from 420 ms to 460 the old screen showed through the new one fading in: two titles over each other.

- **The decision and its reason.** `ToonTransitionHost` composed each screen with `key(screen)` in
  whichever slot of the frame it occupied, so a screen moving from the resting slot to the leaving
  one, or from the arriving one to rest, was a new composition with fresh state: every screen with
  an entrance of its own played it twice. Each screen is now one `movableContentOf`, kept while it
  is the current or the leaving screen. The hole's growth, ×10 in the engine, is enough for the
  page's 180 × 320 screen from its second card and no more; `toonHoleGrow` keeps ×10 where that
  covers the box and grows further where it does not, so no cell of the page changes. The kit's
  host passes the plaque as its `origin`. The engine drops the silhouette at 420 while the new
  screen fades in until 460; from 380, when the hole covers the box and the fade starts, the old
  screen is hidden and the hole's inside fills the box under the fade. The alternative, a growth fixed to a larger factor,
  changes the page's cells and still fails on a box tall enough.
- AC: the new screen shows through the hole while it grows; each screen is composed once across
  a transition, for every kind, with and without reduce motion; the hole leaves none of the old screen at 379 ms on a box larger than the page's; the
  page's own box grows exactly as the engine does; from 380 ms the old screen shows nowhere.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonTransitionHost.kt`,
  `sample/src/commonMain/kotlin/io/github/youndie/toon/sample/ToonUiSample.kt`.

## Findings (2026-11-03)

- `ToonTransitionHostTest` holds both: a screen's `remember` runs once per visit (it ran twice for
  each screen, for every kind, before), and at 379 ms on 300 × 360 not one pixel of the old screen
  shows (854 did with the fixed ×10). Mutants: the screens composed plainly again — `{list=2,
  plan=2}`; the growth fixed at ×10 — 854 pixels. A third test renders the frame at 380…459 ms
  over a red and over a green old screen and finds the two identical; with the old screen shown
  throughout, they differ at 420.
- **No cell departed.** On the page's 180 × 320 box from its second card the box needs ×9.7, so the
  growth stays the engine's ×10; the old screen is hidden only from 380, where no cell of the page
  stands but 460, which the new screen covers whole. The transition goldens and engine frames are
  unchanged.

## Iteration 2 (2026-11-03): the hole showed nothing

The owner, on the fixed kit: "the hole is still strange". Frame by frame it was: the plaque turned
into a blot of the surface's colour, the blot grew over the screen, and for a quarter of a second
(from about 200 ms to 380) the box was empty but for the stars before the new screen faded in. A
card bursting through the screen led into nothing.

- **A departure (D13).** The new screen now shows through the cut from the moment it opens: the old
  screen, the hole's paper edge, the new screen clipped to the hole's silhouette, the stars on top.
  At 380 ms the hole covers the box and the new screen stands whole; nothing fades. The growth and
  the stars are still the engine's `holeScene`, and `toonHoleAt` still matches its curve, `newOp`
  included, though the host no longer uses the fade.
- `toonHoleCut` gives the edge and the cut grown about the card, `toonHoleStarsScene` the stars;
  `toonHoleTransitionScene`, which filled the cut, is gone.
- `ToonTransitionHostTest`: at 60, 120, 200 and 300 ms on 300 × 360 the card's centre shows the new
  screen and the bottom corner the old one. Mutant: the new screen hidden until 380 — fails at 60.
- **Goldens.** Four cells drawn mid-burst were re-recorded, each looked at:
  `Transition_Hole_200ms_InkGouacheLight`, `_330ms_InkGouacheLight`, `_260ms_InkGouacheNight` and
  `_260ms_BrightCel` now show the plan screen inside the hole where they showed the flat surface.
  The cells at 0, 80 and 460 ms did not change. The transition cells have no design reference and
  no engine frame — the page's only record of the hole is its curve — so `ToonDepartures`, which
  lists cells of those two, has nothing to name.
