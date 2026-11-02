---
id: B-41
title: "Segmented button: a shared outline and a paint-drop selection"
status: done
priority: P2
size: S
stage: stage-2-components
epic: feature-v1-components
blocked_by: [B-10]
---

# B-41 — Segmented button: a shared outline and a paint-drop selection

A v1 component (brief §«Инвентарь»). It arrived with the second snapshot of the design (research
§1.1): designed in `reference/design-system/ToonSegmented.dc.html`, drawn by `segmented` and `segSwitch` in the engine, specified on the
`toon-ui v1 Components` page.

- **What to build.** Port the renderer and its curve: the drop stretches toward the new segment, thins as it stretches, and its tail follows 80 ms later; pressed, focused and disabled states.
- Built on the shared pieces like every MVP component (B-04, B-07…B-10, B-12). Props follow the dc
  file's `data-props`, and the reduce-motion word comes from the v1 page (D8).
- The renderer runs in Node, so its goldens come from B-03's harness like the MVP ones.

- AC: the v1 page's cells and frame strips within the floor in three themes; reduce motion as
  specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonSegmented.dc.html`, `reference/design-system/toon-ui v1 Components.dc.html`, `reference/design-system/toon-engine.js`.

## Findings (2026-11-02)

- **Done as decided**, the engine's `segmented` and `segSwitch` with the kit's `C.segmented`, as
  `ToonSegmented`. Props follow `data-props`: `labels`, `selected`, `width` (300), `disabled` as
  `enabled`, `onChange`. A release on another segment moves the drop and reports it; the arrow keys
  walk the segments and Enter or Space chooses; a screen reader has the selected segment as the state
  and an action per segment. Disabled, it takes no input and says so.
- **Laid out 48 dp, drawn 44.** The kit's box is 44 dp; the brief's target is 48 (research §1.6), so
  the drawing sits 2 dp down in a 48 dp box, as the chip's does. Nothing the design draws moves.
- **Shared geometry, found here:** a side of 46 dp at a step of 4 is 11.5 steps. The engine's double
  makes it 11.500000000000004 and rounds up; a Float made it 11.499998 and rounded down, one point
  short — the drop's frame at 0 ms. Sample counts now round half up with 10⁻⁵ of slack
  (`halfUp` in `ToonGeometry.kt`); every other frame and golden held.
- **Frames:** the page's five cells and five switch frames, in three themes, are the engine's (30
  cells, worst coordinate 0.050 dp), and so are three probes of a press on the selected segment.
  `segSwitch` within two Float steps at its 283 dp overshoot (6.1·10⁻⁵). Thirteen mutants caught,
  three of them by a probe, a 699 ms check and a focus-drawing check added for them.
- **Pixels:** every cell under the Divider's ink floor.
- **Reduce motion** is the v1 page's `instant`: the drop moves at once.
- **B-34, looked at first:** the design project still serves `toon-engine.js?v=10` and
  `toon-kit.js?v=11`, so there was nothing to re-import.
