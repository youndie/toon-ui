package io.github.youndie.toon.sample

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/** `./gradlew :sample:run`: the kit and the Screens page's screens in one window. */
fun main() =
    application {
        Window(
            ::exitApplication,
            rememberWindowState(size = DpSize(1280.dp, 900.dp)),
            title = "toon-ui",
        ) { ToonUiSample() }
    }
