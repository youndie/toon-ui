---
id: B-08
title: "ToonTheme provider, system reduce motion, and a counted idle budget"
status: open
priority: P1
size: M
stage: stage-1-materials
blocked_by: [B-07]
---

# B-08 — ToonTheme provider, system reduce motion, and a counted idle budget

The sketch provides the theme and `LocalReduceMotion = false` by default, and `LocalIdleBudget = 2`
counts nothing (research R1).

- **The decision and its reason.** `ToonTheme(theme)` reads the platform's reduce-motion setting
  (Android animator scale, iOS `UIAccessibilityIsReduceMotionEnabled`, desktop and wasm: off unless
  set). An explicit parameter overrides it. Idle animation is a resource: a component asks for an
  idle slot, at most two are granted per theme scope, and a third stays static.
- Reduce motion follows each animation's word in the spec (D8). This item provides the switch; each
  component item implements its word.
- Rejected: the kit's "everything instant" (research §1.6).

- AC: a test shows the third idle request denied; a test flips reduce motion and the Button's press
  pose is instant.
- Anchors: `reference/design-system/toon-kit.js`, `reference/design-system/compose/toon/ui/theme/ToonTheme.kt`.
