---
id: B-91
title: "A consumer can make a type step of its own"
status: open
priority: P2
size: XS
stage: stage-4-product
epic: feature-components
---

# B-91 — A consumer can make a type step of its own

`ToonTypeSpec`'s constructor is `internal` (`scripts/generate-tokens.mjs` writes it into
`ToonTokens.kt`), so the five steps of `ToonTypeScale` are the only ones a consumer can pass to
`ToonText`, `textStyle` or `backed`. The desktop design sets words the scale has no step for: a
consumer that sets a line at 15 sp and weight 500 can only take `Body` (15 / 400), a weight lighter
than the design. `ToonText` and `backed` already work for any size and weight; only the type is
closed.

- **The decision and its reason.** Make the constructor public, and add
  `copy(size, lineHeight, weight, display)` so a step is derived from the scale's
  (`ToonTypeScale.Body.copy(weight = 500)`). Both are generated, so `make tokens` keeps writing them.
  The faces are variable: Figtree draws 300–900 and Fredoka 300–700, and the backing faces map a
  weight as they map the scale's (B-35, B-82).
- Rejected: adding steps to `ToonTypeScale`. The scale is the Core page's five; a desktop line at
  15 / 500 is that design's, not the kit's scale.
- Not covered: tokens for the desktop set's text sizes. Each desktop component keeps its own numbers
  from its `.dc.html`.

- AC: `ToonText("Вид", ToonTypeScale.Body.copy(weight = 500))` compiles in a consumer and draws heavier
  than `Body` and lighter than `Title`'s 700 at the same size, under a test; the ABI dump records the
  constructor and `copy`.
- Anchors: `scripts/generate-tokens.mjs`, `toon-core/src/commonMain/kotlin/io/github/youndie/toon/tokens/ToonTokens.kt`,
  `toon-core/api/`.
