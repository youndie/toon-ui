---
id: B-07
title: "Tokens generated from THEMES: colours, shape, type, motion, three themes"
status: wip
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
