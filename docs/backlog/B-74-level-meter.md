---
id: B-74
title: "ToonLevelMeter: a channel's level, live, silent or clipping"
status: open
priority: P1
size: M
stage: stage-5-desktop
epic: feature-components
blocked_by: [B-68, B-80]
---

# B-74 — ToonLevelMeter: a channel's level, live, silent or clipping

`ToonLevelMeter.dc.html` (engine: `levelMeter`, curve `livelevel`). Props: `label`, `source`, `icon`
mic | wave, `state` live | silent | clip, `level` 0–1, `animate`, `width` 160–600.

- One of two things allowed to move by itself while recording (the other is the record mark). Silent
  says so in words as well as in the bar: a status is never colour alone.
- Reduce motion: the bar holds the given level.
- AC: the three states match their renderer in three themes; with `animate` the bar follows
  `livelevel` frame for frame; reduce motion stops it.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonLevelMeter.kt`.

## Iteration 1, 2026-11-13

The header draws the extension's `mic` or `wave` (`E.icon(T, p.icon || 'mic', …)`), and `ToonIcons` holds
the kit's 25 glyphs only: the extension's 12 were never generated. Filed as B-80, which this item now
waits for; nothing else was built.
