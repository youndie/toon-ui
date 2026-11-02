---
id: B-12
title: "FX layer: eight effects and the named events that fire them"
status: wip
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
