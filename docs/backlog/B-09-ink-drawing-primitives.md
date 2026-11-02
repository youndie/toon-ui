---
id: B-09
title: "Ink drawing primitives: body with hard shadow and night rim, focus ring, placement"
status: done
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
  about a pivot. No geometry cache: B-05 measured geometry at 2.4 ms of a 15 ms frame, and a size-keyed cache missed
  on every animated frame (research H1).
- Rejected: `graphicsLayer.scale` on a shape with a stroke. It scales the ink, which the brief
  forbids (research §1.2, squash).

- AC: the Divider and a plain body match B-06's references within the floor in all three themes.
- Anchors: `reference/design-system/toon-engine.js`.

## Done (2026-11-02)

- `draw/ToonInk.kt`: `smoothPath` (the engine's `smooth`), `polygonPath` (how `inkLine`, `star` and
  `burst` are written), `drawInkBody` (shadow, ink, night rim with the fill 1.6 dp lower),
  `drawFocusRing` (dashed 7/5, 2.5 dp, round caps) and `place`. They are internal: components use
  them, consumers do not.
- No geometry cache, per B-05.
- Parity, with the 6 new Body references from `scripts/design-references.mjs` (the Principles
  page's `blk()`, plain and focused, per theme): plain 0.71 / 0.16 / 0.32 %, focused 1.49 / 1.09 /
  0.47 % (light / night / Bright Cel), all within the Divider's floor of 1.54 %. The Divider fixtures
  now draw through these primitives and the tokens, and their numbers and goldens did not move.
- Mutants: the night fill not lowered (rim hidden) took night's plain body from 0.16 % to 6.02 %
  (DIFF). A 5/7 dash for 7/5 took the focused bodies to 3.63 / 3.22 / 2.47 %: above the floor,
  under viddik's 5 %. That is why parity is read against the floor.
