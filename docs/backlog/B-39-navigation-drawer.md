---
id: B-39
title: "NavigationDrawer: a door on three hinges that swings and shivers"
status: done
priority: P2
size: M
stage: stage-2-components
epic: feature-v1-components
blocked_by: [B-10]
---

# B-39 — NavigationDrawer: a door on three hinges that swings and shivers

A v1 component (brief §«Инвентарь»). It arrived with the second snapshot of the design (research
§1.1): designed in `reference/design-system/ToonNavDrawer.dc.html`, drawn by `drawer`, `drawerOpen` and `drawerClose` in the engine, specified on the
`toon-ui v1 Components` page.

- **What to build.** Port the renderer and its curve: the door swings open with a perspective skew, overshoots and shivers ±0.7° on its hinges; labels fade in once it is flat. Perspective skew is not a plain affine scale, so how it is drawn in Compose is part of the item.
- Built on the shared pieces like every MVP component (B-04, B-07…B-10, B-12). Props follow the dc
  file's `data-props`, and the reduce-motion word comes from the v1 page (D8).
- The renderer runs in Node, so its goldens come from B-03's harness like the MVP ones.

- AC: the v1 page's cells and frame strips within the floor in three themes; reduce motion as
  specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonNavDrawer.dc.html`, `reference/design-system/toon-ui v1 Components.dc.html`, `reference/design-system/toon-engine.js`.

## Findings (2026-11-02)

- **Done as decided**, the engine's `drawer`, `drawerOpen` and `drawerClose` with the kit's
  `C.drawer`, as `ToonNavigationDrawer` (named as the navigation bar is). Props follow `data-props`:
  `items` (a `ToonDrawerItem` each, label and icon), `open`, `title` ("Saturday"), `selected`,
  `onSelect`, `onClose`. Like the dialog it fills its parent; the door is 300 dp wide, or the screen
  less 60. A release on a row chooses it, right of the door closes; closed, it takes no input. The
  arrow keys walk the rows from the selected one, Enter or Space chooses and Escape closes; a screen
  reader has the title as the pane's, the selected row as its state, a dismiss, and an action per
  row. The engine draws no keyboard focus, so the focused row is ringed as a list item's is.
- **The skew is affine, not a perspective.** The item expected otherwise; the engine prefixes the
  door with `matrix(sx k 0 1 0 0) rotate(r 0 H/2)`, a squeeze across and a shear down, so
  `ToonScene.skewedAbout` composes it into each layer's transform, strokes and all, as SVG does. The
  frame comparison learnt `matrix(…)`, written to three decimals, with the rounding that allows.
- **Timing is the kit's:** the open it mounts with plays 600 ms and a change either way 400; a close
  is drawn for 240 ms.
- **Frames:** the page's three cells and five open frames, over its three cards, in three themes,
  are the engine's (24 cells, worst coordinate 0.050 dp), and so are three probes of a press on the
  selected row, which no cell shows. `drawerOpen` and `drawerClose` within 6·10⁻⁷. Sixteen mutants
  caught, the press-on-selection one by the probe added for it.
- **Pixels:** every cell under the Divider's ink floor. The page's own filter lets the cards' times
  show over the door while it overshoots; the port draws the same.
- **Reduce motion** is the v1 page's `fade 120` both ways: the door fades in and out flat.
- **B-34, looked at first:** the design project still serves `toon-engine.js?v=10` and
  `toon-kit.js?v=11`, so there was nothing to re-import.
