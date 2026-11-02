---
id: B-23
title: "Snackbar: flies up on its rope, swings, anticipates down"
status: done
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

## Findings (2026-11-02)

- **Done as decided**, the engine's `snack`, `snkShow` and `snkHide` with the kit's `C.snackbar`.
  Props: `text`, `action` (default empty), `type` (info, success, error), `shown`, `width` (342),
  `onAction`; a tap on the sign's last 110 dp fires the action while it is shown. Demo strings are
  not kept as defaults. The scene gained an end-anchored text (`ToonAnchor.End`) for the action.
- **The host:** `ToonSnackbarHostState.show` queues behind a mutex and returns how the sign left;
  `ToonSnackbarHost` draws the current one and starts the next once the hide is drawn. The design
  gives no hang time, so `ToonSnackbarDuration` takes Material's 4 s and 10 s, plus indefinite with
  `dismiss()` — a decision, documented in the service quirks, not a number from the engine.
- **Frames:** the three Core page cells and the six show and four hide frames, in three themes, are
  the engine's (39 cells, worst coordinate 0.050 dp); each strip frame is the kit's own moment at its
  age, so the cell holds the curve as well as the drawing. `snkShow` and `snkHide` within 1.1·10⁻⁵.
  Ten mutants caught (the swing, the spring, the drop, the speed lines, the reduced fade, the hide's
  drawn time, the action target, the host's wait and duration).
- **Pixels:** within 1.93 % of the cell; in ink 4.27 % at worst, under the Divider's floor
  (14.81 %), so the snackbar is held to that floor rather than to the text floor.
- **Reduce motion** is the Core page's `fade 120` for both the show and the hide: the sign fades in
  and out where it hangs, no flight and no swing.
