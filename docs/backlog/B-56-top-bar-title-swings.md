---
id: B-56
title: "The top app bar's title swings with its sign"
status: done
priority: P2
size: S
stage: stage-2-components
epic: feature-components
---

# B-56 — The top app bar's title swings with its sign

When the top app bar sways, the sign turns and its title stays level and still: labels are only
moved, never turned, by `ToonScene.placed` (the design's rule that text never tilts). The owner:
"the TopAppBar's text does not animate, only the background" (2026-11-03). Departs from the design
(D13).

- **The decision and its reason.** The title turns with the sign about the same pivot, so the sign
  swings as one piece. Other components keep their text level.
- AC: in a sway frame the title is drawn turned by the sign's angle about its pivot; at rest
  nothing changes.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonTopAppBar.kt`,
  `toon-core/src/commonMain/kotlin/io/github/youndie/toon/draw/ToonScene.kt`.

## Findings (2026-11-03)

- `drawToonTopBar` draws the sign's shapes through `placed` as before and its labels inside a
  `rotate` about the same pivot, 40 dp above the sign's middle. The composable draws through it.
- **No cell departed.** The scene (`toonTopBarScene(...).placed(...)`) is unchanged, so every engine
  frame, fixture and golden of the sway strip still shows the design's level title; the departure is
  in how the live bar draws the scene. `ToonTopAppBarTest` holds it: at rest the bar is pixel for
  pixel what it was; at 4° the title is drawn turned, against the level one.
