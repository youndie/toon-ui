---
id: B-93
title: "The scrollbar's thumb stays under the pointer over a lazy list of uneven items"
status: done
priority: P1
size: S
stage: stage-5-desktop
epic: feature-components
---

# B-93 — The scrollbar's thumb stays under the pointer over a lazy list of uneven items

`ToonScrollbar(state: LazyListState)` (B-72) places its thumb by an estimate: the content's length is
the laid-out items' average size times their count, and the view's top is the first item's index times
that average less its offset. A drag, though, moves the list by pixels: the pointer's distance over the
track times the estimated length, through `dispatchRawDelta`. Over items of one size the two agree.
Over items of very different sizes they do not: the items scrolled past are not the average of the ones
in view, and the average itself changes as the view moves, so the list passes a different number of
items than the thumb's travel stands for, and the thumb runs ahead of the pointer and jitters. A
consumer's first live run showed it in a transcript of turns of very different lengths, a few hundred
items long.

- **The decision and its reason.** A drag puts the view where the thumb is held, instead of moving it
  by a distance. At the press the bar keeps the thumb's fraction; each move adds the pointer's travel
  over the track to it and asks the list to stand at that fraction. A lazy list maps the fraction to an
  item and a share of it, through the same estimate the thumb is drawn by, and goes there with
  `scrollToItem(index, offset)`. Its view's top is read as that item index plus the share of the first
  visible item it has scrolled past, by that item's own size, times the average: so the thumb's place
  depends on the position alone, not on which items happen to be laid out, and the position the drag
  asks for is the one the thumb then shows. The pointer's travel is read from the press, not summed
  from moves, so no error gathers over a long drag.
  - The offset into the item is its share of the item's own size when the item is laid out, and of
    the average otherwise; after the jump the item is laid out, and a second step puts the view at its
    exact share. A drag between items in view takes one step.
  - A press beside the thumb still pages by 90 % of the view, in pixels: a page is a distance.
  - The `ScrollState` overload goes through the same path, `scrollTo` a pixel: it was exact before and
    is exact still.
- Rejected: correcting the pixel delta by the estimate's change between moves. It still sums errors
  over a drag, and the average changes on every item that enters or leaves the view.
- Rejected: measuring every item to know the true length. A lazy list composes only what it shows;
  measuring a few hundred turns of a transcript to place a thumb defeats it.
- Not covered: the estimate's end. Over uneven items the thumb at the list's end may stand short of the
  track's end, or reach it before the list does, by the difference between the last view's items and
  the average. The drag reaches the list's end whenever the pointer goes on far enough, and past it
  the list waits at its end until the pointer is back, as before.

- AC: over a lazy list of 400 items alternating 20 and 300 dp, a drag of the thumb keeps the thumb's
  top within a dp of the pointer's travel at every step, down and back, and back at the start the list
  is at its top; the test fails on the pixel drag. The 1,500-row drag of B-72 still moves the list by
  the pointer's distance at every step.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonScrollbar.kt`,
  `toon-core/src/desktopTest/kotlin/io/github/youndie/toon/component/ToonScrollbarTest.kt`.

## Findings, 2026-11-14

- **Before the fix**, over 400 items alternating 20 and 300 dp, 100 moves of 5 dp down and 100 back
  left the thumb's top from 5 dp behind to 80.5 dp ahead of the pointer's travel.
- **After it**, the thumb's top stays within 0.07 dp of the pointer's travel at every one of the 200
  moves, and back at the start the list is at item 0, offset 0. The 1,500 rows of B-72 still move
  287–288 dp for each 2 dp of pointer against 287.7 expected, and past the end the list waits at its end.
- **The fraction at the press, not the pointer's place**, is where the travel is added: where the
  design holds the drawn thumb inside the track near a long list's end, a press there and a move do not
  jump (B-72's reason for its deltas).
- **The scroll** runs in the bar's coroutine scope, started undispatched from the pointer's move, so it
  lands in the same frame; a newer scroll cancels the one before it, as the list's scrolls do.
- **Held** by `ToonScrollbarTest`: the uneven list down and back, and a `ScrollState`'s thumb dragged
  50 moves of 2 dp, its value within half a pixel of the pointer's share of the track at each.
- **5 mutants, each killed**: the pixel drag back (80 dp off); no second step to an item not laid out
  (1.4 dp); the view's top read by the old pixel estimate (1.3 dp); the share into an item by the
  average size (1.3 dp); a `ScrollState`'s fraction over its scroll range instead of its content.

- [x] Over 400 items alternating 20 and 300 dp the thumb's top stays within a dp of the pointer's
  travel, down and back, and the list ends at its top; the pixel drag fails it.
- [x] The 1,500-row drag of B-72 still moves the list by the pointer's distance at every step.
