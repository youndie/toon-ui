---
id: B-80
title: "The desktop icons: the extension's 12 glyphs in ToonIcons"
status: wip
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
