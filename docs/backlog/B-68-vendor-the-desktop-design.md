---
id: B-68
title: "Vendor the desktop components' design from the app project they were drawn in"
status: done
priority: P1
size: M
stage: stage-5-desktop
epic: feature-components
---

# B-68 — Vendor the desktop components' design from the app project they were drawn in

A macOS app built on this kit had nine desktop components and a pointer rule designed in its own
Claude Design project (`fc2c88bc-3b5a-4dc2-a976-c589ef695d0b`), and the owner decided they belong in
toon-ui rather than in the app. None
of it is in this repository: `reference/design-system/` is this kit's own project.

- **The decision and its reason.** Freeze, under `reference/desktop/`, the files the nine components
  are drawn by: the project's engine extension, `*-engine.js` beside `toon-engine.js` (new renderers,
  curves and a token object of its own, added to the prototype of the engine's class without
  touching `toon-engine.js`), the nine `Toon*.dc.html` —
  `ToonTitleBar`, `ToonSidebar`, `ToonSplitHandle`, `ToonScrollbar`, `ToonTranscriptTurn`,
  `ToonSpeakerSticker`, `ToonLevelMeter`, `ToonRecordButton`, `ToonSearchField` — the page
  `… Pointer & Keyboard.dc.html`, and the project's README, at one snapshot named in research the
  way `reference/design-system/` is.
- The snapshot's own copies of `toon-engine.js` and `toon-kit.js` are compared with
  `reference/design-system/` byte for byte; the designer could not check that, and says so.
- **Not vendored: the Cyrillic faces of the project's base stylesheet.** The designer chose Lobster,
  Golos Text and Rubik 800 without this repository; the owner decided the kit's faces stand.
- The golden harness loads the engine extension after `toon-engine.js`, so the new renderers have
  oracles the way the old ones do.
- Known gaps, from the designer's README: no state sheets for the nine, no frames for the eight app
  events, no icon page. The renderers are the source; where they are silent, the component departs
  and `ToonDepartures` records it (D13).
- Rejected: building from screenshots of the app's artboards — they cannot be re-rendered or read for
  numbers.

- AC: `reference/desktop/` holds the files above with their snapshot recorded; the engine copies are
  identical to `reference/design-system/` or the difference is written down; one golden from a new
  renderer (`sticker`) is emitted by the harness.
- Anchors: `reference/desktop/`, `reference/design-system/toon-engine.js`.

## The question

The project's files carry the app's working name — in the engine extension's file name, in the name
of the token object it adds, and in the pointer page's title. Vendored byte for byte, that name
enters this public repository. The owner decides before this item starts: vendor the files as they
are, or rename them on the way in and record the mapping as the first departure of
`reference/desktop/` (the content is otherwise unchanged).

## Answer, 2026-11-12

**Rename on the way in.** The app's working name is replaced by `desktop` wherever it stands in a
file name, an identifier or a page title: the engine extension becomes `desktop-engine.js`, its token
object `DESKTOP`, the pointer page `Desktop Pointer & Keyboard`. The content is otherwise unchanged,
and the rename is the first departure recorded for `reference/desktop/`, described in those terms.

- No file, identifier, comment, commit message or pull request of this repository carries the working
  name; a search for it over the tree finds nothing before the item closes.

## Findings, 2026-11-13

- **Vendored** from the owner's download of the project, taken on 2026-11-13 (74 files): the twelve
  files above, in `reference/desktop/`, with the rename as departure 1
  ([`reference/desktop/DEPARTURES.md`](../../reference/desktop/DEPARTURES.md)). A segment-by-segment
  comparison of each pair finds no other change. Research §1.8 records the snapshot.
- **The project's copies of the kit are this repository's bytes**: `toon-engine.js`, `toon-kit.js`,
  `support.js`, the four tiles and all 27 kit component files. Nothing is written down as a
  difference because there is none.
- **The extension replaces one kit renderer.** It overwrites the engine's `sheet`, the bottom
  sheet, with a panel of another signature that the sidebar draws its body with, and it adds its 12
  icons to the kit's own `ICONS` table. The harness therefore loads it only after the kit's goldens;
  loaded first, it makes the harness die in the bottom sheet's cells. The port keeps the two apart
  (research §1.8, for B-77).
- **The base stylesheet is not vendored at all.** Besides the Cyrillic faces it held one
  `::selection` rule whose colours are the extension's own `tok` values, so nothing is lost.
- **The golden.** `scripts/engine-goldens.mjs` writes `engine-goldens/desktop.json`: the sticker's
  three kinds at sizes 24, 32 and 48 in three themes, 27 frames, from `scripts/desktop-cells.mjs`.
  Every earlier golden is byte for byte what it was. Moving the sticker's shadow from 2 to 3 dp in the
  extension makes `--check` fail on `desktop.json`.
- **A vendored page does not open on its own** (it links the kit's files and the base stylesheet
  beside itself); nothing here needs it to, since the harness loads the engine files by path.

- [x] `reference/desktop/` holds the files above with their snapshot recorded (research §1.8).
- [x] The engine copies are compared with `reference/design-system/`: identical.
- [x] One golden from a new renderer (`sticker`) is emitted by the harness (`desktop.json`).
