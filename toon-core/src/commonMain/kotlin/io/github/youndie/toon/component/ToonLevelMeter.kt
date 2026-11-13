package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonInsets
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonSegments
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.burstPoints
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.inkLinePoints
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.icon.ToonIcon
import io.github.youndie.toon.icon.ToonIconGlyph
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.type.withBacking
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

// A port of the desktop extension's `levelMeter` and `livelevel` and of `ToonLevelMeter.dc.html`
// (B-74, research §1.8): a channel's level as a strip of cel blocks, green, the last three amber, red
// when it clips. One of the two things a recording window moves by itself (the record mark is the
// other), so it says silence and clipping in words as well as in the strip: a status is never colour
// alone.

/** The `state` prop of `ToonLevelMeter.dc.html`. */
public enum class ToonLevelState {
    /** A level is coming in. */
    Live,

    /** Nothing is coming in: the strip is dark and struck through, and the header says so. */
    Silent,

    /** Too loud: the strip is full, its last blocks red, a burst after it, and the header says so. */
    Clip,
}

/**
 * The design's level meter, [width] wide (held to the design's 160–600 dp): [icon], [label] and
 * [source] over a strip of blocks lit to [level], 0 to 1. With [animate] a live strip moves on its own
 * around [level] — the extension's `livelevel`, [seed] setting its phase — and with reduced motion it
 * holds [level]. [state] [ToonLevelState.Silent] and [ToonLevelState.Clip] are said in the header too,
 * as [silentLabel] and [clipLabel].
 */
@Composable
public fun ToonLevelMeter(
    label: String,
    modifier: Modifier = Modifier,
    source: String = "",
    icon: ToonIconGlyph = ToonIcons.Mic,
    state: ToonLevelState = ToonLevelState.Live,
    level: Float = .6f,
    animate: Boolean = true,
    width: Dp = 320.dp,
    seed: Int = 0,
    silentLabel: String = "Silence",
    clipLabel: String = "Too loud",
) {
    val tokens = Toon.tokens
    val c = tokens.colors
    val w = width.value.coerceIn(MIN_W, MAX_W)
    val stripW = w - if (state == ToonLevelState.Clip) CLIP_ROOM else 0f
    val alive = state != ToonLevelState.Silent && animate && !Toon.reduceMotion
    // Ms since the strip began to move, on the frame clock; read only in the draw.
    var age by remember { mutableLongStateOf(0L) }
    LaunchedEffect(alive) {
        age = 0L
        if (!alive) return@LaunchedEffect
        val start = withFrameMillis { it }
        while (true) withFrameMillis { age = it - start }
    }
    // A strip is the same scene for the same lit count: built once per count, not per frame.
    val scenes = remember(tokens, stripW, state) { HashMap<Int, ToonScene>() }
    val backing = Toon.fonts.bodyCyrillic

    @Composable
    fun Words(
        s: String,
        weight: Int,
        size: Float,
        color: Color,
        modifier: Modifier = Modifier,
    ) = BasicText(
        withBacking(s, backing, size, weight),
        modifier,
        TextStyle(fontFamily = Toon.fonts.body, fontWeight = FontWeight(weight), fontSize = size.sp, color = color),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        softWrap = false,
    )
    Column(modifier.width(w.dp), verticalArrangement = Arrangement.spacedBy(ROW_GAP.dp)) {
        // The design's flex row — icon, label, source, status, 10 dp apart — measured so the status
        // is never cut: it and the icon take their room first, the label what is left, the source the
        // rest, each ending in an ellipsis where it must.
        Layout(
            content = {
                ToonIcon(icon, contentDescription = null, size = ICON.dp, color = c.text)
                Words(label, 700, 15f, c.text)
                Words(source, 600, 13f, c.textMuted)
                when (state) {
                    ToonLevelState.Silent -> Words(silentLabel, 800, 13f, c.text)
                    ToonLevelState.Clip -> Words(clipLabel, 800, 13f, c.dangerText)
                    ToonLevelState.Live -> Box(Modifier)
                }
            },
            modifier = Modifier.width(w.dp).heightIn(min = HEADER_MIN.dp),
        ) { measurables, constraints ->
            val gap = HEADER_GAP.dp.roundToPx()
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val (iconM, labelM, sourceM, statusM) = measurables
            val iconP = iconM.measure(loose)
            val statusP = statusM.measure(loose)
            val room = { used: Int -> (constraints.maxWidth - used).coerceAtLeast(0) }
            val labelP = labelM.measure(loose.copy(maxWidth = room(iconP.width + statusP.width + 3 * gap)))
            val sourceP =
                sourceM.measure(
                    loose.copy(
                        maxWidth =
                            room(iconP.width + labelP.width + statusP.width + 3 * gap),
                    ),
                )
            val placed = listOf(iconP, labelP, sourceP, statusP)
            val h = maxOf(constraints.minHeight, placed.maxOf { it.height })
            layout(constraints.maxWidth, h) {
                var x = 0
                placed.forEach { p ->
                    p.placeRelative(x, (h - p.height) / 2)
                    x += p.width + gap
                }
            }
        }
        Canvas(Modifier.width(w.dp).height(STRIP_H.dp)) {
            val drawn =
                when {
                    state == ToonLevelState.Clip -> 1f
                    alive -> (toonLiveLevel(age.toDouble(), seed) * level / LIVE_MEAN).toFloat()
                    else -> level
                }
            val lit = toonLevelLit(stripW, state, drawn)
            // The engine draws in dp (research D5); the canvas is pixels (B-47).
            inDp { drawScene(scenes.getOrPut(lit) { toonLevelMeterScene(tokens, stripW, state, lit) }) }
        }
    }
}

/** The extension's `livelevel`: a channel's level at [t] ms, three sines round 0.42, by [seed]'s phase. */
internal fun toonLiveLevel(
    t: Double,
    seed: Int,
): Double {
    val s = t / 1000 + seed * 7.3
    return (.42 + .22 * sin(s * 5.1) + .14 * sin(s * 13.7 + 1) + .08 * sin(s * 29 + 2)).coerceIn(0.0, 1.0)
}

/** How many blocks a strip [w] dp wide holds: one per 13 dp, at least eight. */
internal fun toonLevelBlocks(w: Float): Int = max(8, floor((w + 3) / 13).toInt())

/** How many of them [level] lights; none while silent. */
internal fun toonLevelLit(
    w: Float,
    state: ToonLevelState,
    level: Float,
): Int = if (state == ToonLevelState.Silent) 0 else (level.coerceIn(0f, 1f) * toonLevelBlocks(w)).roundToInt()

/** The extension's `levelMeter`: the strip [w] dp wide and 20 tall with [lit] blocks lit; no words. */
@OptIn(InternalToonApi::class)
internal fun toonLevelMeterScene(
    tokens: ToonTokens,
    w: Float,
    state: ToonLevelState,
    lit: Int,
): ToonScene {
    val c = tokens.colors
    val seed = tokens.shape.seed
    val n = toonLevelBlocks(w)
    val bw = (w - (n - 1) * GAP) / n
    val layers = mutableListOf<ToonLayer>()
    for (i in 0 until n) {
        val on = i < lit
        val hot = i >= n - 3
        val fill =
            when {
                !on -> c.trackOff
                state == ToonLevelState.Clip && hot -> c.danger
                hot -> c.warning
                else -> c.success
            }
        // An unlit block is 3 dp shorter, from below, its ink faded.
        val shape =
            shapePoints(
                i * (bw + GAP),
                if (on) 0f else 3f,
                bw,
                if (on) STRIP_H else STRIP_H - 3,
                BLOCK_RADII,
                BLOCK_INSETS,
                .3f,
                seed + 4000 + i,
                3f,
            )
        layers += bodyLayers(shape, fill, c, shadow = 0f, inkAlpha = if (on) 1f else .45f, rim = null)
    }
    if (state == ToonLevelState.Silent) {
        layers +=
            ToonLayer(inkLinePoints(2f, w - 2, STRIP_H / 2 + 1, 2.2f, seed + 4050), ToonSegments.Line, true, c.ink, .8f)
    }
    if (state == ToonLevelState.Clip) {
        // The burst after the strip, in the room the clip keeps; the engine writes its place to 0.1.
        val x = ((w + 12) * 10).roundToInt() / 10f
        layers +=
            ToonLayer(burstPoints(9f, 9f, 7, BURST_SEED), ToonSegments.Line, true, c.ink, dx = x, dy = STRIP_H / 2)
        layers +=
            ToonLayer(
                burstPoints(6.5f, 6.5f, 7, BURST_SEED),
                ToonSegments.Line,
                true,
                c.danger,
                dx = x,
                dy =
                    STRIP_H / 2,
            )
    }
    return ToonScene(layers, emptyList())
}

/** The design's numbers: the width editor, the strip, its blocks and the header over it. */
private const val MIN_W = 160f
private const val MAX_W = 600f
private const val STRIP_H = 20f
private const val GAP = 3f
private const val CLIP_ROOM = 24f
private const val ROW_GAP = 8f
private const val HEADER_MIN = 22f
private const val HEADER_GAP = 10f
private const val ICON = 20f
private val BLOCK_RADII = ToonRadii(4f, 3f, 4f, 3f)
private val BLOCK_INSETS = ToonInsets(1.1f, 1.2f, 1.8f, 1.1f)

/** The burst's seed is the engine's own, not the theme's. */
private const val BURST_SEED = 4060

/** `livelevel` swings round 0.42: the design scales it by `level / 0.7`, so 0.7 is the level it means. */
private const val LIVE_MEAN = .7
