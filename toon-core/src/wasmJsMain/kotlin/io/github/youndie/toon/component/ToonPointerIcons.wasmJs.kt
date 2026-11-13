package io.github.youndie.toon.component

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.fromKeyword

// The browser's own CSS cursor, the one the design's handle names.
@OptIn(ExperimentalComposeUiApi::class)
internal actual val toonColumnResizeIcon: PointerIcon = PointerIcon.fromKeyword("col-resize")
