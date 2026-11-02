package io.github.youndie.toon.theme

import androidx.compose.runtime.Composable

/**
 * The platform's own reduce-motion setting, read once per composition of the theme: Android's
 * animator duration scale at 0, iOS's Reduce Motion, the browser's `prefers-reduced-motion`. The
 * desktop JVM has no such setting, so it is off there unless [ToonTheme] is told otherwise.
 */
@Composable
internal expect fun platformReducedMotion(): Boolean
