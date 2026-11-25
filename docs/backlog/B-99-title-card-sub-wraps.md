---
id: B-99
title: "ToonTitleCard: a long sub line wraps, and is scaled only down to a floor"
status: open
priority: P1
size: S
stage: stage-4-product
epic: feature-components
blocked_by: [B-81]
---

# B-99 — ToonTitleCard: a long sub line wraps, and is scaled only down to a floor

Issue [#72](https://github.com/youndie/toon-ui/issues/72), a follow-up to #30 from pyatiletka on toon-core `0.1.0.43`.
[B-81](B-81-title-card-fits-a-long-title.md) fitted the title; the sub line, wider than the room the plaque leaves, is
scaled by `room / width` with no floor and never wrapped (`toonTitlePlaqueFit`): an empty state's sentence comes out
about 7.4 sp in a 340 dp card.

- **The decision** is the kit's. The issue's candidate: wrap the sub onto two or three lines inside the plaque, growing its
  height, and scale only below that, with a floor (say 12 sp) under which the line ends in an ellipsis.
- **Why before the release.** A consumer works round it today (`sub = ""`, its own text under the card), and that
  workaround would ship into every app built on 0.1.0.

- AC: the issue's two sentences read at the floor or above in a 340 × 300 card and on a 390 phone, in every theme; a short
  sub does not move; goldens recorded and looked at; #72 is closed by the pull request.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonTitleCard.kt`
