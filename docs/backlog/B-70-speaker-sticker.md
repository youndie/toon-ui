---
id: B-70
title: "ToonSpeakerSticker: a speaker told apart by shape, colour and letter"
status: wip
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
