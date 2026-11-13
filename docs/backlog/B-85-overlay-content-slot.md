---
id: B-85
title: "Dialog and bottom sheet take content, and their texts have no English defaults"
status: open
priority: P2
size: M
stage: stage-4-product
epic: feature-components
---

# B-85 — Dialog and bottom sheet take content, and their texts have no English defaults

Issue [#34](https://github.com/youndie/toon-ui/issues/34). `ToonDialog` takes a title, a body and
two buttons; `ToonBottomSheet` takes a list of items. A choice of two radio groups and a "Готово"
fits neither, so the consumer draws an overlay of its own. Their texts have English defaults
("OK", "Cancel", "Share plan"), and a consumer that forgets one ships English.

- **The decision and its reason.**
  - Add a `content` slot to `ToonDialog` (between the body and the buttons) and an overload of
    `ToonBottomSheet` with `content` in place of `items`. Each overload has its own name where a
    trailing lambda would be ambiguous (cmp-ui-component §4).
  - The overlay's own rules hold for the slot: closed, it answers no click, no Tab and no reader
    (B-24).
  - The user-facing texts become required parameters. The kit is still `0.1.0.<run>` with one
    consumer, so the break is cheap now. It is recorded in the ABI dump.
- Rejected: keeping the defaults. Text a consumer forgets is the defect the issue names.
- Not covered: scrolling a slot taller than the window.

- AC: a dialog with two radio groups in its slot opens, and Tab walks the groups and the buttons.
  Closed, the slot takes no focus. `ToonDialog(…)` without a `confirm` text does not compile.
  The design's dialog and sheet keep their cells and goldens.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonDialog.kt`, `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonBottomSheet.kt`, `toon-core/api/`.
