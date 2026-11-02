---
id: B-42
title: "Pull-to-refresh: a rubber band that flings the spinner into place"
status: done
priority: P2
size: M
stage: stage-2-components
epic: feature-v1-components
blocked_by: [B-25]
---

# B-42 — Pull-to-refresh: a rubber band that flings the spinner into place

A v1 component (brief §«Инвентарь»). It arrived with the second snapshot of the design (research
§1.1): designed in `reference/design-system/ToonPullToRefresh.dc.html`, drawn by `pull` and `pullRelease` in the engine, specified on the
`toon-ui v1 Components` page.

- **What to build.** Port the renderer and its curve: the band stretches with the finger at 0.7× and thins; past 40 dp the release snaps it back and flings the blob up into the spinning progress blob (B-25). The caller owns `refreshing`.
- Built on the shared pieces like every MVP component (B-04, B-07…B-10, B-12). Props follow the dc
  file's `data-props`, and the reduce-motion word comes from the v1 page (D8).
- The renderer runs in Node, so its goldens come from B-03's harness like the MVP ones.

- AC: the v1 page's cells and frame strips within the floor in three themes; reduce motion as
  specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonPullToRefresh.dc.html`, `reference/design-system/toon-ui v1 Components.dc.html`, `reference/design-system/toon-engine.js`.

## Findings (2026-11-02)

- **Done as decided**, the engine's `pull` and `pullRelease` with the kit's `C.pullrefresh`, as
  `ToonPullToRefresh`. Props follow `data-props`: `refreshing`, `width` (360), `onRefresh`, `onPull`.
  The strip is 140 dp tall and clips, as the kit's box does. A drag down it stretches the band and
  reports the distance; let go past 40 dp, it calls `onRefresh` and spins until the caller turns
  `refreshing` false, when the spinner shrinks away in 260 ms. While refreshing it takes no pull. A
  screen reader is offered a "Refresh" action in place of the drag, and hears an indeterminate
  progress while one runs.
- **The finger counts at 0.8, not 0.7.** The item's 0.7× is the page's content offset (`off = d × 0.7`
  in `pullScene`); the kit's `move` takes the finger's travel at 0.8, up to 120 dp. The port follows
  the kit, and `onPull` reports the band's distance for the caller to move its content by 70 %.
- **The caller owns `refreshing`, as in the kit:** a pull that starts a refresh spins until
  `refreshing` has gone true and back to false. A caller that never sets it leaves it spinning.
- **Not built, because the kit has none:** nested scrolling. The strip takes its own drag, as
  `C.pullrefresh` does; a list above it does not hand its overscroll over.
- **Frames:** the page's three cells, four pull and five release frames, over its cards, in three
  themes, are the engine's (36 cells, worst coordinate 0.050 dp); `pullRelease` within 1.5·10⁻⁵.
  Fifteen mutants caught, the fling's 400 ms by a check added for it.
- **Pixels:** every cell under the Divider's ink floor.
- **Reduce motion** is the v1 page's `static spinner` for both: the band still follows the finger (it
  is the finger, not an animation), the release lands on the spinner without the fling, the spinner
  does not turn, and it goes at once when done — and no frame is asked for while nothing moves.
- **B-34, looked at first:** the design project still serves `toon-engine.js?v=10` and
  `toon-kit.js?v=11`, so there was nothing to re-import.
