---
id: B-22
title: "ListItem and Divider"
status: done
priority: P1
size: S
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-09]
---

# B-22 — ListItem and Divider

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonListItem.dc.html` and drawn by `listItem` and `divider` in the
engine.

- **What to build.** ListItem: the sticker icon, the trailing chevron, switch or check, and the rule that bows like a plank when pressed (`listPress`). Divider: the tapering `inkLine` in s/m/l, inset or full.
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: cells and the press strip within the floor; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonListItem.dc.html`, `reference/design-system/toon-engine.js`.

## Findings (2026-11-02)

- **Done as decided**, the engine's `listItem`, `listPress` and `divider` with the kit's
  `C.listitem` and `C.divider`. ListItem props: `title`, `icon` (default `calendar`), `sub`,
  `trailing` (chevron, switch, check, none), `on`, `index`, `last`, `width`, `enabled`, `onClick`,
  `onCheckedChange` (`onChange`); the trailing switch and check are B-17's and B-18's frames. Divider:
  `width`, `inset`, `weight` (s, m, l), `seed`. Demo strings are not kept as defaults.
- **Frames:** the six MVP page list item cells and six press frames, and the four divider cells, in
  three themes, are the engine's (36 and 12 cells, worst coordinate 0.050 dp); `listPress` within
  6·10⁻⁷. The Divider's fixtures, the floor's ruler since B-06, come from the shared cell list now;
  none of its references or goldens moved, and the floor is still 14.81 %.
- **A mutant no page frame could see:** the press tint's opacity, `c01(sag × 1.4)`, is 0 or 1 in
  every page frame. Three probe frames (sag 0.2, 0.4, 0.6, where a press passes in its first 30 ms)
  are recorded from the engine for the frame test only, and catch it. Nine mutants caught.
- **Pixels:** within 2.47 % of the cell, the row's texts, and within the text floor in ink.
- **Reduce motion** is the MVP page's `instant`: down while pressed, flat once let go.
