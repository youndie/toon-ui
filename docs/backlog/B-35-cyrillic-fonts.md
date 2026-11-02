---
id: B-35
title: "Cyrillic: find OFL faces that carry Russian in each of the four roles"
status: wip
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
