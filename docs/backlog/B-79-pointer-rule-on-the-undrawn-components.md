---
id: B-79
title: "The pointer rule on the pressable components the pointer page does not draw"
status: wip
priority: P2
size: M
stage: stage-5-desktop
epic: feature-components
blocked_by: [B-69]
---

# B-79 — The pointer rule on the pressable components the pointer page does not draw

B-69 put the pointer page's hover on every kit component the page draws, each held to the frame the
page's renderers return (research D14). Eight pressable components are not on the page: the
checkbox, the radio group, the switch, the segmented control, the tabs, the navigation bar, the
navigation drawer's rows and the text field. Their hover is still none.

- **The decision and its reason.** Give each the rule by the page's own words, with the mechanism
  B-69 built (`toonHoverable`, `hoverLifted`, `toonPlateLayer`), so a desktop app gets one rule
  everywhere:
  - a body with a hard shadow lifts (the checkbox's box, the radio's dot, the switch's thumb and the
    segmented control's pressed segment, if each has one — read off its renderer);
  - a flat element gets a plate of the pressed-surface colour in the shape of its press (a tab, a
    navigation bar slot, a drawer row, a field — the page names fields among the flat ones).
- What each draws is not on the page, so each component's look is this item's decision, written in
  the item per component, and held to a golden from the engine's own renderer under the rule
  (`hoverLift` or `plate`), as B-69's cells are.
- Rejected: leaving them without hover — the page's rule 2 says no element is left out, and an app
  mixing lifted buttons and dead tabs reads as broken.

- AC: each of the eight shows hover under a mouse and not under a finger; each look is stated in the
  findings and held by a frame cell in `desktop.json`'s `Hover` group in three themes.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/interaction/ToonHover.kt`,
  `scripts/desktop-cells.mjs`.

## Findings, 2026-11-13

- **The rule, read per component off its renderer** (research D14). Rule 1 lifts a body with a hard
  shadow; rule 2 gives a flat element a `surface2` plate "of the same shape as its press".
  - **Checkbox** — the box (`check`'s `body`, shadow 2) and its tick lift 2 dp toward the light, the
    shadow stays. The label beside it is not on the body and stays on the table.
  - **Radio** — the ring (shadow 2) and its dot lift; the label stays.
  - **Switch** — the track is the body with the shadow (`sh − 1`); the thumb has none and rides on
    it, so the whole switch lifts and the track's shadow stays. Lifted before the row places it,
    since the rule reads a shadow by its own offset (B-69's finding).
  - **Segmented button, tabs, navigation bar, drawer** — flat slots. A hovered slot gets the plate
    its own press draws: the same wobble, seed and place, in the layer order the press puts it (a
    tab's under everything, the bookmark included), at the hover's strength. Nothing else of the
    press comes with it: no label drop, no icon squash. The selected slot gets none, because its
    renderer draws no press on it either; a disabled one none.
  - **Text field** — flat, but its paper is opaque, so a plate under it would not show. The desktop
    design's own field, `searchBox`, fills with `surface2` under the pointer unless focused; this
    one does the same, eased by the hover instead of switched at its middle (the cells are at 1, where
    the two agree). A focused or disabled field shows none.
- **Press over hover.** On the three lifted controls the lift fades as a press goes down, as the
  extension's `liftBody` has it and B-69's bodies do, so hovered and pressed all the way is the page's
  Pressed cell. On a slot, the press plate is whole and the hover adds nothing.
- **Mechanism.** The four slotted components draw on one canvas, so B-69's `toonHoverable` does not
  fit: `toonSlotHoverable` keeps one `ToonHoverState` per slot and lights the one a mouse is over,
  as the menu's items are lit; a finger lights none, leaving lights none, and a disabled segmented
  button or a closed drawer lets go. `toonSlotPlate` is the plate's strength per slot. Both internal;
  the public API is unchanged.
- **The cells.** `scripts/desktop-cells.mjs` makes each from one of the component's rest cells in
  the engine's own renderer: `hoverLift` for the lifted three (their labels put back), the pressed
  frame's `surface2` layer spliced into the rest frame for a slot (the script fails if the press adds
  anything else at that place), and the field's paper recoloured. 16 looks × 3 themes = 48 cells;
  `Hover` is 90. Every existing frame in `desktop.json` and `components.json` is unchanged, and no
  golden moved: `hv` 0 draws the rest cell, which `ToonHoverFramesTest` checks cell by cell.
- **Targets** stay the kit's 48 dp and more (`ToonAccessibilityTest`); rule 4 leaves them.
- No departure: no rest cell changed, so nothing is listed in `ToonDepartures`.

- [x] Each of the eight shows hover under a mouse and not under a finger: `ToonHoverPathTest` (the
  picture changes under a mouse and returns to the pixel on leaving) and `ToonHoverTest` (a finger
  lights no slot; `toonHoverable`'s finger test from B-69 holds the other four).
- [x] Each look is stated above and held by frame cells in `desktop.json`'s `Hover` group in three
  themes: checkbox ×3, radio ×3, switch ×3, segmented, tabs, navigation bar, drawer, text field ×3.
