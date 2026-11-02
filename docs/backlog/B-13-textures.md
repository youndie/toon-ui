---
id: B-13
title: "Textures: get the four tiles, tile them as an ImageShader"
status: open
priority: P2
size: S
stage: stage-1-materials
---

# B-13 — Textures: get the four tiles, tile them as an ImageShader

The four 512 px tiles (`paper` and `gouache`, light and night) did not import: DesignSync caps a
file at 256 KiB (research §1.1, H6).

- **The decision and its reason.** The owner exports them from the project, or they are saved from
  the project opened in a signed-in browser. They are committed under the reference directory with
  the date, and drawn with `ImageShader(…, TileMode.Repeated)` at 256 dp as the stage background.
  A test checks that the tile is seamless (the edge columns match the opposite edge within a few
  levels).
- Not covered: grain or vignette (the brief's optional atmosphere); the design has neither.

- AC: the Screens fixtures (B-29) draw on the gouache tile in both themes.
- Anchors: `reference/design-system/toon-ui Screens.dc.html`.
