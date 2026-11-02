---
id: B-14
title: "Button: filled, outlined, text; loading blob, idle breath, disabled buzz"
status: done
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-11, B-12]
---

# B-14 — Button: filled, outlined, text; loading blob, idle breath, disabled buzz

Component of the MVP set (brief §«Инвентарь»), designed in `reference/design-system/ToonButton.dc.html` and drawn by `btn` in the
engine.

- **What to build.** Port `btn` with its three variants, the loading hand-off (label flies up, blob drops in), idle breathing and boiling under the idle budget, the disabled buzz (`vibe`), and the focus ring. Every sketch divergence listed in research §1.5 is fixed here: rim colour per variant, the text variant's wavy underline and pressed blob, the disabled label colour.
- Built on the shared pieces: geometry (B-04), tokens and theme (B-07, B-08), drawing primitives
  (B-09), press feedback (B-10), FX (B-12). Props follow the dc file's `data-props` as Compose
  parameters: names in Kotlin form, defaults kept.
- Its reduce-motion word comes from the spec and is implemented as written (D8).

- AC: every state on the Core page within the floor in three themes; the press/release strip's values match the goldens at its timestamps; reduce motion as specified; a mutant in the ported curve is caught.
- Anchors: `reference/design-system/ToonButton.dc.html`, `reference/design-system/toon-engine.js`.

## Findings (2026-11-02)

- **Done as decided**, the engine's `btn` and the kit's `C.button`: three variants, the squash, the
  loading hand-off, idle breath and boil under the idle budget, the disabled buzz, the focus ring.
  The sketch's three button divergences (research §1.5) do not arise: the numbers are the engine's.
  Props: `label`, `variant`, `width` (0 fits the label), `enabled` (the dc file's `disabled`,
  inverted as Compose names it), `loading`, `idle`, `onClick`.
- **Every Core page cell is the engine's frame**, layer by layer (`ToonButtonSceneTest`, 93 cells,
  worst coordinate 0.050 dp, the engine's rounding). This replaced pixels as the gate for shapes
  after nine mutants — a shadow factor, the bottom's thickening, the night rim, the ring and blob
  radii, the label's sink and flight, the blob's drop, the underline — went past both pixel floors
  but one; the frame catches all nine. The squash curve's mutant is caught by `ToonCurvesTest` too.
- **Pixels:** label cells 2.6–3.6 % of the cell, the label alone 1.96–2.30 % (text floor, research
  D3); in ink, label cells at most 44.67 % against the text floor's 53.56 %, label-free shapes within
  the Divider's 14.81 %.
- **Reduce motion** per the Core page: press and release `instant` (the press machine, B-10),
  loading `fade 120` (cross-fade from the resting button into the settled loading one), idle `off`.
  The kit has no buzz under reduce motion, and neither has this.
- **Not done, on purpose:** the Core page's release strip draws the label 40 ms behind the squash
  (`pl`), which the kit does not do; the live button follows the kit, and the strip's frames are drawn
  with `pl` from the fixtures. The disabled buzz is drawn by the button, not sent to the FX host
  (which would draw it twice), so no listener hears `button.buzz`.
