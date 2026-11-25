---
id: B-96
title: "Release toon-core 0.1.0 on Maven Central"
status: question
priority: P2
size: XS
stage: stage-4-product
blocked_by: [B-95, B-97, B-98, B-99]
---

# B-96 — Release toon-core 0.1.0 on Maven Central

The tree can publish to Central ([B-95](B-95-central-release-preflight.md)). A version on Central can never be replaced
or withdrawn, so the release is the owner's to start, after the verdict of pyatiletka's trial of the kit on its board (its B-20).

- **The release, in order**, once the owner says yes to 0.1.0 at a named commit of `main` whose `check` and `publish`
  runs are green:
  1. `git tag -a v0.1.0 -m "0.1.0" <sha> && git push origin v0.1.0`
  2. `gh workflow run central.yaml -R youndie/sborka -f repository=youndie/toon-ui -f ref=v0.1.0 -f version=0.1.0 -f runner=macos-15 -f konan-cache=true -f gate=false`
  3. On the Central portal, read the staged deployment's files (six coordinates, each with its `.asc`), and press Publish.
  4. After `repo1.maven.org` lists 0.1.0 for every coordinate: one pull request that moves `version` to `0.1.1` and the
     README's installation to `mavenCentral()` with `io.github.youndie:toon-core:0.1.0`, and a GitHub release naming the
     tag and the sborka run.
- **Why `gate=false` on `macos-15`.** The gate is this repository's `check`, whose goldens are a claim about the
  rasteriser of `macos-latest`, macOS 26 now (research D9, CLAUDE.md "Gates"); `central.yaml` offers `macos-15` or Linux. The gate
  is the green `check.yaml` and `publish.yaml` on the tagged commit, read before the dispatch. A Mac host, because the
  snapshots are published from one; Linux would be the first publication of this tree from Linux, unrehearsed.
- **What it waited for.** pyatiletka's B-20 verdict (2026-11-25) said the kit was not ready, for three of its findings,
  each now done: the dialog's one-button form ([B-97](B-97-dialog-one-button.md), #70, a public signature), the stage
  that differed on Intel ([B-98](B-98-stage-sampled-off-texel-boundaries.md), #71), and the title card's sub line shrunk
  to specks ([B-99](B-99-title-card-sub-wraps.md), #72). The board has yet to run on the snapshot that carries them and
  drop its workarounds.
- **The question.** Release 0.1.0 once the board runs on the snapshot carrying B-97, B-98 and B-99? Who decides: the
  owner, answering B-20's question in pyatiletka too.

- AC: every coordinate answers on `repo1.maven.org`; a consumer with only `mavenCentral()` and `google()` resolves
  `io.github.youndie:toon-core:0.1.0` and builds the README's snippet; the head is 0.1.1.
