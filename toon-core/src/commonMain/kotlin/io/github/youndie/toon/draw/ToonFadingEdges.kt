package io.github.youndie.toon.draw

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// B-92: scrolled content fades out at the edges it has scrolled past, instead of being cut hard. The
// design draws no scrolled area longer than its frame, so this is the kit's own (D13). An alpha mask
// over the content's own layer: two DstIn gradients, each as deep as the content has moved past its
// edge, up to the fade's length, so the fade comes in with the scroll and never pops.

/**
 * Fades this vertically scrolled box out at the top once [state] has scrolled down, and at the bottom
 * while there is more below, instead of cutting the content hard at its edges. Each fade is [length]
 * deep (at most half the box) and grows with the distance scrolled past its edge: a column moved by a
 * quarter of [length] is faded by a quarter, so the fade comes in with the scroll, never at once. With
 * nothing to fade, while the content fits, it draws the content as it is.
 *
 * Put it before the scroll, and the content's padding after it, so the first and last rows stand clear
 * of the fade at rest:
 *
 * ```
 * Column(
 *     Modifier.fillMaxSize()
 *         .toonFadingEdges(scroll, length = 24.dp, slack = 12.dp)
 *         .verticalScroll(scroll)
 *         .padding(horizontal = 16.dp, vertical = 24.dp),
 * )
 * ```
 *
 * The fade draws the content in a layer of its own, and a layer clips: ink the content draws past the
 * box's sides would be cut. [slack] widens the layer by that much at each side, and fades it with the
 * rest; give it as much as the content's ink reaches past the column, as a switch's wobble or a
 * button's hard shadow does at its edge. The box's size and its content's place do not change.
 *
 * It reads the state when it draws, so a scroll redraws the box and does not recompose it. A column
 * that scrolls reversed (`reverseScrolling`) is not read as such: the fades would stand at the wrong
 * edges.
 */
public fun Modifier.toonFadingEdges(
    state: ScrollState,
    length: Dp,
    slack: Dp = 0.dp,
): Modifier = this then FadingEdgesElement(state, length, slack)

/**
 * Fades this vertical lazy list out at the edges it has scrolled past, as the other `toonFadingEdges`
 * does for a column; a list laid out reversed (`reverseLayout`) is faded at the edges it shows. A lazy
 * list knows the distance scrolled only within its first item, and to the end only within its last: a
 * fade is whole once the list is past either, so an end item shorter than [length] lets it in a little
 * faster than the scroll.
 */
public fun Modifier.toonFadingEdges(
    state: LazyListState,
    length: Dp,
    slack: Dp = 0.dp,
): Modifier = this then FadingEdgesElement(state, length, slack)

/** How far, in pixels, the content stands past the box's top and bottom edges. */
internal fun ScrollableState.scrolledPast(): Pair<Float, Float> =
    when (this) {
        is ScrollState -> {
            value.toFloat() to (maxValue - value).toFloat()
        }

        is LazyListState -> {
            val info = layoutInfo
            val back =
                when {
                    !canScrollBackward -> 0f
                    firstVisibleItemIndex > 0 -> Float.POSITIVE_INFINITY
                    else -> firstVisibleItemScrollOffset.toFloat()
                }
            val last = info.visibleItemsInfo.lastOrNull()
            val forward =
                when {
                    !canScrollForward || last == null -> {
                        0f
                    }

                    last.index < info.totalItemsCount - 1 -> {
                        Float.POSITIVE_INFINITY
                    }

                    // Item offsets start after the content padding; the end lies past the padding after.
                    else -> {
                        (last.offset + last.size + info.afterContentPadding - info.viewportEndOffset)
                            .coerceAtLeast(
                                0,
                            ).toFloat()
                    }
                }
            if (info.reverseLayout) forward to back else back to forward
        }

        else -> {
            error("toonFadingEdges reads a ScrollState or a LazyListState")
        }
    }

/** A fade's strength, 0 to 1: the share of its length the content has moved past its edge. */
internal fun fadeStrength(
    pastPx: Float,
    fadePx: Float,
): Float = if (fadePx <= 0f) 0f else (pastPx / fadePx).coerceIn(0f, 1f)

private data class FadingEdgesElement(
    val state: ScrollableState,
    val length: Dp,
    val slack: Dp,
) : ModifierNodeElement<FadingEdgesNode>() {
    override fun create() = FadingEdgesNode(state, length, slack)

    override fun update(node: FadingEdgesNode) {
        node.state = state
        node.length = length
        node.slack = slack
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "toonFadingEdges"
        properties["state"] = state
        properties["length"] = length
        properties["slack"] = slack
    }
}

private class FadingEdgesNode(
    var state: ScrollableState,
    var length: Dp,
    var slack: Dp,
) : Modifier.Node(),
    DrawModifierNode {
    private val layer = Paint()

    override fun ContentDrawScope.draw() {
        val fade = length.toPx().coerceIn(0f, size.height / 2)
        val (back, forward) = state.scrolledPast()
        val top = fadeStrength(back, fade)
        val bottom = fadeStrength(forward, fade)
        if (top <= 0f && bottom <= 0f) {
            drawContent()
            return
        }
        val side = slack.toPx().coerceAtLeast(0f)
        drawIntoCanvas { it.saveLayer(Rect(-side, 0f, size.width + side, size.height), layer) }
        drawContent()
        if (top > 0f) mask(top, -side, 0f, fade, fromEdge = true)
        if (bottom > 0f) mask(bottom, -side, size.height - fade, fade, fromEdge = false)
        drawIntoCanvas { it.restore() }
    }

    /**
     * Keeps the content's alpha by a gradient [fade] tall from [y]: `1 - strength` at the box's edge,
     * whole at the far end of the band. [fromEdge] says the band's top is the edge.
     */
    private fun DrawScope.mask(
        strength: Float,
        x: Float,
        y: Float,
        fade: Float,
        fromEdge: Boolean,
    ) {
        // A mask's colour is only its alpha: DstIn keeps the content by it and ignores the rest.
        val edge = Color.Black.copy(alpha = 1f - strength)
        val colors = if (fromEdge) listOf(edge, Color.Black) else listOf(Color.Black, edge)
        drawRect(
            Brush.verticalGradient(colors, startY = y, endY = y + fade),
            topLeft = Offset(x, y),
            size = Size(size.width - 2 * x, fade),
            blendMode = BlendMode.DstIn,
        )
    }
}
