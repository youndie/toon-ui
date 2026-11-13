# Backlog: toon-ui

> Role of this document: the product backlog. **One file per item in
> [`docs/backlog/`](docs/backlog/)**, named `B-NN-<slug>.md`. What lives here is the index (generated)
> and everything that is not an item: the goal, the stages, and the order of work.
>
> New item: copy [`docs/templates/backlog-item.md`](docs/templates/backlog-item.md), take the next
> free `B-NN`, and run `make fix` after editing.

## Goal

Ship the toon-ui design — the 27 components (18 MVP, ToonIcon and 8 v1), 3 screen transitions, 25 icons, 8 FX and 3 themes the designer built in
Claude Design — as a Compose Multiplatform library whose every shape and every animation
**provably** matches the design. Numbers are checked against golden JSON emitted by the design's own
engine, and pixels against references it renders in Chrome (research D3).

The design is frozen in [`reference/design-system/`](reference/design-system/) at the snapshot
research §1.1 names. It is evidence, not a document this repository maintains.

## Order of work, and why it is this order

1. **What can invalidate the architecture comes first** (stage 0): the skeleton, which fonts can
   carry Cyrillic, the harness that turns the engine into an oracle, the geometry port tested
   against it, the frame cost of rebuilding paths every frame, and the pixel references with
   parity's floor. Each changes how every component is built or verified.
2. **The materials** (stage 1): tokens generated from the engine, the theme with reduce motion and
   the idle budget, the ink drawing primitives, press feedback, icons, the FX layer, textures.
   Every component reads these. A component built before them is rebuilt after them.
3. **Components** (stage 2), the brief's MVP set. Each closes on its frame strips' numbers and its
   cells' pixels.
4. **The demo screens** (stage 3): sign in, the list, settings, day and night, rebuilt from the
   components. This checks that the components compose.
5. **The library as a product** (stage 4): accessibility, the catalogue, publishing, the
   undesigned v1 components, re-importing the design.
6. **The desktop set** (stage 5): nine components and one pointer rule a desktop app needs, designed
   in a desktop app's project and imported from it ([B-68](docs/backlog/B-68-vendor-the-desktop-design.md)).
   The rule comes before the components that hover, and the turn after the sticker it carries.

The owner answered on 2026-11-02: Cyrillic is in scope ([B-35](docs/backlog/B-35-cyrillic-fonts.md)
finds the faces), the destructive button's 4.38:1 is an accepted exception
([B-24](docs/backlog/B-24-dialog.md)), the licence is MIT, and the frame-cost device is a Pixel 6a
([B-05](docs/backlog/B-05-frame-cost-measurement.md)). The v1 components are designed in Claude Design first and arrive by re-import
([B-33](docs/backlog/B-33-v1-components-need-design.md)). Still open: when the repository goes
public ([B-32](docs/backlog/B-32-public-repository-and-publishing.md)).

## Stages

| Stage | What closes it |
|---|---|
| `stage-0-risks` | Goldens and references regenerate deterministically; the geometry matches within a measured tolerance; H1 has a measured verdict |
| `stage-1-materials` | The Divider, a plain body, the icons and the FX match their references; tokens are generated |
| `stage-2-components` | Each MVP component matches its strips and cells in three themes |
| `stage-3-screens` | Nine phone fixtures and the desktop one within the floor; a sample app runs |
| `stage-4-product` | The owner's questions answered; the semantics and contrast tests pass |
| `stage-5-desktop` | The nine desktop components match their renderers in three themes; hover, focus and targets follow one rule |

<!-- BEGIN INDEX -->

## Open (8)

| Task | | Priority | Size | Blocked by |
|---|---|---|---|---|
| [B-72](docs/backlog/B-72-scrollbar.md) `[~]` | ToonScrollbar: an ink thumb that keeps clear of the window's edge | P1 | M | B-68, B-69 |
| [B-74](docs/backlog/B-74-level-meter.md) `[ ]` | ToonLevelMeter: a channel's level, live, silent or clipping | P1 | M | B-68 |
| [B-75](docs/backlog/B-75-record-button.md) `[ ]` | ToonRecordButton: start and stop, with the elapsed time | P1 | M | B-68, B-69 |
| [B-78](docs/backlog/B-78-transcript-turn.md) `[ ]` | ToonTranscriptTurn: one turn of a transcript, calm to read | P1 | L | B-68, B-69, B-70 |
| [B-73](docs/backlog/B-73-split-handle.md) `[ ]` | ToonSplitHandle: the grip between a sidebar and the content | P2 | S | B-68, B-69 |
| [B-76](docs/backlog/B-76-title-bar.md) `[ ]` | ToonTitleBar: the window's title row and its action slot | P2 | M | B-68, B-75 |
| [B-77](docs/backlog/B-77-sidebar.md) `[ ]` | ToonSidebar: a persistent side panel with search, groups and navigation | P2 | L | B-68, B-69, B-71, B-72 |
| [B-79](docs/backlog/B-79-pointer-rule-on-the-undrawn-components.md) `[ ]` | The pointer rule on the pressable components the pointer page does not draw | P2 | M | B-69 |

## Closed (71)

**Goldens and references regenerate deterministically; the geometry matches within a measured tolerance; H1 has a measured verdict**

- [B-01](docs/backlog/B-01-repository-skeleton.md) `[x]` - Repository skeleton: sborka conventions, toon-core on four targets, both gates
- [B-02](docs/backlog/B-02-fonts-and-cyrillic.md) `[x]` - Fonts: bundle the four OFL families the design names
- [B-03](docs/backlog/B-03-engine-golden-harness.md) `[x]` - Engine harness: run toon-engine.js in Node and emit golden JSON
- [B-04](docs/backlog/B-04-geometry-port-and-numeric-parity.md) `[x]` - Geometry port: Mulberry32, rr, wob, shape, squash, inkLine, with numeric parity
- [B-05](docs/backlog/B-05-frame-cost-measurement.md) `[x]` - Measure the frame cost of per-frame geometry before components depend on it
- [B-06](docs/backlog/B-06-pixel-references-and-floor.md) `[x]` - Pixel references: render every state in headless Chrome, measure parity's floor

**The Divider, a plain body, the icons and the FX match their references; tokens are generated**

- [B-07](docs/backlog/B-07-token-generator.md) `[x]` - Tokens generated from THEMES: colours, shape, type, motion, three themes
- [B-08](docs/backlog/B-08-theme-reduce-motion-idle-budget.md) `[x]` - ToonTheme provider, system reduce motion, and a counted idle budget
- [B-09](docs/backlog/B-09-ink-drawing-primitives.md) `[x]` - Ink drawing primitives: body with hard shadow and night rim, focus ring, placement
- [B-10](docs/backlog/B-10-press-feedback.md) `[x]` - Press feedback: squash on press, spring from where it got to, action on release
- [B-11](docs/backlog/B-11-icon-set.md) `[x]` - Icons: 25 centerlines, variable-width outline, Regular and Bold, ToonIcon
- [B-12](docs/backlog/B-12-fx-layer-and-events.md) `[x]` - FX layer: eight effects and the named events that fire them
- [B-13](docs/backlog/B-13-textures.md) `[x]` - Textures: get the four tiles, tile them as an ImageShader
- [B-35](docs/backlog/B-35-cyrillic-fonts.md) `[x]` - Cyrillic: find OFL faces that carry Russian in each of the four roles
- [B-45](docs/backlog/B-45-card-cascade-over-budget.md) `[x]` - The card cascade with dust misses 60 fps on a Pixel 6a: what to simplify
- [B-59](docs/backlog/B-59-seamless-textures.md) `[x]` - The stage tiles are seamless

**Each MVP component matches its strips and cells in three themes**

- [B-14](docs/backlog/B-14-button.md) `[x]` - Button: filled, outlined, text; loading blob, idle breath, disabled buzz
- [B-15](docs/backlog/B-15-icon-button-and-badge.md) `[x]` - IconButton and Badge
- [B-16](docs/backlog/B-16-fab.md) `[x]` - FAB: regular and extended, lands with dust
- [B-17](docs/backlog/B-17-checkbox-and-radio.md) `[x]` - Checkbox and RadioGroup
- [B-18](docs/backlog/B-18-switch.md) `[x]` - Switch: wind-up, smear, slap, colour wave
- [B-19](docs/backlog/B-19-text-field.md) `[x]` - TextField: drawn frame over a real input, label lift, error shake with stars
- [B-20](docs/backlog/B-20-chip.md) `[x]` - Chip: filter, assist, input; inflate and paint-in; a 48 dp target
- [B-21](docs/backlog/B-21-card.md) `[x]` - Card: tilt, drop-in with dust and cascade, press lean, swipe dismiss with a hole
- [B-22](docs/backlog/B-22-list-item-and-divider.md) `[x]` - ListItem and Divider
- [B-23](docs/backlog/B-23-snackbar.md) `[x]` - Snackbar: flies up on its rope, swings, anticipates down
- [B-24](docs/backlog/B-24-dialog.md) `[x]` - Dialog: iris spotlight in, pop and stars out
- [B-25](docs/backlog/B-25-progress.md) `[x]` - Progress: the inchworm and the spinning blob
- [B-26](docs/backlog/B-26-top-app-bar.md) `[x]` - TopAppBar: a sign on two nails that sways
- [B-27](docs/backlog/B-27-navigation-bar.md) `[x]` - NavigationBar: jumping icon, dust, paint-stroke indicator
- [B-28](docs/backlog/B-28-tabs.md) `[x]` - Tabs: bookmarks on an ink shelf, the indicator smears across
- [B-36](docs/backlog/B-36-slider.md) `[x]` - Slider: a ball on a rope that squashes when grabbed
- [B-37](docs/backlog/B-37-menu.md) `[x]` - Menu: a paper scroll that unrolls under its trigger
- [B-38](docs/backlog/B-38-bottom-sheet.md) `[x]` - BottomSheet: a sheet with a tab that slides up with overshoot
- [B-39](docs/backlog/B-39-navigation-drawer.md) `[x]` - NavigationDrawer: a door on three hinges that swings and shivers
- [B-40](docs/backlog/B-40-tooltip.md) `[x]` - Tooltip: a speech balloon that inflates from its tail
- [B-41](docs/backlog/B-41-segmented.md) `[x]` - Segmented button: a shared outline and a paint-drop selection
- [B-42](docs/backlog/B-42-pull-to-refresh.md) `[x]` - Pull-to-refresh: a rubber band that flings the spinner into place
- [B-43](docs/backlog/B-43-title-card.md) `[x]` - Title card: splash and empty state, rings and a plaque
- [B-44](docs/backlog/B-44-screen-transitions.md) `[x]` - Screen transitions: push with smear, iris scene change, hole in the wall
- [B-46](docs/backlog/B-46-card-list.md) `[x]` - Card list: the 40 ms cascade and the close-up after a dismiss
- [B-47](docs/backlog/B-47-density-scaling.md) `[x]` - Draw every component in dp: scale the canvas by the screen density
- [B-48](docs/backlog/B-48-fx-host-event-points.md) `[x]` - FX host: draw each effect where its event puts it
- [B-53](docs/backlog/B-53-press-sticks-on-fast-clicks.md) `[x]` - A press can stick for good under fast clicks
- [B-54](docs/backlog/B-54-chip-widens-smoothly.md) `[x]` - A chip makes room for its tick smoothly
- [B-55](docs/backlog/B-55-title-card-rings-ease.md) `[x]` - Title card rings ease into place, and can loop
- [B-56](docs/backlog/B-56-top-bar-title-swings.md) `[x]` - The top app bar's title swings with its sign
- [B-57](docs/backlog/B-57-transition-keeps-screens.md) `[x]` - A transition keeps both screens' state, and the new screen shows through the hole
- [B-58](docs/backlog/B-58-slider-rope-moves.md) `[x]` - Both halves of the slider's rope move with a drag
- [B-60](docs/backlog/B-60-tooltip-tail-joint.md) `[x]` - No ink seam where the tooltip's tail meets the balloon
- [B-61](docs/backlog/B-61-menu-roll-stripes.md) `[x]` - No stripe across the menu's rolls
- [B-62](docs/backlog/B-62-kit-circular-progress.md) `[x]` - The kit shows a circular progress filling up
- [B-63](docs/backlog/B-63-text-field-label-flies-smoothly.md) `[x]` - The text field's label flies out and back without a jump
- [B-64](docs/backlog/B-64-pull-to-refresh-holds-content.md) `[x]` - Pull-to-refresh pulls the content, not a band by its tip
- [B-65](docs/backlog/B-65-list-item-check-animates.md) `[x]` - A list item's check plays the checkbox's toggle
- [B-66](docs/backlog/B-66-list-dismiss-flies-on.md) `[x]` - A card thrown from a list flies on from the finger and leaves no hole
- [B-67](docs/backlog/B-67-swipe-springs-back-then-flies.md) `[x]` - A swiped card springs back past its slot, then flies; its look and words follow

**Nine phone fixtures and the desktop one within the floor; a sample app runs**

- [B-29](docs/backlog/B-29-demo-screens-and-sample.md) `[x]` - Demo screens: sign in, the list, settings, day and night, and a sample app

**The owner's questions answered; the semantics and contrast tests pass**

- [B-30](docs/backlog/B-30-accessibility.md) `[x]` - Accessibility: semantics, state descriptions, contrast, targets
- [B-31](docs/backlog/B-31-component-catalogue.md) `[x]` - Component catalogue: which composable is which design file, generated
- [B-32](docs/backlog/B-32-public-repository-and-publishing.md) `[x]` - Public repository and publishing
- [B-33](docs/backlog/B-33-v1-components-need-design.md) `[x]` - The brief's v1 components have no design yet
- [B-34](docs/backlog/B-34-re-import-the-design-project.md) `[-]` - Re-import the design project as a diff, never an overwrite
- [B-49](docs/backlog/B-49-top-bar-action-spacing.md) `[x]` - TopAppBar: with three or more actions the middle ones are 40 dp targets
- [B-50](docs/backlog/B-50-menu-middle-item-targets.md) `[x]` - Menu: with three or more items the middle ones are 44 dp targets
- [B-51](docs/backlog/B-51-two-effect-clocks-first-composition.md) `[x]` - Check the two-effect clocks under a first composition
- [B-52](docs/backlog/B-52-sample-theme-reaches-the-screens.md) `[x]` - The kit's theme and reduce motion reach the Screens app

**The nine desktop components match their renderers in three themes; hover, focus and targets follow one rule**

- [B-68](docs/backlog/B-68-vendor-the-desktop-design.md) `[x]` - Vendor the desktop components' design from the app project they were drawn in
- [B-69](docs/backlog/B-69-pointer-and-keyboard-rule.md) `[x]` - One hover, focus and target rule for the whole kit
- [B-70](docs/backlog/B-70-speaker-sticker.md) `[x]` - ToonSpeakerSticker: a speaker told apart by shape, colour and letter
- [B-71](docs/backlog/B-71-search-field.md) `[x]` - ToonSearchField: a search box with a count and a key hint

<!-- END INDEX -->
