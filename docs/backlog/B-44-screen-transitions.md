---
id: B-44
title: "Screen transitions: push with smear, iris scene change, hole in the wall"
status: open
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
