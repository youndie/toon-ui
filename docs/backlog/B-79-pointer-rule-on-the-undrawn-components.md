---
id: B-79
title: "The pointer rule on the pressable components the pointer page does not draw"
status: open
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
