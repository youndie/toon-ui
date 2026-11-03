---
id: B-32
title: "Public repository and publishing"
status: done
priority: P2
size: S
stage: stage-4-product
blocked_by: [B-01]
---

# B-32 — Public repository and publishing

The brief calls toon-ui open source. The repository is local and has no remote.

- **Licence: MIT** (owner, 2026-11-02). `LICENSE` is in the repository and the sborka
  `sborka.licence` property says the same once B-01 creates the build. This departs from oldge-ui
  and kvadrant-ui, which are Apache 2.0.
- **The decision and its reason.** Publish like oldge-ui: `youndie/toon-ui` on GitHub, and every
  push to `main` publishes `<version>.<run>` to the Reposilite through sborka's
  `publish-wip.yaml` on macOS. Credentials come from the infrastructure repository's token
  workflow, never by hand.
- **When: now** (owner, 2026-11-03): "сюда паблиши" — `github.com/youndie/toon-ui`, the library in
  the Reposilite as oldge-ui, a README like oldge-ui's, the kit in a browser on GitHub Pages as
  kvadrant-ui has it, and a desktop sample app to look at.
- Not covered: Maven Central.

- AC: the repository is public with CI green; a `0.1.0.<run>` build resolves for a consumer.
- Anchors: `LICENSE`, `youndie/oldge-ui@618cef8b923d7e0e501752240e6a727eed51e48c!/CLAUDE.md`.

## Findings (2026-11-03)

- **Publishing.** `toon-core` applies `sborka.publish`; `publish.yaml` is oldge-ui's, calling
  sborka's `publish-wip.yaml@v0.5.0.130` on macOS with `check` before the publish. The public API
  is pinned by `abiValidation` (`toon-core/api/`), as oldge-ui's is.
- **Credentials.** The infrastructure repository's `reposilite-token` workflow, for
  `youndie/toon-ui`, name `toon-ui`, the six coordinates `toon-core` and its `-desktop`,
  `-android`, `-iosarm64`, `-iossimulatorarm64` and `-wasm-js`, in `snapshots`. It writes the two
  secrets into the repository, so it runs once the repository exists.
- **The sample** is now `commonMain` with a desktop and a wasm entry point. Its front page is a kit:
  every component on one sheet, under the three themes and reduce motion, with the Screens page's
  three screens on a second tab. `pages.yaml` builds the wasm bundle on every pull request and
  deploys it from `main`, as kvadrant-ui's demo is deployed (without kvadrant-ui's per-component
  pages and Dokka, which this item did not ask for). Looked at in a browser served from the
  production bundle: fonts arrive a moment after the first frame, the theme and the page switch.
- **History checked before going public:** no private project names, no keys (a scan for GitHub,
  AWS, Slack, Anthropic tokens and private keys over every commit), one author.
- **CLAUDE.md** now says what oldge-ui's says: the loop opens a pull request per item and merges it
  itself on green CI over the head commit.
- **Seen from outside (2026-11-03).** The first push ran `check`, `pages` and `publish`. Publish
  failed once, before any build, because the token workflow had not yet written the secrets; rerun
  after it, all three green. `0.1.0.1` is in the Reposilite for the root and all five targets, and
  `publish-wip.yaml`'s consumer job resolved it. https://youndie.github.io/toon-ui/ serves the kit.
- `renovate.json` is oldge-ui's: sborka's presets by name, so the stack hold and the Monday batch
  apply here too.
