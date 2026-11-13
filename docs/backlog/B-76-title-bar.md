---
id: B-76
title: "ToonTitleBar: the window's title row and its action slot"
status: done
priority: P2
size: M
stage: stage-5-desktop
epic: feature-components
blocked_by: [B-68, B-75]
---

# B-76 — ToonTitleBar: the window's title row and its action slot

`ToonTitleBar.dc.html` (engine: `trafficSlots`; the extension's `layout.titleH`, 52). Props: `width`, `title`,
`titleLeft`, `record`, `elapsed`, `slots`.

- **How it meets appframe is this item's first question.** appframe draws the window's title bar and
  takes its colours from `MaterialTheme`; a `MaterialTheme` built from toon-ui's tokens carries
  them, and a height of 52 dp holds the kit's 48 dp buttons where macOS's 28 dp clips them. The kit's component can be the content of appframe's `actions` slot, or a whole bar for an
  app without appframe; the design draws the traffic lights' places, not the lights.
- AC: the bar matches its renderer in three themes at 1280 and 960 wide; the record button sits in
  its slot.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonTitleBar.kt`.

## Findings, 2026-11-13

- **How it meets appframe.** appframe's `TitleBar` is a Material `Surface` with its own window
  controls, and toon-ui depends on no Material artefact (D2), so the kit's bar cannot be drawn inside
  it, nor appframe's around the kit's. The two meet in appframe's `actions` slot: a window framed by
  appframe puts the actions themselves there (a `ToonRecordButton` needs nothing from this item), and
  `ToonTitleBar` is the whole bar for a window that has no frame of its own. Its KDoc says so.
  *Rejected:* a `toon-appframe` adapter module, which would be a Material bridge D2 leaves to a
  consumer who asks for one.
- **The port** (`ToonTitleBar`, `toonTitleBarSlotsScene`): the extension's `trafficSlots`, three rings
  12 dp across, centred 20, 40 and 60 dp in and 26 dp down, wobbled 0.25 (not the theme's wobble)
  from seeds 3900–3902, ink at 0.45, 1.3 dp stroke dashed 2 / 2.4. It is the whole of the renderer's
  frame; the bar draws no background, as the design's HTML draws none, so the window's shows through.
- **Words and the slot**: the design's HTML numbers — the title 14, 700, `muted`, from `titleLeft`
  (96 dp, the `titleStart` parameter) to 220 dp before the end, centred on the 52 dp bar as a flex row
  centres a line, ending in an ellipsis; the action row 20 dp before the end, its items 10 dp apart and
  centred on each other. Where actions are wider than the design's (over 200 dp) the title ends 10 dp
  before them instead of running under them. The title is a heading for accessibility.
- **The resize strip.** The design puts the 40 dp record button 6 dp from the top. An undecorated
  window lays an 8 dp resize strip over its edges (appframe's `resizeBorder`, Compose's
  `resizerThickness`), and a press in it moves the edge, so here the row is centred on the bar but no
  higher than 8 dp from its top: the record button stands at 8, 2 dp below the design's, and a row
  taller than 44 dp (a kit 48 dp button) makes the bar taller rather than reach into the strip. A
  layout of HTML words, not a cell of a frame, so `ToonDepartures` has nothing to list.
- **`trafficSlots` is off by default**, where the design's `slots` is on: the rings mark the places
  of the window's own controls, and a real window draws its controls there (macOS) or has none there
  (Windows, Linux). On for a mock-up or a window whose controls are drawn over it.
- **The API**: `ToonTitleBar(title, modifier, titleStart = 96.dp, trafficSlots = false, actions)`. It
  fills the width it is given, and 1280 dp, the design's default, where nothing bounds it. The design's
  `record`, `elapsed` and `lang` are the record button's own (B-75), passed to it in `actions`.
- **Frames**: the slots in three themes, 3 cells in `desktop.json`, worst coordinate 0.050 dp
  (`ToonTitleBarTest`); the renderer takes no width, so the 1280 and 960 dp layouts are held by the
  words' test. Looked at on a contact sheet at density 2 in three themes, at 1280 and 960 with the
  record button idle, recording and stopping. On Ink Gouache Night the rings are faint, as drawn:
  the design's ink at 0.45 on a dark stage.

- [x] The bar matches its renderer in three themes (the slots, layer by layer) at 1280 and 960 wide
  (the title at 96 dp and centred, 220 dp kept at the end, in each theme's face).
- [x] The record button sits in its slot: 20 dp from the end, 8 dp from the top, and a click on it
  starts the recording.
