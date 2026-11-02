package io.github.youndie.toon.theme

import androidx.compose.runtime.Composable
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@Composable
internal actual fun platformReducedMotion(): Boolean = UIAccessibilityIsReduceMotionEnabled()
