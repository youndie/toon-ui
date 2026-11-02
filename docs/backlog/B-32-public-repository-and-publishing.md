---
id: B-32
title: "Public repository and publishing"
status: open
priority: P2
size: S
stage: stage-4-product
blocked_by: [B-01]
---

# B-32 — Public repository and publishing

The brief calls toon-ui open source. The repository is local and has no remote.

- **Licence: MIT** (owner, 2026-11-02). `LICENSE` is in the repository and the sborka
  `sborka.licence` property says the same once B-01 creates the build. This departs from oldge-ui
  and kvadrant-ui, which are Apache 2.0.
- **The decision and its reason.** Publish like oldge-ui: `youndie/toon-ui` on GitHub, and every
  push to `main` publishes `<version>.<run>` to the Reposilite through sborka's
  `publish-wip.yaml` on macOS. Credentials come from the infrastructure repository's token
  workflow, never by hand.
- Still the owner's: **when** it goes public. It is not done by this item without that go-ahead.
- Not covered: Maven Central.

- AC: the repository is public with CI green; a `0.1.0.<run>` build resolves for a consumer.
- Anchors: `LICENSE`, `youndie/oldge-ui@618cef8b923d7e0e501752240e6a727eed51e48c!/CLAUDE.md`.
