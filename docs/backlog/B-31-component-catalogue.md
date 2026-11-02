---
id: B-31
title: "Component catalogue: which composable is which design file, generated"
status: done
priority: P2
size: S
stage: stage-4-product
blocked_by: [B-14]
---

# B-31 — Component catalogue: which composable is which design file, generated

oldge-ui keeps a generated `docs/components.md` that maps each composable to its design file,
references and goldens. toon-ui needs the same, so the next agent finds a component in one hop.

- AC: `make fix` regenerates `docs/components.md`; `make check` fails when it is stale.
- Anchors: `reference/design-system/README.md`, `youndie/oldge-ui@618cef8b923d7e0e501752240e6a727eed51e48c!/CLAUDE.md`.

## Findings (2026-11-02)

- **Done as decided**, in node like the project's other generators: `scripts/component-catalogue.mjs`
  writes the table between the markers of `docs/components.md`, `make fix` runs it, and `make check`
  runs it with `--check`.
- **What the check holds**, in both directions, as oldge-ui's does: every `Toon*.dc.html` is in the
  map and every mapped one exists; each mapped composable is public in toon-core, each engine
  renderer a method of `toon-engine.js`, each item a file — `done` exactly when the composable
  exists; every public composable is in the map, under its design file (`ALSO`) or among the
  library's own (`OWN`); and the table on disk is the one the sources give. Each was tried: a
  hand-edited count, a stray public composable (written `@Composable` or qualified, on its line or
  above), a renamed composable and a v1 item marked done all fail it.
- **Today:** 19 of 27 design files built; the eight v1 files point at their items (B-36…B-43).
