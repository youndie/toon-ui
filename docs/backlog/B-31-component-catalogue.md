---
id: B-31
title: "Component catalogue: which composable is which design file, generated"
status: wip
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
