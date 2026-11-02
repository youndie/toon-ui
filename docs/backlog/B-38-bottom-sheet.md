---
id: B-38
title: "BottomSheet: a sheet with a tab that slides up with overshoot"
status: done
priority: P2
size: M
stage: stage-2-components
epic: feature-v1-components
blocked_by: [B-10, B-22]
---

# B-38 — BottomSheet: a sheet with a tab that slides up with overshoot

A v1 component (brief §«Инвентарь»). It arrived with the second snapshot of the design (research
§1.1): designed in `reference/design-system/ToonBottomSheet.dc.html`, drawn by `sheet` and `sheetMove` in the engine, specified on the
`toon-ui v1 Components` page.

- **What to build.** Port the renderer and its curve: slides up on its tongue with ~16 % overshoot (spring ζ 0.5, ω 22), the scrim darkens over 200 ms, and it falls in 220 ms on close; it fills its parent, as the Dialog does.
- Built on the shared pieces like every MVP component (B-04, B-07…B-10, B-12). Props follow the dc
  file's `data-props`, and the reduce-motion word comes from the v1 page (D8).
- The renderer runs in Node, so its goldens come from B-03's harness like the MVP ones.

- AC: the v1 page's cells and frame strips within the floor in three themes; reduce motion as
  specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonBottomSheet.dc.html`, `reference/design-system/toon-ui v1 Components.dc.html`, `reference/design-system/toon-engine.js`.

## Findings (2026-11-02)

- **Done as decided**, the engine's `sheet` and `sheetMove` with the kit's `C.sheet`. Props follow
  `data-props`: `items` (a `ToonSheetItem` each, label and icon), `open`, `title` ("Share plan"),
  `onSelect`, `onClose`. Like the dialog it fills its parent; the sheet is as tall as its rows, 110
  plus 72 a row, and leaves 80 dp of the screen above it. A release on a row chooses it, above the
  tongue closes; closed, it takes no input. The arrow keys walk the rows, Enter or Space chooses and
  Escape closes; a screen reader has the title as the pane's, a dismiss, and an action per row.
- **Timing is the kit's:** the open it mounts with plays 600 ms and a change either way 400; a close
  is drawn for 260 ms, falling past its height (1.1 × at 220) before it goes.
- **Frames:** the page's two cells and five open frames, over its three cards, in three themes, are
  the engine's (21 cells, worst coordinate 0.050 dp). The page draws its own rows, so the kit's
  scene — the sheet with a list item per action — is held to the engine by twelve probes, two and
  four rows at rest and opening (worst 0.050 dp). `sheetMove` within 3·10⁻⁵ both ways, the step of a
  Float at 286. Fifteen mutants caught.
- **Pixels:** every cell under the Divider's ink floor.
- **Reduce motion** is the v1 page's `fade 120` both ways: the sheet fades in and out at rest, the
  scrim with it.
- **Not built, because the kit has none:** a drag. The page's "Dragged down" cell is a drawing (the
  sheet 60 dp down, the scrim at 70 %); `C.sheet` takes a release and nothing else.
- **B-34, looked at first:** the design project still serves `toon-engine.js?v=10` and
  `toon-kit.js?v=11`, with the same files, so there was nothing to re-import.
