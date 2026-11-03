---
id: B-13
title: "Textures: get the four tiles, tile them as an ImageShader"
status: done
priority: P2
size: S
stage: stage-1-materials
---

# B-13 — Textures: get the four tiles, tile them as an ImageShader

The four 512 px tiles (`paper` and `gouache`, light and night) did not import through DesignSync,
which caps a file at 256 KiB. The owner supplied them on 2026-11-02, and they are vendored under
`reference/design-system/assets/textures/`. They are not the project's bytes: pixels differ by up to
22 levels (research §1.1, H6).

- **The decision and its reason.** The vendored tiles are the ones both sides use: the library
  bundles them through compose-resources, and B-06 renders the references with the same files. They
  are drawn with `ImageShader(…, TileMode.Repeated)` at 256 dp as the stage background (the design
  shows them at 2×).
- A test checks the wrap. The edge-to-edge difference must stay within the inner neighbour
  difference times a factor measured once here. The gouache tiles' vertical wrap is twice the inner
  difference (2.29 against about 1.2 levels), so it is looked at in a 2×2 tiled render, not only
  measured.
- Rejected: re-fetching the project's own bytes before going on. Nothing visible depends on 22
  levels of texture, and parity is unaffected while both sides share the files.
- Not covered: grain or vignette (the brief's optional atmosphere); the design has neither.

- AC: the Screens fixtures (B-29) draw on the gouache tile in both themes.
- Anchors: `reference/design-system/assets/textures`, `reference/design-system/toon-ui Screens.dc.html`.

## Findings (2026-11-02)

- **Done as decided.** The four tiles are bundled byte for byte under compose-resources'
  `drawable`; `ToonStage` paints the stage colour and, in both Ink & Gouache themes, the gouache tile
  through `ImageShader(…, TileMode.Repeated)` at 256 dp, as the Screens page's CSS does. Bright Cel's
  stage is flat, as the page's `tex: 'data:,'` says.
- **The wrap**, measured once and held by `ToonStageSeamTest`: horizontal wraps 1.21–1.38 levels
  against 1.18–1.37 inside; paper's vertical 1.36–1.39 against 1.34–1.45; gouache's vertical 2.29
  against 1.20–1.21, 1.91 times. In the 2 × 2 golden (`Stage_Tiled_*`) that seam is a faint lighter
  line at 8× zoom, not visible at 1:1. It is in the tile, so Chrome draws it alike; the tile is not
  edited (CLAUDE.md), and a cleaner one is the owner's to give.
- **Tests:** `ToonStageTest` finds the tile repeating every 256 dp both ways at densities 1 and 2,
  and not at half that; Bright Cel's stage is its one colour. Every mutant in the stage's numbers
  fails it.
- **AC carried to B-29:** the Screens fixtures do not exist yet; B-29 now names `ToonStage` as what
  they stand on.
- **Superseded by B-59 (2026-11-03):** the bundled tiles are now made seamless from these by
  `make tiles`, 448 px, every 224 dp; the vendored ones are unchanged.
