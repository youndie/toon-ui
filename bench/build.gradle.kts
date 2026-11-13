// B-05: the frame-cost bench. Not published, not a dependency of anything; it exists to measure
// whether rebuilding wobbly geometry every frame holds 60 fps (research H1).
plugins {
    alias(wip.plugins.kotlinMultiplatform)
    alias(wip.plugins.composeMultiplatform)
    alias(wip.plugins.composeCompiler)
    alias(wip.plugins.androidKotlinMultiplatformLibrary)
    id("io.github.youndie.sborka.kmp")
    id("io.github.youndie.sborka.lint")
}

kotlin {
    // A bench, not a library: sborka's explicit API mode guards a published surface it does not have.
    explicitApi = org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Disabled

    jvm("desktop")

    android {
        namespace = "io.github.youndie.toon.bench"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }

    sourceSets.all { languageSettings.optIn("io.github.youndie.toon.InternalToonApi") }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":toon-core"))
            // `api`: the Android host draws the scene in a foundation Canvas of its own.
            api(wip.compose.runtime)
            api(wip.compose.foundation)
            api(wip.compose.ui)
        }
        getByName("desktopMain").dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

// The desktop half: `./gradlew :bench:runDesktopBench`. Raster on the CPU, offscreen, so it times
// geometry and Skia's path filling, not a GPU or a compositor.
val desktopBench by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Times the B-05 scene per frame on the desktop JVM, both arms interleaved."
    val main = kotlin.jvm("desktop").compilations.getByName("main")
    classpath(main.output.allOutputs, main.runtimeDependencyFiles)
    mainClass.set("io.github.youndie.toon.bench.DesktopBenchKt")
    args(
        rootProject.layout.projectDirectory
            .file("docs/research/b-05/desktop-frames.csv")
            .asFile.path,
    )
}

// B-78: `./gradlew :bench:turnScrollBench`. 1,500 transcript turns scrolled frame by frame at the
// design's 1280 × 800 window, density 2, each frame's render timed on the CPU raster.
val turnScrollBench by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Times 1,500 ToonTranscriptTurns scrolled in a lazy list, frame by frame."
    val main = kotlin.jvm("desktop").compilations.getByName("main")
    classpath(main.output.allOutputs, main.runtimeDependencyFiles)
    mainClass.set("io.github.youndie.toon.bench.TurnScrollBenchKt")
    args(
        rootProject.layout.projectDirectory
            .file("docs/research/b-78/turn-scroll.csv")
            .asFile.path,
    )
}
