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

The Screens page's three phone screens running live on the desktop (B-29): sign in leads to the
list; a card swiped away shows the snackbar, whose Undo puts it back; the navigation bar's Profile
opens the settings, whose "Look" switches the theme, and Sign out goes back to the start. It is the
check that the components compose into an app and behave there, as the parity fixtures check that
they compose into the page's pictures. It is not published, and nothing depends on it.

## 2. API contracts

None. It uses toon-core's public API only, `ToonTypeSpec.textStyle` for the screens' own text.

## 2a. Code anchors

| File | What is there |
|---|---|
| `sample/src/desktopMain/kotlin/io/github/youndie/toon/sample/SampleApp.kt` | the three screens at the page's places, their state, and the theme switch |
| `sample/src/desktopMain/kotlin/io/github/youndie/toon/sample/Main.kt` | the window |
| `sample/src/desktopTest/kotlin/io/github/youndie/toon/sample/SampleAppTest.kt` | sign in, dismiss and undo, settings, sign out, driven through the UI |

## 3. How it is built

A KMP module with one desktop JVM target and Compose's desktop application plugin, sborka's explicit
API mode switched off, since it is not a library. `check` runs its test with the rest.

## 4. Dependencies

| Kind | Name | What for |
|---|---|---|
| Module | toon-core | the components, the stage and the type scale |

## 5. Infrastructure and deploy

Nothing deployed.

## 6. Local setup

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :sample:run
```

## 7. Configuration

None.

## 8. Quirks

- The screens in the app differ from the parity fixtures (`toon-core`'s `ScreensFixtures.kt`) in
  two ways: the cards and the FAB make their entrances, and the screens' own text is set with
  `ToonTypeSpec.textStyle`, in the theme's faces, where the page sets Shrikhand and Figtree in every
  theme. The fixtures follow the page; the app follows the library.
- The window is phone-sized, 390 × 880; on a short screen the bottom of the phone is cut off.
