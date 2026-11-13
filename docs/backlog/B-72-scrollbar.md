---
id: B-72
title: "ToonScrollbar: an ink thumb that keeps clear of the window's edge"
status: wip
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
