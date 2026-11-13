---
id: B-76
title: "ToonTitleBar: the window's title row and its action slot"
status: wip
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
