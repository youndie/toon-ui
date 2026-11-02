plugins {
    alias(wip.plugins.kotlinMultiplatform)
    alias(wip.plugins.composeMultiplatform)
    alias(wip.plugins.composeCompiler)
    alias(wip.plugins.androidKotlinMultiplatformLibrary)
    alias(wip.plugins.ksp)
    alias(libs.plugins.viddik)
    id("io.github.youndie.sborka.kmp")
    id("io.github.youndie.sborka.lint")
}

// Four targets (research D9). Android and iOS lead the brief; desktop is where the screenshot and
// parity gate runs, so a green `check` says nothing visual about the other three.
kotlin {
    jvm("desktop")

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        // Required by the Compose plugin's own check (CMP-4906): without an executable binary the
        // Skiko runtime cannot be bundled for the target's tests, and `build` fails on it.
        binaries.executable()
    }

    iosArm64()
    iosSimulatorArm64()

    android {
        // Off by default in AGP's KMP plugin, and with it off the AAR ships without the bundled
        // fonts B-02 adds, green (kvadrant-ui B-37).
        androidResources { enable = true }

        namespace = "io.github.youndie.toon"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }

    // The geometry is public behind an opt-in for the bench module (B-05); this module itself opts in.
    sourceSets.all { languageSettings.optIn("io.github.youndie.toon.InternalToonApi") }

    // No Material artefact, ever (research D2).
    sourceSets {
        commonMain.dependencies {
            implementation(wip.compose.runtime)
            implementation(wip.compose.foundation)
            implementation(wip.compose.ui)
            // The bundled fonts (B-02). Not in `wip`, and the plugin's accessor is the one place its
            // version follows the Compose plugin's own (oldge-ui does the same).
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        getByName("desktopTest").dependencies {
            // The host's skia. Never in a published source set: the POM would pin the machine
            // that published.
            implementation(compose.desktop.currentOs)
            implementation(wip.compose.ui.test)
            // EngineGoldensTest reads the engine's JSON (B-04); the tree API needs no plugin.
            implementation(wip.kotlinx.serialization.json)
        }
    }
}

// B-06. Goldens are verified by `check`; design parity is a report (`viddikDesignParity`) against
// the PNGs scripts/design-references.mjs renders from the engine in Chrome. The tolerances are
// viddik's defaults, written here to say they were looked at; the floor they are read against is
// measured in B-06 (research D3) and is the number a component's parity is compared with.
viddik {
    verifyOnCheck.set(true)
    shards.set(1)
    designTolerancePercent.set(5.0)
    designChannelTolerance.set(16)
    designStrict.set(false)
}

// A test that reads the golden directory must declare it, or Gradle leaves the test UP-TO-DATE over
// a changed set and reports the last run's verdict (oldge-ui and kvadrant-ui both learnt this).
tasks.withType<Test>().configureEach {
    inputs
        .dir(layout.projectDirectory.dir("src/desktopTest/snapshots"))
        .withPropertyName("viddikSnapshots")
        .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE)
}

compose.resources {
    packageOfResClass = "io.github.youndie.toon.resources"
}

// The shared iOS metadata compilation, and only it, without warnings-as-errors. CMP 1.12's own graph
// puts two lifecycle klibs with one `unique_name` on it, and the KLIB loader's warning about that is
// not something this module can fix (oldge-ui and viddik made the same cut). The per-target iOS
// compilations keep -Werror, so a warning in this library's own code still fails.
tasks
    .matching {
        it.name in
            setOf("compileIosMainKotlinMetadata", "compileAppleMainKotlinMetadata", "compileNativeMainKotlinMetadata")
    }.configureEach {
        (this as org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>).compilerOptions.allWarningsAsErrors.set(
            false,
        )
    }
