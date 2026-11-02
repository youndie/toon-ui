---
id: B-06
title: "Pixel references: render every state in headless Chrome, measure parity's floor"
status: open
priority: P0
size: L
stage: stage-0-risks
blocked_by: [B-01, B-02]
---

# B-06 — Pixel references: render every state in headless Chrome, measure parity's floor

Text widths and icons need a browser (research §1.2). The visual half of D3 is the engine rendering
each component's states to PNG in headless Chrome. viddik measures the Compose render against
those PNGs, as in oldge-ui.

- **The decision and its reason.** A Node + Chrome script loads the engine and the kit, renders
  each `Toon*` component in every state its spec page lists (`Button / Filled / Pressed` …) and
  every frame of its strips, per theme, at 2×, on the paper background without texture. Then it
  measures the floor once: the residual of Compose against the reference on a component whose
  numbers match exactly (the Divider, with no text).
- Rejected: screenshots of the designer's pages, which mix every component on one canvas.
- Not covered: textures (B-13).

- AC: references regenerate deterministically; the floor and how it was measured are in research.
- Anchors: `reference/design-system/toon-kit.js`, `reference/design-system/toon-ui MVP Components.dc.html`, `youndie/oldge-ui@618cef8b923d7e0e501752240e6a727eed51e48c!/CLAUDE.md`.
