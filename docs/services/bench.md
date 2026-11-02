---
id: bench
title: bench
type: service
module: bench
tech_stack: [Kotlin Multiplatform, Compose Multiplatform, Android]
owner: unassigned
depends_on:
  - toon-core
publishes: []
---

# bench

## 1. Responsibility

Measures what drawing toon-ui costs, so a performance claim is a number. It holds one scene today:
B-05's ten-card cascade with dust, in three arms (rebuild geometry each frame, cache it, and a
20 ms busy-wait control). It is not published, and nothing depends on it.

## 2. API contracts

None. It reaches toon-core's geometry through `@InternalToonApi`, an error-level opt-in.

## 2a. Code anchors

| File | What is there |
|---|---|
| `bench/src/commonMain/kotlin/io/github/youndie/toon/bench/FrameCostScene.kt` | the scene and its arms, ported from the engine's `card`, `dust` and `cardAppear` |
| `bench/src/desktopMain/kotlin/io/github/youndie/toon/bench/DesktopBench.kt` | the desktop half: CPU raster into a Pixel 6a-sized bitmap, per-frame draw time to CSV |
| `bench-android/src/main/kotlin/io/github/youndie/toon/bench/android/BenchActivity.kt` | the device half: runs the protocol, logs FrameMetrics per frame under the tag `ToonBench` |
| `docs/research/b-05` | B-05's raw logs and `summarise.py` |

## 3. How it is built

`bench` is a KMP module (desktop JVM and an Android library), with sborka's explicit API mode
switched off, since it is not a library. `bench-android` is an application that only hosts the
scene. Its release build is signed with the debug key: a debuggable Compose app makes frame times
meaningless, and this one is installed by hand onto one phone.

## 4. Dependencies

| Kind | Name | What for |
|---|---|---|
| Module | toon-core | the geometry |
| Library | androidx.activity:activity-compose 1.13.0 | the Android host |

## 5. Infrastructure and deploy

Nothing deployed.

## 6. Local setup

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :bench:desktopBench                       # writes docs/research/b-05/desktop-frames.csv
./gradlew :bench-android:assembleRelease
adb install -r bench-android/build/outputs/apk/release/bench-android-release.apk
adb logcat -c && adb shell am start -n io.github.youndie.toon.bench/io.github.youndie.toon.bench.android.BenchActivity
adb logcat -d -s ToonBench:I > docs/research/b-05/<name>.txt   # once the log says "end", about a minute
python3 docs/research/b-05/summarise.py
```

A worktree needs a `local.properties` with `sdk.dir`; it is not committed.

## 7. Configuration

None.

## 8. Quirks

- A frame's `scene_ns` is the latest draw call's, logged from the FrameMetrics callback, which can
  lag by a frame. Read it as a distribution per arm, not frame by frame.
- Missed vsync slots under-report jank: the pipeline buffers, and the 20 ms control still filled
  78 % of slots. `TOTAL_DURATION` is the measure the bar is set on.
