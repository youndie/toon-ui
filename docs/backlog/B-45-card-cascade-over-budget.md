---
id: B-45
title: "The card cascade with dust misses 60 fps on a Pixel 6a: what to simplify"
status: done
priority: P1
size: S
stage: stage-1-materials
blocked_by: [B-05]
---

# B-45 — The card cascade with dust misses 60 fps on a Pixel 6a: what to simplify

B-05 measured the brief's own target scene: ten cards appearing in the 40 ms cascade, each landing
with two dust puffs. On a Pixel 6a in a release build, 23.6 % of frames take longer than 16.7 ms
and p95 is 18.2 ms. The display still gets about 98 % of its frames, because the pipeline buffers;
the stricter measure is the one set before the run. Geometry is 2.4 ms of a 15 ms frame. The rest
is drawing about 200 filled paths, every card with ink, shadow, fill and sticker, and every dust
cloud with ink and fill (research H1).

The brief: "if an animation does not fit, simplify the animation, do not lower the target".

- **Owner's question:** which simplification?
  - Dust only on the last card of a cascade (or the first and last), not on all ten: about 100
    fewer paths at the peak.
  - Fewer clouds per puff (3 instead of 5).
  - Accept the measured numbers for a ten-card cascade, since 98 % of vsync slots are filled and
    real lists rarely drop ten cards at once.
  - A design change in Claude Design (fewer simultaneous effects), then a re-import (B-34).
- Whichever it is, it is re-measured with the B-05 bench (`docs/services/bench.md`) before the
  item closes.

- AC: the owner's choice recorded; the bench re-run shows the chosen scene within the bar, or the
  accepted numbers written into research H1.
- Anchors: `docs/research/b-05`, `bench/src/commonMain/kotlin/io/github/youndie/toon/bench/FrameCostScene.kt`.

## Decision (owner, 2026-11-02)

**Accept the measured numbers** for a ten-card cascade, the third option: nothing is simplified, and
the numbers are written into research H1 as accepted, with what the measure did not control (R8 off
and no baseline profile in the release build; a fixed 16.7 ms bar on FrameMetrics' total). The
owner chose not to re-measure now.
