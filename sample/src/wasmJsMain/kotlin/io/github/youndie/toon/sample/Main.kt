package io.github.youndie.toon.sample

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

/** The sample in a browser, the GitHub Pages site (B-32): the same `ToonUiSample` as the desktop window. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) { ToonUiSample() }
}
