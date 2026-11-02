---
id: toon-core
title: toon-core
type: service
module: toon-core
tech_stack: [Kotlin Multiplatform, Compose Multiplatform]
owner: unassigned
depends_on:
  - sborka (build conventions and the `wip` catalog)
publishes: []
---

# toon-core

## 1. Responsibility

The library module: everything a consumer of toon-ui depends on. Today it is the build and nothing
else. It compiles for Android, iOS (`iosArm64`, `iosSimulatorArm64`), desktop JVM and wasmJs, and its
desktop test suite composes off-screen (B-01). Tokens, geometry, drawing primitives, icons, FX and the
components land here item by item (backlog stages 0–2).

The bundled fonts are here too (B-02): `type/ToonFontFamilies.kt` gives each theme a display and a
body family, internal until the theme provider (B-08) exposes them.

It deliberately does **not** depend on any Material artefact (research D2). It holds no sample app
and no design references; those get modules or directories of their own when their items arrive.
It is not published yet (B-32).

## 2. API contracts

No public API yet. Packages are `io.github.youndie.toon.*` (research D1).

## 2a. Code anchors

| File | What is there |
|---|---|
| `toon-core/build.gradle.kts` | targets, dependencies, the iOS metadata warnings cut |
| `settings.gradle.kts` | plugin repositories and the sborka settings plugin, which brings the `wip` catalog |
| `gradle/libs.versions.toml` | the sborka version and the Android SDK levels; the compiler and Compose come from `wip` |
| `gradle.properties` | the coordinates and licence sborka reads (`sborka.*`) |
| `toon-core/src/desktopTest/kotlin/io/github/youndie/toon/ComposeSmokeTest.kt` | the smoke test the visual suite stands on |
| `toon-core/src/commonMain/kotlin/io/github/youndie/toon/type/ToonFontFamilies.kt` | the two families per theme, and the weights asked of the variable body faces |
| `toon-core/src/desktopTest/resources/engine-goldens` | the engine's geometry and curves as JSON, written by `scripts/engine-goldens.mjs` (B-03); the port is tested against them |
| `toon-core/src/commonMain/composeResources` | the font files (`font`) and their OFL texts (`files/licences`) |

## 3. How it is built

The build is oldge-ui's, cut down to one module. `io.github.youndie.sborka.settings` supplies the
repositories and the `wip` version catalog, so the Kotlin compiler, AGP and Compose Multiplatform
versions are a sborka bump, never a line here. `sborka.kmp` sets the toolchain and compiler flags,
warnings-as-errors included. `sborka.lint` runs ktlint against the portfolio's `.editorconfig`.

The AGP KMP library plugin is declared, unapplied, in the root build. The Compose plugin can only see
AGP's extension types when both are in the same build classloader. oldge-ui and kvadrant-ui each met
this once.

## 4. Dependencies

| Kind | Name | What for |
|---|---|---|
| Library | Compose runtime, foundation, ui (from `wip`) | everything drawn |
| Library | compose-resources | the bundled fonts |
| Build | sborka 0.4.0.111 | conventions and the `wip` catalog, the same build as oldge-ui |
| Test | Compose ui-test, `compose.desktop.currentOs` (desktopTest only) | off-screen composition on the host's Skia |

## 5. Infrastructure and deploy

Nothing is published or deployed. Publishing is B-32.

## 6. Local setup

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew check
```

On this mac, not on the Linux box: the project is not in `mutagen sync list`, and the screenshot
goldens to come are a claim about the mac's rasteriser. `check` also runs the iOS simulator tests,
so Xcode's simulator runtime has to be installed. `local.properties` names the Android SDK; it is
not committed.

## 7. Configuration

None at runtime.

## 8. Quirks

- `compileIosMainKotlinMetadata` and its two siblings compile without warnings-as-errors. CMP 1.12
  brings two lifecycle klibs with one `unique_name` into the shared iOS metadata, and the loader's
  warning about it is not this module's to fix. The per-target iOS compilations keep `-Werror`.
- `./gradlew check` compiles the desktop, wasmJs and iosSimulatorArm64 main sources and runs the
  desktop tests. It does **not** compile `iosArm64` or Android main. Those are
  `:toon-core:compileKotlinIosArm64` and `:toon-core:compileAndroidMain`, run by hand in B-01 (both
  green). A change that breaks only a device target passes `check`.
- `compose.components.resources` is a deprecated accessor in the build script (two `w:` lines on
  every configuration). The plugin's own accessor keeps its version tied to the Compose plugin's,
  and oldge-ui made the same trade.
- The body faces are variable files with a default instance of Light 300. Below Android 8 a
  `wght` setting may not apply (research H7).
