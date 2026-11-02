---
id: B-27
title: "NavigationBar: jumping icon, dust, paint-stroke indicator"
status: done
priority: P1
size: S
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-12]
---

# B-27 — NavigationBar: jumping icon, dust, paint-stroke indicator

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonNavBar.dc.html` and drawn by `navBar` in the
engine.

- **What to build.** Port `navBar` and `navSelect`: the icon jumps 8 dp and lands with dust, and the paint stroke smears over. The badge dot sits on an item.
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: cells and the select strip within the floor; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonNavBar.dc.html`, `reference/design-system/toon-engine.js`.

## Findings (2026-11-02)

- **Done as decided**, the engine's `navBar` and `navSelect` with the kit's `C.navbar`. Props:
  `items` (glyph and label each; the page's four destinations are not a default), `selected`,
  `badgeIndex` (−1: none), `width` (390), `onChange`. A release on another item selects it; the
  keyboard and a screen reader's actions are added (service quirks).
- **A rounding the comparison did not know:** the pressed icon is scaled about x 146.25, which the
  engine writes as `translate(146.3 …) … translate(-146.2 …)`. The port now carries that exact
  difference (`ToonScene.scaledAbout`), zero on the 0.1 grid; a mutant that drops it is caught.
- **Frames:** the five MVP page cells and the five select frames, in three themes, are the engine's
  (30 cells, worst coordinate 0.050 dp); `navSelect` within 3·10⁻⁵ (two Float steps at 276). Eleven
  mutants caught.
- **Pixels:** within 2.15 % of the cell; 2.52 % in ink at worst, under the Divider's floor (14.81 %).
- **Reduce motion** is the MVP page's `instant`.
