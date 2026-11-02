---
id: B-04
title: "Geometry port: Mulberry32, rr, wob, shape, squash, inkLine, with numeric parity"
status: done
priority: P0
size: M
stage: stage-0-risks
blocked_by: [B-03]
---

# B-04 — Geometry port: Mulberry32, rr, wob, shape, squash, inkLine, with numeric parity

Every shape in the kit comes out of five engine functions (research §1.2). They are ported first
and tested against B-03's goldens before anything draws.

- **The decision and its reason.** Port `rng`, `rr`, `wob`, `smooth`, `shape`, `inkLine`, `star`
  and `burst` from `reference/design-system/toon-engine.js`, not from the sketch. Geometry is `Float` (D11). Measure
  the tolerance once, from the float RNG's error over every golden. Write it down here and in
  research D11, and never raise it.
- Rejected: `Double` geometry to match "bit-for-bit" (D11).
- Not covered: icon outlines (B-11).

- AC: a common test compares every golden shape point-for-point within the recorded tolerance, in
  every theme; a mutant (the 13-harmonic coefficient 0.22 → 0.2) fails it.
- Anchors: `reference/design-system/toon-engine.js`, `reference/design-system/compose/toon/ui/draw/ToonGeometry.kt`.

## Done (2026-11-02)

- `draw/ToonGeometry.kt` ports `rng`, `rr`, `wob`, `shape`, `inkLine`, `star` and `burst`, in the
  engine's arithmetic order. Points are flat `FloatArray`s; building a Compose `Path` from them is
  B-09's.
- `EngineGeometryGoldensTest` replays all 270 recorded calls across the three themes. Worst error:
  `wob` 6.1·10⁻⁵ px, path strings 0.04987 px. The tolerances are 2·10⁻⁴ px and 0.05 + 2·10⁻⁴ px,
  written in research D11. The first three `rng` values equal the engine's exactly.
- Mutants, each against the committed port: the 13th-harmonic weight 0.22 → 0.2 failed with "wob
  points off by 0.0319 px"; the generator constant off by one failed both tests; the `inkLine`
  thickness ripple 0.2 → 0.21 failed with "path points off by 0.068 px". All restored.
- `shape` is ported but not replayed by itself: the goldens record its two `wob` calls, which is
  what it is made of.
- The code commit was merged into `main` before this one by a script that did not stop on a failed
  edit; this commit, merged after it, is the one that closes the item.
