---
id: B-77
title: "ToonSidebar: a persistent side panel with search, groups and navigation"
status: open
priority: P2
size: L
stage: stage-5-desktop
epic: feature-components
blocked_by: [B-68, B-69, B-71, B-72]
---

# B-77 — ToonSidebar: a persistent side panel with search, groups and navigation

`ToonSidebar.dc.html` (engine: `sheet`, `navPlate`, `liftBody`, `inkLine`; the extension's `layout.sidebar`).
Not the modal `ToonNavigationDrawer`: always present, 240–360 wide.

- Props: `groups` of items (title, meta), `selected`, `results` with a highlighted snippet, `recording`
  pinned first with its pulse, `nav` rows at the foot, `emptyTitle` and `emptySub`; `onSelect`,
  `onNav`, `onQuery`.
- Rows 56 dp, navigation rows 44 dp; a selected row is a plate, not a colour alone.
- Composes B-71's field and B-72's scrollbar rather than drawing its own.
- AC: empty, content, search results and a pinned recording match their renderer in three themes;
  arrow keys and Enter select.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonSidebar.kt`.
