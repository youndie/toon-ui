---
id: B-28
title: "Tabs: bookmarks on an ink shelf, the indicator smears across"
status: open
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
