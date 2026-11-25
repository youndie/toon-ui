---
id: B-95
title: "The tree can publish toon-core to Maven Central: the pre-flight of the first release"
status: done
priority: P2
size: S
stage: stage-4-product
---

# B-95 — The tree can publish toon-core to Maven Central: the pre-flight of the first release

toon-core is published only as `0.1.0.<run>` to the snapshot repository (B-32, "Not covered: Maven Central"), so a
stranger has to add a repository to take it. Its consumer, pyatiletka's board, has run on it since B-20 there. This item
is everything a first release needs that does not itself publish, checked against how `appframe` 0.3.0 and `viddik` 0.8.0
went out through sborka's `central.yaml`; the release is [B-96](B-96-release-0-1-0-on-central.md), which waits for the
owner.

- **The decision.** `sborka.central=true`, which adds the javadoc jars and the signatures and lets `central.yaml` build
  the tree; `sborka.jvmFloor` 21 rather than 25; Compose's runtime, foundation and ui as `api`. Nothing else in the build
  moves.
- **Rejected.** Keeping the floor at 25: the published classes were class file 69, and a Compose desktop app on Java 21 —
  the floor `appframe` and `viddik` are released with — failed to load them with `UnsupportedClassVersionError`, after a
  multiplatform consumer had compiled against them without a word. A group of its own (`io.github.youndie.toon`): the
  snapshot line, the token's routes and the board all use `io.github.youndie`, as `appframe` does on Central.
- **Not covered.** The release itself (B-96); the README's installation, which says what exists and moves to Central with
  the release.

- AC: a local publication of 0.1.0 holds every coordinate under one group and version, with sources and javadoc jars;
  the POM carries the licence, the scm and the developer; the desktop variant asks for Java 21 and its classes are class
  file 65; the dependencies resolve from Central (and Google for Android) alone; `check` is green; the ABI check fails on
  a broken dump.
- Anchors: `gradle.properties`, `toon-core/build.gradle.kts`, `docs/services/toon-core.md`

## Findings (2026-11-25)

- **The local publication** (`publishToMavenLocal -PVERSION=0.1.0`): six coordinates — `toon-core` and its `-android`,
  `-desktop`, `-iosarm64`, `-iossimulatorarm64`, `-wasm-js`, the six the snapshot token routes — each with sources and
  javadoc jars, all `io.github.youndie` at `0.1.0`. The POM: MIT with the repository's `LICENSE`, scm, developer
  `youndie`. No signatures locally: `central.yaml` signs with the key it holds.
- **The floor.** `org.gradle.jvm.version` 21 on the desktop variants, and the classes are 65 (was 69). sborka compiles
  with `-Xjdk-release` at the floor, so `check` green is also the proof that no API above 21 is used.
- **`api`.** The desktop api variant now lists Compose runtime, foundation and ui; `components-resources` stays
  `implementation`, as no public signature names it (the ABI dump holds no `org/jetbrains/compose/resources`).
- **Dependencies.** Compose Multiplatform 1.12.1 and Kotlin 2.4.20, both on Central; the Android variant adds AndroidX
  from Google. Nothing of ours and nothing from the snapshot repository: viddik and sborka are build plugins only.
- **Fonts and textures.** The seven faces are OFL and each ships its licence inside the artefact
  (`composeResources/…/files/licences/*-OFL.txt`); the four stage tiles are the owner's, under the repository's MIT.
- **The version.** `0.1.0` is free: Central answers 404 for `io/github/youndie/toon-core`, and the repository has no tag.
  It sorts below the snapshots `0.1.0.<run>` already out, so a consumer pinned to one is not offered it by Renovate:
  moving the board to Central is a person's change.
- **The ABI guard fails when it should.** A line taken out of `toon-core/api/desktop/toon-core.api`, and one out of
  `toon-core.klib.api`, each turned `checkKotlinAbi` red; it runs inside `check`, on the macOS host that took the dumps.
- **As a consumer** (a scratch project with the README's snippet, nothing of this build): desktop and wasm compile, and an
  iOS framework links for the simulator (debug) and for a device (release), against `0.1.0.44` from the snapshot
  repository.
