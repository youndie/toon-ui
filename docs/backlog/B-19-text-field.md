---
id: B-19
title: "TextField: drawn frame over a real input, label lift, error shake with stars"
status: done
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

## Findings (2026-11-02)

- **Done as decided**, the engine's `field`, `fldFocus` and `shake` with the kit's `C.textfield`,
  over a `BasicTextField` with a transparent caret. Props: `value` with `onValueChange` (`onChange`),
  `label`, `helper`, `error` (the message; empty is no error), `width` (342 kept), `enabled`. The
  dc file's demo label is not kept as a default.
- **Typing works on desktop**, inside and outside an FX host (`ToonTextFieldTest`).
- **Frames:** the five Core page cells and the focus, error and caret strips in three themes are
  the engine's frames, worst coordinate 0.050 dp; `fldFocus` within 6·10⁻⁷ at every millisecond. The
  comparison learnt two of the engine's own roundings — an angle to 0.1° for a turned layer, and one
  0.1 step for each translation it writes — and to resolve relative path commands, after a caret
  20 dp tall passed as a ready path. Nine mutants are caught.
- **Pixels:** within 2.66 % of the cell, the label, value and helper text, and within the text floor
  in ink.
- **The caret** is the engine's block and blink at the real cursor, not the kit's end of the text.
  **The error's stars** go to the FX host when there is one (`field.error`), else the field draws
  them.
- **Reduce motion** per the Core page: focus `instant`, the error `instant, red only` (no shake, no
  stars), the caret steady.
