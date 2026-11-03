---
id: sample
title: sample
type: service
module: sample
tech_stack: [Kotlin Multiplatform, Compose Multiplatform]
owner: unassigned
depends_on:
  - toon-core
publishes: []
---

# sample

## 1. Responsibility

Two pages under one header (B-32), on the desktop and in the browser: **Kit**, every component of
the library on one sheet, grouped as the design's pages group them, with the overlays (dialog, bottom
sheet, drawer) opened from it; and **Screens**, the Screens page's three phone screens running live
(B-29). The header switches the page, the theme and reduce motion. In the Screens page, sign in leads
to the list; a card swiped away shows the snackbar, whose Undo puts it back; the navigation bar's Profile
opens the settings, whose "Look" switches the theme, and Sign out goes back to the start. It is the
check that the components compose into an app and behave there, as the parity fixtures check that
they compose into the page's pictures. It is not published, and nothing depends on it.

## 2. API contracts

None. It uses toon-core's public API only, `ToonTypeSpec.textStyle` for the screens' own text.

## 2a. Code anchors

| File | What is there |
|---|---|
| `sample/src/commonMain/kotlin/io/github/youndie/toon/sample/ToonUiSample.kt` | the header, the kit sheet and the overlays (B-32) |
| `sample/src/commonMain/kotlin/io/github/youndie/toon/sample/SampleApp.kt` | the three screens at the page's places, their state, and the theme switch |
| `sample/src/desktopMain/kotlin/io/github/youndie/toon/sample/Main.kt` | the window |
| `sample/src/wasmJsMain/kotlin/io/github/youndie/toon/sample/Main.kt` | the browser entry point, the GitHub Pages site |
| `.github/workflows/pages.yaml` | builds the wasm bundle on every pull request, deploys it from `main` |
| `sample/src/desktopTest/kotlin/io/github/youndie/toon/sample/SampleAppTest.kt` | sign in, dismiss and undo, settings, sign out, driven through the UI |

## 3. How it is built

A KMP module with a desktop JVM target, Compose's desktop application plugin, and a wasmJs browser
executable (`sample.js`), sborka's explicit API mode switched off, since it is not a library. The
code is in `commonMain`; each target has only its entry point. `check` runs its test with the rest.

## 4. Dependencies

| Kind | Name | What for |
|---|---|---|
| Module | toon-core | the components, the stage and the type scale |

## 5. Infrastructure and deploy

The wasm bundle is GitHub Pages: `pages.yaml` runs `:sample:wasmJsBrowserDistribution` and deploys
`sample/build/dist/wasmJs/productionExecutable` from `main` to https://youndie.github.io/toon-ui/.

## 6. Local setup

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :sample:run                          # the desktop window
./gradlew :sample:wasmJsBrowserDistribution    # the browser bundle, then serve its directory
```

## 7. Configuration

None.

## 8. Quirks

- The screens in the app differ from the parity fixtures (`toon-core`'s `ScreensFixtures.kt`) in
  two ways: the cards and the FAB make their entrances, and the screens' own text is set with
  `ToonTypeSpec.textStyle`, in the theme's faces, where the page sets Shrikhand and Figtree in every
  theme. The fixtures follow the page; the app follows the library.
- The window is 1280 × 900. The Screens page keeps the phone's 390 × 844 and scrolls when the window
  is shorter.
- In the browser the fonts arrive a moment after the first frame (compose resources load
  asynchronously on wasm), so the first frame is set in the fallback face.
- The header's switches cover the whole app; the Screens page's own "Look" switches only the phone.
