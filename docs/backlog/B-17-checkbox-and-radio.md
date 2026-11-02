---
id: B-17
title: "Checkbox and RadioGroup"
status: done
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

## Findings (2026-11-02)

- **Done as decided**, the engine's `check`, `chkAnim`, `radio` and `radioSelect` with the kit's
  `C.checkbox` and `C.radio`. Checkbox props: `checked`, `onCheckedChange` (`onChange`), `label`,
  `error`, `enabled`; radio group: `options` (a list, not the dc file's comma string), `selected`
  (`value`), `onSelectedChange`, `enabled`. The dc files' demo labels are not kept as defaults.
- **Frames:** the Core page's seven checkbox cells and its check and uncheck strips, the MVP page's
  six radio cells and drop strip, three themes — 51 and 36 cells — are the engine's frames, worst
  coordinate 0.050 dp; `chkAnim` and `radioSelect` within 2.2·10⁻⁶. Ten mutants caught. The frame now
  carries a stroke's dash offset, the tick's trim.
- **The Core page draws an older `check`** in its own script, without the specks the tip flicks; the
  references are the engine's, which has them.
- **Pixels:** label-free strip frames within 1.21 % of the cell; labelled cells 3–4 %, the label,
  within the text floor in ink.
- **A press squashes** the box and the ring to 0.86, the spec pages' Pressed cells. The kit has no
  press on either; this adds one so that the Pressed state the design draws exists.
- **Reduce motion** per the pages: a check is `fade 120` (the unchecked box cross-fades into the
  checked one), an uncheck and the radio drop are `instant`.
- A row is 48 dp for the checkbox, as the kit lays it out, and 52 dp per radio option.
