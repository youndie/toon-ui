---
id: B-09
title: "Ink drawing primitives: body with hard shadow and night rim, focus ring, placement"
status: open
priority: P1
size: M
stage: stage-1-materials
blocked_by: [B-04, B-05, B-06]
---

# B-09 — Ink drawing primitives: body with hard shadow and night rim, focus ring, placement

Every renderer in the engine draws through `body`, `ring`, `place`, `scaleAbout` and `cat`
(research §1.2). The port needs the same small set in a `DrawScope`.

- **The decision and its reason.** `drawInkBody(shape, fill, shadow, rim)` paints the outline
  offset by the shadow, the outline in ink, the rim fill (night only), and the fill shifted 1.6 dp
  down. A focus ring is drawn as the dashed wobble at 6 dp outset. Placement and rotation work
  about a pivot. Caching follows B-05's verdict.
- Rejected: `graphicsLayer.scale` on a shape with a stroke. It scales the ink, which the brief
  forbids (research §1.2, squash).

- AC: the Divider and a plain body match B-06's references within the floor in all three themes.
- Anchors: `reference/design-system/toon-engine.js`.
