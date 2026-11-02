package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
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
import io.github.youndie.toon.draw.ringLayer
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.motion.toonSpring
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

// A port of the engine's `slider`, `rope` and `sliderGrab` and the kit's `C.slider` (B-36): a ball on
// a rope strung between two pegs. Grabbed, the ball squashes under the finger and the rope sags 5 dp
// beneath it; let go, the ball springs round and the rope twangs back once.

/**
 * The design's slider: [value] from 0 to 1 along a rope [width] wide, set by a press or a drag and
 * reported to [onValueChange]; [steps] above 1 snaps it to that many stops.
 */
@Composable
public fun ToonSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    width: Dp = 300.dp,
    enabled: Boolean = true,
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val clock = remember { ToonSliderClock() }
    val change by rememberUpdatedState(onValueChange)
    val current by rememberUpdatedState(value)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val w = width.value
    LaunchedEffect(clock.starts) { while (clock.running()) withFrameMillis { clock.tick(it) } }

    fun set(v: Float) {
        val snapped = toonSliderSnap(v, steps)
        if (snapped != current) change(snapped)
    }
    // The keyboard and a screen reader move one stop, or a twentieth of the rope without stops.
    val step = if (steps > 1) 1f / (steps - 1) else KEY_STEP

    Canvas(
        modifier
            .size(width, SLIDER_BOX.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(toonSliderSnap(value, steps), 0f..1f, max(0, steps - 2))
                if (enabled) {
                    setProgress {
                        set(it)
                        true
                    }
                } else {
                    disabled()
                }
            }.onKeyEvent {
                if (!enabled || it.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (it.key) {
                    Key.DirectionRight, Key.DirectionUp -> {
                        set(min(1f, current + step))
                        true
                    }

                    Key.DirectionLeft, Key.DirectionDown -> {
                        set(max(0f, current - step))
                        true
                    }

                    else -> {
                        false
                    }
                }
            }.focusable(enabled, interaction)
            .pointerInput(enabled, w, steps) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    clock.grab()
                    set(toonSliderAt(w, down.position.x / density))
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        set(toonSliderAt(w, change.position.x / density))
                        change.consume()
                    }
                    clock.release()
                }
            },
    ) {
        // The engine draws in dp (research D5); the canvas is pixels (B-47).
        inDp {
            val look =
                ToonSliderLook(
                    grab = toonSliderFrameAt(clock.held, clock.age(), reduce),
                    focus = focused,
                    disabled = !enabled,
                )
            drawScene(toonSliderScene(tokens, w, toonSliderSnap(value, steps), look))
        }
    }
}

/** The kit's box: 48 dp, the rope at 24. */
private const val SLIDER_BOX = 48f

/** A key press without stops moves a twentieth of the rope. */
private const val KEY_STEP = .05f

/** The grab plays for 200 ms in the kit, the release for 500. */
private const val GRAB_MS = 200f
private const val RELEASE_MS = 500f

/** The kit's `set`: the value under [x] on a rope [w] wide, its ends 14 dp in. */
internal fun toonSliderAt(
    w: Float,
    x: Float,
): Float = max(0f, min(1f, (x - 14) / (w - 28)))

/** [v] snapped to [steps] stops when there are more than one. */
internal fun toonSliderSnap(
    v: Float,
    steps: Int,
): Float = if (steps > 1) (v * (steps - 1)).roundToInt().toFloat() / (steps - 1) else max(0f, min(1f, v))

/** The engine's `sliderGrab` at [t] ms: the squash eased in while [down], sprung back after. */
internal fun toonSliderGrab(
    t: Float,
    down: Boolean,
): Float = if (down) 1 - (1 - max(0f, min(1f, t / 80))).pow(3) else toonSpring(t / 1000, .35f, 45f)

/**
 * The kit's grab at a moment: held, or let go [ageMs] ago (null when never grabbed). Under reduce
 * motion — the v1 page's `instant` — the ball is squashed while held and round otherwise.
 */
internal fun toonSliderFrameAt(
    held: Boolean,
    ageMs: Float?,
    reduceMotion: Boolean,
): Float =
    when {
        reduceMotion -> if (held) 1f else 0f
        held -> toonSliderGrab(ageMs ?: 1e6f, true)
        ageMs != null && ageMs < RELEASE_MS -> toonSliderGrab(ageMs, false)
        else -> 0f
    }

@Stable
private class ToonSliderClock {
    var nowMs by mutableLongStateOf(0L)
    var starts by mutableStateOf(0)
    var held by mutableStateOf(false)
    private var at: Long? = null

    fun grab() {
        held = true
        at = PENDING_START
        starts++
    }

    fun release() {
        if (!held) return
        held = false
        at = PENDING_START
        starts++
    }

    fun tick(now: Long) {
        nowMs = now
        if (at == PENDING_START) at = now
    }

    fun age(): Float? = at?.let { if (it == PENDING_START) 0f else (nowMs - it).toFloat() }

    fun running(): Boolean = age()?.let { it < (if (held) GRAB_MS else RELEASE_MS) } ?: false
}

private const val PENDING_START = -1L

/** What one frame of the slider draws: the engine's `slider` options. */
internal data class ToonSliderLook(
    val grab: Float = 0f,
    val focus: Boolean = false,
    val disabled: Boolean = false,
)

private val PEG_RADII = ToonRadii(4f, 4f, 4f, 4f)
private val PEG_INSETS = ToonInsets(1.3f, 1.3f, 1.8f, 1.3f)
private val BALL_INSETS = ToonInsets(1.8f, 2.2f, 3f, 1.8f)
private val RING_RADII = ToonRadii(22f, 20f, 22f, 19f)

/** The engine's `rope`: from [x0] to [x1] on [y], sagging [sag] about [sx], in [color] over ink, hatched. */
private fun ropeLayers(
    tokens: ToonTokens,
    x0: Float,
    x1: Float,
    y: Float,
    sx: Float,
    sag: Float,
    color: androidx.compose.ui.graphics.Color,
    seed: Int,
): List<ToonLayer> {
    fun at(x: Float) = y + sag * exp(-((x - sx) / 46).pow(2))
    val n = 40
    val p = FloatArray(2 * (n + 1))
    for (i in 0..n) {
        val u = i.toFloat() / n
        val x = x0 + (x1 - x0) * u
        p[2 * i] = x
        p[2 * i + 1] = at(x) + .5f * sin(u * 11 + seed)
    }
    val hatch = mutableListOf<Float>()
    var x = x0 + 4
    while (x < x1 - 2) {
        hatch += listOf(x - 1.6f, at(x) + 1.8f, x + 1.6f, at(x) - 1.8f)
        x += 6
    }
    val ink = tokens.colors.ink
    return listOf(
        ToonLayer(p, ToonSegments.Line, false, ink, stroke = 7.5f),
        ToonLayer(p, ToonSegments.Line, false, color, stroke = 4f),
        // No hatching on a rope too short for one: the engine writes `M0 0`.
        ToonLayer(
            if (hatch.isEmpty()) floatArrayOf(0f, 0f) else hatch.toFloatArray(),
            ToonSegments.Pairs,
            false,
            ink,
            .45f,
            stroke = 1f,
        ),
    )
}

/** The engine's `slider`: one frame [w] dp wide from its top left, the rope on y 24. */
@OptIn(InternalToonApi::class)
internal fun toonSliderScene(
    tokens: ToonTokens,
    w: Float,
    value: Float,
    o: ToonSliderLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val y = 24f
    val v = max(0f, min(1f, value))
    val q = o.grab
    val x0 = 14f
    val x1 = w - 14
    val tx = x0 + (x1 - x0) * v
    val sag = 5 * max(0f, q)
    val dis = o.disabled
    val layers = mutableListOf<ToonLayer>()
    if (o.focus) layers += ringLayer(tx - 22, 2f, 44f, 44f, RING_RADII, c, sh.wobble, sh.seed + 1501)
    layers += ropeLayers(tokens, tx, x1, y, tx, sag, if (dis) c.disabled else c.trackOff, 3)
    layers += ropeLayers(tokens, x0, tx, y, tx, sag, if (dis) c.disabled else c.primary, 1)
    listOf(x0 - 4, x1 + 4).forEachIndexed { j, px ->
        val g = shapePoints(px - 4, y - 8, 8f, 16f, PEG_RADII, PEG_INSETS, .3f, sh.seed + 1510 + j, 2f)
        layers += smoothLayer(g.ink, c.ink)
        layers += smoothLayer(g.fill, c.nail)
    }
    val r = 12f
    val sx = 1 + .28f * q
    val sy = 1 - .22f * q
    val by = y + sag
    val k = min(sx, sy)
    val g =
        shapePoints(
            tx - r * sx,
            by - r * sy,
            2 * r * sx,
            2 * r * sy,
            ToonRadii(
                r * k,
                r * .95f * k,
                r * k,
                r * .9f * k,
            ),
            BALL_INSETS,
            .5f,
            sh.seed + 1520,
            3f,
        )
    val d = 2 * (1 - max(0f, min(1f, q)))
    if (!dis) layers += smoothLayer(g.ink, c.ink, dx = d, dy = d)
    layers += smoothLayer(g.ink, c.ink, if (dis) .4f else 1f)
    layers += smoothLayer(g.fill, if (dis) c.disabled else c.thumb)
    return ToonScene(layers, emptyList())
}
