---
id: B-03
title: "Engine harness: run toon-engine.js in Node and emit golden JSON"
status: wip
priority: P0
size: M
stage: stage-0-risks
blocked_by: [B-01]
---

# B-03 — Engine harness: run toon-engine.js in Node and emit golden JSON

The engine's geometry and curves run in Node with a `window` stub (research §1.2). That makes them
callable as an oracle, the base of D3's numeric parity.

- **The decision and its reason.** A script loads the vendored engine untouched and writes JSON
  goldens. For each theme: the `wob`/`shape` points for each component's box, radii, insets and
  seed; `squashOf`, the squashed button geometry at p = 0, 0.5, 1, −0.31; `inkLine`; `star`,
  `burst`. Every curve sampled at the timestamps its spec frame strip lists, plus a 10 ms grid to
  400 ms. Text-dependent boxes take the width Chrome measured, recorded beside the goldens (H2).
- The engine is never edited to make it run. If something needs a stub, the stub lives in the
  harness and says why.
- Rejected: porting first and comparing pixels afterwards. A pixel diff cannot say which number
  moved.
- Not covered: icons and text, which need a DOM (B-06, B-11).

- AC: `make goldens` (or the Gradle task) regenerates the JSON deterministically; a second run is
  byte-identical; the files are committed and `make check` fails when they are stale against the
  engine.
- Anchors: `reference/design-system/toon-engine.js`.
