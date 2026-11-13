---
id: B-72
title: "ToonScrollbar: an ink thumb that keeps clear of the window's edge"
status: done
priority: P1
size: M
stage: stage-5-desktop
epic: feature-components
blocked_by: [B-68, B-69]
---

# B-72 — ToonScrollbar: an ink thumb that keeps clear of the window's edge

`ToonScrollbar.dc.html` (engine: `scrollThumb`). States rest | hover | drag; the thumb spans the
visible fraction `a`–`b`, at least 28 high; a press outside it pages by 90 %.

- **8 dp inset from its scroll area**, so it never sits under an undecorated window's 8 dp resize strip
  (appframe's `resizeBorder`); hidden when the content fits.
- Bound to Compose's scroll states rather than a DOM node: a `ScrollState` and a `LazyListState`.
- AC: the three states match their renderer; dragging the thumb scrolls a 1,500-item lazy list
  without a jump; the thumb is grabbable 8 dp from a window edge.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonScrollbar.kt`.

## Findings, 2026-11-13

- **The port** (`ToonScrollbar`, `toonScrollbarScene`): the extension's `scrollThumb` — the thumb as a
  `liftBody` (rest on `off`, lifted on paper under the pointer, held in `chipSel`) and the dotted track
  while it is under the pointer or held. The state switches halfway through the pointer rule's ramp, as
  the search field's fill does (B-71); the design's `st` is a step.
- **Bound to Compose's states**: an overload for `ScrollState` (exact: value, maximum and viewport) and
  one for `LazyListState`, whose content length is the laid-out items' average times their count, as
  Compose's own desktop scrollbar estimates it: exact for rows of one height. The design's `a` and `b`
  are the view's top and bottom over that length; hidden while the content fits, the bar draws nothing
  and holds no pointer input, so a press there reaches the content under it.
- **Where it stands**: the caller lays it over the end edge of the scrolled area, as tall as it. It is
  20 dp wide, the design's 14 dp bar and 6 dp more, so the thumb's body ends 8 dp from that edge; its
  track is the design's, 8 dp in from the top and the bottom. Over an undecorated window's 8 dp resize
  strip (appframe's `resizeBorder`) the thumb stays clear of it: only its outline's wobble, 0.45 dp at
  most, reaches into the strip's first column. In the design the sidebar sets it 4 dp from its edge
  (B-77 places it).
- **The drag moves the content by the pointer's distance** over the track times the content's length
  over the track's: the design's absolute mapping (`scrollTop = (y − grab) / h · scrollHeight`) wherever
  that one is continuous. Near the end of a long list the design holds the drawn thumb inside the track
  while its `a` runs on, and there its press test (against `a · h`, not the drawn thumb) misses the
  thumb's upper part and its mapping would jump on the first move. The press is tested against the
  thumb as drawn, and the move by deltas, so the list cannot jump; past the track's end the list waits
  at its end until the pointer is back. A lazy list moves by pixels, so its estimate never makes a drag
  jump either.
- **A press beside the thumb** pages by 90 % of the view at once, as the design sets `scrollTop`. The bar
  takes no focus: the pointer page leaves the keys to the scrolled area.
- **Frames**: the pointer page's rest, hover and drag at 120 dp, the page's default (220 dp, 0.2–0.45),
  the least thumb (28 dp) and the held one at a long list's end, in three themes — 18 cells in
  `desktop.json`, worst coordinate 0.050 dp (`ToonScrollbarTest`). Looked at on a contact sheet at
  density 3.
- **Not drawn**: no Chrome reference or golden holds the bar's pixels, as for B-70 and B-71.

- [x] The three states match their renderer, in three themes.
- [x] Dragging the thumb scrolls a 1,500-item lazy list without a jump: 120 steps of 2 dp each move the
  list 287–288 dp against 287.7 expected (whole pixels), and none back from past the end jumps.
- [x] The thumb is grabbable 8 dp from a window edge: dragged beside an 8 dp strip that takes every
  press, with its ink clear of the strip but for the wobble.
