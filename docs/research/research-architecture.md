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

**Backing faces, chosen in B-35.** Each candidate was read at the same google/fonts commit as the
design's faces, `6c1dbe27dcf7d817d4b62cec033ba77fa8e2a5ec`, and every one is OFL with a `cyrillic`
subset in its `METADATA.pb`. They were drawn in Chrome beside the design's face, the Cyrillic scaled
to the design face's x-height (CSS `size-adjust`, from `x` measured at 100 px), in a Latin word and
a Russian phrase from the components (`Saturday` / `Суббота · Удалить план?`, `Add plan` /
`Добавить план`, the card's title and meta).

| Role | Chosen | Lost, and why |
|---|---|---|
| Shrikhand (display) | **Rubik Italic 900**, `ofl/rubik/Rubik-Italic[wght].ttf`, x-height 52.0 against 52.9 | Lobster and Pattaya are scripts, thin beside a fat brush; Yeseva One is a high-contrast serif; Rubik 900 upright and Montserrat Italic 900 carry the weight, but upright loses the slant and Montserrat's geometry is colder. No face reads as Shrikhand; Rubik's heavy italic sits next to it as the same voice. |
| Figtree (body) | **Onest**, `ofl/onest/Onest[wght].ttf`, weights 1 : 1, x-height 52.7 against 50.0 | Golos Text is as close and a little darker at 800; Manrope and Montserrat are wider. |
| Titan One (display) | **Nunito 1000**, `ofl/nunito/Nunito[wght].ttf`, x-height 49.7 against 58.0 | Russo One and Days One are square where Titan One is round; Rubik 900 is close but less round. |
| Fredoka (body) | **Nunito**, the same file, two weight steps heavier (Fredoka 700 reads as Nunito 900) | Comfortaa is too light and too geometric; Rubik 700 matches the weight but not the round terminals; Onest is not round. |

The design's face draws Latin and the backing face draws each run of Cyrillic (`withBacking`), as a
browser's fallback from one to the other does; spaces and punctuation stay in the design's face. The
three files ship unmodified, renamed only for compose-resources, with their licences beside the
others (SHA-256 `08c6c401…` Rubik Italic, `966c5c29…` Onest, `bb55a5ca…` Nunito); the font folder
grows from 0.5 to 1.3 MB. `ToonCyrillicMetricsTest` holds the result on the faces themselves at
100 sp: x-height within a pixel row of the Latin, stems within 12 %.

### 1.5 The Kotlin sketch is not a 1:1 port

`compose/README.md` says "every function here is a 1:1 port" and that the RNG "matches the JS
bit-for-bit". Read line by line against the engine:

| Divergence | Engine | Sketch |
|---|---|---|
| **The RNG returns `Float`**, so the phases differ from the engine's in the 8th digit. Visually nothing, but "bit-for-bit" is false, and a numeric parity test has to compare within a tolerance. | `rng` returns a double | `Mulberry32.next(): Float` in `compose/toon/ui/draw/ToonGeometry.kt` |
| **The icon width rule is missing a term.** The engine multiplies by `(1 + .06 · sin(s · 1.3 + ph))`, a 6 % swell along the stroke seeded per icon. | `iconWidth` | `iconOutline` in `ToonGeometry.kt` has no such factor. *B-11 ports it in full* (`toon-core/src/commonMain/kotlin/io/github/youndie/toon/icon/ToonIcon.kt`); dropping the term moves the outline 0.12 dp and the Settings cell to 5.39 % |
| **The filled button's night rim is the wrong colour.** The engine draws a filled button's rim in `btnHi` (`#F2A57E` at night) and an outlined one's in `rim`; the sketch's `ink()` always uses `rim` (`#8392C4`). | `btn` → `rimColor: o.v === 'filled' ? T.btnHi : T.rim` | `ink()` in `compose/toon/ui/components/ToonButton.kt` *Fixed in B-14*, held by `ToonButtonSceneTest`. |
| **The text button is blank.** The engine draws a wavy primary underline and a `surface2` blob while pressed; the sketch draws nothing for `ToonButtonVariant.Text`. | `btn`, `o.v === 'text'` branch | `ToonButton.kt` *Fixed in B-14*, held by `ToonButtonSceneTest`. |
| **A disabled label keeps its colour.** The engine paints a disabled label `muted` at 60 %. | `btn` (`lc`, `lOp`) | `ToonButton.kt` `BasicText` style *Fixed in B-14*, held by `ToonButtonSceneTest`. |
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

### 1.8 The desktop set: a second snapshot, from the project it was drawn in (B-68)

A desktop app built on this kit had nine components and a pointer rule designed in its own Claude
Design project, and the owner moved them here (stage 5). That project is not this kit's: it loads the
kit's engine and runtime and extends them from a file of its own.

| Fact | Where verified |
|---|---|
| The source is the owner's download of claude.ai/design project `fc2c88bc-3b5a-4dc2-a976-c589ef695d0b`, taken on 2026-11-13 and unpacked unchanged: 74 files. | the download's file list |
| **Vendored, in `reference/desktop/`:** the engine extension `desktop-engine.js`, the nine `Toon*.dc.html` (`ToonTitleBar`, `ToonSidebar`, `ToonSplitHandle`, `ToonScrollbar`, `ToonTranscriptTurn`, `ToonSpeakerSticker`, `ToonLevelMeter`, `ToonRecordButton`, `ToonSearchField`), the page `Desktop Pointer & Keyboard.dc.html` and the project's `README.md`. They differ from the project's files only by the rename recorded as departure 1 in [`reference/desktop/DEPARTURES.md`](../../reference/desktop/DEPARTURES.md), and the README also by departure 2: it keeps its kit parts, and what it said about the app's own screens is cut. | a segment-by-segment comparison of each pair, 2026-11-13; every kept README line found verbatim in the project's |
| **Not vendored:** the app's own screens (their pages, the assemblies they are built from, a window-layout page, sample data and a page list), the brief and its addendum, `.thumbnail`, the project's base stylesheet (below) and its copies of the kit (next row). | the download's file list |
| **The project's copies of the kit are this repository's bytes.** `toon-engine.js` (SHA-256 `49d00901…`), `toon-kit.js` (`47e12dd0…`), `support.js` (`8fe7df74…`), the four texture tiles and all 27 kit `Toon*.dc.html` are identical to `reference/design-system/`, byte for byte. The pages load them as `toon-engine.js?v=10` and `toon-kit.js?v=11`, the counters of this kit's second snapshot (§1.1), and the extension as `desktop-engine.js?v=1`. The designer could not make this check and said so in the project's README. | `cmp` of each pair; the `<script>` tags of the vendored pages |
| The extension loads after `toon-engine.js` and `toon-kit.js` and does not edit either file. It adds 12 centerline icons to the engine's shared `ICONS` table (25 → 37, none renamed or replaced), a token object `DESKTOP` on the engine class's prototype (`layout`, `pointer`, `motion`, `sizes`, `themeNames`), 19 more members there — the renderers `sticker`, `liftBody`, `plate`, `hoverLift`, `iconBtn32`, `recBtn`, `searchBox`, `navPlate`, `splitGrip`, `scrollThumb`, `levelMeter`, `trafficSlots`, the curves `hoverRamp`, `recPulse`, `livelevel`, and `accents`, `tok`, `blobPts`, `blobPath` — and a runtime, `window.DesktopKit`, whose controller rides the kit's frame loop. | `reference/desktop/desktop-engine.js`; the engine and the extension run in Node, the prototype's names listed before and after |
| **It also replaces one of the kit's renderers.** The bottom sheet's `sheet(T, o)` is overwritten on that prototype by a panel `sheet(T, w, h, o)`: a body with a deeper shadow, which the sidebar draws its own body with. In any page that loads the extension, the kit's bottom sheet is gone, and its scene called with the kit's options crashes inside `wob`. | the same Node run; `ToonSidebar.dc.html` `paint()` |
| The project's base stylesheet holds eight `@font-face` rules, Golos Text, Lobster, Nunito and Rubik 800 added under the kit's four family names through `unicode-range`, and one `::selection` rule whose colours default to what the extension's `tok` names (`selBg` = `chipSel`, `selFg` = `ink`). The faces were the designer's choice without this repository; **the owner decided the kit's faces stand** (§1.4, B-35), so the file is not vendored and no token is lost with it. | the stylesheet, read 2026-11-13 |
| A vendored page does not open on its own: it links the base stylesheet, the kit's engine, its runtime and Claude Design's `support.js` from beside itself, and only the extension is there. The harness loads the engine files from their paths and needs none of that. The pages have no `<title>`; the pointer page's name is its file name. | the vendored pages' `<head>` |

**The harness.** `scripts/engine-goldens.mjs` loads `desktop-engine.js` after every kit golden has
been taken, so the overwritten `sheet` reaches none of them, and writes the desktop renderers' frames
to `engine-goldens/desktop.json`, shaped as `components.json` is, from the cells in
`scripts/desktop-cells.mjs`. Since the project has no state sheets for the nine, a cell is one of a
component's own prop values from its `.dc.html`. B-68 gives it the sticker's three kinds at its three
sizes in three themes; each component's item adds its group. Every existing golden came out byte for
byte as before; `manifest.json` gained the extension's path and hash. Two controls: the sticker's
shadow moved from 2 to 3 dp makes `make check` report `desktop.json` stale, and the extension loaded
before the kit's frames makes the harness die on the bottom sheet.

**Consequence.** The desktop renderers are an oracle in the same way the kit's are, and the nine
components are built against them as `ToonScene`s. The port does not carry the `sheet` collision: the
sidebar's panel is a scene of its own beside the bottom sheet (B-77). Where the project is silent —
no state sheets, no frames for its events, no icon page — the component departs, and `ToonDepartures`
records it (D13). `reference/desktop/` is not re-imported either: like `reference/design-system/` it is
a record, and a later design of the app's is read as proposals.

**Words over the drawing (B-71).** The desktop renderers draw shapes only: a component's words are
HTML laid over its SVG in its `.dc.html`, and no frame holds them. The port draws them as the kit's
labels at that HTML's numbers, a line centred on its box as a flex row centres it (the face's measured
height, so its own ascent and descent place the baseline), and a test of the layout holds them: the
search field's placeholder stands where its value is typed, to a pixel. A text input is a real
`BasicTextField` with the design's caret colour.

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
measured 4.24 % against its 0.21 %, which still passes viddik's default 5 %. *Extended in B-11:* icons have a
numeric half too, but only a DOM can compute it (`sampleSub` measures an SVG path element), so the
same Chrome run writes the engine's `iconOutline` for every glyph and weight to
`toon-core/src/desktopTest/resources/design-goldens/icon-outlines.json`. *Amended in B-12:* the floor
is a share of the cell, and that does not carry over to a cell its subject inks little of. An effect
covers a small part of its 260 × 150 cell, and wrong effects measured under 1.54 %: stars spinning
one way 0.56 %, sweat drops without their stretch 0.50 %, speed lines 10 dp short 0.64 %. So
`InkParityTest` counts a cell's differing pixels against the pixels either image inks (differs from
the stage colour), takes the floor the same way over the Divider cells — 14.81 % — and fails on any
other reference over it, in `check`. Clean, the worst of 459 cells is 13.27 % (Sweat at 40 ms, two
drops just appearing, all edge); those mutants measure 39–99 %. What no pixel measure sees is a
pattern inside a large fill: the hole with another crack seed measured 8.70 %. Its polygons come from
the engine in Node (`engine-goldens/fx.json`) and are compared point by point. *Amended in B-14:* a
component has a large fill, and in a button neither floor saw a shadow 0.4 dp short, a bottom that
does not thicken or the night rim in the outlined colour. So a component is built as a frame — the
engine's layers (`L`) and texts (`X`) as data (`toon-core/src/commonMain/kotlin/io/github/youndie/toon/draw/ToonScene.kt`) — and drawn from it, and
`engine-goldens/components.json` holds the frame the engine's renderer returns for every cell on the
spec page. The port is compared layer by layer: colour, opacity, stroke, offset exact, paths within
the engine's 0.1 dp rounding. That is the numeric level for components; pixels then check the
drawing, and every button mutant tried is caught by the frame (nine of nine) where the pixel floors
caught one. Text needs a floor of its own: Chrome inks a glyph through CoreText with sub-pixel
advances, Compose through Skia with whole-pixel ones, so the label alone (`TextFloor` cells) differs
by 1.96–2.30 % of its cell, 48–54 % of its ink. Cells with text are held to that ink floor in
`InkParityTest`, and each such group has a twin without text (`ButtonShape`) held to the Divider's.
*Extended in B-22:* where every page frame clamps a value — the list item's press tint is 0 or 1 in
all of them — `scripts/component-cells.mjs` adds probe frames the engine draws at the moments in
between, recorded for the frame tests and never rendered.
*Extended in B-23:* a group whose ink sits under the Divider's floor though it carries text (the
snackbar, 4.27 % at worst against 14.81 %) is held to that floor, not to the text floor.
*Extended in B-24:* a frame carries the iris — a rectangle with a round hole, filled even-odd, held
to the engine's `d` arc by arc — and texts in the theme's display face; the goldens write the fill
rule and the face only where they differ from the default, so no earlier frame changed.
*Extended in B-25:* one circular SVG arc (the determinate ring, `A r r 0 1 1`), its centre found
from its ends and flags; and a number written from its point (`h.01`), which the comparison read as
`01` until then.
*Extended in B-27:* `scaleAbout` writes its pivot pair as `f(p)` and `f(-p)`, and `Math.round` takes a
half up, so a pivot off the 0.1 grid (the navigation bar's 146.25) rounds to 146.3 and −146.2; the
port carries that exact difference, and a translation rounded inside a scale is allowed its scaled
rounding. Both are zero for a pivot on the grid.
*Extended in B-28:* the tabs are three 16 sp labels in a 358 × 66 cell, and in Bright Cel their ink
alone takes the group past the Divider's floor (18.15 %); the group is read against the text floor,
and a twin without labels (`TabsShape`, as `ButtonShape`) holds the rest to the Divider's.
*Extended in B-15:* a frame carries icons as ready paths, held to the engine's own `d` for them —
without a DOM the engine strokes the centerline at the weight's base width, which is what the frame
golden holds, and the outline itself is B-11's — several texts with their own weights, and the
engine's `place`. The frame goldens are taken with the kit's `document` stub removed, or
`iconOutline` reaches for a DOM it does not have. Every later component is built on these frames.
*Corrected in B-16:* the reference page built each scene before its fonts had loaded, so every
width the engine takes from `tw` came from the fallback face — the extended FAB's body and the text
button's underline. The page now builds the scene once the bundled faces load; the extended FAB went
from 10.20 % of its cell to 4.39 %, the text button from 44.67 % of its ink to 36.71 %, and no other
reference moved. What is left on the extended FAB is the label's width itself: Chrome measures
"New plan" at 17 px about 69.4 dp, Compose with whole-pixel advances about 70, and a body sized from
it shifts every wobble point along its outline. So a component's parity
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

*Applied in B-47 (found in B-24):* until then no component scaled its canvas by the density, which
is right only at density 1, where every test and reference runs. A frame is now drawn inside
`inDp`, the one scale by the density; the text renderer lays labels out at density 1 with the
user's font scale, so a label is not scaled twice; pointer positions, the text field's caret and
the FX host's root bounds are read in dp.

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

*Applied to the switch in B-18:* `instant`.

*Applied to the chip in B-20:* `instant`.

*Applied to the list item in B-22:* `instant`.

*Applied to the snackbar in B-23:* show and hide `fade 120`; the faded sign hangs at rest.

*Applied to the dialog in B-24:* open and close `fade 120`; the layer fades at rest, without the pop or the stars.

*Applied to the progress indicators in B-25:* the linear one is a static bar at 33 %, the circular one a static blob.

*Applied to the top app bar in B-26:* `off`; the sign hangs still.

*Applied to the navigation bar in B-27:* `instant`; the selection lands at once, no jump, no dust.

*Applied to the tabs in B-28:* `instant`; the bookmark moves at once and the content does not slide.

*Applied to the slider in B-36:* `instant`; the ball is squashed exactly while held, round otherwise.

*Applied to the menu in B-37:* `fade 120` both ways; the scroll fades in unrolled and fades out without rolling up.

*Applied to the bottom sheet in B-38:* `fade 120` both ways; the sheet and its scrim fade at rest, without sliding.

*Applied to the navigation drawer in B-39:* `fade 120` both ways; the door fades in and out flat, without swinging or shivering.

*Applied to the tooltip in B-40:* `fade 120` both ways; the balloon fades at its size, without inflating.

*Applied to the segmented button in B-41:* `instant`; the drop moves to the new segment at once.

*Applied to pull-to-refresh in B-42:* `static spinner`; the band still follows the finger, the release lands on a spinner that does not turn, and it goes at once when done.

*Applied to the title card in B-43:* `fade 120`; the card fades in at rest, without the pop, the flowing rings or the stars.

*Applied to the screen transitions in B-44:* `cross-fade 120` for all three; the old screen fades out as the new one fades in.

*Applied to the card in B-21:* appear and dismiss `fade 120`, press `instant`.

*Applied to the card list in B-46:* the dismiss's `fade 120` covers the close-up too: the cards below
hold still while the card fades, then move up into its slot at once, without the slide.

*Applied to the text field in B-19:* focus `instant`, the error `instant, red only`, the caret
`steady caret`.

*Applied to the checkbox and radio in B-17:* a check is `fade 120`, the kit's cross-fade from the
unchecked frame to the checked one; an uncheck and the radio's drop are `instant`.

*Applied to the FX in B-12* (the FX page's `reduce motion:` line per effect): dust, stars, speed,
shake, hole and sweat are `off`; shake's "colour change only" is the component's. The flash's
`fade 120` holds its settled frame (the page's 90 ms frame, full size) and fades it out over 120 ms,
a reading of the word, since the kit and the page draw no flash at all under reduce motion. The iris's
`cross-fade 120` is a property of the scene change, which B-44 builds; the wipe itself is off. An
event reaches its listeners either way, so a sound can follow it.

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

*Measured in B-11:* the icon outlines are compared with Chrome's within 0.02 dp, the goldens' two
decimals plus the two arc conversions (Compose's parser writes an SVG arc as cubics, Chrome as
conics); the worst point was 0.0149 dp. That needed one change of method: Skia's `PathMeasure`
flattens a curve to about half a unit, which on the 24 dp grid is a percent of an icon's length (the
search lens measured 43.1 dp against the 43.54 its arcs integrate to), so a centerline is measured
enlarged 1024 times. The outline is computed in `Double` there, because it is sampled once per glyph
and weight, not per frame.

*Amended in B-80:* the desktop extension's 12 glyphs (§1.8) are generated after the kit's 25, and their
outlines are taken from the engine in Chrome with the extension loaded (`scripts/design-references.mjs
--outlines`, which takes only the outlines, so a newer Chrome does not rewrite every reference; the
kit's 25 came out byte for byte the same in Chrome 157 as in 156). One of them showed that Compose's
parser does not draw every arc as the SVG spec does: an arc whose radii fall short of its end point is
scaled by the chord over 1.99999, not 2, which leaves its centre a hair off the chord, and the large arc
long. The `rec` glyph measured 39.04 dp against the 38.96 its arcs integrate to, one sample more per
edge than Chrome takes. A glyph's arcs are now converted by the spec in `Double` (`withSpecArcs`) before
Compose parses the path; the worst outline point fell to 0.0060 dp. Four of the kit's glyphs have such
arcs (`person`, `settings`, `share`, `eye`): `settings` and `share` moved at 48 dp, closer to Chrome's
reference in each of their 12 cells (the summed channel difference against the references fell from
4,808 to 3,533), and those 12 goldens were re-recorded — a correction toward the design, not a departure.

*Rejected:* `Double` geometry to chase the sketch's "bit-for-bit" claim. It costs a conversion at every path operation
and buys a precision nobody can see.

### D12. MIT licence — *a deviation from the siblings*

The owner chose MIT on 2026-11-02 (`LICENSE`). oldge-ui and kvadrant-ui are Apache 2.0. Publishing
otherwise follows oldge-ui (B-32).

### D13. The code is primary from 2026-11-03 — *a deviation from D3*

The owner, on 2026-11-03, looking at the kit: "somewhere we will part from the original design, so
from this point the code is primary to the design". Until then the design was the ruler (D3) and
the code followed it; now the design is where the code started, and the code may leave it where the
owner asks.

- **Nothing is re-imported.** B-34 is dropped: a newer design would be a list of proposals, not a
  snapshot to regenerate everything from. The vendored engine stays as it is, unedited, as the
  record of where each component came from.
- **The checks against the design stay**, as a regression net for everything that has not left it:
  engine frames (B-14), goldens, and ink parity against the Chrome references (B-12).
- **A departure is recorded, not loosened.** A cell where the code now draws something else on
  purpose is listed in `ToonDepartures` (`toon-core/src/desktopTest`) with its item: the engine-frame
  and ink-parity checks skip it there and nowhere else, its golden is re-recorded in the same change,
  and a test keeps every entry naming cells that exist. A cell that is not listed is still held to
  the design.
- **A new component may be designed here** (this replaces the rule of B-33), and is then held to its
  own goldens.

*Rejected:* editing the vendored engine to match the code, so that the frame checks pass again. It
would keep a ruler that measures the code against itself, and lose the record of the design.

### D14. One pointer rule for the whole kit (B-69)

The desktop design's pointer page (`reference/desktop/Desktop Pointer & Keyboard.dc.html`, §1.8) fixes
one hover, focus and target rule for every component, and draws it on the kit's own components with
their own renderers. The kit drew no hover before.

- **Hover is one value, 0 to 1, per element**, set by a mouse over it and never by a finger
  (`Modifier.toonHoverable`, `interaction/ToonHover.kt`). It eases in over 80 ms and out over 120 on
  the kit's press curve, from wherever it is, and is instant under reduce motion: the extension's
  `hoverRamp` and its controller. A disabled element shows none.
- **A body with a hard shadow lifts** 2 dp toward the top left while its shadow stays on the table
  (the extension's `hoverLift`), less as a press drops the shadow, so a pressed body is the kit's
  pressed body: the extension's own `liftBody` has that factor, and `hoverLift` has none. Button
  (filled, outlined), icon button, FAB, chip and card lift whole; the slider lifts its ball only.
  **A flat element gets a plate** of the pressed-surface colour: the text button's where its press
  blob goes, and the list row and the menu item draw the selected shade, as the page draws them. A
  menu item ramps on its own, as the controller keeps one per name.
- **A shadow is the layer's own offset**, an ink fill moved down and right by the same amount. The
  engine tells it by the start of the layer's transform, which a `place` in front changes: the page's
  chip and card rows are placed at 0, 0, and `hoverLift` then keeps their ink outline with the
  shadow. The rule is the page's words, so the goldens lift the bare renderer and place it after.
- **Focus** is the kit's dashed ring, which every pressable already drew except the card; it now takes
  keyboard focus and draws the page's ring, 6 dp out.
- **Targets**: the kit's components keep their 48–52 dp, as the page's rule 4 leaves them; its pointer
  sizes (28, 32, 40, 56, 44) are `ToonPointerTarget`, for the desktop components of stage 5.
- Held by the engine's frames: `engine-goldens/desktop.json` groups `Hover` and `Focus`, each cell one
  of the component's own rest cells under the rule, against `ToonHoverFramesTest`; the numbers against
  the extension's `DESKTOP` in `manifest.json`.

*Rejected:* a hover parameter per component, which the item rejected — one rule drifts once each
component carries its own switch. *Rejected:* `PointerEventType.Enter` from any pointer, which would
leave a finger's press hovered on a touch screen. *Left to their own item* (B-79): the pressable
components the page does not draw — checkbox, radio, switch, segmented, tabs, navigation bar,
drawer, text field — whose hover needs a look the page does not give.

## 3. Risks and open questions

| # | Hypothesis or risk | Mitigation, and where it is settled |
|---|---|---|
| H1 | **Performance.** Rebuilding about 130 points per shape per frame, for 10 cards appearing at once with FX, holds the brief's 60 fps on a Pixel 6a-class phone. The sketch asserts "cheap"; nothing was measured. | Measure before building components on it: frame time on desktop and on a real Android device with the B-21 card cascade, against a cached-path variant. If it fails, cache per (size, seed, squash bucket) and simplify the animation rather than lowering the target, as the brief says (B-05). The device is a Pixel 6a (owner, 2026-11-02). |
| H1 — *verdict, B-05, 2026-11-02* | **Red for the worst case, and geometry is not the cause.** On a Pixel 6a (Android 17, 60 Hz, release build), ten cards in the 40 ms cascade with two dust puffs each give a FrameMetrics total of p50 15.0 ms and p95 18.2 ms. 23.6 % of frames run over 16.7 ms, though only 1.6 % of vsync slots go unfilled. The pre-registered bar (≤ 1 % over 16.7 ms, p95 ≤ 16.7 ms) is missed. The scene's own draw call, geometry rebuilt from the engine plus recording, is p50 2.4 ms and p95 4.1 ms. The rest is the RenderThread issuing commands (p50 5.0 ms) and the GPU (p50 5.6 ms) filling about 200 paths. Caching geometry by quarter-dp size moved p50 to 12.9 ms and p95 not at all (17.8 ms). The squash and the growing dust change sizes every frame, so the cache misses. A 20 ms busy-wait control showed 100 % over 16.7 ms. | B-09 rebuilds geometry and does not cache. What fits the target is fewer paths drawn at once, which is a change to the animation; the owner decides (B-45). Raw logs and the summary script: `docs/research/b-05`. |
| H1 — *accepted, B-45, 2026-11-02* | **The owner accepted the measured numbers for the ten-card cascade with dust** (p50 15.0, p95 18.2 ms, 23.6 % of frames over 16.7 ms, 1.6 % of vsync slots unfilled on the Pixel 6a). The animation is kept as the design draws it; no simplification. Two things the measure did not control, written down so a later re-run knows them: the release build had R8 off and no baseline profile, which bear only on the UI thread's 2.6 ms of the 15; and its bar is FrameMetrics' total against a fixed 16.7 ms, stricter than the platform's own deadline-based jank. | The cascade stays as designed (B-46 builds it); a re-measure with R8, a baseline profile and the deadline measure is the owner's to ask for. |
| H2 | **Text width.** Box widths depend on measured text (`ToonButton` width = text + 56, Chip, Checkbox and Switch labels). Chrome's `measureText` and Skia's `TextMeasurer` disagree by subpixels, and a different box moves the wobble (radii are clamped to the box). | Pixel parity reveals it. Numeric parity is run at the box size Chrome measured, recorded in the golden, so geometry is tested apart from text (B-03, B-06). |
| H3 | **Fonts without Cyrillic** (§1.4). | *Closed by B-35:* each role is backed by an OFL face with Cyrillic, matched in x-height and weight. Shrikhand has no true twin; Rubik Italic 900 is the nearest voice, and a role change was not needed. |
| H4 | **The design keeps moving.** The project is live: the engine went from `?v=8` to `?v=10` within the day of the import (§1.1). | *Closed by D13 (2026-11-03):* the code is primary, nothing is re-imported (B-34 dropped). A later design is read as proposals. |
| H5 | **The sketch does not compile** (§1.5, the `fillMaxSize` helper). | Irrelevant to the port (D6), but stated as read, not run. B-01 compiles nothing from `compose/`; if anyone does, the result goes here. |
| H7 | **Variable weights below Android 8.** Each body weight is a `wght` setting on one variable file. Android applies font variations from API 26, and `minSdk` is 24. On 7.x the body text would then render at the files' default instance, Light 300, for every weight. Read, not run: nothing visual runs on Android (D9). | Checked on a device or emulator at API 24–25 when one is available. If it holds, either raise `minSdk` to 26 or bundle static instances for Android only, which makes them modified fonts (§1.4). The owner decides. |
| H6 | **Textures.** The four tiles came from the owner, not from DesignSync, and differ from the project's pixels by up to 22 levels (§1.1). | *Checked in B-13:* the library bundles the same files byte for byte. Every horizontal wrap and the paper tiles' vertical ones are within 7 % of the inner difference; the gouache tiles' vertical wrap is 1.91 times it, a faint lighter line at 8× zoom in a 2 × 2 render and not visible at 1:1. It is the tile's own, as Chrome draws it too; a cleaner tile is the owner's to supply. |
| R1 | **The FX and idle budget get exceeded silently.** The brief allows at most 2 idle elements per screen; `LocalIdleBudget = 2` in the sketch enforces nothing. | B-08 makes the budget a counted resource, and a third idle request falls back to static, observable in a test. |
| R1 — *closed, B-08* | `ToonTheme` hands out idle slots from a `ToonIdleBudget` of capacity 2 in composition order. A third request is refused, reduced motion grants none, and a refused element stays still when a slot frees up (`ToonThemeTest`). | — |

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
| Desktop design | `reference/desktop/desktop-engine.js` | The desktop set's extension of the engine: icons, `DESKTOP` tokens, renderers, curves (§1.8) |
| Desktop design | `reference/desktop/Desktop Pointer & Keyboard.dc.html` | The hover, focus and target rule for every component (B-69) |
| Desktop design | `reference/desktop/README.md` | The desktop project's README, its kit parts only (departure 2), in Russian |
| Desktop design | `reference/desktop/DEPARTURES.md` | How the vendored files differ from the project's |
| Precedent | `youndie/oldge-ui@618cef8b923d7e0e501752240e6a727eed51e48c!/docs/research/research-architecture.md` | The sibling kit's research: reference rendering, parity floor, fonts |
