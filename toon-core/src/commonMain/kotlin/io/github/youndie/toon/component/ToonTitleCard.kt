package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonAnchor
import io.github.youndie.toon.draw.ToonLabel
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonSegments
import io.github.youndie.toon.draw.ToonTextStyle
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.ellipseLayer
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.starPoints
import io.github.youndie.toon.draw.wobblePoints
import io.github.youndie.toon.fx.toonStarsScene
import io.github.youndie.toon.motion.toonSpring
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

// A port of the engine's `titleCard` and `splashAt` and the kit's `C.titlecard` (B-43): an old
// title card — concentric rings flowing toward the centre, a plaque that pops in with an overshoot
// and two bursts of stars, its text arriving once it lands. The splash and the empty state are the
// same card at two sizes.

/** Which title card: the splash a screen opens on, or the empty state of a list with nothing in it. */
public enum class ToonTitleCardVariant { Splash, Empty }

/**
 * The design's title card over [width] × [height]: [title] and [sub] on a plaque among rings, both
 * the [variant]'s own when null ("Saturday" and its line for the splash, "Nothing yet" and its line
 * for the empty state; an empty [sub] is none). It plays its entrance when it appears and again on a
 * click, which also calls [onClick]. With [loop] the rings keep flowing after the entrance, about a
 * ring every 1.6 s, their widths and their speed swelling and ebbing, for a screen that waits (B-55);
 * reduce motion holds them still.
 */
@Composable
public fun ToonTitleCard(
    modifier: Modifier = Modifier,
    variant: ToonTitleCardVariant = ToonTitleCardVariant.Splash,
    title: String? = null,
    sub: String? = null,
    width: Dp = 360.dp,
    height: Dp = 520.dp,
    loop: Boolean = false,
    onClick: () -> Unit = {},
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val texts = rememberToonText()
    val clock = remember { ToonTitleCardClock() }
    val click by rememberUpdatedState(onClick)
    val empty = variant == ToonTitleCardVariant.Empty
    val t = title ?: if (empty) "Nothing yet" else "Saturday"
    val s = sub ?: if (empty) "Your weekend is wide open" else "Plans for the good part of the week"
    val looping = loop && !reduce
    LaunchedEffect(clock.starts, looping) { while (clock.running(looping)) withFrameMillis { clock.tick(it) } }

    Canvas(
        modifier
            .size(width, height)
            .clipToBounds()
            .semantics {
                heading()
                contentDescription = if (s.isEmpty()) t else "$t. $s"
            }.clickable(remember { MutableInteractionSource() }, indication = null) {
                clock.restart()
                click()
            },
    ) {
        // The engine draws in dp (research D5); the canvas is pixels (B-47).
        inDp {
            val w = width.value
            val h = height.value
            val m = toonTitleCardFrameAt(clock.age(looping), reduce, looping)
            val scene = toonTitleCardKitScene(tokens, w, h, empty, t, s, m.motion)
            drawScene(if (m.fade == 1f) scene else scene.placed(0f, 0f, m.fade), texts)
        }
    }
}

/** The entrance plays for 900 ms in the kit; reduce motion — the v1 page's `fade 120` — fades the card at rest. */
private const val ENTER_MS = 900f
private const val REDUCED_FADE_MS = 120f

/** The kit's moment at rest: `splashAt` a long time after, the rings where they stop. */
private const val AT_REST_MS = 1e6f

/** The rings' period: one ring every 34 dp. The two colours alternate, so the picture repeats every two. */
private const val RING_PERIOD = 34f

/** The live rings' entrance: two periods, eased in and out, so it ends on the picture it started from (B-55). */
private const val RINGS_ENTER_MS = 1400f

/** Looping, the rings move a period every 1.6 s on average (B-55). */
private const val LOOP_MS = 1600f

/** The loop's speed swells and ebbs over 4.7 s, between 0.14 and 1.86 of the average, never backwards. */
private const val SURGE_MS = 4700f
private const val SURGE = .4f

/** The loop's width wave: rings from 0.41 to 1.59 of the period wide, a crest every 3.7 rings, round in 2.9 s. */
private const val WARP = .35f
private const val WARP_RINGS = 3.7f
private const val SWIRL_MS = 2900f

/** The loop's speed and width wave come in over this long from the rings at rest. */
private const val LOOP_RAMP_MS = 700f

/** Where the live rings stand: [phase] in dp travelled, never reduced, and the loop's width wave. */
internal data class ToonRingMotion(
    val phase: Float,
    val warp: Float = 0f,
    val swirl: Float = 0f,
)

/**
 * The live card's rings [t] ms in (B-55), a departure from the engine's `splashAt`, whose rings run
 * at a constant 0.04 dp a ms and then jump to where they rest. Here the entrance moves them two
 * periods, eased in and out, and ends on the resting phase; [loop] then keeps them flowing, the speed
 * and the widths of the rings swelling and ebbing, both coming in from rest. The phase is the whole
 * travel, so the colours stay with their rings across a period.
 */
internal fun toonRings(
    t: Float,
    loop: Boolean,
): ToonRingMotion {
    val rest = toonSplashAt(AT_REST_MS).phase % RING_PERIOD
    if (t < RINGS_ENTER_MS) {
        return ToonRingMotion(rest - 2 * RING_PERIOD + 2 * RING_PERIOD * easeInOut(max(0f, t) / RINGS_ENTER_MS))
    }
    if (!loop) return ToonRingMotion(rest)
    val u = t - RINGS_ENTER_MS
    val ramp = 1 - exp(-u / LOOP_RAMP_MS)
    // Rings travelled: the average speed reached from rest, plus a surge that starts and ends still.
    val rings =
        (u - LOOP_RAMP_MS * ramp) / LOOP_MS + SURGE * SURGE_MS / (2 * PI.toFloat() * LOOP_MS) *
            (1 - cos(2 * PI.toFloat() * u / SURGE_MS))
    return ToonRingMotion(rest + RING_PERIOD * rings, WARP * ramp, 2 * PI.toFloat() * u / SWIRL_MS)
}

/** Cubic ease-in-out over 0..1. */
private fun easeInOut(u: Float): Float = if (u < .5f) 4 * u * u * u else 1 - (-2 * u + 2).pow(3) / 2

/** The engine's `splashAt`: the plaque's scale, the rings' travel in dp, the text's opacity and the stars' age. */
internal data class ToonSplashMotion(
    val s: Float = 1f,
    val phase: Float = 0f,
    val textOp: Float = 1f,
    val stars: Float = 0f,
    val warp: Float = 0f,
    val swirl: Float = 0f,
)

/** The engine's `splashAt` at [t] ms: the plaque on a spring, the rings 0.04 dp a ms, text from 160, stars from 120 to 470. */
internal fun toonSplashAt(t: Float): ToonSplashMotion =
    ToonSplashMotion(
        s = if (t < 0) 0f else max(0f, 1 - toonSpring(t / 1000, .4f, 20f)),
        phase = t * .04f,
        textOp = max(0f, min(1f, (t - 160) / 120)),
        stars = if (t > 120 && t < 470) (t - 120) / 350 else 0f,
    )

/** A moment of the card: its motion, and the fade of the whole under reduce motion. */
internal data class ToonTitleCardMoment(
    val motion: ToonSplashMotion,
    val fade: Float = 1f,
)

/**
 * The live card's scene [ageMs] into the entrance, null at rest: `splashAt` at that age with the rings
 * where [toonRings] has them, or the card at rest once it has played; looping, the rings keep
 * moving. Under reduce motion the card at rest fades in over 120 ms, without stars.
 */
internal fun toonTitleCardFrameAt(
    ageMs: Float?,
    reduceMotion: Boolean,
    loop: Boolean = false,
): ToonTitleCardMoment {
    val rest = toonSplashAt(AT_REST_MS)
    if (reduceMotion) return ToonTitleCardMoment(rest, if (ageMs == null) 1f else min(1f, ageMs / REDUCED_FADE_MS))
    if (ageMs == null) return ToonTitleCardMoment(rest)
    val motion = if (ageMs < ENTER_MS) toonSplashAt(ageMs) else rest
    val rings = toonRings(ageMs, loop)
    return ToonTitleCardMoment(motion.copy(phase = rings.phase, warp = rings.warp, swirl = rings.swirl))
}

/** The kit's clock: the entrance plays from the card's first frame, and again on a click. */
@Stable
internal class ToonTitleCardClock {
    var nowMs by mutableLongStateOf(0L)
    var starts by mutableStateOf(0)
    private var at: Long? = PENDING_START

    fun restart() {
        at = PENDING_START
        starts++
    }

    fun tick(now: Long) {
        nowMs = now
        if (at == PENDING_START) at = now
    }

    /** Milliseconds into the entrance, or null once it has played; looping, it never has. */
    fun age(loop: Boolean = false): Float? =
        nowMs
            .let { now -> at?.let { if (it == PENDING_START) 0f else (now - it).toFloat() } }
            ?.takeIf { loop || it < RINGS_ENTER_MS }

    fun running(loop: Boolean = false): Boolean = age(loop) != null
}

private const val PENDING_START = -1L

/** What one frame of the card draws: the engine's `titleCard` options. */
internal data class ToonTitleLook(
    val title: String = "Saturday",
    val sub: String = "",
    val cy: Float? = null,
    val pw: Float = 280f,
    val ph: Float = 120f,
    val fs: Float = 44f,
    val dy: Float = 0f,
    val s: Float = 1f,
    val phase: Float = 0f,
    val textOp: Float = 1f,
    val warp: Float = 0f,
    val swirl: Float = 0f,
)

private val PLAQUE_RADII = ToonRadii(26f, 18f, 30f, 20f)
private val DASH_RADII = ToonRadii(18f, 12f, 22f, 14f)
private val DASH = floatArrayOf(2f, 5f)
private val SUB_STYLE = ToonTextStyle(600, 14f)

/** The engine's `titleCard`: the rings over [w] × [h], then the plaque and its text. */
@OptIn(InternalToonApi::class)
internal fun toonTitleCardScene(
    tokens: ToonTokens,
    w: Float,
    h: Float,
    o: ToonTitleLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val cx = w / 2
    val cy = o.cy ?: (h * .42f)
    val layers = mutableListOf<ToonLayer>()
    toonRingRadii(hypot(w, h) * .62f, o.phase, o.warp, o.swirl).forEach { (r, odd) ->
        layers += ellipseLayer(cx, cy, r, r * .96f, if (odd) c.stage else c.surfacePressed)
    }
    val px = cx - o.pw / 2
    val py = cy - o.ph / 2 + o.dy
    val plaque = mutableListOf<ToonLayer>()
    plaque +=
        bodyLayers(
            shapePoints(px, py, o.pw, o.ph, PLAQUE_RADII, sh.insets, sh.wobble, sh.seed + 2200),
            c.surface,
            c,
            sh.shadow + 3,
        )
    plaque +=
        smoothLayer(
            wobblePoints(px + 9, py + 9, o.pw - 18, o.ph - 18, DASH_RADII, sh.wobble * .6f, sh.seed + 2201),
            c.ink,
            .55f,
            stroke = 1.6f,
            dash = DASH,
        )
    listOf(Triple(px + 20, py + 20, 0), Triple(px + o.pw - 20, py + o.ph - 20, 1)).forEach { (sx, sy, j) ->
        plaque += ToonLayer(starPoints(7.5f, 3.4f, 90 + j), ToonSegments.Line, true, c.ink, dx = sx, dy = sy)
        plaque += ToonLayer(starPoints(5.4f, 2.4f, 90 + j), ToonSegments.Line, true, c.warning, dx = sx, dy = sy)
    }
    val top = o.textOp * max(0f, min(1f, (o.s - .9f) / .1f))
    val labels = mutableListOf<ToonLabel>()
    labels +=
        ToonLabel(
            o.title,
            cx,
            py + o.ph / 2 + (if (o.sub.isNotEmpty()) 4 else 15),
            c.text,
            top,
            ToonTextStyle(400, o.fs, display = true),
            ToonAnchor.Middle,
        )
    if (o.sub.isNotEmpty()) {
        labels +=
            ToonLabel(o.sub, cx, py + o.ph / 2 + 32, c.textMuted, top, SUB_STYLE, ToonAnchor.Middle)
    }
    return ToonScene(layers, emptyList()) + ToonScene(plaque, emptyList()).scaledAbout(cx, cy + o.dy, o.s, o.s) +
        ToonScene(emptyList(), labels)
}

/**
 * The rings from the outside in: each radius over [base], and whether it is the second colour. The
 * engine draws the [phase] modulo the period from the first colour; here a whole period travelled
 * also swaps the colours, so the picture moves with the phase and never jumps (B-55). [warp] bends
 * the widths into a wave turned by [swirl], one more ring outside so the edge stays covered.
 */
internal fun toonRingRadii(
    base: Float,
    phase: Float,
    warp: Float = 0f,
    swirl: Float = 0f,
): List<Pair<Float, Boolean>> {
    val turn = floor(phase / RING_PERIOD).toInt() - if (warp > 0f) 1 else 0
    var lin = base + (phase - turn * RING_PERIOD)
    var i = turn
    val rings = mutableListOf<Pair<Float, Boolean>>()
    while (true) {
        val r =
            if (warp >
                0f
            ) {
                lin + RING_PERIOD * warp * sin(2 * PI.toFloat() * (lin - base) / (RING_PERIOD * WARP_RINGS) + swirl)
            } else {
                lin
            }
        if (r <= 24) break
        rings += r to ((i and 1) == 1)
        lin -= RING_PERIOD
        i++
    }
    return rings
}

/** The kit's `scene`: the card at its variant's size, and the stars bursting over it during the entrance. */
internal fun toonTitleCardKitScene(
    tokens: ToonTokens,
    w: Float,
    h: Float,
    empty: Boolean,
    title: String,
    sub: String,
    a: ToonSplashMotion,
): ToonScene {
    val look =
        ToonTitleLook(
            title = title,
            sub = sub,
            cy = if (empty) h * .4f else h * .42f,
            pw = if (empty) 260f else 280f,
            ph = if (empty) 110f else 120f,
            fs = if (empty) 34f else 44f,
            s = a.s,
            phase = a.phase,
            textOp = a.textOp,
            warp = a.warp,
            swirl = a.swirl,
        )
    return toonTitleCardScene(tokens, w, h, look) + toonStarsScene(tokens.colors, a.stars, w / 2, h * .42f - 60, 90f)
}
