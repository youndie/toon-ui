---
id: B-59
title: "The stage tiles are seamless"
status: done
priority: P2
size: S
stage: stage-1-materials
---

# B-59 — The stage tiles are seamless

The owner, in the running kit (2026-11-03): "all the textures need re-rendering — their joins
show". B-13 had measured it and left it: the design's gouache tiles' last row stands 1.91 times the
inner difference from the first, a faint line across the stage every 256 dp, and the tile was not
to be edited while the design was the source. Since D13 the code is.

- **The decision and its reason.** The bundled tiles are made from the design's by `make tiles`
  (`scripts/seamless-tiles.java`), which folds the last 64 columns onto the first and then the last
  64 rows onto the first, **cutting** rather than blending (image quilting): across the overlap no
  pixel is mixed, a path that moves at most one pixel a line divides the folded end from the tile's
  own start where the two agree best, and its ends meet, so the other axis still wraps. The tiles
  come out 448 px, keep their five to nine posterised colours and their grain, and show each part of
  the design's tile once; the stage draws them at 2 px a dp, as the design draws its own, so it
  repeats every 224 dp. The design's tiles stay vendored and unedited.
- **Rejected, each tried and measured:** a blend with the tile rolled by half — the wrap is clean,
  but the tile's middle appears twice, a repeat at half the period that `ToonStageTest` caught; a
  cross-fade of the overlap, variance-kept and then put back on the tile's palette — it leaves a
  line where the two weights are even, plain in a 2 × 2 view of gouache light.
- AC: no column or row of any tile, the wraps included, stands out from the median of its
  neighbours by more than the design's own inner lines do; every tile's colours are the design
  tile's; the stage repeats at the tile's size and not at half of it.
- Anchors: `scripts/seamless-tiles.java`, `toon-core/src/commonMain/composeResources/drawable`,
  `toon-core/src/commonMain/kotlin/io/github/youndie/toon/theme/ToonStage.kt`.

## Findings (2026-11-03)

- Mean neighbour difference, the worst line against the median, wrap included:

  | Tile | Design, columns / rows | Seamless, columns / rows |
  |---|---|---|
  | gouache light | 1.30 / **1.90** (row 511) | 1.29 / 1.38 |
  | gouache night | 1.40 / **1.93** (row 511) | 1.41 / 1.41 |
  | paper light | 1.35 / 1.33 | 1.38 / 1.33 |
  | paper night | 1.29 / 1.28 | 1.30 / 1.23 |

  The worst lines of the seamless tiles are the design tile's own, inside it (columns 326, 377, 441,
  73); none is at a wrap or in the cut.
- `ToonStageSeamTest` holds the table at 1.6 and that each tile's colours are the design tile's;
  `ToonStageTest` finds the stage repeating every 224 dp at densities 1 and 2 and not at half that.
  Mutant: the design's tiles bundled again — row 511 at 2.29 against the median, and the period
  test fails.
- **Goldens.** Re-recorded and looked at: `Stage_*` (now 2 × 2 tiles of 224 dp, so 448 × 448; the
  Bright Cel cell only changed size) and the seven `Screens_*` cells on the gouache stage. No line
  shows in the 2 × 2 tile at 1:1. `InkParityTest` is unchanged: the Chrome references draw the
  stage flat, and the texture is below the ink threshold, so `ToonDepartures` has nothing to name.
- The paper tiles are healed too, though nothing draws them yet.
