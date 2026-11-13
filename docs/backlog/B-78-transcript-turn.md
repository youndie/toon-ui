---
id: B-78
title: "ToonTranscriptTurn: one turn of a transcript, calm to read"
status: open
priority: P1
size: L
stage: stage-5-desktop
epic: feature-components
blocked_by: [B-68, B-69, B-70, B-80]
---

# B-78 — ToonTranscriptTurn: one turn of a transcript, calm to read

`ToonTranscriptTurn.dc.html` (engine: `plate`, `ring`, `iconBtn32`; the extension's `layout.textMax`, 640).
Props: `speaker`, `letter`, `kind`, `shape`, `color`, `time`, `text` or `paras`, `state` rest | hover
| focus | menu | playing | found, `audio`, `mark` with `cur`/`curMark`, `onAction('play' | 'copy' |
'reassign' | 'rename' | 'name')`.

- **The reading area is still**: no idle, no tilt, no FX over text. Two things may mark a turn: the
  one playing (bold time and a wave mark) and a match from search (accent highlight; the current one
  in `primary` with an ink outline). Selection colour is `chipSel`, so it is never mistaken for a match.
- Text 16/1.6, at most 640 wide, Cyrillic in the kit's faces. Hover shows three actions (play, copy,
  more); every one is also on the keyboard — Space, ⌘C, Shift+F10 — and in the context menu
  (`ToonMenu`). An unnamed voice carries a "name this voice" link.
- **Measured budget:** 1,500 turns built from the kit's ink icons scrolled at 60 FPS with about 1 ms
  of work per frame on an Apple M1. This component is re-measured the same way.
- AC: the six states match their renderer in three themes; the keyboard paths reach every action; a
  1,500-turn list holds 60 FPS on the same harness.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonTranscriptTurn.kt`.
