---
id: B-47
title: "Draw every component in dp: scale the canvas by the screen density"
status: done
priority: P1
size: S
stage: stage-2-components
epic: feature-components
blocked_by: []
---

# B-47 — Draw every component in dp: scale the canvas by the screen density

Found in B-24. Research D5 says one engine px is one dp, and `ToonInk.kt`'s header says "a component
scales the draw scope by its density once, then works in the engine's numbers". None does: every
component from B-14 to B-24 hands `drawScene` its frame in the canvas's pixels, and lays out its box
in dp (`Modifier.size(w.dp, …)`). The desktop tests and the references run at density 1, where the
two agree, so nothing has failed. On a phone at density 2.625 (the Pixel 6a of B-05) the box is
2.625 times the drawing, the shapes come out at 38 % of their size, and the labels — measured in sp,
which the text measurer does convert — come out at full size on top of them. The dialog's hit areas
and the snackbar's action target are read in the same pixels.

- **What to build.** One place that scales by `density` before drawing a frame (a `drawScene` that
  takes the density, or a canvas wrapper every component uses), and the pointer handlers that read
  positions in dp. Text keeps its sp size and is placed at the scaled position.
- **Why not per component.** Nineteen copies of one `scale(density)` are nineteen chances to forget
  one, as all of them did.
- AC: a test draws a component at density 2 and at 1 and finds the same picture at twice the size,
  text included; the hit tests (the dialog's buttons, the snackbar's action, a swipe on the card)
  pass at density 2; the existing goldens do not move.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/draw/ToonScene.kt`, `toon-core/src/commonMain/kotlin/io/github/youndie/toon/draw/ToonInk.kt`.

## Findings (2026-11-02)

- **Done as decided**, in one place: `inDp` scales a draw scope by its density, and every
  component's canvas, the FX host's layer and the test's `SceneCell` draw inside it. The text
  renderer lays labels out at density 1 with the user's font scale, so a label is drawn at its sp
  size once. Gestures read `position / density`; the text field's caret, from its own pixel layout,
  is divided too, and the field's error point is sent in root pixels as the host expects.
- **At density 1 nothing moved:** 131 desktop tests and 1318 goldens as before.
- **Tests:** `ToonDensityTest` draws a button and a card at density 1 and 2 (the ink's box doubles
  to the pixel, the dark pixels grow 4.35 and 4.48 times), lays a label out at densities 1, 2 and
  2.625 with a font scale of 1.3 (its height is the dp height), and taps the snackbar's action, the
  dialog's buttons, a card swipe, a tab, a destination and the back arrow at density 2, each at a
  point where pixels read as dp would land elsewhere. Every mutant that drops a conversion fails it.
- **Found on the way:** the FX host ignores the points its events carry (B-48).
