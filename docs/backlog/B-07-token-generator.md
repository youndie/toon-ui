---
id: B-07
title: "Tokens generated from THEMES: colours, shape, type, motion, three themes"
status: done
priority: P1
size: M
stage: stage-1-materials
blocked_by: [B-03]
---

# B-07 — Tokens generated from THEMES: colours, shape, type, motion, three themes

D4: the token layer is generated from `THEMES` in the vendored engine and never typed.

- **The decision and its reason.** A generator, run through Node, reads the three themes and writes
  `ToonColors`, `ToonShape` (wobble, insets, button radii, radius scale, shadow, tilts, seed),
  `ToonType` and `ToonMotion` (80/160/260/420/600 ms, cascade 40, hold 60, and the springs as ζ and
  ω pairs), mapping the engine's short keys to readable names in one table. `make check` and
  `./gradlew check` fail on a stale file.
- The palette is fixed at what the design has (owner, 2026-11-02; research §1.3): 3 sticker
  colours and single semantic tokens, with no dark/light step per colour. The generator emits what
  the engine has and nothing more.
- Rejected: the sketch's `ToonTheme.kt` (typed by hand, already holds `stageWash`).

- AC: the three themes compile into `toon-core`; a test asserts one value per family against the
  engine.
- Anchors: `reference/design-system/toon-engine.js`, `reference/design-system/compose/toon/ui/theme/ToonTheme.kt`.

## Done (2026-11-02)

- `scripts/generate-tokens.mjs` writes `tokens/ToonTokens.kt`: `ToonColors` (all 27 colour keys, the
  night-only rim pair, the card fills and stickers), `ToonShape`, `ToonTokens` per theme,
  `ToonTypeScale`, `ToonStroke`, `ToonSize`, `ToonMotion` and `ToonSpring`. `make tokens` writes it;
  `make check` runs `--check`.
- **Found:** the type scale, sizes and motion are not in THEMES but in the Core page's token table.
  They are taken from there, each held to its page text (research D4). The design's fonts are names
  here; binding them to the bundled files is B-08's theme.
- `ToonTokensTest` compares every colour, shape number and font name of the three themes with the
  engine's own THEMES (`engine-goldens/themes.json`, added to B-03's harness). One more test fails
  if THEMES gains a key the tokens do not carry. Mutant: stage `#CBC3A6` → `#CBC3A7` failed the test
  ("il.bg") and `--check` ("ToonTokens.kt is stale").
- `ToonRadii` and `ToonInsets` left `@InternalToonApi`: they are token types now.
