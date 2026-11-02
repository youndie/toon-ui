---
id: research-architecture
title: toon-ui — architecture research
type: research
status: active
date: 2026-11-02
---

# Research: the architecture of toon-ui

toon-ui is a UI kit for Compose Multiplatform in the look of 1940s–50s theatrical cartoons. It has
a thick ink outline of variable weight, flat cel fills, a hard offset shadow, and motion that
squashes, overshoots and kicks up dust. The owner wrote a brief for a designer
(`reference/design-system/uploads/brief.md`). The designer, working in Claude Design, answered it
with a working library rather than a Figma file: a JavaScript shape-and-motion engine, a runtime,
27 component files, nine spec pages and a sketch of a Kotlin port (counts as of the second snapshot, §1.1). This repository is the Compose
Multiplatform implementation of that library. It follows the same principle as its sibling
oldge-ui: take the Material 3 inventory and redraw every piece in one aesthetic.

This document records what was verified before any Kotlin existed, and against what. It names the
decisions taken on that basis and what each one rejected, and the questions still open, each with
the backlog item that answers it. It is amended, not rewritten: when the implementation contradicts
something here, the correction goes in at that point and keeps the reason the first idea failed.

## 0. Where these facts come from, and how much each is worth

| Source | Worth | Why |
|---|---|---|
| The design project, vendored in `reference/design-system/` | **authoritative for intent and for numbers** | `toon-engine.js` calls itself the single source of truth, and every spec page draws from it. Where the spec page text and the engine disagree, the engine is what the designer actually rendered. |
| The brief, `reference/design-system/uploads/brief.md` | **authoritative for the owner's constraints** | It names the hard limits: the touch target, contrast, reduce motion, the ban on borrowing, fonts under an open licence. Where the design deviates from the brief, the deviation is recorded below (§1.3), not silently adopted. |
| The Kotlin sketch, `reference/design-system/compose/` | **evidence, not a source** | It calls itself a 1:1 port. Reading it against the engine shows it is not (§1.5), and it has never been compiled. |
| The engine run in Node, and the Google Fonts repository | **verified** | Read or executed on the day, with the command or commit given. |
| Sibling repositories (`oldge-ui`, `kvadrant-ui`, `viddik`, `sborka`) | **verified for what they say about themselves** | oldge-ui is the direct precedent, built on the same toolchain and with the same parity method. A file there is cited as `youndie/<repo>@<commit>!/<path>`. |
| Recollection of how Compose behaves | **not a source** | Anything from memory is written as a hypothesis in §3, with the item that checks it. |

## 1. Verified facts

### 1.1 What was imported, and what could not be

| Fact | Where verified |
|---|---|
| The design project is the claude.ai/design project `72369202-fef7-4ced-b990-f25e2e4d1f26`, named "Уточнение по бриву", type `PROJECT_TYPE_PROJECT` (not a Design System project), owner Pavel. It was read on 2026-11-02 through the DesignSync tool (`get_project`, `list_files`, `get_file`). | DesignSync `get_project` |
| **Two snapshots were taken on 2026-11-02.** The first (pages loading `toon-engine.js?v=8`) had 19 components and 8 spec pages. The designer then answered the gap list of §1.3 and §1.6, and the second snapshot replaced the first before anything was built on it. Pages now load `toon-engine.js?v=10` and `toon-kit.js?v=11`. | DesignSync `list_files` twice; `?v=` in every `.dc.html` |
| The current snapshot vendors `README.md`, `toon-engine.js`, `toon-kit.js`, `support.js`, the **27** `Toon*.dc.html` component files, the **9** spec pages `toon-ui *.dc.html` (the new one is `toon-ui v1 Components`), `compose/` and `uploads/brief.md`. The four `compose/**/*.kt` files were not re-fetched: the designer marked the directory frozen ("FROZEN — not maintained" in `compose/README.md`), so they are the first snapshot's. | `reference/design-system/` |
| Between the snapshots, by function: `THEMES` is unchanged. `ICONS` grew from 17 to 25 (`menu`, `settings`, `delete`, `share`, `filter`, `refresh`, `lock`, `eye`). 22 engine methods were added: `rope`, `slider`, `sliderGrab`, `menu`, `menuUnroll`, `sheet`, `sheetMove`, `drawer`, `drawerOpen`, `drawerClose`, `tooltip`, `tipInflate`, `segmented`, `segSwitch`, `pull`, `pullRelease`, `titleCard`, `splashAt`, `miniScreen`, `pushAt`, `pushScene`, `holeScene`. Changed: `dialog`, `dialogScene`, `topBar`, `navBar`, `tabs`, `listItem` (pressed, focused and disabled states), `cell` and the constructor. None was removed. Of the 19 old wrappers, only the icon lists (Chip, FAB, Icon, IconButton, ListItem) and the Chip's 48 dp box changed beyond the version strings. | a script comparing each method's source between the two engines, and the wrappers with `?v=` masked |
| **Not vendored:** the project's screenshots directory (68 of the designer's working captures, iteration debris such as `01-dbg3.jpg`), `.thumbnail`, and `uploads/cartoon-ui - бриф для дизайнера.md`. The last is the same brief under its earlier name; a copy of it is in the owner's Downloads folder. | DesignSync `list_files` |
| **The four texture tiles could not be imported through DesignSync.** `get_file` caps a file at 256 KiB, and each of `assets/textures/{paper,gouache}-{light,night}.png` came back `truncated: true` as base64 at that cap, which is the first 196,608 bytes of each PNG. | DesignSync `get_file` result, field `truncated` |
| **The owner supplied the four tiles in chat on 2026-11-02, and they are vendored under `reference/design-system/assets/textures/`.** Each is a 512 × 512 RGBA PNG with no colour-profile chunk, 104–159 kB. They were told apart by mean colour, which matches the theme's background: gouache-light `#C7BFA3` (stage `#CBC3A6`), gouache-night `#19203F` (`#1A2140`), paper-light `#F3EAD5` (paper `#F4EBD6`), paper-night `#2A355E` (`#2C3760`). | the PNGs, decoded with a stdlib-only script |
| **They are not the project's files byte for byte, nor pixel for pixel.** Decoding the truncated originals' first 239–323 rows and comparing them with the supplied tiles row by row: no row is identical, and the largest channel difference is 9 (gouache-light), 8 (gouache-night), 22 (paper-light) and 18 (paper-night) levels. Whether the project's tiles changed since the fetch, or the supplied ones went through a re-encode, is not known. | the same script over the truncated DesignSync payloads |
| The tiles wrap. The mean channel difference across the horizontal wrap (last column against the first) is 1.21–1.38 levels, against 1.04–1.25 between two inner columns. Across the vertical wrap it is 2.29 for both gouache tiles, about twice the inner 1.20–1.28, and 1.36–1.39 for paper. | the same script |
| `support.js` is not toon-ui's: its first line reads `GENERATED from dc-runtime/src/*.ts — do not edit`. It is Claude Design's runtime for `.dc.html` files (the template parser, `DCLogic`, the `sc-for`/`sc-if` directives). It is kept only so that the vendored pages still open. | `reference/design-system/support.js` line 1 |

**Consequence.** The texture tiles are the owner's copies, not the project's bytes. That costs parity
nothing as long as both sides use the same files: B-06 renders the references with the vendored tiles,
and the library ships the same ones. The vertical wrap of the gouache tiles is the one place a seam
could show, and B-13's seamless check is aimed at it.

The import is a snapshot taken from a project the owner and the designer can keep
editing. The engine file carries a cache-busting `?v=` in every page that loads it, which is the
designer's own version counter: 8 in the first snapshot, 10 in the second. A re-import is an item of its own, never an overwrite (B-34).

### 1.2 The engine is a pure function from (theme, size, state, time) to vector paths

| Fact | Where verified |
|---|---|
| `toon-engine.js` holds `THEMES` (three token sets), `ICONS` (25 centerline paths since the second snapshot; 17 before) and one class, `ToonEngine`. The class has the geometry (`rr`, `smooth`, `wob`, `shape`, `body`, `inkLine`, `star`, `burst`), one renderer per component (`btn`, `check`, `sw`, `card`, `field`, `snack`, `iconBtn`, `fab`, `radio`, `chip`, `dialog`, `linear`, `circular`, `topBar`, `navBar`, `tabs`, `listItem`, `divider`, `badge`), eight FX (`dust`, `stars`, `speed`, `flash`, `vibe`, `irisScene`, `hole`, `sweat`), and one closed-form curve per animation (`chkAnim`, `swAnim`, `fabAppear`, `radioSelect`, `chipSelect`, `dialogOpen`, `dialogClose`, `worm`, `navSelect`, `tabsSwitch`, `listPress`, `badgeAppear`, `badgeCount`, `sway`, `cardAppear`, `cardDismiss`, `snkShow`, `snkHide`, `fldFocus`). The second snapshot adds the eight v1 renderers with their curves and three screen transitions (§1.1). | `reference/design-system/toon-engine.js` |
| A renderer returns a *scene*: `{ L: [paths], X: [texts] }`. A path carries `d`, fill, stroke, a transform string and an opacity. A text carries the string, position, font family, weight, size and colour. `toSvg` turns a scene into SVG markup. Nothing in a renderer reads a clock: time enters only through a curve's output. | `toon-engine.js` (`L`, `X`, `toSvg`) |
| Every shape comes from one generator. `rr` lays points along a rounded rectangle with **four independent radii** every 6 px. `wob` pushes each point along its normal by `amp · (0.6 sin 3s + 0.3 sin 7s + 0.22 sin 13s)`, with three phases drawn from a seeded Mulberry32. `smooth` joins the points with quadratic midpoints. `shape` returns the outline and a fill inset by **different amounts per side** (`ins = [top, right, bottom, left]`), and the ink showing between them is the variable-weight line. | `toon-engine.js` (`rr`, `wob`, `shape`) |
| `body` composes a piece: the outline offset by `sh` as the hard shadow, the outline in ink, then (night only) the fill in the rim colour, then the fill shifted 1.6 px down. The pale rim of the night theme is therefore the fill drawn twice, not a stroke. | `toon-engine.js` (`body`) |
| Squash keeps the stroke weight. `btn` rebuilds the shape on squashed geometry anchored to the bottom edge (`sy = 1 − 0.15p`, `sx = 1 + 0.12p` when pressed; `1 − 0.26p`, `1 + 0.1p` on the overshoot) and grows the bottom inset by `1 + 0.3p`. This is the brief's "contour at squash" decision, implemented. | `toon-engine.js` (`btn`, `squashOf`) |
| **Run in Node with `global.window = {}`, the engine loads, and the geometry and curves work**: `wob` returns a 1,519-character path, `btn` returns a scene with 3 paths and 1 text, `toSvg` returns 5,289 characters, `spr(0.08, .35, 45)` = −0.3004. Two things degrade without a DOM. `tw` (text width) falls back to `s.length · fs · 0.55`, because it measures on a canvas. `icon` returns no paths, because `iconOutline` samples the centerline through SVG `getTotalLength` and returns `null` when `document` is undefined. The v1 renderers of the second snapshot (`slider`, `menu`, `sheet`, `drawer`, `tooltip`, `segmented`, `pull`, `titleCard`, `pushScene`, `holeScene`) run the same way and return non-empty scenes. | `node -e` runs on 2026-11-02 (Node v24.8.0) over each snapshot |

**Consequence.** The design can be checked against at two levels, and the cheaper one is the
stronger one. **Numbers:** geometry and curves are pure functions, so the engine run in Node can
emit golden JSON (path points for a size, radii and seed; curve values at the spec's timestamps),
and the Kotlin port is tested against it numerically. **Pixels:** text widths and icons need a
browser, so per-state reference PNGs are rendered in headless Chrome and parity is measured with
viddik, as oldge-ui does. A component whose numbers match and whose pixels do not has a text or
font problem, and that split makes the cause visible (D3, B-03, B-06).

### 1.3 What the design delivered, against what the brief asked

| Brief asked | Design delivers | Where verified |
|---|---|---|
| A Figma file with variables, components, motion strips. | A Claude Design project: a live JS library, `.dc.html` components with props, spec pages that draw every frame strip from the engine. There is no Figma file. | `reference/design-system/README.md`; brief §«Формат передачи» |
| Light theme plus a dark "night scene". | **Three** themes: `il` Ink & Gouache light (the base), `in` its night, and `bc` Bright Cel as a second light theme. The Directions page records the choice: "Ink & Gouache (1a) is the base … Bright Cel (1b) ships as a second light theme." | `toon-engine.js` `THEMES`; `toon-ui Directions.dc.html` |
| A cel palette of **8** saturated colours plus semantics, each with a dark and a light step. | **3** sticker colours per theme (`sColors`), 3 card fills (`cardColors`), and single semantic tokens (`primary`, `danger`, `success`, `successLight`, `warn`). There are no dark/light steps per colour; pressed and disabled use `surface2` and `disabledFill`. *Fixed by the owner on 2026-11-02* ("цвета пока фиксируем"), and recorded by the designer under "Decisions" in the design README. | `toon-engine.js` `THEMES`; `reference/design-system/README.md` |
| Outline in three weights s/m/l, about 2/3/4 dp. | Weight is expressed as **fill insets per side**, `[2, 2.6, 4, 2.2]` for Ink & Gouache and `[2.6, 3, 4.6, 2.6]` for Bright Cel. The Divider takes s/m/l = 2.4/3/4.6. | `toon-engine.js` `THEMES.*.ins`; `toon-kit.js` `C.divider` |
| 16 redrawn icons in 2 weights. | **25** since the second snapshot (17 before, `tune` the extra) as centerlines, with Regular 2.1 and Bold 2.7 computed by one width rule. | `toon-engine.js` `ICONS`; `toon-ui Icons.dc.html` rule 3 |
| 18 MVP components, then the v1 list. | All 18 MVP components plus `ToonIcon` (19 files) in the first snapshot. The second adds the whole v1 list as 8 components (`ToonSlider`, `ToonMenu`, `ToonBottomSheet`, `ToonNavDrawer`, `ToonTooltip`, `ToonSegmented`, `ToonPullToRefresh`, `ToonTitleCard` for splash and empty state) and the three screen transitions (push, scene change, hole in the wall) on the `toon-ui v1 Components` page. That makes 27 component files. | `reference/design-system/Toon*.dc.html`; `toon-ui v1 Components.dc.html` |
| Demo screens in mobile and desktop size, both themes. | Sign in, list and settings in all three themes (Bright Cel on a flat backdrop, no texture), plus one 1280 × 800 desktop layout, since the second snapshot. | `toon-ui Screens.dc.html` |
| Fonts under an open licence. | Shrikhand + Figtree (base), Titan One + Fredoka (Bright Cel), loaded from Google Fonts. | `README.md` "Visual foundations"; `Toon*.dc.html` `<link>` |

*Decided on 2026-11-02:* the v1 components are designed in Claude Design first and arrive by
re-import (B-33, B-34). The same day the designer drew them, and the second snapshot brought them
in (§1.1). Each is now a backlog item of its own (B-36…B-44).

**Consequence.** The design is narrower than the brief in the palette and wider in the themes. The
code follows the design: it is what was drawn and can be measured against. The gaps are written
into the items that meet them (B-07), so they are not lost.

### 1.4 Fonts: all four are OFL, and none of them has Cyrillic

| Fact | Where verified |
|---|---|
| Shrikhand: `license: "OFL"`, one file `Shrikhand-Regular.ttf`, subsets gujarati, latin, latin-ext. | `google/fonts@6c1dbe27dcf7d817d4b62cec033ba77fa8e2a5ec!/ofl/shrikhand/METADATA.pb` |
| Figtree: OFL, variable `Figtree[wght].ttf` and an italic, subsets latin, latin-ext. | `google/fonts@6c1dbe27dcf7d817d4b62cec033ba77fa8e2a5ec!/ofl/figtree/METADATA.pb` |
| Titan One: OFL, `TitanOne-Regular.ttf`, subsets latin, latin-ext. | `google/fonts@6c1dbe27dcf7d817d4b62cec033ba77fa8e2a5ec!/ofl/titanone/METADATA.pb` |
| Fredoka: OFL, variable `Fredoka[wdth,wght].ttf`, subsets hebrew, latin, latin-ext. | `google/fonts@6c1dbe27dcf7d817d4b62cec033ba77fa8e2a5ec!/ofl/fredoka/METADATA.pb` |

**Bundled in B-02.** The four files are in `toon-core/src/commonMain/composeResources/font`,
byte for byte as google/fonts has them at that commit (SHA-256 `8ba32f6d…` Shrikhand, `26ad3db9…`
Figtree, `563ff6de…` Titan One, `2ba02e68…` Fredoka), renamed only to fit compose-resources. Their
licences are in `toon-core/src/commonMain/composeResources/files/licences`. The `fvar` tables read:
Figtree `wght` 300–900, default 300; Fredoka `wght` 300–700, default 300, and `wdth` 75–125. Titan
One's OFL reserves the name "Titan", one more reason to ship the files unmodified rather than cut
static instances.

**Consequence.** All four can be bundled: the brief's licence condition holds. But no family covers
Cyrillic, and the owner's apps are Russian-language. A Russian label in toon-ui would be drawn in a
fallback font, which is exactly the oldge-ui finding (`youndie/oldge-ui@618cef8b923d7e0e501752240e6a727eed51e48c!/docs/research/research-architecture.md`, §1.6).
*Answered on 2026-11-02:* toon-ui supports Cyrillic, and the owner left the choice of faces to be
found in the work. B-02 bundles the four faces as designed, and B-35 finds OFL families with a
`cyrillic` subset to back each role, recording each candidate here.

### 1.5 The Kotlin sketch is not a 1:1 port

`compose/README.md` says "every function here is a 1:1 port" and that the RNG "matches the JS
bit-for-bit". Read line by line against the engine:

| Divergence | Engine | Sketch |
|---|---|---|
| **The RNG returns `Float`**, so the phases differ from the engine's in the 8th digit. Visually nothing, but "bit-for-bit" is false, and a numeric parity test has to compare within a tolerance. | `rng` returns a double | `Mulberry32.next(): Float` in `compose/toon/ui/draw/ToonGeometry.kt` |
| **The icon width rule is missing a term.** The engine multiplies by `(1 + .06 · sin(s · 1.3 + ph))`, a 6 % swell along the stroke seeded per icon. | `iconWidth` | `iconOutline` in `ToonGeometry.kt` has no such factor |
| **The filled button's night rim is the wrong colour.** The engine draws a filled button's rim in `btnHi` (`#F2A57E` at night) and an outlined one's in `rim`; the sketch's `ink()` always uses `rim` (`#8392C4`). | `btn` → `rimColor: o.v === 'filled' ? T.btnHi : T.rim` | `ink()` in `compose/toon/ui/components/ToonButton.kt` |
| **The text button is blank.** The engine draws a wavy primary underline and a `surface2` blob while pressed; the sketch draws nothing for `ToonButtonVariant.Text`. | `btn`, `o.v === 'text'` branch | `ToonButton.kt` |
| **A disabled label keeps its colour.** The engine paints a disabled label `muted` at 60 %. | `btn` (`lc`, `lOp`) | `ToonButton.kt` `BasicText` style |
| **Release always springs from fully pressed.** The kit integrates the spring from wherever the press got to. The sketch's `releaseCurve(t)` starts from 1, so a tap released inside the 80 ms press jumps to full squash before springing back. | `toon-kit.js` `Base.stepSq` | `ToonButton.kt` (`releasedAt` branch) |
| **Missing in the sketch:** focus ring, loading, idle on other components, the switch's colour wave and echo, and 16 of the 18 components. The README says so under "Still to port". | — | `compose/README.md` |
| **Package `toon.ui`**, which no Maven group of the owner's matches. | — | every `compose/**/*.kt` `package` line |

*Second snapshot:* the designer has marked `compose/` frozen ("FROZEN — not maintained. Numbers live
only in `toon-engine.js`"), which agrees with D6.

It also ends with
`private fun Modifier.fillMaxSize(): Modifier = this.then(androidx.compose.foundation.layout.fillMaxSize()…)`,
which calls `fillMaxSize` with no receiver. That it does not compile is read, not run; B-01 settles it
(H5).

The sketch also carries `stageWash`. No engine theme has it; it appears only in the Directions page.

**Consequence.** The sketch is a good map of the *pattern*: a frame clock, closed-form curves,
geometry rebuilt per frame, the touch target fixed while the drawing moves, the text never scaled.
It is a bad source of numbers. The port is written against `toon-engine.js`, and the sketch is read
for structure only (D6).

### 1.6 Two of the brief's hard limits are broken in the design

| Fact | Where verified |
|---|---|
| **Contrast.** The brief requires ≥ 4.5:1 for text on buttons, fields and cards in every theme. Computed with the WCAG 2 formula from the engine's hex values, `onDanger` on `danger` (the destructive Dialog button, `#F4EBD6` on `#C23F2C`) is **4.38:1** in Ink & Gouache light. Everything else checked passes: `onPrimary`/`primary` 4.58, 5.54 and 5.46; `text`/`paper` 13.24, 9.29 and 15.74; `muted`/`bg` 5.19, 8.05 and 8.58 (il, in, bc). | `THEMES` in `toon-engine.js`; computed 2026-11-02 |
| **Touch target.** The brief requires ≥ 48 dp even for a squashed component. In the first snapshot the kit's Chip laid out at `36` high, with its pointer handlers on that box, so the chip's hit area was 36 dp. *Fixed in the second snapshot:* `C.chip.box()` returns 48 and draws the chip 6 dp down inside it. All the other interactive components are ≥ 48 (Button 52, IconButton 48, FAB 64, Checkbox 48, Radio rows 52, Switch 48, ListItem 72, Tabs 52, NavBar 80, Card 84). | `toon-kit.js` `C.chip.box()` and the other `box()` methods |
| **Reduce motion.** The spec names a fallback per animation: `instant`, `fade 120`, `off`, `static bar at 33 %`. The kit implements one: `age()` returns `1e6` under reduce motion, so every animation jumps to its end state. `fade 120` (FAB land, Dialog open/close, FX flash, the iris as a cross-fade) was specified but not drawn. *Fixed in the second snapshot:* `Base.start(name, dur, fade)` cross-fades the old scene into the new one over 120 ms under reduce motion, for the animations the spec marks so; the rest still switch instantly. | `toon-kit.js` `Base.age`; `toon-ui MVP Components.dc.html` (`reduce:` per spec); `toon-ui FX.dc.html` |

**Consequence.** The port follows the brief where the design breaks it: chip touch target 48 with
the drawing at 36, and every reduce-motion word as specified. Since the second snapshot the design
does both itself, so the port and the design agree again. The destructive colour pair went back
to the owner, because changing a token is changing the design. *Answered on 2026-11-02:* accepted as
an exception. The token stays as designed, and the contrast test (B-30) names this pair as its one
exception (B-24).

### 1.7 What sibling projects already settled

| Fact | Where verified |
|---|---|
| oldge-ui renders per-state references from the design system's HTML in headless Chrome and measures the Compose render against them with `viddikDesignParity`. It measures parity's floor once, and keeps the goldens' gate on macOS because the goldens are a claim about the rasteriser that recorded them. | `youndie/oldge-ui@618cef8b923d7e0e501752240e6a727eed51e48c!/CLAUDE.md` ("Gates") |
| oldge-ui's build uses sborka's conventions (`io.github.youndie.sborka.settings`, `.kmp`, `.lint`), group `io.github.youndie`, packages `io.github.youndie.oldge.*`, and depends on no Material artefact. | `youndie/oldge-ui@618cef8b923d7e0e501752240e6a727eed51e48c!/settings.gradle.kts`; `youndie/oldge-ui@618cef8b923d7e0e501752240e6a727eed51e48c!/gradle.properties` |
| oldge-ui's documentation and code are in English, by an explicit owner decision that departs from the portfolio's "docs in Russian" default. kvadrant-ui did the same before it. | `youndie/oldge-ui@618cef8b923d7e0e501752240e6a727eed51e48c!/CLAUDE.md` ("Language") |

## 2. Decisions

### D1. `Toon` in every identifier, package `io.github.youndie.toon`

The design already names everything `Toon*`, and the owner's other kits do the same (`Oldge*`,
`Kvadrant*`). The package follows the group, `io.github.youndie.toon.<area>`. *Rejected:* the
sketch's `toon.ui`, a top-level namespace nobody owns.

### D2. The core depends on no Material artefact

Brief and sketch agree: only `compose.foundation`, `.ui`, `.animation` and `.runtime`. A
`toon-material-adapter` (Material theming over toon tokens) is not planned. If a consumer asks for
one, it becomes a module of its own, as in kvadrant-ui.

### D3. `toon-engine.js` is the ruler, and parity is measured twice

The vendored engine is the specification. The Kotlin port is tested against it **numerically**
(golden JSON emitted by the engine in Node: wobble points, insets, squash geometry, curve values at
the spec's frame timestamps) and **visually** (per-state PNGs rendered by the engine in headless
Chrome, measured with viddik). *Built in B-03:* `scripts/engine-goldens.mjs` runs the engine in Node and writes
`toon-core/src/desktopTest/resources/engine-goldens`. It records every outline a component's renderer
asks `wob` for (its arguments and its unrounded points), the `inkLine`, `star` and `burst` paths, and
43 curves sampled every millisecond. *Built in B-06:* `scripts/design-references.mjs` draws each reference with the engine in headless
Chrome and the bundled fonts, at device scale 1, because viddik captures at 1 dp = 1 px. The PNGs go
to `toon-core/src/desktopTest/snapshots/design`; `viddikDesignParity` measures the Compose fixtures
against them. **The floor** is measured on the Divider, whose geometry matches within 0.05 px (B-04).
Over its four states and three themes, 0.00–1.54 % of pixels differ by more than ±16 per channel:
Ink & Gouache light 0.63–1.54 %, Bright Cel 0.21–1.39 %, night 0.00 %, where ink on navy keeps every
difference under the channel slack. That is the two rasterisers and nothing else. Two mutants showed
the scale. A line 20 % thicker measured 7.80 % against its 1.54 %. A wrong seed on the short divider
measured 4.24 % against its 0.21 %, which still passes viddik's default 5 %. So a component's parity
is read against its own Divider-like floor in the commit body, not against the 5 % default, and the
default stays the report's threshold only. *Rejected:* pixels only, as in oldge-ui. There the CSS is the only
implementation and the HTML can only be photographed. Here the design is a function that can be
called, and a pixel diff of a wobbly outline cannot say whether a seed, a radius or a font moved it.

### D4. Tokens are generated from `THEMES`, never typed

A generator reads `THEMES` from the vendored engine (in Node) and writes the Kotlin token layer.
The token names are the sketch's readable ones (`stage`, `surface`, `surfacePressed`,
`primaryHighlight`), mapped from the engine's short keys (`bg`, `paper`, `surface2`, `btnHi`) in one
table. `make check` fails when the generated file is stale.

*Built in B-07, with one finding:* `THEMES` holds colours, shape numbers and font names only. The
readable names, the type scale (Display 40/44 … Label 13/16), the control sizes, the motion durations,
springs and cascade live in the Core page's token table (`colorTokens`, `typeScale`, `shapeTokens` in
`toon-ui Core.dc.html`), as page code. So `scripts/generate-tokens.mjs` takes values from THEMES by
running the engine, and the rest from that table. Every value of the second kind is held to the exact
page text it was read from, and `--check` fails if a re-import changes it. *Rejected:* the sketch's hand-typed
`ToonTheme.kt`, which already holds a token (`stageWash`) the engine does not.

### D5. 1 px = 1 dp; text in sp

The engine works in CSS px at 390-wide phone frames. One px is one dp. Text is in sp so that it
scales with the system font, and text never scales with its shape (the brief's rule and the
sketch's pattern).

### D6. The `compose/` sketch is read for its pattern, not copied

The pattern is adopted: one frame clock per animating component that stops when done, closed-form
curves of elapsed ms, geometry rebuilt when the shape changes and cached when static, the layout
box as the fixed touch target, `BasicText` offset and never scaled, FX as a separate layer fired by
named events. The code is not adopted, for the divergences in §1.5.

### D7. Curves are functions of elapsed time, not `Animatable` chains

Every animation is a closed-form function `t → pose` ported from the engine. The spec's frame
strips are values at given times (`0 ms`, `80 ms · held`, `+70 ms` …), and a pure function can be
tested at exactly those times. *Rejected:* Compose `spring()`/`Animatable`. Their integrator is not
the engine's, so the pose at a given millisecond would differ and could not be tested against the
frame strip. The one exception is the kit's press-then-release, which depends on when the finger
lifted. It stays a small state machine (press 80 ms ease-out-cubic, then a spring integrated from
the current position and velocity), tested by feeding it a timeline.

### D8. Reduce motion follows the spec's word per animation

`instant`, `fade 120`, `off` and the named static states are implemented as written. This was a
deviation from the kit while the kit made everything instant. The second snapshot's kit
cross-fades too (§1.6), so it is no longer one.

### D9. Targets: Android, iOS, desktop, wasm — the gate runs on desktop

Android and iOS lead the brief, and desktop and wasm come "if they work without extra effort". The
build declares all four, like oldge-ui. The screenshot and parity gate runs on desktop JVM on macOS
(oldge-ui §1.9's reason). Nothing visual is executed on Android, iOS or wasm by that gate, and the
gate does not claim otherwise.

### D10. English in the repository; the brief stays Russian

Documentation, code, KDoc, test names and commits are in English, as in oldge-ui and kvadrant-ui.
The vendored brief stays as it is. The demo strings come from the design (English: "Add plan",
"Pancake breakfast"), so fixtures copy them verbatim.

### D11. Geometry is `Float`, and numeric parity has a tolerance

Compose paths are `Float`. The golden JSON is compared within a tolerance, measured once in B-04
from the RNG's float error and fixed there, never raised to make a test pass.

*Measured in B-04:* over the 270 outlines in the goldens, the worst `wob` point is 6.1·10⁻⁵ px off
the engine, and the tolerance is fixed at 2·10⁻⁴ px. `inkLine`, `star` and `burst` only exist as the
engine's path strings, rounded to 0.1 px, so theirs is 0.05 px plus the same 2·10⁻⁴; the worst was
0.04987. The random generator keeps 32-bit integer arithmetic and divides only in `Double`, so its
sequence equals the engine's exactly.

*Rejected:* `Double` geometry to chase the sketch's "bit-for-bit" claim. It costs a conversion at every path operation
and buys a precision nobody can see.

### D12. MIT licence — *a deviation from the siblings*

The owner chose MIT on 2026-11-02 (`LICENSE`). oldge-ui and kvadrant-ui are Apache 2.0. Publishing
otherwise follows oldge-ui (B-32).

## 3. Risks and open questions

| # | Hypothesis or risk | Mitigation, and where it is settled |
|---|---|---|
| H1 | **Performance.** Rebuilding about 130 points per shape per frame, for 10 cards appearing at once with FX, holds the brief's 60 fps on a Pixel 6a-class phone. The sketch asserts "cheap"; nothing was measured. | Measure before building components on it: frame time on desktop and on a real Android device with the B-21 card cascade, against a cached-path variant. If it fails, cache per (size, seed, squash bucket) and simplify the animation rather than lowering the target, as the brief says (B-05). The device is a Pixel 6a (owner, 2026-11-02). |
| H1 — *verdict, B-05, 2026-11-02* | **Red for the worst case, and geometry is not the cause.** On a Pixel 6a (Android 17, 60 Hz, release build), ten cards in the 40 ms cascade with two dust puffs each give a FrameMetrics total of p50 15.0 ms and p95 18.2 ms. 23.6 % of frames run over 16.7 ms, though only 1.6 % of vsync slots go unfilled. The pre-registered bar (≤ 1 % over 16.7 ms, p95 ≤ 16.7 ms) is missed. The scene's own draw call, geometry rebuilt from the engine plus recording, is p50 2.4 ms and p95 4.1 ms. The rest is the RenderThread issuing commands (p50 5.0 ms) and the GPU (p50 5.6 ms) filling about 200 paths. Caching geometry by quarter-dp size moved p50 to 12.9 ms and p95 not at all (17.8 ms). The squash and the growing dust change sizes every frame, so the cache misses. A 20 ms busy-wait control showed 100 % over 16.7 ms. | B-09 rebuilds geometry and does not cache. What fits the target is fewer paths drawn at once, which is a change to the animation; the owner decides (B-45). Raw logs and the summary script: `docs/research/b-05`. |
| H2 | **Text width.** Box widths depend on measured text (`ToonButton` width = text + 56, Chip, Checkbox and Switch labels). Chrome's `measureText` and Skia's `TextMeasurer` disagree by subpixels, and a different box moves the wobble (radii are clamped to the box). | Pixel parity reveals it. Numeric parity is run at the box size Chrome measured, recorded in the golden, so geometry is tested apart from text (B-03, B-06). |
| H3 | **Fonts without Cyrillic** (§1.4). | Cyrillic is in scope (owner, 2026-11-02). B-35 finds the backing faces; the risk left is that no OFL face reads as one family with Shrikhand, and then a role changes face, which goes back to the owner. |
| H4 | **The design keeps moving.** The project is live: the engine went from `?v=8` to `?v=10` within the day of the import (§1.1). | The import is a snapshot. A re-import is an item that diffs the engine and re-emits the goldens (B-34). |
| H5 | **The sketch does not compile** (§1.5, the `fillMaxSize` helper). | Irrelevant to the port (D6), but stated as read, not run. B-01 compiles nothing from `compose/`; if anyone does, the result goes here. |
| H7 | **Variable weights below Android 8.** Each body weight is a `wght` setting on one variable file. Android applies font variations from API 26, and `minSdk` is 24. On 7.x the body text would then render at the files' default instance, Light 300, for every weight. Read, not run: nothing visual runs on Android (D9). | Checked on a device or emulator at API 24–25 when one is available. If it holds, either raise `minSdk` to 26 or bundle static instances for Android only, which makes them modified fonts (§1.4). The owner decides. |
| H6 | **Textures.** The four tiles came from the owner, not from DesignSync, and differ from the project's pixels by up to 22 levels (§1.1). | References and library use the same vendored files, so parity is unaffected. B-13 checks the wrap, the gouache tiles' vertical one first. |
| R1 | **The FX and idle budget get exceeded silently.** The brief allows at most 2 idle elements per screen; `LocalIdleBudget = 2` in the sketch enforces nothing. | B-08 makes the budget a counted resource, and a third idle request falls back to static, observable in a test. |

## 4. What happens next

Stage 0 settles what can invalidate the architecture: the skeleton, the fonts question, the engine
harness that emits the goldens, numeric parity of the geometry, the frame-cost measurement and the
pixel references. Stage 1 builds the tokens, theme, drawing primitives, press feedback, icons and FX
layer on top. Components come only after that. The order and the reasons are in
[backlog.md](../../backlog.md).

## Code anchors

| Kind | Path | What it is |
|---|---|---|
| Design project | `reference/design-system/README.md` | The designer's index of the library and its props |
| Design project | `reference/design-system/toon-engine.js` | Themes, icons, geometry, renderers, FX, curves: the ruler |
| Design project | `reference/design-system/toon-kit.js` | Runtime: frame loop, press/spring, one controller per component, reduce motion |
| Design project | `reference/design-system/compose` | The Kotlin sketch: read for pattern, not numbers |
| Design project | `reference/design-system/toon-ui Core.dc.html` | Tokens and the six core components with frame strips |
| Design project | `reference/design-system/toon-ui MVP Components.dc.html` | The other twelve, with frame strips and reduce-motion words |
| Design project | `reference/design-system/toon-ui v1 Components.dc.html` | The eight v1 components and the three screen transitions |
| Design project | `reference/design-system/toon-ui FX.dc.html` | The eight effects, their events and durations |
| Design project | `reference/design-system/toon-ui Principles.dc.html` | Seven rules and the recipe for a new component |
| Brief | `reference/design-system/uploads/brief.md` | The owner's brief to the designer, in Russian |
| Precedent | `youndie/oldge-ui@618cef8b923d7e0e501752240e6a727eed51e48c!/docs/research/research-architecture.md` | The sibling kit's research: reference rendering, parity floor, fonts |
