---
id: B-75
title: "ToonRecordButton: start and stop, with the elapsed time"
status: done
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

## Findings, 2026-11-13

- **The port** (`ToonRecordButton`, `toonRecordButtonScene`): the extension's `recBtn` — a `liftBody`
  (paper with its rim idle, `danger` with 1.3× ink recording, `disabled` and shadowless stopping), the
  red mark on it (a paper one recording, breathing 5.2–6.8 dp on `recPulse`), the spinner stopping, and
  the ring on focus. The words are labels where the design's HTML puts them (research §1.8): the label
  36 dp in, the time 10 dp after it in tabular figures, both centred and moved with the body — up and
  left by the lift, down by the press's squash and 1.2 dp.
- **Width** is the design's `width()`: 36 dp, the label, 10 dp and the width of `00:00:00` while
  recording, 18 dp, rounded up; measured in the theme's face, so it changes with the state's label and
  not with the running time.
- **Pointer, keys, focus**: hover is the pointer rule's (B-69), the press the kit's machine (80 ms in,
  the spring out), firing on the release over the button as the design's `pointerup` does. Enter and
  Space fire too. The ring shows on keyboard focus: a click does not focus it, where the design's
  `tabIndex` div takes focus on a click and rings. Saving, it is inert and read as disabled.
- **The API**: `ToonRecordButton(state, onStart, onStop, modifier, elapsed, recordLabel = "Record",
  stopLabel = "Stop", savingLabel = "Saving")`; the design's `onClick('start' | 'stop')` is two
  callbacks, its `lang` the three labels (English by default, as the kit's other words). It is read as
  its label, and as the time while recording.
- **Motion**: the pulse and the spinner run on the frame clock from the moment the state begins, where
  the design reads its global clock, so a recording starts on the small mark. Neither takes an idle
  slot (B-08): the mark is a status, not decoration. With reduced motion there are no frames, the mark
  is at its full size and the spinner still, as the design's `pulse: reduce ? 1 : …` and its `alive`.
- **Frames**: idle, recording at three points of the pulse, stopping at two turns of the spinner,
  hover in each state, focus idle and recording, and a press half and all the way down, in three themes
  — 39 cells in `desktop.json`, worst coordinate 0.050 dp (`ToonRecordButtonTest`). Looked at on a
  contact sheet at density 2.
- **Not drawn**: no Chrome reference or golden holds the button's pixels, as for B-70–B-72.

- [x] The three states, hover and focus match their renderer in three themes.
- [x] The pulse follows `recPulse` (every 50 ms over two cycles), and the drawn mark breathes with it;
  reduced motion holds it at full size.
- [x] Enter and Space fire the action: start when idle, stop when recording, nothing when saving.
