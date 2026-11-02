---
id: B-33
title: "The brief's v1 components have no design yet"
status: done
priority: P3
size: XS
stage: stage-4-product
---

# B-33 — The brief's v1 components have no design yet

Slider, Menu, BottomSheet, NavigationDrawer, Tooltip, Segmented button, Pull-to-refresh, Empty
state and the screen transitions are in the brief's v1 list. None is in the design project, except
the iris wipe as an FX (research §1.3).

- **Decided by the owner on 2026-11-02: in Claude Design.** Each v1 component is drawn in the
  design project first: a renderer and its curves in `toon-engine.js`, a `Toon<Name>.dc.html` with
  its props, and a spec page with frame strips and a reduce-motion word. This repository builds a
  component only once a re-import (B-34) has brought it in, and then like any MVP component,
  against goldens and references.
- Rejected: drawing v1 in Kotlin from the Principles page's recipe. It is faster, but the
  component's screenshots would be their own goldens, and parity would prove nothing.
- What this means for the backlog: no v1 item is written ahead of its design. A re-import that
  brings in a new component opens its item (B-34's AC).

- AC: the answer is recorded here, in research §1.3 and in `CLAUDE.md`.
- Anchors: `reference/design-system/toon-ui Principles.dc.html`, `reference/design-system/uploads/brief.md`.
