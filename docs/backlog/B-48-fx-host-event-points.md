---
id: B-48
title: "FX host: draw each effect where its event puts it"
status: open
priority: P2
size: S
stage: stage-2-components
epic: feature-components
blocked_by: []
---

# B-48 — FX host: draw each effect where its event puts it

Found in B-47. The events carry where their effect goes — `at` and `spread` for the stars, `size`
for the flash, `puffs` and `spread` for the dust — and the components send the engine's numbers in
them (the field's stars at (w − 30, −4) at 26, the dialog's pop at its centre at 70, the card's dust
from (34, 86) at 36). `drawToonFx` in `ToonFxHost.kt` reads none of them: it draws every effect from
the event's `bounds` with the FX page's demo-block defaults. A dialog's pop, whose bounds are the
whole layer, is drawn above the layer's top edge instead of on the dialog.

- **What to build.** The host draws each effect at the event's own point and size, converted from
  root pixels to dp as the bounds are (B-47); the defaults stay the FX page's.
- AC: a host test per event kind finds the effect drawn at the point and size it was sent with, at
  densities 1 and 2; the dialog's pop lands on the dialog.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/fx/ToonFxHost.kt`.
