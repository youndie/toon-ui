---
id: B-15
title: "IconButton and Badge"
status: done
priority: P1
size: S
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-11, B-12]
---

# B-15 — IconButton and Badge

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonIconButton.dc.html` and drawn by `iconBtn` and `badge` in the
engine.

- **What to build.** IconButton: the hand-drawn circle squashes like a button, and the icon hops on its own spring (`iconBounce`); a toggle adds the flash. Badge: the star-burst pop (`badgeAppear`), the count swap (`badgeCount`), the dot, and the overflow `99+`.
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: the MVP page's IconButton and Badge cells and strips within the floor; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonIconButton.dc.html`, `reference/design-system/toon-engine.js`.

## Findings (2026-11-02)

- **Done as decided**, the engine's `iconBtn` and `badge` with the kit's `C.iconbutton` and `C.badge`.
  Props: `icon`, `variant`, `selected`, `toggle`, `enabled` (the dc file's `disabled`), `onClick`,
  `onSelectedChange` (`onChange`); `contentDescription` is added, since an icon alone says nothing.
  The badge takes `count` and `dot`.
- **Frames:** every MVP page cell is the engine's frame — 39 icon button cells, 45 badge cells
  including the badge on an icon button — worst coordinate 0.050 dp, the engine's rounding. Ten
  mutants (shadow, insets, the icon's sink, the selected fill, the ring's seed, the hop, the burst's
  width and seed, the number's baseline, the old number's flight) are all caught; the hop and the
  flight by the curve test too.
- **Pixels:** icon buttons within 0.99 % of their cell, under the Divider's 1.54 %; badges, which
  carry a number in a 60 × 40 cell, 2.0–4.25 % of the cell and within the text floor in ink.
- **Reduce motion**, the MVP page's `instant` for the tap and both badge animations: the hop and the
  pops are over at once, no flash.
- The frame model grew for this item (icons, several texts, placement), which every later component
  uses (research D3).
