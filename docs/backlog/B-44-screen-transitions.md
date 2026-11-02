---
id: B-44
title: "Screen transitions: push with smear, iris scene change, hole in the wall"
status: done
priority: P2
size: M
stage: stage-2-components
epic: feature-v1-components
blocked_by: [B-12]
---

# B-44 — Screen transitions: push with smear, iris scene change, hole in the wall

The three transitions on the `toon-ui v1 Components` page, each at most 600 ms. Push (`pushAt`,
`pushScene`, 450 ms): the old screen leans 6 dp the wrong way, then leaves left with a 1.35× smear,
and the new one flies in from the right, overshooting 12 dp. Scene change: the FX 6 iris
(`irisAt`, 600 ms). Hole in the wall (`holeScene`, 460 ms): the tapped card's cracked silhouette
(FX 7) grows until the new screen fills it.

- **The decision and its reason.** They are drawn as transitions of a navigation host the library
  owns: a container that takes the old and new content as composables. They are not tied to any
  navigation library, which would make one a dependency of the core (D2). The engine's
  `miniScreen` stands in for real screens in the references; the Compose fixture draws real
  composables, so only the motion values are compared numerically, not the pixels of the contents.
- Reduce motion: `cross-fade 120` for all three, as the page says.

- AC: the motion values match the goldens at the page's timestamps; a desktop fixture shows each
  transition between two real screens.
- Anchors: `reference/design-system/toon-ui v1 Components.dc.html`, `reference/design-system/toon-engine.js`.

## Findings (2026-11-02)

- **Done as decided**, a host the library owns: `ToonTransitionHost(target, transition, origin)
  { screen -> … }` keeps the shown screen, and when `target` changes plays `Push`, `Scene` or
  `Hole` from the old screen's composable to the new one's, both composed while it plays, only the
  new one after. It clips its box and depends on no navigation library. `origin` is the tapped
  element in dp: the iris closes on its centre and the hole bursts from it; without one, the page's
  own places (46 dp above the bottom middle, and the second card).
- **Motion, against the engine at every millisecond:** `pushAt` within two Float steps at its 186 dp
  (3·10⁻⁵), `irisAt` (the FX 6 port of B-12) within 10⁻⁵, and the hole — which has no curve of its
  own in the engine — read off `holeScene`'s frames: the silhouette's `scale(…)` within the three
  decimals the engine writes, over the 419 ms it is drawn, and the new screen's fade exactly. Durations
  are the page's: 450, 600 and 460 ms.
- **Fixtures:** the page's sixteen frames of the three, in Ink & Gouache light, and its three cells
  in night and Bright Cel, between two real screens (a list, then a plan with its button). The
  engine's references draw `miniScreen` stand-ins, so these are goldens only (`viddikVerify`); the
  motion is held numerically, as the item decided. Fourteen mutants caught, three of them in the
  host's drawing by the goldens.
- **The push smears the old screen with `graphicsLayer`, strokes and all,** as the engine scales its
  `miniScreen`: the screen is the caller's, so it cannot be rebuilt squashed.
- **Reduce motion** is the page's `cross-fade 120` for all three.
- **Found on the way:** with the frame loop in a second effect keyed on the clock's start count, the
  host's first test of the class stalled at 0 ms; one effect that switches and asks for frames fixed
  it. The other components use the two-effect shape and were not rechecked: B-51. *B-51 found the
  cause elsewhere: the clock's `age()` did not read the time while a play was pending, so the
  composition that read it then never recomposed; one effect only made the first tick come first.*
- **B-34, looked at first:** the design project still serves `toon-engine.js?v=10` and
  `toon-kit.js?v=11`, so there was nothing to re-import.
