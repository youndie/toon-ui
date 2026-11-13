---
id: B-78
title: "ToonTranscriptTurn: one turn of a transcript, calm to read"
status: done
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

## Findings, 2026-11-13

- **The port** (`ToonTranscriptTurn`, `toonTranscriptTurnScene`, `toonIconButton32Scene`): the
  `.dc.html`'s drawing as its `paint()` lays it out — the plate `plate(T, 0, 2, w, h − 4)` and the focus
  `ring` in the turn's own box, the 32 dp sticker 12 dp in and 10 down, the user's mic and the playing
  turn's bold wave centred on the 32 dp header row, and the three `iconBtn32` actions 10 dp from the top
  and the end, 6 apart. `iconBtn32` is ported whole (lift, press squash, disabled).
- **What shows** is the design's `vis()`, its `state` read from the turn: `playing` is `playing`, and
  `found` is a turn holding the current match (`currentMark ≥ 0`), the port having no `state` prop. The
  plate under a mouse, on focus, with the menu open, playing or found; the ring on focus without the
  menu; the actions under a mouse, or on focus and with the menu unless the turn plays or is found.
- **The reading area is still**: the turn's own plate is instant and a mouse's only (the design's
  README: in the reading zone the plate is always instant); the three actions take the pointer rule's
  ramp (B-69) and the kit's press, as the record button does. No idle, no tilt, no FX.
- **Words** (research §1.8): the header is the page's flex row — name 15 bold (muted for an unnamed
  voice), the mic pulled 4 dp back, the wave, the time 13 in figures of one width (500, 800 while
  playing), the naming link 14 bold with a wavy `primary` underline — 10 dp apart and centred on its
  32 dp, keeping 120 dp clear of the actions while they show; the name gives way first, with an
  ellipsis. The paragraphs are real text — wrapping, selectable — 16/1.6, at most the extension's
  `textMax` (640, now in the manifest's `desktopTokens.layout` and held by a test), 4 dp apart, 4 dp
  under the header; Cyrillic in the kit's backing face. Skia sets each line on whole pixels: 25.6 dp is
  26 px at density 1, 51 at 2.
- **Highlights**: each match from where the query occurs to the end of its word (`[\p{L}\p{N}-]`),
  ignoring case, numbered through the turn's paragraphs; a match on the second accent, the current one
  on `primary` ringed in ink, each piece as tall as the face's ascent and descent plus the 2 dp the
  page's `box-shadow` spreads, drawn behind the words. The selection is `chipSelected`, through
  `LocalTextSelectionColors`; selecting is the caller's `SelectionContainer`, so it can run across turns.
- **Keys and the menu**: Space plays, ⌘C or Ctrl+C copies, Shift+F10 or the menu key opens the menu
  (the kit's `ToonMenu`, 272 dp, 4 from the end and 46 down, in a `Popup` over the turns below) and
  hands it the keys, Escape closes it, the arrows move to the turn below or above; a right click and
  the more button open it too. The turn's custom accessibility actions are the menu's.
- **Where the port does what the design does not** (behaviour only; every cell is the design's, so
  `ToonDepartures` has no entry):
  1. Without audio the play button is disabled, as designed, and Space and the menu do not play either
     (the design fires `play` from both).
  2. An unnamed voice's menu names it where a named one renames, so naming is on the keyboard too (the
     design's link is a `div` the keys never reach).
  3. The menu is not closed by the pointer leaving the turn: in a popup the pointer leaves on its way
     in. It closes on a pick, a click outside, Escape or the more button.
  4. A match covers at least the query. The design's loop ends a match at the first non-word
     character, so a query starting with one (a space) matches nothing and loops forever.
  5. The actions' press is the kit's press machine, not the extension's controller curve (80 ms in,
     a 400 ms spring out); the frames at a given press are the extension's.
  6. The wavy underline is Chrome's `wavy` decoration drawn by hand (8 dp wave, 1.5 dp high); the
     engine draws no frame of it.
- **Frames**: sixteen looks — rest, hover, focus, menu, playing, playing under a mouse, found, the
  user's turn, the user's turn playing, unnamed, unnamed under a mouse, hover without audio, an action
  hovered, half and fully pressed, and a two-line focused turn on another shape, colour and plate seed
  — in three themes: 48 cells in `desktop.json`, worst coordinate 0.050 dp (`ToonTranscriptTurnTest`).
  Looked at on contact sheets at density 2 in all three themes.
- **Measured budget**: `./gradlew :bench:turnScrollBench`, 1,500 turns (four voices, one to three
  paragraphs) in a `LazyColumn` in the design's 1280 × 800 window at density 2, a mouse resting in the
  list, each frame scrolled a fixed distance and its `render` timed — composition, layout and drawing
  rasterised on the CPU, an upper bound — on this Apple M1, while other builds ran on it. Three rounds
  after a warm-up pass (`docs/research/b-78/turn-scroll.csv`):

  | Arm | Frames | p50 | p95 | p99 | Max | Over 16.7 ms |
  |---|---|---|---|---|---|---|
  | reading, 4 dp a frame | 900 | 2.39 ms | 3.66 ms | 4.61 ms | 9.01 ms | 0 |
  | wheel, 30 dp a frame | 900 | 2.65 ms | 3.86 ms | 5.02 ms | 18.31 ms | 1 |
  | fling, 240 dp a frame, the whole list | 3,288 | 3.67 ms | 5.49 ms | 8.31 ms | 25.82 ms | 8 |
  | wheel with every "релиз" marked | 900 | 2.68 ms | 3.87 ms | 4.68 ms | 16.01 ms | 0 |

  The frames over budget are single frames apart from each other (9 of 5,988); the design's own harness
  is not in the snapshot, so "the same harness" is the same list, machine and frame rate.

- [x] The six states match their renderer in three themes (the menu's scroll is the kit's `ToonMenu`,
  held by its own cells).
- [x] The keyboard paths reach every action: Space, ⌘C/Ctrl+C, Shift+F10 or the menu key and then the
  menu's arrows and Enter (play, copy, reassign, rename or name), Escape, the arrows between turns.
- [x] A 1,500-turn list holds 60 FPS on an Apple M1: p99 at most 8.3 ms a frame, 9 single frames of
  5,988 over 16.7 ms.
