---
id: B-35
title: "Cyrillic: find OFL faces that carry Russian in each of the four roles"
status: done
priority: P1
size: M
stage: stage-1-materials
blocked_by: [B-02]
---

# B-35 — Cyrillic: find OFL faces that carry Russian in each of the four roles

toon-ui supports Cyrillic (owner, 2026-11-02). None of the design's four families has it: Shrikhand
and Figtree are latin/latin-ext, Titan One latin/latin-ext, Fredoka adds only Hebrew (research
§1.4). Without this item, a Russian label is drawn in the platform's fallback face.

- **The decision and its reason.** For each role (base display, base body, Bright Cel display,
  Bright Cel body), find OFL families with a `cyrillic` subset whose Latin is close enough to sit
  next to the design's face. Each candidate goes through the same check: the same Russian and
  English string rendered side by side on the role's component (Button label, card title,
  title-card display), with x-height and weight matched to the original. The design's face stays
  primary for Latin, and the candidate backs it for Cyrillic. Only if no backing face reads as one
  family does a role switch faces entirely, and then that is a design change, raised with the
  owner.
- The candidates and their verdicts are written into research §1.4, with the google/fonts commit
  each was read at.
- Rejected: a system font as the Cyrillic fallback. It looks nothing like a title card, which is
  the problem this item exists for.
- Not covered: other scripts.

- AC: a desktop screenshot of every text-bearing component with a Russian label, in all three
  themes, drawn in the chosen faces; the licences are in the repository; research §1.4 names each
  choice and the alternatives that lost.
- Anchors: `docs/research/research-architecture.md`, `reference/design-system/README.md`.

## Findings (2026-11-02)

- **Done as decided.** Each role keeps the design's face for Latin and is backed for Cyrillic:
  Rubik Italic 900 under Shrikhand, Onest under Figtree, Nunito 1000 under Titan One, Nunito two
  weight steps heavier under Fredoka. The candidates, the verdicts and the commit are in research
  §1.4; no role had to switch faces, so nothing went back to the owner.
- **How it is drawn:** every label (`ToonTextRenderer`) and the text field's value (a visual
  transformation) put runs of Cyrillic in the backing face, scaled to the design face's x-height.
  Latin is untouched: none of the 1315 goldens moved.
- **Screenshots:** `Cyrillic_Sheet_*`, one per theme — the top app bar, tabs, buttons, chips,
  checkbox and radio, switch, text field, card, list item, FAB and badge, snackbar, dialog and
  navigation bar, every label Russian.
- **Corrected in B-29:** this entry used to say viddik's default verification let a wrong backing
  scale through the sheets. It did not: the count of failed goldens was read with `grep -o`, which
  cannot match the failure on the line after its test case, and so read 0. Re-run on 2026-11-02,
  a Titan One backing at 1.1 fails `Cyrillic_Sheet_BrightCel` (0.62 %) and Nunito at Fredoka's own
  weight fails it too (4.02 %). `ToonCyrillicMetricsTest` stays: it names the cause (x-height
  within a row, stems within 12 %) where a golden only says that pixels moved, and every scale and
  weight mutant fails it. The display backings are one instance each, so the weight asked of them
  cannot change them.
- **Licences** are in `toon-core/src/commonMain/composeResources/files/licences` (Rubik, Onest,
  Nunito, beside the four). The font folder grows by 0.8 MB.
