---
id: B-28
title: "Tabs: bookmarks on an ink shelf, the indicator smears across"
status: done
priority: P1
size: S
stage: stage-2-components
epic: feature-components
blocked_by: [B-10]
---

# B-28 — Tabs: bookmarks on an ink shelf, the indicator smears across

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonTabs.dc.html` and drawn by `tabs` in the
engine.

- **What to build.** Port `tabs` and `tabsSwitch`, with the content offset the strip names.
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: cells and the switch strip within the floor; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonTabs.dc.html`, `reference/design-system/toon-engine.js`.

## Findings (2026-11-02)

- **Done as decided**, the engine's `tabs` and `tabsSwitch` with the kit's `C.tabs`. Props: `labels`
  (the page's three are not a default), `selected`, `width` (342), `onChange`; a release on another
  tab switches to it, and the keyboard and a screen reader's actions are added, as in the navigation
  bar.
- **The content offset the strip names** is `ToonTabsState.contentOffset`, the engine's `cx`: the
  caller slides its content by it (service quirks).
- **Frames:** the five MVP page cells and the five switch frames, in three themes, are the engine's
  (30 cells, worst coordinate 0.050 dp); `tabsSwitch` within 1.5·10⁻⁵. Nine mutants caught.
- **Pixels:** within 4.66 % of the cell, all of it text: three 16 sp labels in a 358 × 66 cell. In
  ink the labels take Bright Cel to 18.15 %, past the Divider's floor, so the tabs are read against
  the text floor (53.56 %) and a label-less twin, `TabsShape`, holds the bookmark, the shelf and the
  press to the Divider's: within 0.79 % of its reference.
- **Reduce motion** is the MVP page's `instant`.
