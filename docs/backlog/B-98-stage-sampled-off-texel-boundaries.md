---
id: B-98
title: "ToonStage samples its tile on texel boundaries, so it differs on Intel under a perspective term"
status: done
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

## Findings (2026-11-25)

- **Built: the shift.** `drawToonStage` draws the tile an eighth of a texel off the canvas's origin
  (`STAGE_TILE_SHIFT`), so a pixel's centre samples `(x + ½) / k − ⅛` and never the line between two texels. Chosen over
  baking the tile at the target density: one constant rather than a bitmap per density, and the same texel the Mac took
  before at 1 and 2 px a dp, so **no golden of the kit moved** (`ToonStageSamplingTest`'s texel-per-pixel test, unchanged,
  and `viddikVerify` both green). The bake would also stop nearest sampling from dropping three texels in four at 1 px a
  dp; that is a look, not a determinism, and stays out.
- **Why an eighth.** Over x up to 3,000 px at the densities 1 to 4 in quarters and at 2.625 and 2.75, a shift of a
  quarter or a half lands a centre on a line at some density; an eighth keeps every centre at least 0.008 texel off one,
  four orders above the perspective term's effect.
- **The tests now reach the mechanism**, on any CPU: a frame nudged a thousandth of a pixel either way must not change
  (a centre on a line goes to the texels either side of it), at 1, 2 and 2.75 px a dp; and the consumer's frame — the
  stage under a perspective term of 1e-9, as viddik captures — must equal the plain one at 1 and 2. **With the old
  sampling both fail on this Mac too**, so #71's tie is reproduced without an Intel runner.
- **Mutants**, each caught: no shift (the nudge, the capture matrix and the margin tests), a shift of a quarter (the
  margin test), the shift's translation dropped (the nudge and the capture matrix).
- `stageTexture = false` stays, as a consumer may still want a flat stage; it is no longer needed for determinism.
