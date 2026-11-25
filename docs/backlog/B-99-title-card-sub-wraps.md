---
id: B-99
title: "ToonTitleCard: a long sub line wraps, and is scaled only down to a floor"
status: done
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

## Findings (2026-11-25)

- **Built.** `toonTitleSubFit` sets the sub: one line at the design's 14 where the widest plaque holds it — the design's
  subs and every sub that fitted before, so their scenes are unchanged; else up to three lines at 14; only then 13 and
  12; and at 12 a fourth line's words go into an ellipsis on the third, as does a single word wider than the room. A
  wrapped sub takes the widest plaque, which grows down by a line for each line past the first; the title and the first
  line stay where they were. `toonTitlePlaqueFit` still scales the title, and a sub only when no fit is given.
- **Every number designed here (D13):**
  | What | Value | Why |
  |---|---|---|
  | lines at most | 3 | the issue's; an empty state's sentence, not a paragraph |
  | line pitch | 18 dp | the sub's 14 at about 1.3 |
  | floor | 12 sp | the issue's; the kit's smallest body size |
  | sizes tried | 14, 13, 12 | whole sizes, so a sub never lands on 12.37 |
  | room for wrapped lines | the words' room less 8 dp a side | the lower star's points reach 27.5 dp in, past the words' 24 (first golden: «настройках.» ran into it) |
- **The issue's sentences**, measured by the kit's own text in the theme's face, in cards 340 and 390 wide: at most three
  lines, at 14 sp, every word kept in order, each line inside the room (`the issue's sentences wrap…`).
- **Goldens** `TitleCard/LongSub_InkGouacheLight`, `…Night`, `…BrightCel`: the board's empty state, 340 × 300, «Репозиториев
  нет» over three lines at 14; looked at. No other golden moved.
- **Mutants**, each caught: one line at most (two tests), a floor of 6 (two tests), the plaque not growing, a pitch of 14
  (the plaque test), no clearance from the star (the `LongSub` goldens of Ink & Gouache; the unit tests measure their own
  room, so they do not see it).
