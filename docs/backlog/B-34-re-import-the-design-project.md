---
id: B-34
title: "Re-import the design project as a diff, never an overwrite"
status: open
priority: P2
size: S
stage: stage-4-product
blocked_by: [B-03, B-06, B-07]
---

# B-34 — Re-import the design project as a diff, never an overwrite

The design project is live and versioned by the designer (`?v=8`, research H4). A newer engine
changes goldens, references and tokens at once.

- **The decision and its reason.** A re-import runs on a `chore/import-design-<date>` branch. It
  fetches the project with DesignSync, diffs `toon-engine.js` by function (themes, icons, each
  renderer and curve), regenerates tokens (B-07), goldens (B-03) and references (B-06), and puts the
  diff in the commit body. Research §1.1 records the new snapshot.
- The first re-import happened on 2026-11-02, the day of the import: the second snapshot replaced
  the first before any golden, reference or token existed, so there was nothing to regenerate.
  The by-function diff is in research §1.1. The AC below waits for the first re-import that has
  goldens to move.
- A script cannot fetch: DesignSync needs the signed-in session. The procedure is written into
  `CLAUDE.md` for an agent to follow.

- AC: one re-import done end to end with the diff in its commit; every component the re-import
  brought in has its own item, built like stage 2 (B-33).
- Anchors: `reference/design-system/README.md`, `CLAUDE.md`.
