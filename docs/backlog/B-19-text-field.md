---
id: B-19
title: "TextField: drawn frame over a real input, label lift, error shake with stars"
status: wip
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-12]
---

# B-19 — TextField: drawn frame over a real input, label lift, error shake with stars

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonTextField.dc.html` and drawn by `field` in the
engine.

- **What to build.** Port `field`, `fldFocus` and `shake` over a `BasicTextField` whose own caret is hidden. The caret is the engine's: a 3.2 dp block, 500 on and 300 off, no fade. Error fires `field.error` (stars).
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: every TextField cell within the floor; typing works on desktop; error shake strip matches; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonTextField.dc.html`, `reference/design-system/toon-engine.js`.
