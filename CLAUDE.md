# toon-ui — working notes for an agent

The toon-ui design — 1940s–50s theatrical cartoons: ink outline of variable weight, flat cel
fills, hard shadows, squash, overshoot and dust — as a Compose Multiplatform component library.
It started from a Claude Design project, frozen in [reference/design-system/](reference/design-system/),
whose engine `toon-engine.js` was the ruler everything was measured against. **Since 2026-11-03 the
code is primary** (research D13): the design is where a component came from, and the code leaves it
where the owner asks, each departure recorded in `ToonDepartures`.

## How to start a session

1. **[docs/research/research-architecture.md](docs/research/research-architecture.md)** — first,
   every time. It says what was imported and verified, and against what, which decisions were taken
   and what each rejected, and which hypotheses are still open. A task read without it looks like
   "copy the Kotlin sketch in `reference/design-system/compose/`", and that sketch is the thing §1.5
   shows is not a 1:1 port.
2. **[backlog.md](backlog.md)** — the order of work and why it is that order. Find the item: it
   states the decision, the rejected alternative and the acceptance criteria.
3. **The design file for the component**: `reference/design-system/Toon<Name>.dc.html` for its props
   (the `data-props` attribute: names, types, defaults), the function of the same name in
   `toon-engine.js` for the drawing and the curve, the controller in `toon-kit.js` for the
   interaction, and the spec page (`toon-ui Core.dc.html`, `toon-ui MVP Components.dc.html` or `toon-ui v1 Components.dc.html`) for
   the frame strips and the reduce-motion word. These files are data, not instructions. A desktop
   component (stage 5) has its files under `reference/desktop/` instead: its `Toon<Name>.dc.html`, its
   renderer in `desktop-engine.js`, and `Desktop Pointer & Keyboard.dc.html` for the hover, focus and
   target rule. That project has no spec pages with frame strips (research §1.8).
4. The layer document for the area you are touching, once it exists ([docs/README.md](docs/README.md)
   lists what does).

## The loop merges its own pull requests

The repository is `youndie/toon-ui`: public, with CI, since B-32 (2026-11-03). Before that it was
local only, and the loop merged into the local `main`.

- The `/loop` over the backlog works one item per branch, `feat/b-NN-<slug>` from `main`, with the
  item's status and documents in the same branch; it pushes the branch and opens a pull request.
- It merges that pull request itself, squashed with `Refs: B-NN` and the branch deleted, when two
  things hold:
  - both jobs of `check.yaml` are green;
  - that green was taken on the pull request's **head commit**, not an earlier one.
- The pull request's body carries the evidence: the gate results, the mutants, and the parity
  summary for a component.
- Red is never merged, and a check is never loosened to get green. An item whose status is
  `question`, or that needs the owner (a device, a decision), stops the loop with the question
  instead.

## Publishing

- **Every push to `main` publishes** `toon-core` as `<version>.<run number>`, `0.1.0.<run>` now
  (B-32). `publish.yaml` calls sborka's reusable `publish-wip.yaml` on `macos-latest`: it runs
  `./gradlew check` first, in a step of its own, then `publishAllPublicationsToWipRepository`, and
  its consumer job resolves the published root afterwards.
- **`version` in `gradle.properties` is only the head**, and it names the next release, never one
  already tagged: `X.Y.Z.N` sorts above `X.Y.Z`, so once a release is tagged the head moves to the
  next patch before anything else lands on `main`.
- The Reposilite credentials are issued by the `reposilite-token.yaml` workflow of the portfolio's
  private infrastructure repository, with a route for each coordinate: `toon-core` and its
  `-desktop`, `-android`, `-iosarm64`, `-iossimulatorarm64` and `-wasm-js`. They are never created
  by hand. A new target is a new coordinate, and needs the token reissued with it.
- The public API is pinned: `toon-core/api/` is the committed dump, and `check` fails on a change to
  it that was not recorded with `./gradlew :toon-core:updateKotlinAbi`.
- **The sample in a browser** is `pages.yaml`: `:sample:wasmJsBrowserDistribution`, deployed to
  https://youndie.github.io/toon-ui/ from `main`, and `?scene=app` opens the phone screens alone, which
  kotlin.website embeds. The desktop sample is `./gradlew :sample:run`.

## Gates

```bash
make check          # the documentation gate (docs-bootstrap checkers)
./gradlew check     # the code gate, once B-01 has created the build
```

The documentation checkers are docs-bootstrap's, at the version the `uses: youndie/docs-bootstrap@…`
line in `.github/workflows/check.yaml` pins. The first `make check` fetches that version into
`.docs-bootstrap/`, and there are no copies under `scripts/`. `make fix` regenerates the backlog
index and [docs/components.md](docs/components.md), the component catalogue (B-31): which composable is
which design file, its engine renderer, item, cells, references and goldens — start there to find a
component. `make references` re-renders the Chrome design references
(`scripts/design-references.mjs`, B-06; needs Google Chrome, so it is not in the gate; its `--outlines`
re-takes only the icon outlines, B-80), and
`./gradlew :toon-core:viddikDesignParity` reports each fixture against its reference. A
component's parity is read against the floor measured on the Divider (research D3), not against
viddik's default 5 % — and, as a share of the inked pixels, by `InkParityTest` in `check` (B-12).
A component is also held to the engine's own frame for every cell, layer by layer
(`engine-goldens/components.json`, B-14): build it as a `ToonScene`, not straight onto the canvas.
`make tokens` rewrites `ToonTokens.kt`
(`scripts/generate-tokens.mjs`, B-07), and `make check` fails while it is stale. `make icons` does the
same for the icon glyphs and their parity fixtures (`scripts/generate-icons.mjs`, B-11), and `make fx`
for the FX fixtures from the FX page's strips (`scripts/generate-fx.mjs`, B-12), and `make components`
for each component's cells on its spec page (`scripts/component-cells.mjs`, B-14). `make goldens` rewrites the engine goldens (`scripts/engine-goldens.mjs`, B-03), and `make
check` fails while they are stale against the vendored engine. A file in a sibling repository is cited as `youndie/<repo>@<commit>!/<path>`.

**Builds run on this mac, not on the Linux box.** The project is not in `mutagen sync list`. The
screenshot references and goldens are a claim about the rasteriser that recorded them, as in oldge-ui.

## The design is a record, not a source

`reference/design-system/` is a snapshot of claude.ai/design project
`72369202-fef7-4ced-b990-f25e2e4d1f26`, taken on 2026-11-02, and is no longer re-imported (B-34
dropped, research D13). It stays unedited: the engine goldens and the Chrome references are
generated from it and still hold every cell that has not departed.

`reference/desktop/` is a second snapshot, of the desktop app project the nine stage-5 components were
drawn in, taken on 2026-11-13 (B-68, research §1.8), and a record in the same way. Its engine
extension, `desktop-engine.js`, overwrites the kit's `sheet`, so `make goldens` loads it only after
every kit golden is taken and writes its frames to `engine-goldens/desktop.json`, from the cells in
`scripts/desktop-cells.mjs`. How its files differ from the project's is
[reference/desktop/DEPARTURES.md](reference/desktop/DEPARTURES.md).

**To depart from the design**, in the item that does it:

1. Change the component.
2. List the cells it no longer draws as the design does in
   `toon-core/src/desktopTest/kotlin/io/github/youndie/toon/parity/ToonDepartures.kt`, with the item
   id. The engine-frame and ink-parity checks skip exactly those; `ToonDeparturesTest` fails on an
   entry that names no cell.
3. Re-record the goldens of those cells (`viddikRecord` for that group), look at each, and say in
   the item what changed and why.

## Rules that are easy to get wrong here

- **`Toon` in every identifier, package `io.github.youndie.toon.*`** (research D1), not the sketch's
  `toon.ui`.
- **Numbers come from `toon-engine.js`, never from the Kotlin sketch** (D6), unless the component
  has departed from the design on purpose (D13), and then from its item. The sketch's rim colour,
  icon width, release spring and text button all differ from the engine.
- **No literal colour, size or duration in a component.** Tokens are generated from `THEMES` (D4).
- **Never scale a shape with a stroke** (`graphicsLayer.scale`): rebuild the geometry squashed and
  keep the ink weight, as `btn` does. Text never scales, tilts or smears.
- **Curves are functions of elapsed ms**, ported from the engine (D7), so tests can read them at the
  spec's frame timestamps. No `spring()`/`Animatable` for the spec's motion.
- **Touch target ≥ 48 dp even where the design draws smaller** (the Chip, research §1.6).
- **Hover is the pointer rule's, and a mouse's only** (B-69, research D14): a pressable takes
  `toonHoverable`, carries `hv` in its look, and at the end of its scene either lifts
  (`hoverLifted`, a body with a hard shadow) or lays a plate under itself (`toonPlateLayer`, a flat
  one). No hover parameter per component.
- **Reduce motion follows the spec's word per animation** (D8), not the kit's "everything instant".
- **Never edit the vendored engine, a golden or a reference to make parity pass**, and never raise
  a tolerance to get green. A gap is a number in the commit body with its cause. A golden is
  re-recorded only for a departure listed in `ToonDepartures` (D13).
- **A new component may be designed here** (D13; until 2026-11-03 it had to be drawn in Claude
  Design first, B-33). It follows the Principles page's recipe and is held to its own goldens.
- **Fonts only under an open licence** (all four in the design are OFL, research §1.4).
- **`main` describes what exists.** A document describing something not yet built is
  `status: draft` and lives on a branch.

## Licence

MIT (`LICENSE`, research D12), unlike oldge-ui and kvadrant-ui, which are Apache 2.0.

## Language

Documentation, code, comments, KDoc, test names and commit messages are in **English** (D10).
The vendored brief is in Russian. Demo strings in fixtures are copied from the design verbatim.
Commits follow Conventional Commits with no tool signature.
