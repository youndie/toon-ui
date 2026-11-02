---
id: B-23
title: "Snackbar: flies up on its rope, swings, anticipates down"
status: open
priority: P1
size: S
stage: stage-2-components
epic: feature-components
blocked_by: [B-12]
---

# B-23 — Snackbar: flies up on its rope, swings, anticipates down

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonSnackbar.dc.html` and drawn by `snack` in the
engine.

- **What to build.** Port `snack`, `snkShow` and `snkHide` with the three types. The host queues one at a time.
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: cells and both strips within the floor; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonSnackbar.dc.html`, `reference/design-system/toon-engine.js`.
