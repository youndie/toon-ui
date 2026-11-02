---
id: B-13
title: "Textures: get the four tiles, tile them as an ImageShader"
status: open
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
