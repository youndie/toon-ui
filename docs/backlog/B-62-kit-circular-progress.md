---
id: B-62
title: "The kit shows a circular progress filling up"
status: done
priority: P3
size: S
stage: stage-2-components
epic: feature-components
---

# B-62 — The kit shows a circular progress filling up

The owner, in the running kit (2026-11-03): "an animated circle progress needs adding to the kit".
The kit's Progress plate showed the circular progress at 62 %, still, and the spinner; the ring of
a determinate one was never seen moving.

- **The decision and its reason.** A third circular progress beside them, its value filling from 0
  to 1 over 2.4 s, held full for 0.6 s, then from 0 again, driven by the frame clock in the sample.
  Only the sample changes: the component already draws any value, and its cells are unchanged.
- Rejected: an animated value inside `ToonProgress` — a determinate progress shows the caller's
  value, and moving it on its own would lie about the work.
- AC: the kit's filling progress reports a growing value over time.
- Anchors: `sample/src/commonMain/kotlin/io/github/youndie/toon/sample/ToonUiSample.kt`.

## Findings (2026-11-03)

- `SampleAppTest` reads the filling progress's semantics: its value grows over 600 ms. Mutant: the
  value fixed at 0.3 — "the value grows: 0.3 then 0.3".
