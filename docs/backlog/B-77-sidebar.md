---
id: B-77
title: "ToonSidebar: a persistent side panel with search, groups and navigation"
status: done
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

## Findings, 2026-11-13

- **The port** (`ToonSidebar`, `toonSidebarPanelScene`, `toonSidebarRowScene`, `toonSidebarNavScene`,
  `toonSidebarRecordingScene`). The panel is the extension's `sheet`, the paper body the sidebar draws
  with the theme's shadow, and `inkLine` as the rule 8 dp over the navigation rows. In the design that
  `sheet` overwrites the kit's bottom sheet (research §1.8). Here it is a scene of its own beside
  `ToonBottomSheet`, which is unchanged. A list row and a navigation row are `navPlate`:
  - selected, a `chipSel` plate with absolute radii and a 1.3 dp wobble;
  - hovered, the pointer rule's plate (`toonPlateLayer`) at the hover's opacity;
  - focused, the ring 2 dp out.

  The recording is `liftBody` in `danger` with a 3 dp shadow and no rim, and the record mark on it
  breathes by `recPulse`.
- **Composed, not redrawn.** The search field is B-71's `ToonSearchField`: `width − 24` wide at 12, 12,
  with `placeholder`, `count` and `keyHint` passed through. The scrollbar is B-72's `ToonScrollbar` over
  the list's `LazyListState`.
  - **The scrollbar's place.** The design sets the 14 dp bar 4 dp from the sidebar's edge, from 8 dp
    under the list's top to 8 dp above its end, so the thumb ends 6 dp from the edge. `ToonScrollbar` is
    20 dp wide: its bar and 6 dp after it. The sidebar keeps the design's place and lets the scrollbar
    reach 2 dp past its edge (`offset(2.dp)`). That strip draws nothing and takes no press. The 8 dp B-72
    keeps is for a window's resize strip, and the sidebar's edge is not the window's.
- **Words**: the design's HTML at its numbers, as Compose text:
  - group headers in 11 sp, 800, upper case, spaced 0.12 em, padded 16 / 12 / 6;
  - a row's title (700, 15) over its meta (500, 13, figures of one width), 5 dp apart, centred on the
    56 dp row, each line its `line-height` tall with the text centred in it, as CSS centres a line;
  - a result's snippet (500, 13, line-height 1.3) on an 80 dp row, the match in 700 ink on a plate of
    the second accent with 3 dp corners and a 2 dp spread, the span's `box-shadow`;
  - the recording's title (800) and `elapsed · source` (700, figures of one width) in `onDanger`, 40 dp in;
  - a navigation row's 22 dp icon at 12, 11 (ink over paper when selected) and its label 46 dp in;
  - the empty state's title and text 22 dp under the list's top, 18 dp in.

  Every single line ends in an ellipsis. A selected row's words turn ink.
- **Selection**:
  - `selected` names a list row or the recording; `selectedNav` names a navigation row, and while one
    is set no list row shows as selected, as the design's `sel = id === selected && !nav`.
  - The recording's ink is 1.4 times as thick while it, or a navigation row, is selected. That is the
    design's `insMul: p.nav || p.selected === 'rec' ? 1.4 : 1`, ported as written.
  - `results`, while set, replaces `groups`, as `p.results || p.groups` does, and an empty list shows
    `emptyTitle` and `emptyText`.
- **Keys**: a row, the recording and a navigation row take focus. A click, Enter or Space selects
  (`onSelect`, or `onNav` for a navigation row), as the design's rows do. Up and Down move the focus to
  the row before or after, so the keys alone reach any row, the list scrolling to it. The design has no
  arrow keys; the item asked for them. They move the focus and do not select, so arrowing past a long
  list does not open every row on the way.
- **The pointer rule (B-69)**: rows take the plate under a mouse only, never a finger. The recording
  lifts 2 dp under a mouse, its shadow left on the table, the mark and the words with it. The design
  passes no `hv` to its `liftBody`. The renderer has the option, and the rule says a body with a hard
  shadow lifts, so the lift is held by a frame (`RecordingHover`). The recording also draws the row's
  focus ring under keyboard focus, which the design leaves out. Neither moves a frame the design draws.
- **The API**: `ToonSidebar(groups, selected, onSelect, query, onQuery, modifier, results, count,
  recording, nav, selectedNav, onNav, placeholder = "Search", keyHint = "⌘K",
  emptyTitle = "Nothing here yet", emptyText = "New items appear here.", width = 300.dp, listState)`,
  with `ToonSidebarGroup`, `ToonSidebarItem`, `ToonSidebarSnippet`, `ToonSidebarRecording` and
  `ToonSidebarNav`.
  - The design's `nav` is two fixed rows named for the app it was drawn for. Here it is any list of
    rows, each with its id, label and icon, and the foot keeps room for as many. With none there is no
    rule, and the list runs to 10 dp above the panel's foot.
  - The design's empty-state words, placeholder and nav labels describe that app. The defaults here are
    English and generic.
  - `width` is held to the design's 240–360 dp, and the sidebar is as tall as it is given.
- **Motion**: the mark breathes from the moment the recording is pinned, on the frame clock. With
  reduced motion it holds its full size.
- **Frames**: 24 looks in three themes, 72 cells in `desktop.json`, worst coordinate 0.050 dp
  (`ToonSidebarTest`):
  - the panel at 300 × 700, 240 × 400 and 360 × 1200;
  - a row at rest, hovered, half hovered, selected, focused and their pairs, and a result's row;
  - the recording at three points of the pulse, selected and hovered;
  - the navigation rows with both icons, selected, hovered and focused.

  Looked at on a contact sheet at density 2 in the three themes: content with a hovered row, a pinned
  recording, search results under a selected navigation row, and the empty state.
- **Not drawn**: no Chrome reference or golden holds the sidebar's pixels, as for the other desktop
  components (B-70).

- [x] Empty, content, search results and a pinned recording match their renderer in three themes:
  every part each of them draws is one of the 72 frames, and where the parts and their words stand is
  held by layout tests.
- [x] Arrow keys and Enter select: Up and Down move the focus from row to row, into the navigation
  rows, and Enter, the number pad's Enter or Space select, under a test.
- [x] Rows 56 dp, navigation rows 44 dp, a result's row 80 dp; a selected row is a plate, not a
  colour alone.
- [x] Composes B-71's field and B-72's scrollbar rather than drawing its own.
