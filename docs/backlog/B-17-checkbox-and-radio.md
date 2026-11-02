---
id: B-17
title: "Checkbox and RadioGroup"
status: open
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-12]
---

# B-17 — Checkbox and RadioGroup

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonCheckbox.dc.html` and drawn by `check` and `radio` in the
engine.

- **What to build.** Checkbox: the tick drawn as one trimmed stroke, overshooting the box (`chkAnim`), and the error state. RadioGroup: the drop that hangs, falls and splats (`radioSelect`), while the old dot deflates. Each row is a 52 dp target.
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: the Core and MVP cells and strips within the floor; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonCheckbox.dc.html`, `reference/design-system/toon-engine.js`.
