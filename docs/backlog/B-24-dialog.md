---
id: B-24
title: "Dialog: iris spotlight in, pop and stars out"
status: done
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

## Findings (2026-11-02)

- **Done as decided**, the engine's `dialog`, `dialogOpen`, `dialogClose` and `iris` with the kit's
  `C.dialog`: a full-screen layer, the body wrapped at 252 dp as the kit's `lines`, the kit's hit
  areas (confirm; cancel or anywhere outside the dialog). Props: `title`, `open` (true), `body`,
  `confirm` (`OK`, the kit's fallback), `cancel`, `destructive` (true, as `data-props`), `onConfirm`,
  `onCancel`. Demo strings are not kept as defaults. Keyboard and a screen reader's actions are added
  (the service quirks).
- **The frame grew** the iris (even-odd, its arcs held number by number), the display face for the
  title, and a button `fill` and label colour (`destructive`). Fill rule and face are written to the
  goldens only where they are not the default: no earlier golden changed.
- **Frames:** the four MVP page cells and the five open and five close frames, over the page's demo
  screen, in three themes, are the engine's (42 cells, worst coordinate 0.050 dp); `dialogOpen` and
  `dialogClose` within 3·10⁻⁵. Sixteen mutants caught; two (the cancel area's 110 dp and the 252 dp
  wrap) only after boundary assertions were added, and the iris's path by 33 goldens, not the frame.
- **Pixels:** within 2.16 % of the cell; 4.35 % in ink at worst, under the Divider's floor (14.81 %),
  so the dialog is held to that floor.
- **The contrast exception** — `onDanger` on `danger`, 4.38:1 in Ink & Gouache light — is pinned by
  `ToonDialogTest` (4.38 there, ≥ 4.5 in the other two themes) and named in B-30, whose contrast test
  takes it over.
- **Reduce motion** is the MVP page's `fade 120` for both: the layer fades at rest, no pop, no stars.
- **Found on the way:** no component scales its canvas by the screen density (research D5). The
  dialog draws as the others do, and B-47 fixes all of them in one place.
