package io.github.youndie.toon.sample

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/** `./gradlew :sample:run`: the sample in a phone-sized window. */
fun main() =
    application {
        Window(
            ::exitApplication,
            rememberWindowState(size = DpSize(390.dp, 880.dp)),
            title = "toon-ui sample",
        ) { SampleApp() }
    }
