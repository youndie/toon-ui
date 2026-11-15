---
id: B-94
title: "A closed navigation drawer takes every click from the content under it"
status: done
priority: P0
size: S
stage: stage-4-product
epic: feature-components
---

# B-94 — A closed navigation drawer takes every click from the content under it

The owner, 15.11.2026: on https://youndie.github.io/toon-ui/ no click does anything. The sample keeps
its dialog, sheet and drawer composed over the whole window, closed, as overlays are used. B-79 gave
the drawer the pointer rule's hover through `toonSlotHoverable(hovers, clock.open, …)`. Disabled, that
modifier still puts a `pointerInput` node on the drawer's full-window canvas. Compose hands a press to
the topmost sibling with a pointer input node under it, so the closed drawer took every press and the
content under it got none, by mouse and by finger.

- **The decision and its reason.** The drawer puts the hover listener on only while open, and lets go
  of its hover with an effect when it closes. That is the rule B-24 set for the dialog and the sheet:
  closed, an overlay takes no input. The hover modifier's KDoc now says that disabling it still
  blocks the siblings under it.
- Rejected: making `toonSlotHoverable` add nothing when disabled. It could not then reset the
  hover states, which it does on disabling, without a write during composition.
- Not covered: other overlays of the caller's own.

- AC: under a closed drawer, dialog, sheet and sheet with a slot, the content takes a mouse click and a
  tap; in the whole sample, a click and a tap on the header's theme switch it.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonNavigationDrawer.kt`,
  `toon-core/src/commonMain/kotlin/io/github/youndie/toon/interaction/ToonHover.kt`.

## Findings

- Found by elimination in a desktop test: a segmented button under each closed overlay alone. The
  dialog and the sheet passed the click, and the drawer took it. The same with the whole
  `ToonUiSample`: the header's theme did not switch by mouse or by touch. In the browser it was
  reproduced in a headless Chrome; the app's own browser pane was hidden, and there wasm draws no
  frames.
- No test held "closed, takes no input" for the drawer by a click *under* it. Its tests clicked the
  drawer itself, and the sample's tests drove its screens through semantics, which no overlay blocks.
- Held by `ToonClosedOverlayTest` (all four overlays, mouse and touch) and the sample's
  `SampleClicksTest`.
