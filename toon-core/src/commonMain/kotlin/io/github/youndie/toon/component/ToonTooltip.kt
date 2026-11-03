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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonAnchor
import io.github.youndie.toon.draw.ToonInsets
import io.github.youndie.toon.draw.ToonLabel
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonSegments
import io.github.youndie.toon.draw.ToonTextStyle
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.motion.toonSpring
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

// A port of the engine's `tooltip` and `tipInflate` and the kit's `C.tooltip` (B-40): a speech
// balloon whose tail tip is its origin, blown up from that tip with a wobble, its text in once it is
// past 0.85; hidden, it deflates in 120 ms. The paper inside the tail reaches 3.5 dp further up into
// the balloon than the engine's (B-60): the engine's stops 0.2 dp short of the balloon's paper in
// Bright Cel, whose bottom inset is the deepest, and leaves an ink hairline across the joint.

/**
 * The design's tooltip: [text] in a balloon whose tail points down at the bottom middle of its box,
 * which inflates when [show] turns true and deflates when it turns false. The box is the balloon's
 * width and 46 dp tall; the balloon overshoots it while it inflates.
 */
@Composable
public fun ToonTooltip(
    text: String,
    modifier: Modifier = Modifier,
    show: Boolean = true,
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val texts = rememberToonText()
    val textWidth = remember(text, texts) { texts.measure(text, TIP_TEXT).multiParagraph.width }
    val w = ceil(toonTooltipWidth(textWidth))
    val clock = remember { ToonTooltipClock(show) }
    LaunchedEffect(show) { clock.changed(show) }
    LaunchedEffect(clock.starts) { while (clock.running()) withFrameMillis { clock.tick(it) } }

    Canvas(
        modifier
            .size(w.dp, BOX_H.dp)
            .semantics {
                if (clock.show) {
                    contentDescription = text
                    liveRegion = LiveRegionMode.Polite
                }
            },
    ) {
        // The engine draws in dp (research D5); the canvas is pixels (B-47).
        inDp {
            val m = toonTooltipFrameAt(clock.show, clock.age(), reduce) ?: return@inDp
            val scene = toonTooltipScene(tokens, text, textWidth, m.s).placed(w / 2, BOX_H, m.fade)
            drawScene(scene, texts)
        }
    }
}

/** The kit's box: as wide as the balloon, 46 dp tall, the tail's tip at its bottom middle. */
private const val BOX_H = 46f

/** The balloon is its text and 14 dp a side. */
internal fun toonTooltipWidth(textWidth: Float): Float = textWidth + 28

/** The kit plays a show or a hide for 500 ms; reduce motion — the v1 page's `fade 120` — fades. */
private const val PLAY_MS = 500f
private const val REDUCED_FADE_MS = 120f

/** The engine's `tipInflate` at [t] ms: blown up on a wobbly spring when [show], let down in 120 ms when not. */
internal fun toonTipInflate(
    t: Float,
    show: Boolean,
): Float = if (show) max(0f, 1 - toonSpring(t / 1000, .3f, 30f)) else max(0f, 1 - max(0f, min(1f, t / 120)))

/** The balloon's scale [s], and the fade of the whole under reduce motion. */
internal data class ToonTooltipMoment(
    val s: Float,
    val fade: Float = 1f,
)

/**
 * The kit's scene at a moment: [show], changed [ageMs] ago (null at rest). Null when nothing shows.
 * Under reduce motion the balloon fades in and out at its size.
 */
internal fun toonTooltipFrameAt(
    show: Boolean,
    ageMs: Float?,
    reduceMotion: Boolean,
): ToonTooltipMoment? {
    if (reduceMotion && ageMs != null) {
        val f = min(1f, ageMs / REDUCED_FADE_MS)
        if (!show && f >= 1) return null
        return ToonTooltipMoment(1f, if (show) f else 1 - f)
    }
    val s = if (ageMs == null) (if (show) 1f else 0f) else toonTipInflate(ageMs, show)
    return if (s < .02f) null else ToonTooltipMoment(s)
}

/** The kit's clock: a show or a hide plays 500 ms from its first frame, then the tooltip is at rest. */
@Stable
internal class ToonTooltipClock(
    initial: Boolean,
) {
    var nowMs by mutableLongStateOf(0L)
    var starts by mutableStateOf(0)
    var show by mutableStateOf(initial)
    private var at: Long? = if (initial) PENDING_START else null

    fun changed(now: Boolean) {
        if (now == show) return
        show = now
        at = PENDING_START
        starts++
    }

    fun tick(now: Long) {
        nowMs = now
        if (at == PENDING_START) at = now
    }

    /** Milliseconds since the change, or null at rest — as the kit's `t` is. */
    fun age(): Float? =
        nowMs
            .let { now -> at?.let { if (it == PENDING_START) 0f else (now - it).toFloat() } }
            ?.takeIf { it < PLAY_MS }

    fun running(): Boolean = age() != null
}

private const val PENDING_START = -1L

private val BALLOON_RADII = ToonRadii(14f, 16f, 13f, 15f)
internal val TIP_TEXT = ToonTextStyle(600, 14f)

/** The tail: two curves from the balloon's foot to the tip at the origin, and the paper inside it. */
private val TAIL = floatArrayOf(-7f, -14f, -2f, -6f, 1f, 0f, 4f, -7f, 9f, -14f)
private val TAIL_IN_AS_DESIGNED = floatArrayOf(-4.6f, -15.5f, -1.5f, -8f, .9f, -3.4f, 3f, -8.5f, 6.6f, -15.5f)

/**
 * The engine's paper with both sides carried on straight along their tangents from y −15.5 to −19, so
 * it overlaps the balloon's paper in every theme: the balloon's paper ends 12 dp + 0.8 × its bottom
 * inset above the tip, give or take 1.12 × 0.8² × its wobble — 15.7 ± 0.4 in Bright Cel, 15.2 ± 0.8
 * in Ink & Gouache. The part below −15.5, the only part that shows, is the engine's.
 */
private val TAIL_IN =
    floatArrayOf(
        -6.05f,
        -19f,
        -5.325f,
        -17.25f,
        -4.6f,
        -15.5f,
        -1.5f,
        -8f,
        .9f,
        -3.4f,
        3f,
        -8.5f,
        6.6f,
        -15.5f,
        7.5f,
        -17.25f,
        8.4f,
        -19f,
    )

/**
 * The engine's `tooltip`: the balloon over [text], [textWidth] wide, scaled by [s] about its tail's tip
 * at the origin. The text is not scaled; it shows once the balloon is past 0.85. [asDesigned] draws the
 * tail's paper as the engine does, short of the balloon's (B-60), for the engine-frame test.
 */
@OptIn(InternalToonApi::class)
internal fun toonTooltipScene(
    tokens: ToonTokens,
    text: String,
    textWidth: Float,
    s: Float,
    asDesigned: Boolean = false,
): ToonScene {
    if (s < .02f) return ToonScene.Empty
    val c = tokens.colors
    val sh = tokens.shape
    val w = toonTooltipWidth(textWidth)
    val h = 34f
    val by = -h - 12
    val ins = sh.insets.let { ToonInsets(it.top * .8f, it.right * .8f, it.bottom * .8f, it.left * .8f) }
    val g = shapePoints(-w / 2, by, w, h, BALLOON_RADII, ins, sh.wobble * .8f, sh.seed + 1900, 4f)
    val balloon =
        ToonScene(
            listOf(
                smoothLayer(g.ink, c.ink, dx = 3f, dy = 3f),
                ToonLayer(TAIL, ToonSegments.Quad, true, c.ink),
                smoothLayer(g.ink, c.ink),
                smoothLayer(g.fill, c.surface),
                ToonLayer(if (asDesigned) TAIL_IN_AS_DESIGNED else TAIL_IN, ToonSegments.Quad, true, c.surface),
            ),
            emptyList(),
        ).scaledAbout(0f, 0f, s, s)
    val op = max(0f, min(1f, (s - .85f) / .15f))
    return balloon +
        ToonScene(emptyList(), listOf(ToonLabel(text, 0f, by + 22, c.text, op, TIP_TEXT, ToonAnchor.Middle)))
}
