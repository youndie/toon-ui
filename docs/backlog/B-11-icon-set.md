---
id: B-11
title: "Icons: 25 centerlines, variable-width outline, Regular and Bold, ToonIcon"
status: wip
priority: P1
size: M
stage: stage-1-materials
blocked_by: [B-06, B-09]
---

# B-11 — Icons: 25 centerlines, variable-width outline, Regular and Bold, ToonIcon

The engine stores each icon as one centerline and computes the ink weight from it
(`iconOutline`, `iconWidth`). The sketch's port drops the 6 % swell term (research §1.5).

- **The decision and its reason.** Port `iconOutline` and the full `iconWidth`, sample with
  `PathMeasure` every 0.3 dp, cache per (icon, size, weight). The 25 centerlines (17 in the first snapshot, 8 added for v1 and settings) are generated from
  `ICONS`, never typed. `ToonIcon(icon, size, weight, filled, color)` takes the `ToonIcon.dc.html`
  props.
- Icons need the DOM in the engine, so their references come from B-06's Chrome renders, not from
  B-03's JSON.

- AC: all 25 icons in both weights within the floor against their references, in every theme.
- Anchors: `reference/design-system/toon-engine.js`, `reference/design-system/toon-ui Icons.dc.html`, `reference/design-system/ToonIcon.dc.html`.
