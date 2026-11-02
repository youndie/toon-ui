// B-05, device half: the bench scene in a release build on the attached phone, frame times from the
// platform's FrameMetrics. `./gradlew :bench-android:installRelease`, then the activity runs itself
// and logs one line per frame under the tag ToonBench.
plugins {
    alias(wip.plugins.androidApplication)
    // No `org.jetbrains.kotlin.android`: since AGP 9 the Android plugin brings Kotlin itself.
    alias(wip.plugins.composeCompiler)
    id("io.github.youndie.sborka.base")
    id("io.github.youndie.sborka.lint")
}

android {
    namespace = "io.github.youndie.toon.bench.android"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "io.github.youndie.toon.bench"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "0.1"
    }

    buildFeatures { compose = true }

    // Release, because a debuggable Compose app runs interpreted code paths that make any frame time
    // meaningless. Signed with the debug key: it is installed by hand onto one phone, never shipped.
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(project(":bench"))
    implementation(libs.androidx.activity.compose)
}
