package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonInsets
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonSegments
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.sizeDp
import io.github.youndie.toon.interaction.hoverLifted
import io.github.youndie.toon.interaction.rememberToonHoverState
import io.github.youndie.toon.interaction.toonHoverable
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// A port of the desktop extension's `scrollThumb` and of `ToonScrollbar.dc.html` (B-72, research
// §1.8): an ink thumb over the visible span of a scroll state, bound to Compose's states instead of a
// DOM node. The thumb keeps 8 dp inside its scroll area — at the top and the bottom, as the design
// insets its track, and at the end, so an undecorated window's 8 dp resize strip never covers it.

/**
 * The design's scrollbar for a vertically scrolled [state]: lay it over the end edge of the scrolled
 * area, as tall as it. It is 20 dp wide — the design's 14 dp bar and 6 dp to keep the thumb 8 dp from
 * the edge — and draws nothing, nor takes a press, while the content fits.
 *
 * A mouse over it widens and lifts the thumb; dragging the thumb scrolls with it; a press beside the
 * thumb pages by 90 % of the view. It takes no focus: the keys scroll the area itself.
 */
@Composable
public fun ToonScrollbar(
    state: ScrollState,
    modifier: Modifier = Modifier,
) {
    val span = remember(state) { toonScrollSpan(state) }
    ToonScrollbarImpl(span, modifier)
}

/**
 * The design's scrollbar for a vertical lazy list's [state], as the other `ToonScrollbar`. A lazy list
 * knows only the items it laid out, so the content's length is read from their average size: exact for
 * items of one size, an estimate otherwise. The thumb stands at the items above the view, and the share
 * of the first one scrolled past, over all of them; a drag puts the view where the thumb is held, so the
 * thumb stays under the pointer over items of any sizes.
 */
@Composable
public fun ToonScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
) {
    val span = remember(state) { toonScrollSpan(state) }
    ToonScrollbarImpl(span, modifier)
}

/** Where a scrolled area stands, in pixels: what the design's `sync` reads off its DOM node. */
@Stable
internal interface ToonScrollSpan {
    /** The content can scroll: the design hides the bar when it fits. */
    val scrolls: Boolean

    /** The content's length, the view's, and the view's top in the content (`scrollHeight`, `clientHeight`, `scrollTop`). */
    val contentPx: Float
    val viewportPx: Float
    val offsetPx: Float

    /** Scrolls by [px] at once, as the design sets `scrollTop`. */
    fun scrollBy(px: Float)

    /** Scrolls so the view's top stands at [fraction] of the content, as [fractions] reads it back. */
    suspend fun scrollTo(fraction: Float)
}

/** What the bar reads off a [ScrollState]. */
internal fun toonScrollSpan(state: ScrollState): ToonScrollSpan = ScrollStateSpan(state)

/** What the bar reads off a [LazyListState]: the content's length estimated from the items laid out. */
internal fun toonScrollSpan(state: LazyListState): ToonScrollSpan = LazyListSpan(state)

private class ScrollStateSpan(
    private val state: ScrollState,
) : ToonScrollSpan {
    override val scrolls get() = state.maxValue > 0
    override val contentPx get() = (state.maxValue + state.viewportSize).toFloat()
    override val viewportPx get() = state.viewportSize.toFloat()
    override val offsetPx get() = state.value.toFloat()

    override fun scrollBy(px: Float) {
        state.dispatchRawDelta(px)
    }

    override suspend fun scrollTo(fraction: Float) {
        state.scrollTo((fraction * contentPx).roundToInt())
    }
}

private class LazyListSpan(
    private val state: LazyListState,
) : ToonScrollSpan {
    override val scrolls get() = state.canScrollForward || state.canScrollBackward

    /** One item and the spacing after it, averaged over the items laid out. */
    private fun line(): Float {
        val items = state.layoutInfo.visibleItemsInfo
        if (items.isEmpty()) return 0f
        val first = items.first()
        val last = items.last()
        return (last.offset + last.size - first.offset + state.layoutInfo.mainAxisItemSpacing).toFloat() / items.size
    }

    override val contentPx: Float
        get() {
            val info = state.layoutInfo
            return line() * info.totalItemsCount - info.mainAxisItemSpacing + info.beforeContentPadding +
                info.afterContentPadding
        }
    override val viewportPx get() =
        state.layoutInfo.viewportSize.height
            .toFloat()

    /**
     * The items above the view, and the share of the first one it has scrolled past by that item's own
     * size, at the average size: a function of the position alone, whichever items are laid out (B-93).
     * The state's first item and its offset are counted from the content padding's end, as the items
     * are, and the view from the padding's start.
     */
    override val offsetPx: Float
        get() {
            val i = state.firstVisibleItemIndex
            val size = sizeOf(i) ?: return 0f
            return (i + state.firstVisibleItemScrollOffset / size) * line()
        }

    /** The laid-out item [index] and the spacing after it, or null when it is not laid out. */
    private fun sizeOf(index: Int): Float? {
        val info = state.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return null
        return (item.size + info.mainAxisItemSpacing).toFloat().takeIf { it > 0f }
    }

    override fun scrollBy(px: Float) {
        state.dispatchRawDelta(px)
    }

    /**
     * [offsetPx] backwards: the fraction is an item and a share of it, and the list goes there by
     * position, so the thumb then stands where it was asked to, however the items differ in size.
     */
    override suspend fun scrollTo(fraction: Float) {
        val n = state.layoutInfo.totalItemsCount
        val l = line()
        if (n == 0 || l <= 0f) return
        val t = (fraction * contentPx / l).coerceIn(0f, n.toFloat())
        val i = min(t.toInt(), n - 1)
        val share = t - i
        // Into the item by its own size. One not laid out yet is reached at its top first: an offset
        // by the average could pass a short item and leave the view in the next one.
        val known = sizeOf(i)
        state.scrollToItem(i, known?.let { (share * it).toInt() } ?: 0)
        if (known == null) sizeOf(i)?.let { state.scrollToItem(i, (share * it).toInt()) }
    }
}

/** The visible span as the design's `a` and `b`: the view's top and bottom over the content's length. */
internal fun ToonScrollSpan.fractions(): Pair<Float, Float> {
    val c = max(1f, contentPx)
    return offsetPx / c to (offsetPx + viewportPx) / c
}

@Composable
private fun ToonScrollbarImpl(
    span: ToonScrollSpan,
    modifier: Modifier,
) {
    val tokens = Toon.tokens
    val hover = rememberToonHoverState()
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    // Read in composition only when it changes: the scroll itself is read in the draw.
    val scrolls by remember(span) { derivedStateOf { span.scrolls } }
    Box(modifier.fillMaxHeight().width(BOX_W.dp)) {
        if (!scrolls) return@Box
        Box(
            Modifier
                .fillMaxHeight()
                .padding(top = INSET.dp, bottom = INSET.dp, end = (BOX_W - BAR_W).dp)
                .width(BAR_W.dp)
                .toonHoverable(hover)
                .pointerInput(span) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val h = toonScrollTrack(size.height / density)
                        val (a, b) = span.fractions()
                        val (top, bottom) = toonScrollThumbSpan(h, a, b)
                        val y = down.position.y / density
                        down.consume()
                        if (y < top || y > bottom) {
                            // Beside the thumb: a page, 90 % of the view, toward the press.
                            span.scrollBy((if (y < top) -1 else 1) * PAGE * span.viewportPx)
                            return@awaitEachGesture
                        }
                        dragging = true
                        try {
                            drag(down.id) { change ->
                                // The thumb follows the pointer: the view stands at the thumb's fraction
                                // at the press plus the pointer's travel since over the track (B-93).
                                // From the fraction the list had, not the pointer's place, so it cannot
                                // jump where the thumb is held inside the track; by position, not by
                                // pixels, so a lazy list's estimate cannot move the thumb off the pointer.
                                val to = a + (change.position.y - down.position.y) / density / h
                                // A newer scroll cancels the one before it, as a scroll does.
                                scope.launch(start = CoroutineStart.UNDISPATCHED) { span.scrollTo(to) }
                                change.consume()
                            }
                        } finally {
                            dragging = false
                        }
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                // The engine draws in dp (research D5); the canvas is pixels (B-47).
                inDp {
                    val (a, b) = span.fractions()
                    val st =
                        when {
                            dragging -> ToonScrollbarState.Drag
                            hover.value > .5f -> ToonScrollbarState.Hover
                            else -> ToonScrollbarState.Rest
                        }
                    drawScene(toonScrollbarScene(tokens, toonScrollTrack(sizeDp.height), a, b, st))
                }
            }
        }
    }
}

/** The design's `st`: at rest, under the pointer, or held. */
internal enum class ToonScrollbarState { Rest, Hover, Drag }

/** The track: the area's height less the design's 8 dp insets, at least 40 dp. */
internal fun toonScrollTrack(innerHeight: Float): Float = max(MIN_TRACK, innerHeight)

/** The thumb's top and bottom on a track [h] dp long, as `scrollThumb` draws them: at least 28 dp, held inside. */
internal fun toonScrollThumbSpan(
    h: Float,
    a: Float,
    b: Float,
): Pair<Float, Float> {
    val y0 = a.coerceIn(0f, 1f) * h
    val y1 = max(y0 + MIN_THUMB, b.coerceIn(0f, 1f) * h)
    val top = min(y0, h - MIN_THUMB)
    return top to top + (y1 - y0)
}

/** The extension's `scrollThumb`: the bar 14 dp wide and [h] long, from its top left. */
@OptIn(InternalToonApi::class)
internal fun toonScrollbarScene(
    tokens: ToonTokens,
    h: Float,
    a: Float,
    b: Float,
    st: ToonScrollbarState,
): ToonScene {
    val c = tokens.colors
    val on = st != ToonScrollbarState.Rest
    val w = if (on) 10f else 7f
    val (top, bottom) = toonScrollThumbSpan(h, a, b)
    val layers = mutableListOf<ToonLayer>()
    if (on) {
        // The track, a dotted ink line, while the thumb is under the pointer or held.
        layers +=
            ToonLayer(
                floatArrayOf(7f, 4f, 7.4f, oneDecimal(h - 4)),
                ToonSegments.Line,
                false,
                c.ink,
                .5f,
                stroke = 1.4f,
                dash = TRACK_DASH,
            )
    }
    // `liftBody`: a hover lifts the body 2 dp and leaves its 2 dp shadow on the table; held, the
    // press drops the shadow and the lift.
    val shadow = if (st == ToonScrollbarState.Hover) 2f else 0f
    val fill =
        when (st) {
            ToonScrollbarState.Drag -> c.chipSelected
            ToonScrollbarState.Hover -> c.surface
            ToonScrollbarState.Rest -> c.trackOff
        }
    val shape =
        shapePoints(
            12 - w,
            top,
            w,
            bottom - top,
            ToonRadii(w / 2, w / 2 - .5f, w / 2, w / 2 - 1),
            THUMB_INSETS,
            .45f,
            tokens.shape.seed + 3700,
            4f,
        )
    val body = ToonScene(bodyLayers(shape, fill, c, shadow = shadow, rim = null), emptyList())
    val lifted = if (st == ToonScrollbarState.Hover) body.hoverLifted(2f, c.ink) else body
    return ToonScene(layers + lifted.layers, emptyList())
}

private fun oneDecimal(v: Float): Float = (v * 10).roundToInt() / 10f

/** The box: 20 dp wide, its 14 dp bar 8 dp inside the area at the top and bottom. */
private const val BOX_W = 20f
private const val BAR_W = 14f
private const val INSET = 8f
private const val MIN_TRACK = 40f
private const val MIN_THUMB = 28f
private const val PAGE = .9f
private val TRACK_DASH = floatArrayOf(2f, 6f)
private val THUMB_INSETS = ToonInsets(1.2f, 1.4f, 2.2f, 1.3f)
