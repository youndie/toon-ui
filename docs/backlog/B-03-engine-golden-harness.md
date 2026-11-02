---
id: B-03
title: "Engine harness: run toon-engine.js in Node and emit golden JSON"
status: done
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

## Done (2026-11-02)

- `scripts/engine-goldens.mjs` loads `toon-engine.js` untouched. It wraps `smooth`, `wob`,
  `inkLine`, `star` and `burst` on the engine instance, then calls 29 renderers per theme (MVP and
  v1, the button at p = 0, 0.5, 1 and −0.31). Every geometry call they make is recorded with its
  arguments, and `wob` with its points before the 0.1 px path rounding. The other three only exist
  as rounded path strings, so those strings are the golden and B-04 compares within half a step.
- 43 curves sampled every millisecond over their full length (every frame-strip timestamp falls
  on it), columnar per field, six decimals. `manifest.json` names the engine's SHA-256
  (`49d00901…`).
- `make goldens` writes them. Two runs are byte-identical. `make check` runs
  `engine-goldens.mjs --check` under `gate`: one changed number in `curves.json` turned it red
  ("engine goldens are stale: curves.json"), and restoring it turned it green.
- **Narrower than written:** box widths that depend on text are the ones Node's fallback `tw`
  gives (length × size × 0.55), not Chrome's. A golden records the arguments the shape was drawn
  with, so geometry parity does not need Chrome's widths; they matter to the pixel references and
  move to B-06 (research H2).
- Size: 935 kB of JSON, most of it the 1 ms curve grid (550 kB).
