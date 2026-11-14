---
id: B-92
title: "Scrolled content fades at the edges it has scrolled past"
status: done
priority: P2
size: S
stage: stage-4-product
epic: feature-components
---

# B-92 — Scrolled content fades at the edges it has scrolled past

A column or a list that scrolls under a screen's header is cut hard at the header's line, and at the
window's bottom: a row half out of view is sliced through its ink. The design draws no scrolled area
longer than its frame, so it says nothing about the cut, and the kit has nothing for it. A consumer
building a settings list on the kit found the cut on its first live run and wrote a fade of its own:
an offscreen `graphicsLayer` with two `DstIn` gradients, top and bottom, each drawn while the content
can scroll that way. Every Toon screen that scrolls needs the same, so the kit should own it.

What the consumer learned using it:

- The offscreen layer clips to its box, so a switch's wobbly ink or a button's hard shadow at the
  column's side is cut; it had to widen the layer by 12 dp each side and pad the content back in.
- It goes before `verticalScroll(…)`, and the content's own padding after it, so the first and last
  rows sit clear of the fade at rest.
- The fade appears whole on the first pixel of scroll, since it is drawn by whether the content can
  scroll, not by how far it has.

- **The decision and its reason.** A public `Modifier.toonFadingEdges(state, length, slack = 0.dp)`,
  for a `ScrollState` and for a `LazyListState`, as `ToonScrollbar` takes both (B-72). A modifier, not
  a container, because the fade belongs to whatever box scrolls, and a consumer already has that box.
  - **The fade grows with the distance scrolled**, up to `length`: the top edge is as faded as the
    content has moved past it, so it comes in over the first `length` of scroll and never pops. The
    kit's motion is continuous everywhere else (D7), and a fade that jumps on the first pixel is a jump.
  - **One draw node with a layer of its own** (`saveLayer` over the box widened by `slack` at each
    side), instead of `graphicsLayer(Offscreen)`. The layer is where the content may draw; widening
    it is the slack, and the content needs no padding back in. With nothing to fade (the content
    fits) no layer is made at all.
  - Named `toon…` as the kit's other public modifier, `toonPressable` (D1), in the `draw` package.
- Rejected: a version taking any `ScrollableState`. `canScrollBackward` says whether, not how far, so
  it can only draw the fade that pops; a grid or a pager is added when one is asked for.
- Rejected: a default `length`. The design has no fade to take one from, and a number made up here
  would be a literal size in the kit (D4).
- Not covered: horizontal scrolling, a `reverseScrolling` column (the lazy list's `reverseLayout` is
  read), and reduce motion, which does not apply: the fade is static, a function of where the content
  stands.

- AC: a column scrolled by a few pixels is faded at the top by that share of `length`, under a test;
  at rest it is faded only at the bottom, at the end only at the top, and not at all while it fits.
  The same for a lazy list, `reverseLayout` included. Ink overflowing the box by less than `slack`
  is drawn, and faded with the rest. Over each theme's paper the faded pixels are the content's laid
  over the stage by the mask's alpha, held by goldens in the three themes.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/draw/ToonFadingEdges.kt`,
  `scripts/component-catalogue.mjs`, `sample/src/commonMain/kotlin/io/github/youndie/toon/sample/ToonUiSample.kt`.

## Findings, 2026-11-14

- **The API.** `Modifier.toonFadingEdges(state: ScrollState, length: Dp, slack: Dp = 0.dp)` and the
  same for a `LazyListState`, in `io.github.youndie.toon.draw`. One `ModifierNodeElement`, equal by its
  state, length and slack, over a `DrawModifierNode`: it reads the state while it draws, so a scroll
  redraws the box and recomposes nothing.
- **The fade.** Each band is `length` deep, at most half the box, so two bands meet in the middle and
  never cross. Its strength is the distance the content stands past that edge over `length`, held to
  0–1: a gradient from `1 - strength` at the edge to whole at the band's far end, drawn `DstIn` over
  the content's layer. A column reads `value` and `maxValue - value`; a lazy list reads the first
  item's scroll offset while it is the first visible, and the last item's end against the viewport's
  end and the padding after it while it is the last, and is whole past either. `reverseLayout` swaps
  the two.
- **The layer.** A `saveLayer` over the box widened by `slack` at each side, and the bands as wide. It
  is made only while a band has a strength: a column that fits draws its content straight, pixel for
  pixel as without the modifier.
- **Over the paper.** The fade is the content laid over the stage by the mask's alpha: over the bottom
  band of a list of rows on each theme's stage, every channel of every pixel is within 2 / 255 of
  `a · content + (1 − a) · stage`, except one antialiased pixel at a switch's edge in Bright Cel, at
  3.75 / 255. Three 8-bit roundings stand between the two (the layer's premultiplied store, the
  `DstIn` product, the composite), and a partly transparent pixel gathers them.
- **Held** by `ToonFadingEdgesTest` (10 tests, a red column over white, so a pixel's green reads the
  mask): at rest only the bottom is faded, half way through its band at half; scrolled 5, 10, 20 and
  30 dp the top row is faded by that share of the length, at densities 1 and 2; swept a pixel at a time
  over 60 px the top fade never grows by more than a pixel's share; at the end only the top, and 7 px
  before it the bottom by 7 px of the length; a column that fits, and one of length 0, are drawn as
  they are; a length past half the box meets in the middle; no box from 0 to 300 dp throws; the slack
  keeps the ink 10 dp past both sides, fades it, and leaves the box where it was; a lazy list at rest, a
  quarter in and at the end, and its distances with content padding before and after and reversed;
  and the paper check above, in the three themes.
- **13 mutants, each killed**: the bottom band's gradient reversed; the fade whole on the first pixel;
  the fade not held to half the box; a column's edges swapped; a column's end a pixel short; the layer
  without the slack; the bands not widened by it (survived the first run, which read the start side
  only: the band still covered it; the test now reads both); a lazy list's first item, its padding
  after and its reversed layout not read; `DstOut` for `DstIn`; no layer; the length read in pixels.
- **Goldens**, group `FadingEdges`: a settings column on each theme's stage at rest, in the middle and at
  the end (9), with 12 dp of slack, so the switches' ink at the column's end is whole and faded with
  the rest. No Chrome references: the design draws no scrolled column.
- **The sample.** The kit page's column fades under the header, and a "Fading edges" plate shows a
  lazy list of rows with switches, 12 dp of slack and 20 dp of content padding.
- **The catalogue** lists public modifiers now, `toonPressable` (B-10) among them, and `make check` fails
  on a public modifier it does not list.

- [x] A column scrolled by a share of `length` is faded at the top by that share; at rest only at the
  bottom, at the end only at the top, not at all while it fits. The same for a lazy list, reversed
  included.
- [x] Ink past the box by less than `slack` is drawn and faded.
- [x] Over each theme's paper the faded pixels are the content over the stage by the mask's alpha;
  goldens in the three themes.
