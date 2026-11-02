---
id: B-14
title: "Button: filled, outlined, text; loading blob, idle breath, disabled buzz"
status: wip
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
