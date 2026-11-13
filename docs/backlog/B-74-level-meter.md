---
id: B-74
title: "ToonLevelMeter: a channel's level, live, silent or clipping"
status: done
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

## Findings, 2026-11-13

- **The port** (`ToonLevelMeter`, `toonLevelMeterScene`): the extension's `levelMeter` — one cel block
  per 13 dp, at least eight, green, the last three amber (red when clipping), unlit blocks 3 dp lower
  on `off` with their ink faded; silent struck through with an ink line; clipping with a burst in the
  24 dp the strip keeps for it. A strip is built once per lit count, not per frame.
- **Motion**: with `animate`, a live strip's level is the extension's `livelevel` times `level / 0.7`,
  on the frame clock from the moment it begins to move (the design reads its global clock), in
  `Double` so hours of it stay exact. Reduced motion, silence or `animate = false`: no frames, the
  given level. It takes no idle slot (B-08): it is a status, not decoration.
- **Words**: the header is the design's HTML row — icon, label, source, status, 10 dp apart — laid out
  so the status is never cut: the icon and the status take their room first, the label what is left,
  the source the rest, each with an ellipsis. In the design the label and the status do not shrink
  and the row runs out of its box when they do not fit; here the label gives way first. Silence and
  clipping are said as `silentLabel` and `clipLabel` (English by default, as the kit's other words),
  readable text, so a status is never colour alone.
- **The API**: `ToonLevelMeter(label, modifier, source, icon = ToonIcons.Mic, state, level = 0.6,
  animate = true, width = 320.dp, seed, silentLabel = "Silence", clipLabel = "Too loud")` and
  `ToonLevelState`. The design's `icon` is `mic` or `wave` (B-80); here any glyph. `width` is held to
  the design's 160–600 dp.
- **Frames**: live at 0.6, 0 and 1, silent, clipping, the narrowest and widest strips, and a narrow
  silent one, in three themes — 24 cells in `desktop.json`, worst coordinate 0.050 dp
  (`ToonLevelMeterTest`). Looked at on a contact sheet at density 1.5.

- [x] The three states match their renderer in three themes.
- [x] With `animate` the strip follows `livelevel` frame for frame: 60 frames' lit blocks, read off the
  drawn strip, equal `livelevel`'s at the frames' ages.
- [x] Reduced motion stops it: the strip holds the given level.
