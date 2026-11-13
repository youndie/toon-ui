package io.github.youndie.toon.component

import androidx.compose.ui.input.pointer.PointerIcon

/**
 * The column-resize pointer the design's handle asks for (`cursor: col-resize`): the platform's
 * left-right arrow on a desktop, on Android and in a browser; iOS keeps its default pointer.
 */
internal expect val toonColumnResizeIcon: PointerIcon
