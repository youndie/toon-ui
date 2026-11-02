---
id: B-49
title: "TopAppBar: with three or more actions the middle ones are 40 dp targets"
status: done
priority: P2
size: S
stage: stage-4-product
epic: feature-components
blocked_by: []
---

# B-49 — TopAppBar: with three or more actions the middle ones are 40 dp targets

Found in B-30. The design stands the top bar's action icons 40 dp apart (`topBar` in the engine:
`x = sx + sw − 16 − 24 − i × 40`), and the kit gives each a 40 dp target around its icon. The brief
asks for 48 dp. B-30 widened the outermost and the innermost target to the sign's edge and 8 dp
further in, which makes one or two actions — every use on the design's pages — 48 dp targets
without moving anything the design draws. A middle action of three or more cannot grow without
taking from its neighbours: it stays 40 dp wide (and 76 tall).

## Question for the owner

Which of these, for a bar with three or more actions?

1. **Space the icons 48 dp apart** — a design change, drawn in Claude Design first (B-33, B-34).
2. **Accept 40 × 76 dp** for a middle action as a named exception, as the destructive button's
   contrast was (B-24), and name it in `ToonAccessibilityTest`.
3. **Allow at most two actions** and put the rest in an overflow menu (`more`), which the design
   already draws as an icon; the menu itself is B-37.

- AC (once answered): every action of the chosen bar is a 48 dp target, or the exception is named
  in the test.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonTopAppBar.kt`.

## Decision (owner, 2026-11-02)

**Option 2: accept 40 × 76 dp** for a middle action as a named exception. Nothing the design draws
moves. `ToonAccessibilityTest` holds the outermost and innermost actions to 48 dp and, for three and
four actions, every action between them to exactly 40 (`topBarMiddleAction`).
