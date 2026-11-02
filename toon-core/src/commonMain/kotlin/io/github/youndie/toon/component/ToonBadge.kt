package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonInsets
import io.github.youndie.toon.draw.ToonLabel
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonSegments
import io.github.youndie.toon.draw.ToonTextStyle
import io.github.youndie.toon.draw.burstPoints
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.fx.drawToonFlash
import io.github.youndie.toon.motion.toonOscillation
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

// A port of the engine's `badge` and the kit's `C.badge` (B-15): a star-burst with the count that
// pops in from a point (`badgeAppear`), swaps numbers by flinging the old one up and dropping the new
// one in while the burst shivers (`badgeCount`), shrinks away at zero, a dot without a count, and
// `99+` past 99.

/**
 * The design's badge: [count] in a red star-burst, or a plain dot when [dot]. It pops in when it
 * first shows, and again when [count] rises from 0; a new count swaps in; at 0 it shrinks away.
 */
@Composable
public fun ToonBadge(
    count: Int = 3,
    modifier: Modifier = Modifier,
    dot: Boolean = false,
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val text = rememberToonText()
    val clock = remember { ToonBadgeClock(count) }
    LaunchedEffect(count) { clock.changed(count) }
    LaunchedEffect(clock.starts) {
        while (clock.running()) withFrameMillis { clock.tick(it) }
    }
    val moment =
        toonBadgeFrameAt(
            clock.n,
            clock.old,
            dot,
            clock.age(clock.appearAt),
            clock.age(clock.countAt),
            clock.age(clock.hideAt),
            reduce,
        )
    Canvas(
        modifier.size(BADGE_W.dp, BADGE_H.dp).semantics {
            if (!dot && count > 0) this.text = AnnotatedString(badgeLabel(count))
        },
    ) {
        if (moment.flash != null) drawToonFlash(tokens.colors, moment.flash, CX, CY, FLASH_SIZE)
        moment.look?.let { drawScene(toonBadgeScene(tokens, it).placed(CX, CY), text) }
    }
}

/** The kit's box: 28 × 24, the burst centred in it. */
private const val BADGE_W = 28f
private const val BADGE_H = 24f
private const val CX = 14f
private const val CY = 12f
private const val FLASH_SIZE = 15f

/** Appear and count change last 400 ms, the hide 200 (drawn for 120); the flash 220. */
private const val APPEAR_MS = 400f
private const val COUNT_MS = 400f
private const val HIDE_MS = 120f
private const val FLASH_MS = 220f

/** The count as the badge writes it: `99+` past 99. */
internal fun badgeLabel(count: Int): String = if (count > 99) "99+" else count.toString()

/** What the badge draws at a moment: its [look], or null when hidden, and the appear flash's life. */
internal class ToonBadgeMoment(
    val look: ToonBadgeLook?,
    val flash: Float?,
)

/**
 * The kit's `C.badge.scene` over time: count [n] after [old], the appear, count change and hide
 * [appearAgeMs], [countAgeMs], [hideAgeMs] ago (null when not started). Under reduce motion — the
 * MVP page's `instant` for both the appear and the count change — every animation is already over.
 */
internal fun toonBadgeFrameAt(
    n: Int,
    old: Int?,
    dot: Boolean,
    appearAgeMs: Float?,
    countAgeMs: Float?,
    hideAgeMs: Float?,
    reduceMotion: Boolean,
): ToonBadgeMoment {
    val ta = if (reduceMotion) appearAgeMs?.let { SETTLED_MS } else appearAgeMs
    val tc = if (reduceMotion) countAgeMs?.let { SETTLED_MS } else countAgeMs
    val th = if (reduceMotion) hideAgeMs?.let { SETTLED_MS } else hideAgeMs
    if (dot) return ToonBadgeMoment(ToonBadgeLook("", dot = true, s = ta?.let { toonBadgeAppear(it).s } ?: 1f), null)
    if (n <= 0) {
        if (th == null || th > HIDE_MS) return ToonBadgeMoment(null, null)
        return ToonBadgeMoment(ToonBadgeLook(badgeLabel(old ?: 1), s = 1 - th / HIDE_MS), null)
    }
    val ap = ta?.let { toonBadgeAppear(it) } ?: ToonBadgeAppear(1f, 1f)
    var look = ToonBadgeLook(badgeLabel(n), s = ap.s, numOp = ap.numOp)
    if (tc != null && tc < COUNT_MS) {
        val c = toonBadgeCount(tc)
        look =
            look.copy(
                old = old?.let(::badgeLabel),
                oldDy = c.oldDy,
                oldOp = c.oldOp,
                numDy = c.numDy,
                numOp = min(look.numOp, c.numOp),
                dx = c.dx,
            )
    }
    return ToonBadgeMoment(look, ta?.takeIf { it < FLASH_MS }?.let { it / FLASH_MS })
}

/** The kit's age of an animation under reduce motion: long finished. */
private const val SETTLED_MS = 1e6f

/** The engine's `badgeAppear`: scale [s] and the number's opacity at [t] ms. */
internal class ToonBadgeAppear(
    val s: Float,
    val numOp: Float,
)

/** The engine's `badgeAppear`: from a point to 1.3 in 90 ms, then a spring to 1; the number from 42 ms. */
internal fun toonBadgeAppear(t: Float): ToonBadgeAppear {
    if (t < 0) return ToonBadgeAppear(0f, 0f)
    val s = if (t < 90) 1.3f * (1 - (1 - t / 90).pow(2)) else 1 + .3f * toonOscillation((t - 90) / 1000, .4f, 45f)
    return ToonBadgeAppear(s, if (t > 42) 1f else 0f)
}

/** The engine's `badgeCount` at [t] ms: the old number's flight and fade, the new one's drop, the shiver. */
internal class ToonBadgeCount(
    val oldDy: Float,
    val oldOp: Float,
    val numDy: Float,
    val numOp: Float,
    val dx: Float,
)

internal fun toonBadgeCount(t: Float): ToonBadgeCount {
    val u = c01(t / 100)
    val v = c01((t - 60) / 120)
    return ToonBadgeCount(
        oldDy = -14 * u * u,
        oldOp = 1 - u,
        numDy = -12 * (1 - v).pow(2),
        numOp = c01((t - 60) / 40),
        dx = if (t < 240) 2 * sin(2 * PI.toFloat() * t / 60) * exp(-t / 90) else 0f,
    )
}

private fun c01(v: Float): Float = max(0f, min(1f, v))

@Stable
private class ToonBadgeClock(
    initial: Int,
) {
    var nowMs by mutableLongStateOf(0L)
    var starts by mutableStateOf(0)
    var n by mutableStateOf(initial)
    var old: Int? by mutableStateOf(null)
    var appearAt: Long? = PENDING_START
    var countAt: Long? = null
    var hideAt: Long? = null

    /** The kit's `changed`: a rise from 0 pops in, a fall to 0 hides, anything else swaps. */
    fun changed(count: Int) {
        if (count == n) return
        old = n
        when {
            n <= 0 && count > 0 -> appearAt = PENDING_START
            count <= 0 -> hideAt = PENDING_START
            else -> countAt = PENDING_START
        }
        n = count
        starts++
    }

    fun tick(now: Long) {
        nowMs = now
        if (appearAt == PENDING_START) appearAt = now
        if (countAt == PENDING_START) countAt = now
        if (hideAt == PENDING_START) hideAt = now
    }

    fun age(at: Long?): Float? = at?.let { if (it == PENDING_START) 0f else (nowMs - it).toFloat() }

    fun running(): Boolean =
        listOf(age(appearAt) to APPEAR_MS, age(countAt) to COUNT_MS, age(hideAt) to HIDE_MS).any { (a, d) ->
            a !=
                null &&
                a < d
        }
}

private const val PENDING_START = -1L

/** What one frame of the badge draws: the engine's `badge` options. */
internal data class ToonBadgeLook(
    val n: String,
    val dot: Boolean = false,
    val s: Float = 1f,
    val numOp: Float = 1f,
    val numDy: Float = 0f,
    val old: String? = null,
    val oldOp: Float = 0f,
    val oldDy: Float = 0f,
    val dx: Float = 0f,
)

/** The badge's number: 12 sp, extra bold, in the theme's body face. */
private val BADGE_NUMBER = ToonTextStyle(800, 12f)

/** The engine's `badge`: one frame about the origin, the burst's centre. */
@OptIn(InternalToonApi::class)
internal fun toonBadgeScene(
    tokens: ToonTokens,
    o: ToonBadgeLook,
): ToonScene {
    val c = tokens.colors
    val s = o.s
    if (s < .02f) return ToonScene.Empty
    if (o.dot) {
        val r = 5.5f * s
        val e = r + 1.5f
        val g = shapePoints(-e, -e, 2 * e, 2 * e, ToonRadii(e, e, e, e), DOT_INSETS, .3f, tokens.shape.seed + 950, 2f)
        return ToonScene(listOf(smoothLayer(g.ink, c.ink), smoothLayer(g.fill, c.danger)), emptyList())
    }
    val rx = (12 + (o.n.length - 1) * 4.6f) * s
    val ry = 12 * s
    val layers =
        listOf(
            ToonLayer(burstPoints(rx + 3.2f, ry + 3.2f, 9, 960), ToonSegments.Line, true, c.ink, dx = o.dx),
            ToonLayer(burstPoints(rx, ry, 9, 960), ToonSegments.Line, true, c.danger, dx = o.dx),
        )
    val labels =
        buildList {
            add(ToonLabel(o.n, o.dx, 4.3f + o.numDy, c.onDanger, o.numOp, BADGE_NUMBER))
            if (o.old != null &&
                o.oldOp > .01f
            ) {
                add(ToonLabel(o.old, o.dx, 4.3f + o.oldDy, c.onDanger, o.oldOp, BADGE_NUMBER))
            }
        }
    return ToonScene(layers, labels)
}

private val DOT_INSETS = ToonInsets(1.4f, 1.4f, 1.9f, 1.4f)
