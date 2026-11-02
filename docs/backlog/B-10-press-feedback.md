---
id: B-10
title: "Press feedback: squash on press, spring from where it got to, action on release"
status: open
priority: P1
size: S
stage: stage-1-materials
blocked_by: [B-08]
---

# B-10 — Press feedback: squash on press, spring from where it got to, action on release

Every pressable component squashes the same way: 80 ms ease-out-cubic to p = 1, then a spring
(ζ 0.35, ω 45) from the current position and velocity. The sketch springs from 1 every time
(research §1.5).

- **The decision and its reason.** One state machine, `rememberToonPress()`, ported from
  `toon-kit.js` `Base.press/release/stepSq`, driven by the frame clock. The action fires on release
  and never waits for the animation. The layout box is the touch target, at least 48 dp.
- D7's one exception to closed-form curves, tested by feeding it timelines: a tap released at
  40 ms, a long press, a press that leaves the box.

- AC: tests over timelines match the kit's integration within tolerance; releasing at 40 ms
  produces no jump.
- Anchors: `reference/design-system/toon-kit.js`.
