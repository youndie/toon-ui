---
id: B-87
title: "The list item has a selected state for a master-detail list"
status: done
priority: P2
size: S
stage: stage-4-product
epic: feature-components
---

# B-87 — The list item has a selected state for a master-detail list

Issue [#37](https://github.com/youndie/toon-ui/issues/37). `ToonListItem` has no selected state. In
a master-detail layout (a list beside the chosen entry's panel) the chosen row cannot look chosen.
`on` is the switch's state, and `ToonCard.selected` is a card's.

- **The decision and its reason.** `selected: Boolean = false` on `ToonListItem`. It draws the
  plate the kit already uses for a chosen flat element: the segmented control's thumb colour behind
  the row, in the shape of its press. The design draws no selected row, so this is a departure, listed
  in `ToonDepartures` with an `asDesigned` reading of false. It is also announced as
  `selected` to a reader.
- Rejected: reusing the card's selected border. A row has no border of its own to thicken.
- Not covered: multi-select.

- AC: a selected row draws its plate in three themes, held by goldens. A reader hears "selected".
  The unselected row keeps its cells.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonListItem.kt`, `toon-core/src/desktopTest/kotlin/io/github/youndie/toon/parity/ToonDepartures.kt`.

## Findings

- **Not a departure, unlike the item said.** The engine's `listItem` already draws `o.sel`: the
  pressed surface's plate under the whole row at full strength, the plate a press draws as it sinks.
  `ToonListItemLook.selected` carried it, and `ListItemParityFixtures` held it to the engine's own
  cell in three themes. Only the composable did not take it. Nothing goes into `ToonDepartures`, and
  no golden moves.
- `ToonListItem(selected = true)` passes it to the look and sets `selected` in the row's semantics.
- Held by `ToonListItemSelectedTest`:
  - in three themes, the plate covers the row between the words and the chevron when selected, and
    is absent otherwise (pixels of the real composable);
  - a reader gets `Selected = true` on the chosen row only.
