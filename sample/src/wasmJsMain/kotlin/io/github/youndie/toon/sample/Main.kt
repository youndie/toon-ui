package io.github.youndie.toon.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import io.github.youndie.toon.fx.ToonFxHost
import io.github.youndie.toon.theme.ToonTheme
import kotlinx.browser.document
import kotlinx.browser.window

/**
 * The sample in a browser, the GitHub Pages site (B-32): the same `ToonUiSample` as the desktop window.
 * `?scene=app` opens the phone screens alone, without the sheet around them, for a page to embed.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val embedded = window.location.search.contains("scene=app")
    ComposeViewport(document.body!!) {
        if (embedded) EmbeddedApp() else ToonUiSample()
    }
}

/** The phone screens with the FX host around them, so dust lands in the theme the settings picked. */
@Composable
private fun EmbeddedApp() {
    var look by remember { mutableIntStateOf(0) }
    var calm by remember { mutableStateOf(false) }
    ToonTheme(THEMES[look], reduceMotion = calm) {
        ToonFxHost(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize(), Alignment.TopCenter) {
                SampleApp(look, { look = it }, calm, { calm = it })
            }
        }
    }
}
