---
id: B-53
title: "A press can stick for good under fast clicks"
<<<<<<< HEAD
status: done
=======
status: open
>>>>>>> origin/main
priority: P1
size: S
stage: stage-2-components
epic: feature-components
---

# B-53 — A press can stick for good under fast clicks

The owner saw "Add plan" stick after clicking it long and hard (2026-11-03). The press machine's
frame loop is a `LaunchedEffect` keyed on a Boolean, `running`: when the loop ends it writes
`running = false`, and a press or release that lands before the next recomposition writes `true`
again. Compose sees `true → true`, does not relaunch the effect, nobody steps the machine, and since
the loop is what clears `running`, it never relaunches again: the button stays wherever it was.
Every component on `rememberToonPressState` is exposed.

- **The decision and its reason.** Key the loop on a counter that every press and release bumps,
  as the clocks of B-51 do, so each event relaunches it whatever the loop was doing.
- AC: a release that lands on the frame the press loop ends springs the button back to rest; a
  test does exactly that, and fails on the Boolean key.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/interaction/ToonPress.kt`.
<<<<<<< HEAD

## Findings (2026-11-03)

- **Reproduced** by `ToonPressTest`'s new case: press until fully down, release on that same frame,
  advance 1.5 s — on the Boolean key the button stayed at progress 1.0.
- **Fixed** by keying the loop on `wakes`, bumped by every press and release. A relaunch while the
  machine moves steps the time since the cancelled loop's last frame (`lastFrameMs`), so the
  timeline is the one a single loop would have run: every golden and press timeline held.
- Not reproduced through a pointer in a test (long, short, moving, inside a scroll, with and
  without the idle breath): the race needs the release on the exact frame the loop ends, which fast
  repeated clicks hit sooner or later.
=======
>>>>>>> origin/main
