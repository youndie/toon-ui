package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
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
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.spinnerPoints
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

// A port of the engine's `linear`, `worm` and `circular` and the kit's `C.progress` (B-25): a tube
// an inchworm crawls through, arching out of it when it bunches up, and an ink blob with three lobes
// that spins unevenly while they morph. A value from 0 to 1 fills the tube or rings the blob instead.

/** The `kind` prop of `ToonProgress.dc.html`. */
public enum class ToonProgressKind {
    Linear,
    Circular,
}

/**
 * The design's progress indicator: [value] from 0 to 1, or below 0 (the default) while it is not
 * known — then the linear one crawls and the circular one spins. A linear one is [width] wide and
 * 14 dp tall, a circular one 48 dp square.
 */
@Composable
public fun ToonProgress(
    modifier: Modifier = Modifier,
    kind: ToonProgressKind = ToonProgressKind.Linear,
    value: Float = -1f,
    width: Dp = 280.dp,
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val alive = value < 0
    var clock by remember { mutableLongStateOf(0L) }
    LaunchedEffect(alive, reduce) {
        if (!alive || reduce) return@LaunchedEffect
        val start = withFrameMillis { it }
        while (true) withFrameMillis { clock = it - start }
    }
    val linear = kind == ToonProgressKind.Linear
    Canvas(
        modifier
            .then(if (linear) Modifier.size(width, LINEAR_H.dp) else Modifier.size(CIRCLE.dp))
            .semantics {
                progressBarRangeInfo =
                    if (alive) ProgressBarRangeInfo.Indeterminate else ProgressBarRangeInfo(min(1f, value), 0f..1f)
            },
    ) {
        // The engine draws in dp (research D5); the canvas is pixels (B-47).
        inDp {
            drawScene(toonProgressFrameAt(tokens, kind, width.value, value, clock.toFloat(), reduce))
        }
    }
}

/**
 * The kit's `C.progress.scene` at [t] ms into the loop. Under reduce motion — the MVP page's words —
 * the linear one is a static bar at 33 % and the circular one a static blob.
 */
internal fun toonProgressFrameAt(
    tokens: ToonTokens,
    kind: ToonProgressKind,
    w: Float,
    value: Float,
    t: Float,
    reduceMotion: Boolean,
): ToonScene {
    val alive = value < 0
    if (kind ==
        ToonProgressKind.Circular
    ) {
        return toonCircularScene(tokens, if (alive) null else min(1f, value), if (reduceMotion) 0f else t)
    }
    return when {
        !alive -> toonLinearScene(tokens, w, 0f, value, worm = false)
        reduceMotion -> toonLinearScene(tokens, w, 0f, REDUCED_SHARE, worm = false)
        else -> toonWormScene(tokens, w, t)
    }
}

private const val LINEAR_H = 14f
private const val CIRCLE = 48f

/** The MVP page's reduce-motion word for the linear one: a static bar at 33 %. */
private const val REDUCED_SHARE = .33f

/** The inchworm's tail [a] and head [b] along the tube, each from −0.15 to 1.2 over a 1200 ms loop. */
internal data class ToonWorm(
    val a: Float,
    val b: Float,
)

/** The engine's `worm` at [t] ms: the head lunges in two bursts per loop, the tail 240 ms behind. */
internal fun toonWorm(t: Float): ToonWorm {
    val u = ((t % 1200) + 1200) % 1200 / 1200

    fun j(v: Float) = v - sin(4 * PI.toFloat() * v) / (4 * PI.toFloat()) * .9f
    return ToonWorm(-.15f + 1.35f * j(max(0f, u - .2f)), -.15f + 1.35f * j(u))
}

/** The crawling tube [w] wide at [t] ms into the loop. */
internal fun toonWormScene(
    tokens: ToonTokens,
    w: Float,
    t: Float,
): ToonScene = toonWorm(t).let { toonLinearScene(tokens, w, it.a, it.b, worm = true) }

private fun c01(v: Float): Float = max(0f, min(1f, v))

private val TUBE_RADII = ToonRadii(7f, 6f, 7f, 6f)
private val FILL_RADII = ToonRadii(4f, 4f, 4f, 4f)
private val FILL_INSETS = ToonInsets(1f, 1.2f, 1.6f, 1f)

/** The engine's `linear`: a tube [w] wide from [a] to [b] full, as a crawling worm when [worm] is set. */
@OptIn(InternalToonApi::class)
internal fun toonLinearScene(
    tokens: ToonTokens,
    w: Float,
    a0: Float,
    b0: Float,
    worm: Boolean,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val h = LINEAR_H
    val ins = sh.insets.let { ToonInsets(it.top * .6f, it.right * .6f, it.bottom * .6f, it.left * .6f) }
    val layers =
        bodyLayers(
            shapePoints(0f, 0f, w, h, TUBE_RADII, ins, sh.wobble * .6f, sh.seed + 700),
            c.trackOff,
            c,
            2f,
            rim = null,
        ).toMutableList()
    val a = c01(a0)
    val b = c01(b0)
    if (b - a > .01f && worm) {
        // The shorter the body gets, the higher it arches out of the tube.
        val x0 = 3 + (w - 6) * a + 4
        val x1 = max(x0 + .5f, 3 + (w - 6) * b - 4)
        val len = x1 - x0
        val nat = (w - 6) * .34f
        val arch = 11 * c01(1 - len / nat).pow(.8f)
        val n = 24
        val p = FloatArray(2 * (n + 1))
        for (i in 0..n) {
            val u = i.toFloat() / n
            // sin π is a hair below 0 in Float, and above it in the engine's doubles.
            val k = max(0f, sin(PI.toFloat() * u)).pow(1.3f)
            p[2 * i] = x0 + len * u
            p[2 * i + 1] = 7 - arch * k + .6f * sin(u * 9 + x0 * .05f)
        }
        layers += ToonLayer(p, ToonSegments.Line, false, c.ink, dy = .9f, stroke = 11f)
        layers += ToonLayer(p, ToonSegments.Line, false, c.ink, stroke = 10.4f)
        layers += ToonLayer(p, ToonSegments.Line, false, c.primary, stroke = 7.4f)
        // The eye: a dot just behind the head, on its upper side.
        val hx = p[2 * n]
        val hy = p[2 * n + 1]
        val ang = atan2(hy - p[2 * n - 3], hx - p[2 * n - 4])
        val ex = hx - cos(ang) * .5f - sin(ang) * 1.2f
        val ey = hy - sin(ang) * .5f + cos(ang) * -1.2f
        layers += ToonLayer(floatArrayOf(ex, ey, ex + .01f, ey), ToonSegments.Line, false, c.ink, stroke = 2.6f)
    } else if (b - a > .01f) {
        layers +=
            bodyLayers(
                shapePoints(
                    3 + (w - 6) * a,
                    3f,
                    (w - 6) * (b - a),
                    h - 6,
                    FILL_RADII,
                    FILL_INSETS,
                    .3f,
                    sh.seed + 701,
                    3f,
                ),
                c.primary,
                c,
                0f,
                rim = null,
            )
    }
    return ToonScene(layers, emptyList())
}

private val DISC_RADII = ToonRadii(19f, 18f, 19f, 17.5f)

/** The ring's radius about the 48 dp box's centre. */
private const val RING_R = 13f

/** The engine's `circular`: a ring [value] round its disc, or (null) the blob at phase [ph] ms. */
@OptIn(InternalToonApi::class)
internal fun toonCircularScene(
    tokens: ToonTokens,
    value: Float?,
    ph: Float,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    if (value != null) {
        val ins = sh.insets.let { ToonInsets(it.top * .7f, it.right * .7f, it.bottom * .7f, it.left * .7f) }
        val disc =
            bodyLayers(
                shapePoints(5f, 5f, 38f, 38f, DISC_RADII, ins, sh.wobble, sh.seed + 720, 3f),
                c.trackOff,
                c,
                0f,
                rim = null,
            )
        // The engine writes the dash to one decimal, which is what the ring is drawn with.
        val dash = (2 * PI.toFloat() * RING_R * value * 10).roundToInt() / 10f
        val ring =
            ToonLayer(
                floatArrayOf(24f, 24 - RING_R, RING_R, RING_R, 0f, 1f, 1f, 23.99f, 24 - RING_R),
                ToonSegments.Arc,
                false,
                c.primary,
                stroke = 7f,
                dash = floatArrayOf(dash, 200f),
            )
        return ToonScene(disc + ring, emptyList())
    }
    val turn = ph / 1000 * 2 * PI.toFloat()
    return ToonScene(
        listOf(
            smoothLayer(spinnerPoints(24f, 24f, RING_R, RING_R + 3, turn, 1f, 1f), c.ink),
            smoothLayer(spinnerPoints(24f, 24f, RING_R, RING_R, turn, 1f, 1f), c.primary),
        ),
        emptyList(),
    )
}
