package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonInsets
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonSegments
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.spinnerPoints
import io.github.youndie.toon.motion.toonSpring
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// A port of the engine's `pull` and `pullRelease` and the kit's `C.pullrefresh` (B-42): a rubber
// band between two pegs that a finger drags down, thinning as it stretches, with the blob at its tip.
// Let go past 40 dp, it snaps back past the pegs and flings the blob up into the spinning progress
// blob, which turns until the caller is done refreshing.
//
// B-64 puts content under it (`ToonPullToRefreshBox`): the strip alone is a band pulled by its tip, with nothing attached,
// and a spinner that shrinks away where it hung. With content, the content is what the finger pulls;
// the band stretches in the gap that opens above it, the content settles under the spinning blob
// while refreshing, and closes the gap, squashing the blob flat.

/**
 * The design's pull-to-refresh strip, [width] wide and 140 dp tall: a drag down it stretches the band
 * and reports the distance to [onPull] (the content above it follows at 70 %, as the v1 page draws);
 * let go past 40 dp, it calls [onRefresh] and spins until [refreshing] turns false.
 */
@Composable
public fun ToonPullToRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 360.dp,
    onPull: (Float) -> Unit = {},
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val texts = rememberToonText()
    val state = remember { ToonPullState(refreshing) }
    val refresh by rememberUpdatedState(onRefresh)
    val pull by rememberUpdatedState(onPull)
    val w = width.value
    LaunchedEffect(refreshing) { state.refreshingChanged(refreshing) }
    LaunchedEffect(state.starts, state.ref, reduce) { while (state.running(reduce)) withFrameMillis { state.tick(it) } }

    Canvas(
        modifier
            .size(width, PULL_H.dp)
            .clipToBounds()
            .semantics {
                if (state.ref) {
                    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                } else {
                    customActions =
                        listOf(
                            CustomAccessibilityAction(
                                REFRESH_ACTION,
                            ) { state.release(PULL_THRESHOLD + 1).also { refresh() } },
                        )
                }
            }.pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (state.ref) return@awaitEachGesture
                    // Coordinates are taken as dp, as every component draws them (B-47).
                    val y0 = down.position.y / density
                    state.dragging = true
                    state.d = 0f
                    while (true) {
                        val e = awaitPointerEvent()
                        val c = e.changes.firstOrNull { it.id == down.id } ?: break
                        if (!c.pressed) break
                        if (c.positionChanged()) {
                            state.d = toonPullDistance(c.position.y / density - y0)
                            pull(state.d)
                            c.consume()
                        }
                    }
                    if (state.release(state.d)) refresh()
                }
            },
    ) {
        // The engine draws in dp (research D5); the canvas is pixels (B-47).
        inDp {
            drawScene(toonPullScene(tokens, w, state.look(reduce)), texts)
        }
    }
}

/**
 * Pull-to-refresh over [content]: a drag down the box pulls the content down after it, a band
 * stretching in the gap that opens above; let go past 40 dp, [onRefresh] is called, the content
 * settles 60 dp down under the spinning blob until [refreshing] turns false, and then springs back
 * up, squashing the blob flat as it closes the gap (B-64). Short of 40 dp the content springs straight back. A drag
 * that a child consumes, a list scrolling for instance, does not pull.
 */
@Composable
public fun ToonPullToRefreshBox(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val texts = rememberToonText()
    val state = remember { ToonPullState(refreshing) }
    val refresh by rememberUpdatedState(onRefresh)
    LaunchedEffect(refreshing) { state.refreshingChanged(refreshing) }
    LaunchedEffect(state.starts, state.ref, reduce) {
        while (state.running(reduce, held = true)) withFrameMillis { state.tick(it) }
    }
    BoxWithConstraints(
        modifier
            .clipToBounds()
            .semantics {
                if (state.ref) {
                    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                } else {
                    customActions =
                        listOf(
                            CustomAccessibilityAction(
                                REFRESH_ACTION,
                            ) { state.release(PULL_THRESHOLD + 1).also { refresh() } },
                        )
                }
            }.pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (state.ref) return@awaitEachGesture
                    val y0 = down.position.y / density
                    var pulling = false
                    while (true) {
                        val e = awaitPointerEvent()
                        val c = e.changes.firstOrNull { it.id == down.id } ?: break
                        if (!c.pressed) break
                        if (!pulling && c.isConsumed) return@awaitEachGesture
                        val travel = c.position.y / density - y0
                        if (!pulling && travel > PULL_SLOP) {
                            pulling = true
                            state.dragging = true
                        }
                        if (pulling && c.positionChanged()) {
                            state.d = toonPullDistance(travel - PULL_SLOP)
                            c.consume()
                        }
                    }
                    if (pulling && state.release(state.d)) refresh()
                }
            },
    ) {
        val w = maxWidth.value
        val gap = state.gap(reduce)
        Box(Modifier.fillMaxSize().graphicsLayer { translationY = gap * density }) { content() }
        if (gap > 0f) {
            Canvas(Modifier.fillMaxWidth().height(gap.dp).clipToBounds()) {
                inDp { drawScene(toonPullScene(tokens, w, state.boxLook(reduce)), texts) }
            }
        }
    }
}

/** A drag must travel this far down before it pulls, so a tap or a sideways drag stays the content's. */
private const val PULL_SLOP = 8f

/** While refreshing the content waits this far down, under the blob spinning at 40. */
private const val HOLD_GAP = 60f

/** Done, the content springs back up over this long, squashing the blob (B-64). */
private const val CLOSE_MS = 420f

/**
 * The gap over the content (B-64), in dp: while dragged, the band's travel and room for the blob
 * under its tip, the room growing in as the band reaches 40 dp; after a release past 40, from where
 * the content was to [HOLD_GAP], on the engine's release spring; held while refreshing; done, back
 * to 0 on a stiffer spring; a short release springs straight back.
 */
internal fun toonPullGapAt(
    dragging: Boolean,
    d: Float,
    refreshing: Boolean,
    releaseAge: Float?,
    d0: Float,
    doneAge: Float?,
    backAge: Float?,
    reduceMotion: Boolean,
): Float {
    fun dragged(d: Float) = d + BLOB_ROOM * min(1f, d / PULL_THRESHOLD)
    return when {
        dragging -> {
            dragged(d)
        }

        refreshing && reduceMotion -> {
            HOLD_GAP
        }

        refreshing && releaseAge != null -> {
            HOLD_GAP +
                (dragged(d0) - HOLD_GAP) * toonSpring(releaseAge / 1000, .4f, 30f)
        }

        refreshing -> {
            HOLD_GAP
        }

        reduceMotion -> {
            0f
        }

        doneAge != null && doneAge < CLOSE_MS -> {
            max(0f, HOLD_GAP * toonSpring(doneAge / 1000, .7f, 22f))
        }

        backAge != null && backAge < CLOSE_MS -> {
            max(0f, dragged(d0) * toonSpring(backAge / 1000, .7f, 22f))
        }

        else -> {
            0f
        }
    }
}

/**
 * The box's own look while its [gap] closes (B-64), or null where the strip's is the one. Done, the
 * blob keeps spinning in the middle of the gap and the content coming back up squashes it, wider
 * and flatter, until the gap is gone: one motion instead of the strip's shrink in place and a slack
 * band popping in after it. A short pull let go, the band contracts with the gap, the blob on its
 * tip, as it stretched.
 */
internal fun toonPullBoxLookAt(
    gap: Float,
    dragging: Boolean,
    refreshing: Boolean,
    doneAge: Float?,
    backAge: Float?,
    nowMs: Float,
    reduceMotion: Boolean,
): ToonPullLook? {
    if (reduceMotion || dragging || refreshing) return null
    val g = max(0f, min(1f, gap / HOLD_GAP))
    return when {
        doneAge != null && doneAge < CLOSE_MS -> {
            ToonPullLook(band = false, blobY = HELD_BLOB_Y * g, ph = nowMs, bsx = 1 + .6f * (1 - g), bsy = g)
        }

        backAge != null && backAge < CLOSE_MS -> {
            // The drag's gap run backwards: d + room, the room in full from 40 dp.
            val d = if (gap >= PULL_THRESHOLD + BLOB_ROOM) gap - BLOB_ROOM else gap / (1 + BLOB_ROOM / PULL_THRESHOLD)
            ToonPullLook(d = d, bsy = 1 + d / 240, bsx = 1 - d / 600)
        }

        else -> {
            null
        }
    }
}

/** Where the blob spins while the content waits: the strip's 40 dp. */
private const val HELD_BLOB_Y = 40f

/** The blob, 14 dp with its rim and up to 1.5 times as tall stretched, hangs 14 dp under the band's tip: room for it. */
private const val BLOB_ROOM = 34f

/** The kit's box: 140 dp tall, clipped. */
private const val PULL_H = 140f

/** A release past this many dp refreshes; short of it the band just goes back. */
private const val PULL_THRESHOLD = 40f

/** What a screen reader offers in place of the drag. */
private const val REFRESH_ACTION = "Refresh"

/** The kit's `move`: the finger's travel at 0.8, from 0 to 120 dp. */
internal fun toonPullDistance(travel: Float): Float = max(0f, min(120f, travel * .8f))

/** The engine's `pullRelease` at [t] ms after letting go [d0] dp down. */
internal fun toonPullRelease(
    t: Float,
    d0: Float,
): ToonPullLook {
    val u = c01(t / 120)
    val st = 1 + .5f * max(0f, 1 - t / 160)
    return ToonPullLook(
        by = max(6f, d0 * (1 - u) * (1 - u) - 12 * sin(PI.toFloat() * u)),
        blobY = 40 + (d0 + 14 - 40) * toonSpring(t / 1000, .4f, 30f),
        bsx = 1 / sqrt(st),
        bsy = st,
        band = t < 160,
    )
}

private fun c01(v: Float) = max(0f, min(1f, v))

/** The kit draws the fling for 400 ms of the release and shrinks the spinner away in 260 once done. */
private const val RELEASE_DRAWN_MS = 400f
private const val DONE_MS = 260f

/** The strip's state: the drag, the release, the refresh and its end, and the clock that moves them. */
@Stable
internal class ToonPullState(
    refreshing: Boolean,
) {
    var nowMs by mutableLongStateOf(0L)
    var starts by mutableStateOf(0)
    var dragging by mutableStateOf(false)
    var d by mutableFloatStateOf(0f)
    var ref by mutableStateOf(refreshing)
    private var d0 = 0f
    private var releasedAt: Long? = null
    private var doneAt: Long? = null
    private var backAt: Long? = null

    /** The kit's `up`: past 40 dp it starts a refresh and says so; short of it the content springs back (B-64). */
    fun release(distance: Float): Boolean {
        dragging = false
        d = 0f
        if (distance <= PULL_THRESHOLD) {
            if (distance > 0f) {
                d0 = distance
                backAt = PENDING_START
                starts++
            }
            return false
        }
        d0 = distance
        ref = true
        releasedAt = PENDING_START
        doneAt = null
        backAt = null
        starts++
        return true
    }

    /** The kit's `changed`: the caller's refresh ending shrinks the spinner away. */
    fun refreshingChanged(now: Boolean) {
        if (now == ref) return
        ref = now
        releasedAt = null
        doneAt = if (now) null else PENDING_START
        starts++
    }

    fun tick(now: Long) {
        nowMs = now
        if (releasedAt == PENDING_START) releasedAt = now
        if (doneAt == PENDING_START) doneAt = now
        if (backAt == PENDING_START) backAt = now
    }

    private fun age(at: Long?): Float? =
        nowMs.let { now ->
            at?.let {
                if (it ==
                    PENDING_START
                ) {
                    0f
                } else {
                    (now - it).toFloat()
                }
            }
        }

    /**
     * The spinner turns for as long as the refresh lasts, and the end plays out; under reduce motion
     * nothing moves, so no frame is asked for.
     */
    fun running(
        reduceMotion: Boolean,
        held: Boolean = false,
    ): Boolean {
        if (reduceMotion) return false
        val end = if (held) CLOSE_MS else DONE_MS
        return ref || (age(doneAt)?.let { it < end } ?: false) ||
            (held && (age(backAt)?.let { it < CLOSE_MS } ?: false))
    }

    /** What the box draws in its gap (B-64): the strip's look, but for the box's own close and spring-back. */
    fun boxLook(reduceMotion: Boolean): ToonPullLook =
        toonPullBoxLookAt(gap(reduceMotion), dragging, ref, age(doneAt), age(backAt), nowMs.toFloat(), reduceMotion)
            ?: look(reduceMotion)

    /** The gap over held content (B-64). */
    fun gap(reduceMotion: Boolean): Float =
        toonPullGapAt(dragging, d, ref, age(releasedAt), d0, age(doneAt), age(backAt), reduceMotion)

    fun look(reduceMotion: Boolean): ToonPullLook =
        toonPullFrameAt(dragging, d, ref, age(releasedAt), d0, age(doneAt), nowMs.toFloat(), reduceMotion)
}

private const val PENDING_START = -1L

/**
 * The kit's `scene` for a moment: dragged [d] dp; refreshing, [releaseAge] ms after a release of
 * [d0] dp; or [doneAge] ms after the refresh ended. The spinner turns once a second by [nowMs]. Under
 * reduce motion — the v1 page's `static spinner` — the band still follows the finger, but the release
 * lands on the spinner without the fling, it does not turn, and it goes at once when done.
 */
internal fun toonPullFrameAt(
    dragging: Boolean,
    d: Float,
    refreshing: Boolean,
    releaseAge: Float?,
    d0: Float,
    doneAge: Float?,
    nowMs: Float,
    reduceMotion: Boolean,
): ToonPullLook {
    val ph = if (reduceMotion) 0f else nowMs
    return when {
        dragging -> {
            ToonPullLook(d = d, bsy = 1 + d / 240, bsx = 1 - d / 600)
        }

        refreshing && !reduceMotion && releaseAge != null && releaseAge < RELEASE_DRAWN_MS -> {
            toonPullRelease(releaseAge, if (d0 > 0) d0 else 80f).copy(ph = ph)
        }

        refreshing -> {
            ToonPullLook(band = false, blobY = 40f, ph = ph)
        }

        !reduceMotion && doneAge != null && doneAge < DONE_MS -> {
            val s = 1 - doneAge / DONE_MS
            ToonPullLook(band = false, blobY = 40f, ph = ph, bsx = s, bsy = s)
        }

        else -> {
            ToonPullLook(by = 6f, blobY = -40f)
        }
    }
}

/** What one frame of the strip draws: the engine's `pull` options. */
internal data class ToonPullLook(
    val d: Float = 0f,
    val by: Float? = null,
    val band: Boolean = true,
    val blobY: Float? = null,
    val ph: Float = 0f,
    val bsx: Float = 1f,
    val bsy: Float = 1f,
)

private val PEG_RADII = ToonRadii(4f, 4f, 5f, 5f)
private val PEG_INSETS = ToonInsets(1.3f, 1.3f, 1.8f, 1.3f)
private const val BLOB_R = 11f

/** The engine's `f`: a number as it writes it into a path, to a tenth. */
private fun tenth(v: Float) = floor(v * 10 + .5f) / 10

/** The engine's `pull`: the two pegs, the band between them, and the blob, [w] dp wide. */
@OptIn(InternalToonApi::class)
internal fun toonPullScene(
    tokens: ToonTokens,
    w: Float,
    o: ToonPullLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val cx = w / 2
    val by = o.by ?: o.d
    val layers = mutableListOf<ToonLayer>()
    listOf(cx - 46, cx + 46).forEachIndexed { j, px ->
        val g = shapePoints(px - 5, -2f, 10f, 12f, PEG_RADII, PEG_INSETS, .3f, sh.seed + 2100 + j, 2f)
        layers += smoothLayer(g.ink, c.ink)
        layers += smoothLayer(g.fill, c.nail)
    }
    if (o.band) {
        val band =
            floatArrayOf(
                cx - 46,
                6f,
                cx - 16,
                tenth(by * .92f),
                cx,
                tenth(by),
                cx + 16,
                tenth(by * .92f),
                cx + 46,
                6f,
            )
        val th = 3.8f - min(2.2f, max(0f, by) / 55)
        layers += ToonLayer(band, ToonSegments.Quad, false, c.ink, stroke = th + 2.6f)
        layers += ToonLayer(band, ToonSegments.Quad, false, c.primary, stroke = th)
    }
    val turn = o.ph / 1000 * 2 * PI.toFloat()
    val cy = o.blobY ?: (by + 14)
    layers += smoothLayer(spinnerPoints(cx, cy, BLOB_R, BLOB_R + 3, turn, o.bsx, o.bsy), c.ink)
    layers += smoothLayer(spinnerPoints(cx, cy, BLOB_R, BLOB_R, turn, o.bsx, o.bsy), c.primary)
    return ToonScene(layers, emptyList())
}
