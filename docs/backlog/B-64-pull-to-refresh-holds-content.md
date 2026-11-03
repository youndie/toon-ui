---
id: B-64
title: "Pull-to-refresh pulls the content, not a band by its tip"
status: done
priority: P2
size: M
stage: stage-2-components
epic: feature-components
---

# B-64 — Pull-to-refresh pulls the content, not a band by its tip

The owner, in the running kit (2026-11-03): "pull-to-refresh needs thinking through — now it is as
if we pull it by the nose, and then it disappears once it has spun". `ToonPullToRefresh` is the
design's strip on its own: a drag stretches a band by its middle with the blob at its tip, nothing
is attached to the band, the content (reported through `onPull`, which the kit never used) does not
move, and when the refresh ends the spinner shrinks to nothing where it hung.

- **The decision and its reason.** `ToonPullToRefreshBox(refreshing, onRefresh, modifier) { content }`
  holds the content, and the content is what the finger pulls:
  - dragged, the content follows and a gap opens above it, the band stretched across the gap from
    its pegs to the blob just over the content;
  - let go past 40 dp, the band snaps and flings the blob up (the engine's `pullRelease`), and the
    content springs to 60 dp down on the same spring and waits under the spinning blob;
  - done, the blob shrinks as before (260 ms), then the content springs back up and closes the gap
    (420 ms), the slack band on its pegs going with it;
  - let go short of 40 dp, the content springs straight back.

  The strip and all its cells stay as the design has them; the box only adds the gap
  (`toonPullGapAt`) and draws the strip in it. A drag a child consumes does not pull, and a pull
  needs 8 dp of travel down first, so taps and sideways drags stay the content's.
- Rejected: changing the strip itself so its band pulls something — it has nothing to pull; and an
  overload of `ToonPullToRefresh` with a content slot — a trailing lambda matches both it and
  `onPull`, and the call is ambiguous.
- AC: held content moves with the drag, waits 60 dp down while refreshing, and comes back when
  done; the gap never jumps, from the drag through the release, the hold and the close.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonPullToRefresh.kt`,
  `sample/src/commonMain/kotlin/io/github/youndie/toon/sample/ToonUiSample.kt`.

## Findings (2026-11-03)

- The kit's Pull to refresh plate is now a three-item list in the box; drawn frame by frame it shows
  the gap and the band during the drag, the blob spinning over the list while it waits, and the
  list closing the gap after the blob has gone.
- `ToonPullToRefreshTest`: the gap grows under 2 dp a dp of drag and never steps more than 2 dp a
  millisecond in the close; it starts the release where the drag left it and settles at 60; a short
  pull springs back to 0; under reduce motion it is where it ends at once. On a real drag the
  content's top moves more than 80 dp, waits at 60 dp while refreshing and is back when done.
  Mutants: the content left in place — "the content follows the finger: 0.0 dp"; the gap closed at
  once when done — "done 0 ms: the gap 60.0 then 0.0".
- **No cell changed**: the strip draws as before, its tests and goldens untouched. The public API
  gains `ToonPullToRefreshBox`, recorded in `toon-core/api/`.
- Not covered: overscroll of a scrolling child (nested scroll). Inside a list that scrolls, the
  pull works where the list does not consume the drag — a mouse drag on desktop, a touch above the
  list. Pulling a list from its top by touch wants a nested-scroll connection, a later item.
