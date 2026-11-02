---
id: B-30
title: "Accessibility: semantics, state descriptions, contrast, targets"
status: open
priority: P2
size: M
stage: stage-4-product
blocked_by: [B-14, B-15, B-16, B-17, B-18, B-19, B-20, B-21, B-22, B-23, B-24, B-25, B-26, B-27, B-28]
---

# B-30 — Accessibility: semantics, state descriptions, contrast, targets

The brief's hard limits (contrast ≥ 4.5, targets ≥ 48 dp, reduce motion, nothing flashing faster
than 3 per second) and the semantics a screen reader needs.

- **The decision and its reason.** One test per component asserts role, state description and
  target size. One test over the generated tokens computes every text/background pair a component
  uses and fails below 4.5, except the one exception the owner accepted: `onDanger` on `danger` in Ink & Gouache light, 4.38:1 (B-24).
- Not covered: localisation of state descriptions beyond English, which waits on B-02.

- AC: the semantics and contrast tests pass for all 19 components.
- Anchors: `reference/design-system/uploads/brief.md`, `docs/research/research-architecture.md`.
