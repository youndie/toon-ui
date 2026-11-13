---
id: B-80
title: "The desktop icons: the extension's 12 glyphs in ToonIcons"
status: done
priority: P1
size: M
stage: stage-5-desktop
epic: feature-components
blocked_by: [B-68]
---

# B-80 — The desktop icons: the extension's 12 glyphs in ToonIcons

The desktop extension adds 12 centerline icons to the engine's shared `ICONS` table — `play`, `pause`,
`stop`, `rec`, `mic`, `wave`, `copy`, `folder`, `key`, `link`, `merge`, `download` (25 → 37, research
§1.8). `ToonIcons` is generated from the kit's 25 only, so a desktop component that draws one of them
cannot be built: the level meter's `mic` and `wave` (B-74), the transcript turn's `copy`, `mic`, `play`
and `wave` (B-78).

- **Decision:** generate them into `ToonIcons` with the same script (`scripts/generate-icons.mjs`), the
  extension's 12 after the kit's 25 in the engine's order, read by running the engine with the
  extension loaded, never typed: the extension adds them to the kit's own table, so `ToonIcon` keeps
  one source of glyphs. *Rejected:* a separate object for the desktop's, which would split one table in
  two; hand-typed paths.
- **Held by** the engine's own outlines: `icon-outlines.json` is the engine's `iconOutline` taken in
  Chrome (`make references`), and gains the 12 at both weights. No page draws them, so no Chrome
  reference or parity fixture is added; the Icons page's cells stay the kit's 25.
- AC: the 12 glyphs' outlines at Regular and Bold match the engine's within the icon outlines' floor;
  the kit's 25 and their fixtures are unchanged; `make check` fails while `ToonIcons.kt` is stale against
  the extension.
- Anchors: `scripts/generate-icons.mjs`, `toon-core/src/commonMain/kotlin/io/github/youndie/toon/icon/ToonIcons.kt`.

## Findings, 2026-11-13

- **The 12 glyphs** (`play`, `pause`, `stop`, `rec`, `mic`, `wave`, `copy`, `folder`, `key`, `link`,
  `merge`, `download`) are generated into `ToonIcons` after the kit's 25, `ToonIcons.all` now 37. They
  are read by running the engine and the extension in a `vm` context of their own
  (`scripts/desktop-icons.mjs`), since the extension replaces the kit's `sheet` for anything loaded with
  it; the script fails if the extension ever changes one of the kit's glyphs. The parity fixtures and
  Chrome references stay the Icons page's 25 (348 fixtures, unchanged).
- **Their outlines** are the engine's `iconOutline` taken in Chrome with the extension loaded on that
  page only. `scripts/design-references.mjs --outlines` takes only `icon-outlines.json`: this mac's
  Chrome is 157, the references were drawn in 156, and the outlines alone need not rewrite them. The
  kit's 25 came out byte for byte the same.
- **`rec` exposed Compose's arcs**: an arc whose radii fall short of its end point keeps, in Compose's
  parser, a centre a hair off the chord (it scales by the chord over 1.99999, not 2), so `rec`'s large
  arcs measured 39.04 dp against the 38.96 they integrate to and Chrome samples — 131 samples where the
  engine takes 130. The port now converts a glyph's arcs by the SVG spec in `Double` before Compose
  parses it (`withSpecArcs`, `ToonSvgArcs.kt`, research D11). Every glyph's outline is within 0.0060 dp
  of the engine's at both weights, where the kit's 25 were within 0.0149.
- **Goldens re-recorded, toward the design**: four kit glyphs have such arcs, and `settings` and `share`
  moved at 48 dp (17–23 pixels a cell, channel deltas up to 48). Each of the 12 cells is now closer to
  its Chrome reference than its old golden was (summed channel difference 4,808 → 3,533; pixels off
  the reference down or level in every cell), so the 12 goldens were re-recorded (`viddikRecord
  --component Icon`, which rewrites only those a verification rejects). Not a departure: nothing moved
  away from the design, and `ToonDepartures` lists nothing.

- [x] The 12 glyphs' outlines at Regular and Bold match the engine's within the outlines' 0.02 dp
  (worst 0.0060).
- [x] The kit's 25 and their 348 fixtures are unchanged as glyphs; two of them draw closer to the
  design (above).
- [x] `make check` fails while `ToonIcons.kt` is stale against the extension (`generate-icons.mjs --check`
  reads the extension).
