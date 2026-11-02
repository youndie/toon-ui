---
id: B-24
title: "Dialog: iris spotlight in, pop and stars out"
status: wip
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-12]
---

# B-24 — Dialog: iris spotlight in, pop and stars out

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonDialog.dc.html` and drawn by `dialog` and `dialogScene` in the
engine.

- **What to build.** Port `dialogScene`, `dialogOpen` and `dialogClose` as a full-screen layer. The destructive button draws `onDanger` on `danger` at 4.38:1 in Ink & Gouache light, below the brief's 4.5 (research §1.6). **The owner accepted it as an exception on 2026-11-02**: the design's value is kept, and B-30's contrast test lists this pair as its one named exception.
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: cells and both strips within the floor; the exception is in B-30's contrast test by name; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonDialog.dc.html`, `reference/design-system/toon-engine.js`.
