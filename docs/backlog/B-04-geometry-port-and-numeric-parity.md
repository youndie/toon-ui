---
id: B-04
title: "Geometry port: Mulberry32, rr, wob, shape, squash, inkLine, with numeric parity"
status: open
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
