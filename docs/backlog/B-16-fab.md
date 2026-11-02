---
id: B-16
title: "FAB: regular and extended, lands with dust"
status: done
priority: P1
size: S
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-12]
---

# B-16 — FAB: regular and extended, lands with dust

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonFab.dc.html` and drawn by `fab` in the
engine.

- **What to build.** Port `fab` and `fabAppear` (snap-hold 90 ms, fall, land, dust). The idle slot comes from the budget; the default `idle: true` of the dc file is kept, and still counted.
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: the FAB cells and land strip within the floor; reduce motion is `fade 120`; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonFab.dc.html`, `reference/design-system/toon-engine.js`.

## Findings (2026-11-02)

- **Done as decided**, the engine's `fab` and `fabAppear` with the kit's `C.fab`. Props: `icon`
  (default `plus`), `label` (empty for the round FAB), `land`, `delay`, `enabled` (the dc file's
  `disabled`), `idle` (default true, counted against the idle budget), `onClick`; `contentDescription`
  is added for the round FAB, whose icon is all it shows.
- **Frames:** all five MVP page cells and six landing frames in three themes are the engine's (33
  cells, worst coordinate 0.050 dp); `fabAppear` within 7.6·10⁻⁶ at every millisecond. Eight mutants
  are caught — the dash pattern only after `EngineFrames` learnt to compare dashes, which it had not.
- **Pixels:** the round FAB within 0.87 % of its cell; the extended one 4.0–5.2 %, all of it the
  label's measured width (research D3), within the Divider's ink floor.
- **The references were wrong** for anything sized from text: the page built its scene before the
  fonts loaded. Fixed here for every reference (research D3); the text button's references moved.
- **Reduce motion** is the MVP page's `fade 120`: the landed FAB fades in over 120 ms, no fall and no
  dust; idle is off.
