---
id: B-26
title: "TopAppBar: a sign on two nails that sways"
status: done
priority: P1
size: S
stage: stage-2-components
epic: feature-components
blocked_by: [B-10]
---

# B-26 — TopAppBar: a sign on two nails that sways

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonTopAppBar.dc.html` and drawn by `topBar` in the
engine.

- **What to build.** Port `topBar` and the pendulum `sway`, kicked by a scroll fling or a scrolled flip. Small and center variants, and actions from the icon set.
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: cells and the sway strip within the floor; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonTopAppBar.dc.html`, `reference/design-system/toon-engine.js`.

## Findings (2026-11-02)

- **Done as decided**, the engine's `topBar` and `sway` with the kit's `C.topbar`. Props: `title`,
  `variant` (small, center), `actions` (glyphs, search and more by default), `nav`, `scrolled`,
  `width` (390), `onNav`, `onAction` (the glyph). The demo title is not kept as a default.
- **The kick:** a tap anywhere and a change of `scrolled`, as the kit; a scroll fling through
  `ToonTopAppBarState.nestedScrollConnection`, or `kick()` — the design names the fling but the kit
  has no scroll, so the state is the library's (service quirks).
- **Frames:** the seven MVP page cells and the six sway frames, in three themes, are the engine's
  (39 cells, worst coordinate 0.050 dp); `sway` within 7·10⁻⁷. Eleven mutants caught, the sway's
  900 ms only once a frame at 890 ms was asserted.
- **Pixels:** within 2.02 % of the cell; 3.57 % in ink at worst, under the Divider's floor (14.81 %).
- **Reduce motion** is the MVP page's `off`: the sign never swings.
