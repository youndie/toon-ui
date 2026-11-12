---
id: B-69
title: "One hover, focus and target rule for the whole kit"
status: open
priority: P1
size: L
stage: stage-5-desktop
epic: feature-components
blocked_by: [B-68]
---

# B-69 — One hover, focus and target rule for the whole kit

The kit draws no hover at all and its targets are sized for a finger. A desktop app needs both, and
the desktop design's `Pointer & Keyboard` page fixes one rule for every component.

- **The decision and its reason.** Apply the page's rule to every existing component, so a desktop
  app gets it without a flag per component:
  - **Hover:** a body with a hard shadow lifts −2, −2 while its shadow stays (4 → 6); a press drops the
    shadow to 0. A flat element gets the `surface2` backdrop instead of a lift. In 80 ms on the kit's
    press curve, out in 120 ms; reduce motion: instant. In a reading area the backdrop is always
    instant.
  - **Focus:** the kit's dashed ring on the `focus` token; a field's ink also thickens ×1.5.
  - **Targets:** at least 28 dp; icon button 32, control 40, list row 56, navigation row 44.
- Hover only on a pointer that hovers; touch keeps today's look. No action exists only on hover.
- Rejected: a hover parameter per component — the page is one rule, and per-component switches drift.

- AC: every component that is pressable shows the rule's hover, measured against the page's renderer
  output; focus is visible in all three themes; a test holds the target sizes.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/press/ToonPress.kt`, `reference/desktop/`.
