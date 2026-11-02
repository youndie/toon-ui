---
id: B-05
title: "Measure the frame cost of per-frame geometry before components depend on it"
status: wip
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
