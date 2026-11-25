---
id: B-98
title: "ToonStage samples its tile on texel boundaries, so it differs on Intel under a perspective term"
status: open
priority: P1
size: M
stage: stage-4-product
blocked_by: [B-86]
---

# B-98 — ToonStage samples its tile on texel boundaries, so it differs on Intel under a perspective term

Issue [#71](https://github.com/youndie/toon-ui/issues/71), a follow-up to #35 from pyatiletka on toon-core `0.1.0.43`.
[B-86](B-86-stage-same-on-every-x86.md) added `ToonTheme(stageTexture = false)`, which hides the texture rather than fixing
its sampling: at density 1 each pixel centre still samples the 448 px tile exactly on a texel boundary, and under viddik's
capture matrix (perspective term `1e-9`) Intel Xeon and AMD EPYC runners resolve the tie differently, so a consumer's
goldens of a textured theme fail on part of the runners. `ToonStageSamplingTest` draws without the perspective term, so it
cannot see it.

- **The decision** is the kit's. The issue's candidates: bake the tile at the target density once and draw it 1:1 (which
  also stops nearest sampling from dropping three texels in four), or offset the shader to texel middles; either moves the
  kit's own stage goldens once, which is the point of the fix.
- **Why before the release.** A consumer's goldens would depend on its runner's CPU from the first Central version on.

- AC: one stage frame under a `1e-9` perspective term hashes the same on an Intel and an AMD runner (the reproducer in
  #71); the kit's test reaches the mechanism (it fails with the old sampling); the stage goldens re-recorded and looked at;
  #71 is closed by the pull request.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/theme/ToonStage.kt`
