---
id: B-02
title: "Fonts: bundle the four OFL families the design names"
status: done
priority: P0
size: S
stage: stage-0-risks
blocked_by: [B-01]
---

# B-02 — Fonts: bundle the four OFL families the design names

The design uses Shrikhand and Figtree (base) and Titan One and Fredoka (Bright Cel), loaded from
Google Fonts. All four are OFL (research §1.4).

- **The decision and its reason.** Bundle the four faces through compose-resources with their
  `OFL.txt`, from the google/fonts commit research §1.4 cites. `ToonFonts` stops being a
  constructor parameter (the sketch makes the caller supply them).
- **Cyrillic: yes** (owner, 2026-11-02: "найдём в процессе"). None of the four has it. Finding the
  families that back each face is [B-35](B-35-cyrillic-fonts.md), so this item is not blocked on
  the search: it ships Latin, and B-35 adds the Cyrillic faces.
- Rejected: Google Fonts downloads at runtime. That needs the network on first draw and does not
  exist on iOS.

- AC: the four faces render in a desktop screenshot test; the licence files are in the repository.
- Anchors: `reference/design-system/README.md`, `docs/research/research-architecture.md`.

## Done (2026-11-02)

- The four files and their OFL texts are bundled unmodified (research §1.4 has the hashes and axes).
  `ToonFontFamilies` gives each theme a display and a body family. It is internal until B-08's
  theme exposes it.
- AC checked by measurement, not a screenshot: `ToonFontFamiliesTest` shows every face measures
  unlike the platform fallback and unlike each other, and that `wght` 800/700 is wider than 400 on
  both variable faces. Screenshots start with B-06's references; a width tells a fallback apart
  without a golden.
- Mutants, after the commit: every Figtree weight pinned to Normal fails the `wght` test; Titan One
  replaced by Shrikhand fails "two faces measured alike". Both restored.
- `./gradlew check` 3/3 tests, `make check` green.
- Found: variable `wght` may not apply below Android 8, with `minSdk` 24 (research H7, not run).
