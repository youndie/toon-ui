---
id: B-89
title: "ToonPanel: the paper panel a desktop window lays each content pane on"
status: done
priority: P1
size: M
stage: stage-5-desktop
epic: feature-components
blocked_by: [B-68, B-77]
---

# B-89 — ToonPanel: the paper panel a desktop window lays each content pane on

The desktop design lays every content pane of a window on a paper panel: the extension's
`sheet(T, w, h, o)` (`reference/desktop/desktop-engine.js`), a paper body with the hand-drawn ink outline
and a shadow one step deeper than the theme's. The app's screens that project draws (its empty state,
and the views beside the sidebar) each call it as `E.sheet(T, w, h, { seed })`, the renderer's own
defaults otherwise; those screens are not vendored (research §1.8). toon-core has no public composable
for it. `ToonSidebar` draws the same renderer with options of its own, but only inside
(`toonSidebarPanelScene` is `internal`), so a consumer can lay out the sidebar and not the pane beside
it. A window built from the kit's title bar, sidebar and split handle shows the stage where its content
should stand.

- **The decision and its reason.** A public `ToonPanel(modifier, seed = 0, content)`: the extension's
  `sheet` at its defaults, as big as the layout makes it, with the content over it in the panel's own
  box. The screens position their content inside the sheet themselves (their paddings differ per
  view), so the panel adds none. The shadow and the wobble reach past the box, as the design's SVG
  overflows its own; the panel takes no input and has no semantics, a surface rather than a control.
- **One scene.** The sidebar's panel is the same renderer with its own radii, the theme's shadow and
  seed 1, plus its rule; it calls the panel's scene with those options instead of keeping a copy.
- Rejected: a `contentPadding` defaulting to the extension's `layout.sheetPadX` / `sheetPadTop`. Each
  view of the design pads its pane differently (the list of one view starts 80 dp down, another's
  header 22), so a default would be right for one of them and a second number to undo for the rest.
- Rejected: exposing the renderer's radii, fill and shadow options. The screens call it with the
  defaults; the sidebar's options stay the sidebar's. A popover or a card-like sheet is a component of
  its own when one is needed.
- Not covered: a pane's own content (headers, rules, lists). The rules the views draw over the sheet
  are `inkLine`s placed by each view.

- AC: `ToonPanel` draws the extension's `sheet` in the three themes, held layer by layer to the
  engine's frames at several sizes and seeds, and in pixels to Chrome's rendering of the renderer and
  to goldens. The sidebar's frames do not move. Content fills the panel's box; no size throws.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonPanel.kt`,
  `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonSidebar.kt`,
  `scripts/desktop-cells.mjs`, `scripts/design-references.mjs`, `scripts/component-catalogue.mjs`.

## Findings, 2026-11-14

- **The port.** `toonPanelScene(tokens, w, h, seed, radii, shadow, fill)` is the extension's `sheet`
  whole: `body` at the origin with the radii scaled by the theme's `rs`, the wobble seeded at the
  theme's seed plus 3800 plus `seed`, paper, and a shadow of the theme's plus 2, which is the kit's
  `shadow/l` (`ToonStroke.SHADOW_L_EXTRA`, unused until now). `ToonPanel` builds it in a
  `drawWithCache` behind a `Box`, so it is rebuilt only when the size, the theme or the seed changes,
  and draws it in dp at any density.
- **The sidebar shares it.** `toonSidebarPanelScene` is now `toonPanelScene` with the sidebar's radii
  (18, 12, 22, 14), the theme's shadow and seed 1, plus its rule; its 72 frames did not move.
- **The API**: `ToonPanel(modifier, seed = 0, content: BoxScope.() -> Unit)`.
  - Sized as a `Box` is: by the modifier, or by the content where the modifier leaves it free; under an
    unbounded width it is its content's width, and no size throws (0 × 0 to 3000 dp, swept).
  - The content has the panel's whole box from its top start corner. No padding, no clip, no input,
    no semantics: a click on the panel reaches what is under it.
  - The shadow and the wobble reach past the box at the end and the bottom (6 dp in Ink & Gouache, 8
    in Bright Cel), as the design's SVG overflows its own; the KDoc says to leave the room.
- **Held**:
  - **Frames**: group `Panel` in `desktop.json`, four sizes and seeds (940 × 716 seed 2, 640 × 480
    seed 0, 320 × 180 seed 7, 420 × 1100 seed 11) in three themes, 12 cells, worst coordinate
    0.050 dp (`ToonPanelTest`). Every other group of `desktop.json` is byte-identical.
  - **Pixels**: six Chrome references of the renderer itself (320 × 200 seed 0, 480 × 160 seed 5, three
    themes), drawn as the B-09 body is, in a page that loads the extension, and six goldens of the
    composable on the stage (`PanelParityFixtures`). Ink parity 0.11–0.37 % against the shapes floor of
    14.81 %. That measure is diluted by the paper: a panel's inked pixels are mostly fill. Counted over
    the outline and its shadow alone, 1.26–3.78 %, held under the same floor by `ToonPanelTest`.
  - **The composable is its scene**: the same pixels as the scene drawn straight on a canvas, in three
    themes, at two sizes and seeds and at density 2; and again after the seed and the theme change.
  - Looked at: the goldens beside their references and a 4× diff (anti-aliasing along the outline
    only), and a 1280 × 800 window of the title bar, the sidebar, the split handle and a panel in the
    three themes.
- **References under a newer Chrome.** The committed references were rendered by Chrome 156; this
  machine has 157, which renders 1,555 of them byte for byte and one, `Screens_Settings_BrightCel`,
  differently. `scripts/design-references.mjs --only <group>` renders the named groups alone and records
  their browser per entry in the manifest, so the panel's six were added without rewriting the rest.
- **Not drawn here**: the rules and headers the design's views lay over their panels. A view composes
  them from the kit's parts over a `ToonPanel`.

- [x] `ToonPanel` draws the extension's `sheet` in the three themes, held layer by layer to the
  engine's frames at four sizes and seeds, and in pixels to Chrome's rendering and to goldens.
- [x] The sidebar's frames do not move.
- [x] Content fills the panel's box; no size throws.
