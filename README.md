# toon-ui

A Compose Multiplatform UI kit in the look of golden-age theatrical cartoons: a thick ink outline of
variable weight, flat cel fills, hard offset shadows, and motion that squashes, overshoots and lands
in a puff of dust. Mobile first (Android, iOS), with desktop and wasm alongside. No Material
dependency.

<table>
  <tr>
    <td align="center"><img src="toon-core/src/desktopTest/snapshots/Screens_List_InkGouacheLight.png" width="260" alt="The plan list in the Ink &amp; Gouache theme"></td>
    <td align="center"><img src="toon-core/src/desktopTest/snapshots/Screens_List_InkGouacheNight.png" width="260" alt="The plan list in the Night scene theme"></td>
    <td align="center"><img src="toon-core/src/desktopTest/snapshots/Screens_List_BrightCel.png" width="260" alt="The plan list in the Bright Cel theme"></td>
  </tr>
  <tr>
    <td align="center"><b>Ink &amp; Gouache</b></td>
    <td align="center"><b>Night scene</b></td>
    <td align="center"><b>Bright Cel</b></td>
  </tr>
</table>

**[Try it in the browser →](https://youndie.github.io/toon-ui/)** Every component on one sheet,
under the three themes and reduce motion, and the three demo screens beside it.

All 27 components of the design are built, and a list of cards besides. Each is held to the design
twice: its geometry, frame by frame, against the design's own drawing engine run in Node, and its
pixels against references rendered from the design in Chrome.

Every image here is a screenshot golden: the picture `./gradlew check` compares the code's render
against on every build. So it is what the library draws, not a mock-up.

## Components

<table>
  <tr>
    <td><img src="toon-core/src/desktopTest/snapshots/Dialog_Destructive_InkGouacheNight.png" width="260" alt="A dialog"></td>
    <td><img src="toon-core/src/desktopTest/snapshots/BottomSheet_Open_BrightCel.png" width="260" alt="A bottom sheet"></td>
    <td><img src="toon-core/src/desktopTest/snapshots/NavigationDrawer_Open_InkGouacheNight.png" width="260" alt="A navigation drawer"></td>
  </tr>
  <tr>
    <td><img src="toon-core/src/desktopTest/snapshots/TitleCard_Splash_InkGouacheLight.png" width="260" alt="A title card"></td>
    <td colspan="2"><img src="toon-core/src/desktopTest/snapshots/Card_Dismiss_300ms_InkGouacheLight.png" width="420" alt="A card thrown out of a list, the one below closing up"><br><img src="toon-core/src/desktopTest/snapshots/Menu_Open_InkGouacheLight.png" width="216" alt="A menu"></td>
  </tr>
</table>

[docs/components.md](docs/components.md) lists all of them: the design file, the engine's
renderer, and the goldens that show each.

## Motion

Curves are functions of elapsed time, ported from the design's engine, so the tests read them at
the design's own frame timestamps. A dialog opening, at 0, 60, 120, 200 and 400 ms:

<table>
  <tr>
    <td><img src="toon-core/src/desktopTest/snapshots/Dialog_Open_0ms_InkGouacheLight.png" width="150" alt="0 ms"></td>
    <td><img src="toon-core/src/desktopTest/snapshots/Dialog_Open_60ms_InkGouacheLight.png" width="150" alt="60 ms"></td>
    <td><img src="toon-core/src/desktopTest/snapshots/Dialog_Open_120ms_InkGouacheLight.png" width="150" alt="120 ms"></td>
    <td><img src="toon-core/src/desktopTest/snapshots/Dialog_Open_200ms_InkGouacheLight.png" width="150" alt="200 ms"></td>
    <td><img src="toon-core/src/desktopTest/snapshots/Dialog_Open_400ms_InkGouacheLight.png" width="150" alt="400 ms"></td>
  </tr>
</table>

Reduce motion follows the design's word for each animation — a fade, a static spinner, or nothing —
rather than switching everything off.

## Using it

```kotlin
repositories {
    maven("https://reposilite.kotlin.website/snapshots") {
        mavenContent { includeGroupAndSubgroups("io.github.youndie") }
    }
}

dependencies {
    implementation("io.github.youndie:toon-core:0.1.0.<build>")
}
```

Every build of `main` publishes a version: `<head>.<build>`, where the head is the next release —
`0.1.0.<build>` now. The latest is the last `<version>` in
[maven-metadata.xml](https://reposilite.kotlin.website/snapshots/io/github/youndie/toon-core/maven-metadata.xml).
There is no release yet.

```kotlin
ToonTheme(ToonThemes.InkGouacheLight) {
    ToonStage {
        ToonFxHost {
            ToonButton("Add plan", onClick = {})
        }
    }
}
```

`ToonStage` paints the theme's backdrop and texture; `ToonFxHost` draws the dust, stars and speed
lines the components fire.

Targets: desktop (JVM), Android, iOS (arm64 and the arm64 simulator), and wasm.

The sample app, the kit and the demo screens in one window: `./gradlew :sample:run`.

## Where things are

[docs/](docs/README.md) holds the documentation. Start with the
[architecture research](docs/research/research-architecture.md), then the [backlog](backlog.md).
The design itself is vendored in [reference/design-system/](reference/design-system/).

## Licence

- **The code** is under the MIT License ([LICENSE](LICENSE)).
- **The bundled fonts** are under the SIL Open Font License 1.1. Their full texts are in
  `toon-core/src/commonMain/composeResources/files/licences/`.
