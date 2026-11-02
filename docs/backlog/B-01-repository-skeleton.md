---
id: B-01
title: "Repository skeleton: sborka conventions, toon-core on four targets, both gates"
status: wip
priority: P0
size: M
stage: stage-0-risks
---

# B-01 — Repository skeleton: sborka conventions, toon-core on four targets, both gates

Nothing builds yet. The repository holds the vendored design project and these documents only.

- **The decision and its reason.** Lay it out like oldge-ui (research §1.7). Use sborka's settings,
  KMP and lint conventions, group `io.github.youndie`, one library module `toon-core`, packages
  `io.github.youndie.toon.*` (D1), and targets Android, iOS (`iosArm64`, `iosSimulatorArm64`),
  desktop JVM and wasmJs (D9). No Material dependency (D2). `./gradlew check` and `make check` are
  the two gates, and CI runs `make check` on Linux and `./gradlew check` on macOS.
- Rejected: copying `reference/design-system/compose` in as a starting point. It is a sketch with known divergences
  and a helper that reads as uncompilable (research §1.5, H5).
- Not covered: publishing (B-32), any component.
- Writes `docs/services/toon-core.md` once the module exists, and moves the services layer out of
  "none yet" in `docs/README.md`.

- AC: `./gradlew check` is green on the mac with an empty `toon-core` that compiles for all four
  targets; `make check` is green; a smoke test in `desktopTest` runs.
- Anchors: `youndie/oldge-ui@618cef8b923d7e0e501752240e6a727eed51e48c!/settings.gradle.kts`, `docs/research/research-architecture.md`.
