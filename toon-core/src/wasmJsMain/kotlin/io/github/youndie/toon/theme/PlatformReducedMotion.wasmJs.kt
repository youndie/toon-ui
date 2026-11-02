package io.github.youndie.toon.theme

import androidx.compose.runtime.Composable
import kotlinx.browser.window

@Composable
internal actual fun platformReducedMotion(): Boolean = window.matchMedia("(prefers-reduced-motion: reduce)").matches
