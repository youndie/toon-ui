---
id: B-10
title: "Press feedback: squash on press, spring from where it got to, action on release"
status: done
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

- AC: under `ToonTheme(reduceMotion = true)` the press pose is instant (moved here from B-08); tests over timelines match the kit's integration within tolerance; releasing at 40 ms
  produces no jump.
- Anchors: `reference/design-system/toon-kit.js`.

## Done (2026-11-02)

- `interaction/ToonPress.kt`: `ToonPressMachine` (the kit's `press`, `release` and `stepSq`, in
  Double, in its order), `rememberToonPressState()` driving it on the frame clock with the kit's
  50 ms frame cap, and `Modifier.toonPressable(state, enabled, role, onClick)`. The modifier clicks
  on release, springs back without a click on a cancelled gesture, and is at least 48 dp square.
- The golden is the kit itself: B-03's harness now loads `toon-kit.js` with inert window and
  document stubs, and replays a button controller's press machine over five timelines
  (`engine-goldens/press.json`). The port matches every step within the goldens' six decimals,
  release results and modes included.
- `ToonPressTest`, 4 tests, including the AC moved from B-08: under reduced motion the press pose is
  instant. Mutants: release snapping to 1 (the sketch's bug) failed two tests ("jumped from 0.875",
  "off the kit by 0.125"); the click at the press instead of the release failed the gesture test.
- Found in the work: a held press kept asking for frames, which hung the UI test waiting for idle
  and would cost a real app frames for nothing. It now sleeps once fully down.
