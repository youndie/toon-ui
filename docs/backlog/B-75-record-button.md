---
id: B-75
title: "ToonRecordButton: start and stop, with the elapsed time"
status: wip
priority: P1
size: M
stage: stage-5-desktop
epic: feature-components
blocked_by: [B-68, B-69]
---

# B-75 — ToonRecordButton: start and stop, with the elapsed time

`ToonRecordButton.dc.html` (engine: `recBtn`, curve `recPulse`). `state` idle | recording | stopping;
`elapsed`; `onClick('start' | 'stop')`. 40 dp tall; its width follows its label.

- The most noticeable element of a window that is recording: in `recording` it fills with `danger`,
  pulses, and shows the time in tabular figures; `stopping` is inert.
- Reduce motion: no pulse, the mark stays at full size.
- AC: the three states, hover and focus match their renderer in three themes; the pulse follows
  `recPulse`; Enter and Space fire the action.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonRecordButton.kt`.
