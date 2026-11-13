---
id: B-90
title: "The sidebar's empty text breaks its lines as the design's does"
status: open
priority: P2
size: S
stage: stage-5-desktop
epic: feature-components
---

# B-90 — The sidebar's empty text breaks its lines as the design's does

A consumer laid `ToonSidebar` out at 300 dp with nothing to list and compared it with the design's
rendering: the empty-state paragraph (`emptyText`) keeps one word more on its first line than the
design, so the next line starts a word later. The design sets that paragraph with `text-wrap:pretty`
(`reference/desktop/ToonSidebar.dc.html`, the empty state's second line), under which Chrome avoids a
last line of one short word and carries a word of the line before it down. Compose breaks greedily,
and Skia's paragraph has no such strategy: `LineBreak.Paragraph` changes nothing on desktop.

- **The decision and its reason.** Lay the paragraph out greedily, and when its last line holds one
  word and there is a line before it, lay it out again narrowed by that line's last word, so the word
  goes down. That is the case `pretty` exists for, and the one the consumer met. Chrome's rule is
  wider (it may rebalance the last four lines); the item measures the port against Chrome on the
  design's own text and on a few lengths, and records where they still part.
- Rejected: `LineBreak.Paragraph` / `Strategy.Balanced`. Android honours them, desktop and the browser
  do not, and the sidebar is a desktop component.
- Not covered: other paragraphs. The transcript turn's and the kit's texts set no `text-wrap`.

- AC: at 300 dp, the design's empty text and the kit's default break where Chrome breaks them, under
  a test that reads the line ends from the layout; a text whose greedy last line is not a lone word
  lays out as before.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonSidebar.kt`.
