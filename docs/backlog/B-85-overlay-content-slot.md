---
id: B-85
title: "Dialog and bottom sheet take content, and their texts have no English defaults"
status: done
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

## Findings

- **The slot rides the card; it is not part of the drawn scene.** The dialog and the sheet are
  canvases drawing the engine's scenes, so the slot is a composable placed over the card with a
  `layout` modifier, under a graphics layer. In the dialog it scales with the pop about the card's
  centre and fades with the words. In the sheet it is translated with the slide and faded under
  reduce motion. The card grows by the slot's measured height (`toonDialogHeight(lines, slotH)`,
  `toonSheetHeight(h, n, slotH)`), and the hit tests take it.
- **The slot is composed only while the overlay is open** (B-24: closed, no input). It leaves at the
  start of the close while the card's own words fade over 60 ms: four frames in which the slot is
  gone and the card is still there. That is recorded as a quirk, not hidden.
- **Focus.** With a slot, the overlay is a focus group whose exit is cancelled, so Tab never leaves
  it. The dialog's canvas keeps its own Confirm and Cancel stops. Tab from Cancel moves into the
  slot, and from the slot's last focusable comes back to Confirm. Escape from inside the slot cancels
  the dialog or closes the sheet. The sheet's canvas is not focusable when it has no rows.
- **Without a slot nothing changes**: the dialog and the sheet are the same bare canvas with the
  caller's `modifier` on it, so the semantics, the tags and the goldens stay where they were. With a
  slot the `modifier` goes on a box around both. To focus the dialog itself, find it by its
  `paneTitle`.
- **The sheet with a slot is `ToonBottomSheetBox(title, …, content)`**, its own name, so a trailing
  lambda is never ambiguous with `ToonBottomSheet(items, …, onClose)`.
- **Breaking:** `ToonDialog(title, confirm, cancel, …)` and `ToonBottomSheet(items, title, …)` take
  their words positionally with no defaults. Callers in the sample and the tests were updated; the ABI
  dump records it.
- Held by `ToonOverlayContentTest`:
  - the dialog's slot between the body and the buttons, and the card's growth (bounds of the real
    composable);
  - the sheet's slot where the first row would be, with the sheet ending 40 dp under it;
  - the Tab walk Confirm, Cancel, the four options, and round to the dialog;
  - Escape from either slot;
  - no slot composed once closed.
