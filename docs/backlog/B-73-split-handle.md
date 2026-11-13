---
id: B-73
title: "ToonSplitHandle: the grip between a sidebar and the content"
status: done
priority: P2
size: S
stage: stage-5-desktop
epic: feature-components
blocked_by: [B-68, B-69]
---

# B-73 — ToonSplitHandle: the grip between a sidebar and the content

`ToonSplitHandle.dc.html` (engine: `splitGrip`). 16 dp wide; states rest | hover | drag | focus;
`onDragStart`, `onDrag(dxFromStart)`, `onDragEnd`; arrow keys move it by 8.

- A separator to accessibility, with a column-resize pointer.
- AC: the four states match their renderer; drag and keys report the offsets above under a test.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonSplitHandle.kt`.

## Findings, 2026-11-13

- **The port** (`ToonSplitHandle`, `toonSplitHandleScene`): the extension's `splitGrip` — two nails
  (6 dp, `nail` over ink) 12 dp above and below the middle, and from hover on a grip 10 × 60 dp, a
  `liftBody` with no rim: under the mouse paper, risen 2 dp over its 2 dp shadow with the nails riding
  it; held, the press takes the lift and the shadow back and fills it with `chipSelected`. Focus pushes
  the page's ring (−2, middle − 40, 20 × 80) after it. No words: the design draws none.
- **Size**: 16 dp wide (`DESKTOP.layout.split`) and as tall as it is let be, at least 80 dp (the least
  of the design's `height` editor) where nothing bounds it. At 16 dp it is narrower than the pointer
  page's 28 dp least target (rule 4), as in the design: the handle is the gutter between two panes,
  and a wider target would take presses from them. Left as the design draws it.
- **Drag**: a press calls `onDragStart` at once, as the design's `pointerdown` does (no slop), each
  move `onDrag` with the pointer's distance from the press in dp, and the release or a cancel
  `onDragEnd`. The distance is summed from each move's own change, because the handle moves with the
  sidebar it resizes: read off the pointer's place in the handle, it would shrink to the last step.
  A finger drags it too; only a mouse hovers it (D14).
- **Keys**: with focus, ← and → are a whole drag of −8 and 8 (start, drag, end), as the design's `key`;
  other keys pass on. The ring shows on keyboard focus only: a press does not focus it, where the
  design's `tabIndex` div takes focus on a press and rings.
- **Focus draws the grip at rest.** The pointer page's focus cell and the design's `state: 'focus'`
  draw the nails at rest with the ring; the design's live `paint()` also lifts the grip on focus
  (`st()` counts `fc` as hover). The port follows the page: a lift is the mouse's (D14), and focus is
  the ring. No cell departs: every frame the design names is drawn as it draws it.
- **Accessibility**: Compose has no separator role. The handle is read as its `label` ("Resize" by
  default) and offers two custom actions, `moveLeftLabel` and `moveRightLabel` ("Move left", "Move
  right"), that do what the keys do.
- **Cursor**: `cursor: col-resize` is `toonColumnResizeIcon` — AWT's `E_RESIZE_CURSOR` on the desktop,
  `TYPE_HORIZONTAL_DOUBLE_ARROW` on Android, the browser's own `col-resize` on the web
  (`PointerIcon.fromKeyword`, experimental in Compose 1.12). Compose offers none on iOS, which keeps
  its default pointer.
- **Frames**: rest, hover, drag and focus at the page's 120 dp, focus with hover and with drag, the
  component's 200 dp default, and both ends of its `height` editor (80 dp hovered and focused, 900 dp
  held), in three themes — 27 cells in `desktop.json`, worst coordinate 0.050 dp
  (`ToonSplitHandleTest`). The composable picks the scene each state names, pixel for pixel against
  the scene drawn alone. Looked at on a contact sheet at density 2 in all three themes.
- **Not drawn**: no Chrome reference or golden holds the handle's pixels, as for B-70–B-75.

- [x] The four states match their renderer: rest, hover, drag and focus, in three themes, at three
  heights.
- [x] Drag reports the pointer's distance from the press — through a sidebar that moves the handle
  with it — and ← → report −8 and 8, each a whole drag, under a test.
- [x] A separator to accessibility (its label and two moves; Compose has no separator role), with a
  column-resize pointer.
