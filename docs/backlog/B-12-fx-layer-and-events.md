---
id: B-12
title: "FX layer: eight effects and the named events that fire them"
status: done
priority: P1
size: L
stage: stage-1-materials
blocked_by: [B-09]
---

# B-12 — FX layer: eight effects and the named events that fire them

FX are a separate draw layer above components, fired by named events (`card.land`, `field.error`,
`dialog.pop` …), each played once in 200–600 ms. The same names will carry sounds later (brief
§«Не-цели»).

- **The decision and its reason.** Port `dust`, `stars`, `speed`, `flash`, `vibe`, `irisScene`,
  `hole` and `sweat`. An `FxHost` overlays the screen, and components emit events with a position.
  The events form a public sealed hierarchy whose names are the spec's, so a consumer can listen
  (for sound) without touching drawing. Each FX takes its reduce-motion word from the FX page
  (mostly `off`, flash `fade 120`, iris `cross-fade 120`).
- Rejected: drawing FX inside each component's own canvas. It clips at the box, and dust spreads
  past it.

- AC: each FX within the floor against its frame strip; reduce motion turns each off or fades as
  specified; an event listener test receives `card.land`.
- Anchors: `reference/design-system/toon-engine.js`, `reference/design-system/toon-ui FX.dc.html`.

## Findings (2026-11-02)

- **Done as decided.** The eight effects and their curves are in
  `toon-core/src/commonMain/kotlin/io/github/youndie/toon/fx/ToonFxDraw.kt`; `ToonFxHost` draws them above
  its content and `ToonFxEvent` carries the FX page's names. `irisAt` within 6.0·10⁻⁷ and `shake`
  within 2.2·10⁻⁶ dp of the engine's curves; the hole's silhouette within the engine's 0.1 dp path
  rounding.
- **Pixels:** the FX page's 35 strip frames in three themes (105 cells, the effect alone on the
  stage) are under the Divider's 1.54 % at worst 0.37 %, but that floor did not catch half the
  mutants, so parity is now gated by the ink ratio (research D3): floor 14.81 %, worst FX 13.27 %,
  mutants 39–99 %. All 459 references pass it, icons and bodies included (worst 5.06 %).
- **Not a pixel question:** the hole's crack pattern (another seed measured 8.70 %, under the floor),
  hence `ToonHoleGoldensTest`; both hole mutants miss it by 2.6–2.9 dp.
- **Placement belongs to the component.** The engine places each component's effects with its own
  numbers, so the events that take a point take it (`at`, `puffs`, `spread`, `size`), defaulting to
  the FX page's block. `dust` takes `k`, `gap` and `dirs` for the navigation bar (B-27). Speed lines
  start at the element's left edge when it fired; the card's dismiss moves them with the card in the
  engine, which B-21 has to feed per frame.
- **Reduce motion** (research D8): off everywhere but the flash, which fades its settled frame out in
  120 ms. The iris's cross-fade is the scene change's (B-44). Listeners hear every event regardless.
- "action denied" on the page is `action.denied` here, so every name is dotted like the rest.
- Not done: the element's own motion the page animates under each effect (the block's fall, the
  shake's offset, the dismiss) is each component's item; `toonVibeOffset` and `toonShake` are the
  curves they will take.
