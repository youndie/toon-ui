package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonSegments
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.sizeDp
import io.github.youndie.toon.fx.drawToonIris
import io.github.youndie.toon.fx.toonHoleOutline
import io.github.youndie.toon.fx.toonIrisAt
import io.github.youndie.toon.fx.toonStarsScene
import io.github.youndie.toon.motion.toonSpring
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonColors
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

// The three screen transitions of the v1 page (B-44), ported from the engine's `pushAt`, `irisAt` and
// `holeScene`: a host the library owns, which takes the old and the new screen as composables and
// is tied to no navigation library (research D2). The engine draws `miniScreen` stand-ins; here the
// screens are whatever the caller composes, so only the motion is the engine's.

/** How a [ToonTransitionHost] goes from one screen to the next. */
public enum class ToonTransition {
    /** The old screen leans 6 dp the wrong way and leaves left with a smear; the new one flies in from the right. */
    Push,

    /** The FX 6 iris: closes on the tapped control, holds on ink, opens on the new scene. */
    Scene,

    /** The tapped card bursts through the screen: its cracked silhouette grows until the new screen fills it. */
    Hole,
}

/**
 * Shows [content] for [target], and when [target] changes plays [transition] from the old screen to
 * the new one over the whole of its box, which it clips. [origin] is the tapped element in dp within
 * the box: the iris closes on its centre and the hole bursts from it. Without one, the page's own
 * places: the iris on a point 46 dp above the bottom middle, the hole from the second card.
 */
@Composable
public fun <S> ToonTransitionHost(
    target: S,
    modifier: Modifier = Modifier,
    transition: ToonTransition = ToonTransition.Push,
    origin: Rect? = null,
    content: @Composable (S) -> Unit,
) {
    val reduce = Toon.reduceMotion
    val state = remember { ToonTransitionState(target) }
    // The switch and its frames in one effect: a new target cancels the one playing and starts its own.
    LaunchedEffect(target) {
        state.go(target, transition)
        while (state.running()) withFrameMillis { state.tick(it) }
    }
    val age = state.age()
    val from = state.from
    ToonTransitionFrame(
        state.kind,
        age,
        reduce,
        origin,
        modifier,
        from = if (age != null && from != null) ({ key(from) { content(from) } }) else null,
        to = { key(state.current) { content(state.current) } },
    )
}

/** How long each transition plays, as the v1 page gives it: push 450 ms, scene 600, hole 460. */
internal fun toonTransitionMs(kind: ToonTransition): Float =
    when (kind) {
        ToonTransition.Push -> 450f
        ToonTransition.Scene -> 600f
        ToonTransition.Hole -> 460f
    }

/** Under reduce motion every transition is the page's `cross-fade 120`. */
private const val REDUCED_FADE_MS = 120f

/** The host's clock: which screen is shown, which it came from, and how long ago. */
@Stable
internal class ToonTransitionState<S>(
    initial: S,
) {
    var nowMs by mutableLongStateOf(0L)
    var starts by mutableStateOf(0)
    var current by mutableStateOf(initial)
    var from by mutableStateOf<S?>(null)
    var kind by mutableStateOf(ToonTransition.Push)
    private var at: Long? = null

    fun go(
        to: S,
        how: ToonTransition,
    ) {
        if (to == current) return
        from = current
        current = to
        kind = how
        at = PENDING_START
        starts++
    }

    fun tick(now: Long) {
        nowMs = now
        if (at == PENDING_START) at = now
    }

    /** Milliseconds into the transition, or null once it has played. */
    fun age(): Float? =
        nowMs
            .let { now -> at?.let { if (it == PENDING_START) 0f else (now - it).toFloat() } }
            ?.takeIf { it < toonTransitionMs(kind) }

    fun running(): Boolean = age() != null
}

private const val PENDING_START = -1L

/** The engine's `pushAt` at [t] ms over a screen [w] dp wide: the old screen's offset and smear, the new one's offset. */
internal data class ToonPushMotion(
    val ax: Float,
    val asx: Float,
    val bx: Float,
)

/** The engine's `pushAt`: leans 6 dp for 60 ms, leaves with ease-in in 180 and a 1.35× smear; the new screen springs in from 90. */
internal fun toonPushAt(
    t: Float,
    w: Float,
): ToonPushMotion {
    var ax = 0f
    var asx = 1f
    if (t < 60) {
        ax = 6 * t / 60
    } else {
        val u = c01((t - 60) / 180)
        ax = 6 - (w + 6) * u * u
        asx = 1 + .35f * sin(PI.toFloat() * u)
    }
    return ToonPushMotion(ax, asx, if (t < 90) w else w * toonSpring((t - 90) / 1000, .74f, 24f))
}

/** The hole transition at [t] ms: how much the silhouette has grown, and the new screen's opacity. */
internal data class ToonHoleMotion(
    val scale: Float,
    val newOp: Float,
)

/** The engine's `holeScene` motion: the silhouette holds 15 % of 380 ms, then grows ×10 ease-in 2.2; the new screen fades in from 380 in 80. */
internal fun toonHoleAt(t: Float): ToonHoleMotion {
    val u = c01(t / 380)
    val e = if (u < .15f) 0f else ((u - .15f) / .85f).pow(2.2f)
    return ToonHoleMotion(1 + e * 9, if (t >= 380) c01((t - 380) / 80) else 0f)
}

private fun c01(v: Float) = max(0f, min(1f, v))

/** The page's tapped card for the hole: the second of three, 14 dp in, 82 down, 46 tall. */
private val HOLE_CARD_RADII = ToonRadii(10f, 7f, 11f, 8f)

/**
 * The hole's silhouette over a screen and the stars of its impact, [t] ms in, from the card
 * [x], [y], [w], [h]: the engine's `hole` at 0.05, its paper edge and its inside, grown about the
 * card's centre.
 */
@OptIn(InternalToonApi::class)
internal fun toonHoleTransitionScene(
    colors: ToonColors,
    t: Float,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
): ToonScene {
    if (t <= 0 || t >= 420) return ToonScene.Empty
    val cx = x + w / 2
    val cy = y + h / 2
    val outline = toonHoleOutline(.05f, x, y, w, h, HOLE_CARD_RADII, 5)
    val silhouette =
        ToonScene(
            listOf(
                ToonLayer(outline.edge, ToonSegments.Line, true, colors.holeEdge),
                ToonLayer(outline.ink, ToonSegments.Line, true, colors.surfacePressed),
            ),
            emptyList(),
        ).scaledAbout(cx, cy, toonHoleAt(t).scale, toonHoleAt(t).scale)
    return silhouette + toonStarsScene(colors, t / 380, cx, y, 30f)
}

/**
 * One frame of a transition [ageMs] in (null: none playing), between [from] and [to] over the whole
 * box: the host's drawing, and what a fixture freezes at a page timestamp.
 */
@Composable
internal fun ToonTransitionFrame(
    kind: ToonTransition,
    ageMs: Float?,
    reduceMotion: Boolean,
    origin: Rect?,
    modifier: Modifier = Modifier,
    from: (@Composable () -> Unit)?,
    to: @Composable () -> Unit,
) {
    val colors = Toon.tokens.colors
    val density = LocalDensity.current.density
    BoxWithConstraints(modifier.clipToBounds()) {
        val w = maxWidth.value
        val h = maxHeight.value
        val t = ageMs
        if (t == null || from == null) {
            to()
            return@BoxWithConstraints
        }
        if (reduceMotion) {
            val f = min(1f, t / REDUCED_FADE_MS)
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1 - f }) { from() }
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = f }) { to() }
            return@BoxWithConstraints
        }
        when (kind) {
            ToonTransition.Push -> {
                val a = toonPushAt(t, w)
                // The engine's `translate(ax) scaleAbout(W, H/2)`: the old screen smeared from its right edge.
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        translationX = a.ax * density
                        scaleX = a.asx
                        transformOrigin = TransformOrigin(1f, .5f)
                    },
                ) { from() }
                Box(Modifier.fillMaxSize().graphicsLayer { translationX = a.bx * density }) { to() }
            }

            ToonTransition.Scene -> {
                val a = toonIrisAt(t)
                val cx = origin?.center?.x ?: (w / 2)
                val cy = origin?.center?.y ?: (h - 46)
                // Both screens stay composed; the iris shows the old one until it has closed.
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (a.scene == 0) 1f else 0f }) { from() }
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (a.scene == 1) 1f else 0f }) { to() }
                Canvas(
                    Modifier.fillMaxSize(),
                ) { inDp { drawToonIris(colors, a.k, sizeDp.width, sizeDp.height, cx, cy) } }
            }

            ToonTransition.Hole -> {
                val card = origin ?: Rect(14f, 82f, 14f + (w - 28), 82f + 46)
                Box(Modifier.fillMaxSize()) { from() }
                Canvas(Modifier.fillMaxSize()) {
                    inDp { drawScene(toonHoleTransitionScene(colors, t, card.left, card.top, card.width, card.height)) }
                }
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = toonHoleAt(t).newOp }) { to() }
            }
        }
    }
}
