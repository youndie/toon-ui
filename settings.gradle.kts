pluginManagement {
    repositories {
        // AGP is published only here. Filtered, like every third-party repository in this file: an
        // unfiltered one takes part in resolving every plugin, and the day its host is unreachable
        // Gradle disables it and fails plugins that live elsewhere.
        google {
            content {
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                includeGroupAndSubgroups("androidx")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // The build conventions are not on the plugin portal. Spelled out by hand because
        // `pluginManagement` is evaluated before any settings plugin — including sborka's own — applies.
        maven("https://reposilite.kotlin.website/snapshots") {
            content { includeGroupAndSubgroups("io.github.youndie") }
        }
    }
}

plugins {
    // Repositories with their filters, the `wip` catalog (the compiler, KSP, AGP and Compose
    // Multiplatform come from it, so a compiler bump is a sborka bump), and the `.editorconfig` check.
    id("io.github.youndie.sborka.settings") version "0.4.0.111"
}

rootProject.name = "toon-ui"

include(":toon-core")
include(":bench")
include(":bench-android")
include(":sample")
