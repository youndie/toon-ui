---
id: B-05
title: "Measure the frame cost of per-frame geometry before components depend on it"
status: done
priority: P0
size: M
stage: stage-0-risks
blocked_by: [B-04]
---

# B-05 — Measure the frame cost of per-frame geometry before components depend on it

The pattern rebuilds about 130 points per shape on every frame a shape changes. The brief's target
is 60 fps on a Pixel 6a-class phone, with 10 cards appearing at once with FX. Nothing has measured
it (research H1).

- **The decision and its reason.** A spike before stage 2. Ten wobbly bodies run the card-appear
  curve with dust, with geometry (a) rebuilt per frame and (b) cached per (size, seed, squash
  bucket). Frame time is measured on desktop and on the Pixel 6a, in release build. The
  verdict decides whether B-09 caches.
- **The device is a Pixel 6a** (owner, 2026-11-02), the phone the brief names, so the verdict is
  read against the brief's target directly.
- Rejected: trusting "cheap, ≈130 points" from the sketch.
- Not covered: iOS. It goes in the same harness if the owner wants it.

- AC: a table of frame time per variant and device, with the raw logs kept; a one-line verdict on
  H1 written into research §3.
- Anchors: `reference/design-system/compose/README.md`, `docs/research/research-architecture.md`.

## Done (2026-11-02)

**Pre-registered** before the first run: the scene described above (Ink & Gouache light, text not
drawn). Arms: Rebuild, Cache (quarter-dp size keys) and a 20 ms busy-wait Control. Order: 5 warm-up
replays per arm, 20 ABBA rounds of 1 s replays, 5 Control replays. Bar: H1 holds if Rebuild has
≤ 1 % of frames with FrameMetrics `TOTAL_DURATION` over 16.7 ms and p95 ≤ 16.7 ms.

| Pixel 6a, Android 17, 60 Hz, release, on AC, 31.7–32.5 °C | total p50 | p95 | > 16.7 ms | vsync slots missed | scene draw p50 / p95 | RenderThread issue p50 | GPU p50 |
|---|---|---|---|---|---|---|---|
| Rebuild (run 2) | 15.02 | 18.19 | 23.6 % | 1.6 % | 2.39 / 4.14 | 4.98 | 5.61 |
| Cache (run 2) | 12.89 | 17.75 | 12.0 % | 2.0 % | 1.89 / 4.38 | 4.57 | 5.10 |
| Control (run 2) | 43.43 | 52.04 | 100 % | 22.3 % | 20.95 / 21.93 | 3.40 | 5.61 |

Run 1, without the breakdown, agrees: Rebuild 14.80 / 17.78 / 21.4 % / 1.6 %, Cache 13.17 /
17.84 / 13.7 % / 1.8 %. On the desktop JVM, CPU raster into a 1080 × 2400 bitmap, one draw call per
frame: Rebuild p50 5.42 ms, p95 11.79 ms; Cache 5.11 / 10.85. Rebuild builds 45.5 shapes per frame on
average, Cache 21.9.

**Verdict:** red by the bar for this worst case, and the cost is not the geometry (research H1).
B-09 does not cache. Making the cascade fit is an animation change, the owner's question, in
[B-45](B-45-card-cascade-over-budget.md).

- The missed-slot count under-reports: Control spent 21 ms of UI time on every frame and still
  missed only 22 % of slots. The pipeline buffers frames, so `TOTAL_DURATION` is the stricter
  measure, and the bar was set on it.
- Raw data in `docs/research/b-05`: both logcat runs, the desktop CSV, and `summarise.py`, which
  prints the table above from them.
- The bench is two modules, `bench` and `bench-android`, described in `docs/services/bench.md`. To
  reach the geometry they need, it is public behind `@InternalToonApi` (error-level opt-in), which
  toon-core opts itself into.
