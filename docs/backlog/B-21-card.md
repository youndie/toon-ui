---
id: B-21
title: "Card: tilt, drop-in with dust and cascade, press lean, swipe dismiss with a hole"
status: done
priority: P1
size: L
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-12]
---

# B-21 — Card: tilt, drop-in with dust and cascade, press lean, swipe dismiss with a hole

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonCard.dc.html` and drawn by `card` in the
engine.

- **What to build.** Port `card`, `cardAppear` (with the 40 ms cascade), the press lean toward the finger, and `cardDismiss`: speed lines, then the hole FX on the spot, and the neighbours closing up. This is the scene B-05 measures.
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: cells and strips within the floor; a swipe past 80 dp dismisses and fires `card.gone`; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonCard.dc.html`, `reference/design-system/toon-engine.js`.

## Findings (2026-11-02)

- **Done as decided**, the engine's `card`, `cardAppear` and `cardDismiss` with the kit's `C.card`:
  the tilt, the drop-in with dust (`delay` makes the cascade), the press lean toward the finger, the
  swipe past 80 dp that throws the card with speed lines and the hole and calls `onDismiss`. Props
  as the dc file's; the demo strings are not kept as defaults.
- **`card.gone` fires** on a dismiss in either direction (`ToonCardTest`), and the FX host draws the
  hole; outside a host the card draws it.
- **Frames:** the four Core page cells and the appear, press and dismiss strips in three themes —
  the dismiss strip composed as the page's `cardStack` — are the engine's (54 cells, worst coordinate
  0.050 dp). That needed the frame model to carry `place`'s turn about a pivot and `scaleAbout`, with
  the roundings the engine writes them with. `cardAppear` and `cardDismiss` within 3·10⁻⁵ at every
  millisecond. Ten mutants caught.
- **Pixels:** within 3.41 % of the cell, the card's texts, and within the text floor in ink.
- **Reduce motion** per the Core page: appear and dismiss `fade 120`, press `instant`.
- **Not done here:** the neighbours closing up after a dismiss and the list's cascade belong to a
  list, which the library does not have yet: B-46. The dust per card in a cascade waits on B-45.
