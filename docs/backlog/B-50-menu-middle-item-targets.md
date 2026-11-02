---
id: B-50
title: "Menu: with three or more items the middle ones are 44 dp targets"
status: done
priority: P2
size: S
stage: stage-4-product
epic: feature-components
blocked_by: []
---

# B-50 — Menu: with three or more items the middle ones are 44 dp targets

Found in B-37. The design stacks the menu's items 44 dp apart (`menu` in the engine: `ih = 44`), and
the kit's `down` takes the 44 dp row under the finger. The brief asks for 48 dp. B-37 widened the
first item's target to the scroll's top and the last one's to its bottom, which makes one or two
items 48 dp targets and more without moving anything the design draws. An item between them cannot
grow without taking from its neighbours: it stays 44 dp tall (and as wide as the scroll).

## Question for the owner

Which of these, for a menu of three or more items?

1. **Stack the items 48 dp apart** — a design change, drawn in Claude Design first (B-33, B-34).
2. **Accept 44 dp** for a middle item as a named exception, as the destructive button's contrast
   was (B-24), and name it in `ToonAccessibilityTest`. 44 is Apple's minimum, and the row is the
   scroll's whole width.

- AC (once answered): every item of the chosen menu is a 48 dp target, or the exception is named
  in the test.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonMenu.kt`.

## Decision (owner, 2026-11-02)

**Option 2: accept 44 dp** for a middle item as a named exception. Nothing the design draws moves.
`ToonAccessibilityTest` holds the first and last items to 48 dp and every item between them to
exactly 44 (`menuMiddleItem`), so a narrower row fails as a wider one is noticed.
