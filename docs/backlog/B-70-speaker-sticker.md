---
id: B-70
title: "ToonSpeakerSticker: a speaker told apart by shape, colour and letter"
status: done
priority: P1
size: M
stage: stage-5-desktop
epic: feature-components
blocked_by: [B-68]
---

# B-70 — ToonSpeakerSticker: a speaker told apart by shape, colour and letter

`ToonSpeakerSticker.dc.html` (engine: `sticker`). Props: `size` 24 | 32 | 48, `kind` named | unnamed |
me, `shape` 0–7, `color` 0–2, `letter`.

- A call can have eight voices and the kit has three accents, so speakers differ by shape and letter
  as well as colour: 8 shapes × 3 accents. "Me" is a capsule in `primary` with a microphone mark; an
  unnamed voice, or an expected participant who never spoke, is a dashed outline on paper.
- Still: no idle motion. It is drawn once per turn in a long list — 1,500 turns for a two-hour call.
- AC: every `kind` × the three sizes matches its renderer in three themes; the 24 shapes are distinct
  at 24 dp.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonSpeakerSticker.kt`.

## Findings, 2026-11-13

- **The port** (`ToonSpeakerSticker`, `toonSpeakerStickerScene`): the extension's `sticker`, with its
  `blobPts` (eight radial shapes and the capsule) and `blobPath`. `blobPath` moves each point along its
  normal exactly as the kit's `wob` does, so the two now share `wobbleAlongNormals`; `wobblePoints` is
  unchanged. The accents are the extension's: the theme's sticker colours, or Bright Cel's card colours,
  since its sticker colours are all paper. `size` is the design's three, `ToonSpeakerSize`; `shape` and
  `color` wrap, where the engine would draw no fill for a negative colour. A letter of two or more
  characters is set smaller, as the renderer does.
- **Still and cheap**: no clock and no idle; the scene is remembered per look, as the engine caches it.
  It says nothing to a screen reader unless described, being decoration beside a name.
- **Frames**: every kind at 24, 32 and 48 dp, and the seven other shapes at 24 dp in the accent their
  index wraps to, in three themes — 48 cells in `desktop.json`, worst coordinate 0.050 dp, within the
  engine's 0.1 dp rounding (`ToonSpeakerStickerTest`).
- **Distinct at 24 dp**: no two of the eight shapes share an outline, and the three accents are three
  hues, at least 79 apart in RGB (Ink & Gouache night's yellow against its pink). **The closest outlines
  are the circle (shape 0) and the four-petal shape (shape 4): 24 pixels of 576 differ in the two Ink &
  Gouache themes, 19 in Bright Cel.** On an enlarged sheet the four-petal reads as a softened octagon,
  told from the circle, but it is the pair a reader confuses first; with a different letter or accent
  the two speakers still differ. Recorded for the owner, not changed: the shapes are the design's.
- **Not drawn**: the desktop project has no spec pages, so no Chrome reference or golden holds the
  sticker's pixels; the frames hold its numbers. The component catalogue now lists the desktop set
  beside the kit's.

- [x] Every `kind` × the three sizes matches its renderer in three themes.
- [x] The 24 shapes are distinct at 24 dp (outlines and accents, numbers above).
