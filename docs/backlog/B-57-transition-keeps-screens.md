---
id: B-57
title: "A transition keeps both screens' state, and the hole covers any box"
status: done
priority: P2
size: S
stage: stage-2-components
epic: feature-components
---

# B-57 — A transition keeps both screens' state, and the hole covers any box

The owner, in the running kit (2026-11-03): "debug the transition too — the hole is crooked". Drawn
frame by frame at the kit's size (300 × 360, a title card on each screen), the hole transition had
three faults:

- the old screen's plaque popped in from nothing as the hole opened, and the new screen's plaque
  vanished and popped in again the moment the transition ended;
- the hole stopped short of the bottom of the box, so a strip of the old screen showed under a torn
  edge until the new screen faded over it;
- in the kit it burst from the page's default card, 82 dp from the top, where the kit draws nothing.

- **The decision and its reason.** `ToonTransitionHost` composed each screen with `key(screen)` in
  whichever slot of the frame it occupied, so a screen moving from the resting slot to the leaving
  one, or from the arriving one to rest, was a new composition with fresh state: every screen with
  an entrance of its own played it twice. Each screen is now one `movableContentOf`, kept while it
  is the current or the leaving screen. The hole's growth, ×10 in the engine, is enough for the
  page's 180 × 320 screen from its second card and no more; `toonHoleGrow` keeps ×10 where that
  covers the box and grows further where it does not, so no cell of the page changes. The kit's
  host passes the plaque as its `origin`. The alternative, a growth fixed to a larger factor,
  changes the page's cells and still fails on a box tall enough.
- AC: each screen is composed once across a transition, for every kind, with and without reduce
  motion; the hole leaves none of the old screen at 379 ms on a box larger than the page's; the
  page's own box grows exactly as the engine does.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonTransitionHost.kt`,
  `sample/src/commonMain/kotlin/io/github/youndie/toon/sample/ToonUiSample.kt`.

## Findings (2026-11-03)

- `ToonTransitionHostTest` holds both: a screen's `remember` runs once per visit (it ran twice for
  each screen, for every kind, before), and at 379 ms on 300 × 360 not one pixel of the old screen
  shows (854 did with the fixed ×10). Mutants: the screens composed plainly again — `{list=2,
  plan=2}`; the growth fixed at ×10 — 854 pixels.
- **No cell departed.** On the page's 180 × 320 box from its second card the box needs ×9.7, so the
  growth stays the engine's ×10, and the transition goldens and engine frames are unchanged.
