package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonLabel
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonTextStyle
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.roundRectPoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.wobbleAlongNormals
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// A port of the desktop extension's `sticker`, `blobPts` and `blobPath` (B-70, research §1.8): a
// speaker in a call, told apart by its shape and letter as well as its colour, since a call can have
// eight voices and a theme has three accents. It is still — no idle — because a transcript draws one
// per turn, 1,500 turns in a two-hour call.

/** The `kind` prop of `ToonSpeakerSticker.dc.html`. */
public enum class ToonSpeakerKind {
    /** A named speaker: one of eight shapes in one of three accents. */
    Named,

    /** A voice without a name, or an expected participant who never spoke: a dashed outline on paper. */
    Unnamed,

    /** The user: a capsule in the primary colour. */
    Me,
}

/** The `size` prop of `ToonSpeakerSticker.dc.html`: 24, 32 or 48 dp. */
public enum class ToonSpeakerSize(
    internal val dp: Float,
) {
    Small(24f),
    Medium(32f),
    Large(48f),
}

/**
 * The design's speaker sticker: [letter] on one of eight [shape]s in one of the theme's three accents
 * ([color]), or the user's capsule, or an unnamed voice's dashed outline ([kind]). [shape] and [color]
 * wrap, so a speaker's index can be passed as it is. A letter of two or more characters is set smaller.
 *
 * It is decoration beside the speaker's name, so it says nothing to a screen reader unless given a
 * [contentDescription].
 */
@Composable
public fun ToonSpeakerSticker(
    letter: String,
    modifier: Modifier = Modifier,
    kind: ToonSpeakerKind = ToonSpeakerKind.Named,
    shape: Int = 0,
    color: Int = 0,
    size: ToonSpeakerSize = ToonSpeakerSize.Medium,
    contentDescription: String? = null,
) {
    val tokens = Toon.tokens
    val text = rememberToonText()
    // The engine caches each sticker by its look; a transcript draws the same few hundreds of times.
    val scene =
        remember(tokens, size, kind, shape, color, letter) {
            toonSpeakerStickerScene(tokens, size.dp, kind, shape, color, letter)
        }
    val described = contentDescription?.let { d -> Modifier.semantics { this.contentDescription = d } } ?: Modifier
    Canvas(modifier.size(size.dp.dp).then(described)) {
        // The engine draws in dp (research D5); the canvas is pixels (B-47).
        inDp { drawScene(scene, text) }
    }
}

/** The extension's `accents`: Bright Cel's stickers are all paper, so its card colours stand in. */
private fun accentsOf(tokens: ToonTokens): List<Color> {
    val brightCel = tokens.name == "brightCel"
    return if (brightCel) tokens.colors.cardFills else tokens.colors.stickers
}

/**
 * The extension's `blobPts`: [n] points round ([cx], [cy]) at radius [r] shaped by one of eight radial
 * functions — circle, squircle, rounded triangle, rounded hexagon, four-petal, tilted ellipse, rounded
 * diamond, five-lobed flower — or, for [ToonSpeakerKind.Me] (`shape` null), a capsule.
 */
internal fun toonSpeakerBlobPoints(
    shape: Int?,
    cx: Float,
    cy: Float,
    r: Float,
    n: Int = 60,
): FloatArray {
    if (shape == null) {
        val k = r * .78f
        return roundRectPoints(cx - k, cy - r, r * 1.56f, r * 2, ToonRadii(k, k, k, k), max(1.2f, r / 9))
    }

    fun poly(
        k: Int,
        th0: Double,
        th: Double,
    ): Double {
        val s = 2 * PI / k
        val a = (((th - th0) % s) + s) % s - s / 2
        return cos(PI / k) / cos(a)
    }
    val rf: (Double) -> Double =
        when (shape) {
            0 -> { _ -> .93 }
            1 -> { th -> .86 * (abs(cos(th)).pow(4) + abs(sin(th)).pow(4)).pow(-.25) }
            2 -> { th -> 1.22 * (.7 * poly(3, -PI / 2, th) + .3 * .5) }
            3 -> { th -> .98 * (.8 * poly(6, 0.0, th) + .2 * .866) }
            4 -> { th -> .87 + .11 * abs(cos(4 * th)) }
            5 -> { th -> (th + .44).let { t -> .99 / sqrt(cos(t).pow(2) + (sin(t) / .76).pow(2)) } }
            6 -> { th -> 1.1 * (.72 * poly(4, -PI / 2, th) + .28 * .707) }
            else -> { th -> .81 + .19 * cos(5 * (th + PI / 2)) }
        }
    val dy = if (shape == 2) r * .12 else 0.0
    val out = FloatArray(2 * n)
    for (i in 0 until n) {
        val th = -PI / 2 + i.toDouble() / n * 2 * PI
        val rr = r * rf(th)
        out[2 * i] = (cx + cos(th) * rr).toFloat()
        out[2 * i + 1] = (cy + dy + sin(th) * rr).toFloat()
    }
    return out
}

/** The design's letter: the display face at weight 400. */
private fun letterStyle(fs: Float) = ToonTextStyle(400, fs, display = true)

/** The extension's `sticker`: one sticker [s] dp square, from its top left. */
@OptIn(InternalToonApi::class)
internal fun toonSpeakerStickerScene(
    tokens: ToonTokens,
    s: Float,
    kind: ToonSpeakerKind,
    shape: Int,
    color: Int,
    letter: String,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val shp = if (kind == ToonSpeakerKind.Me) null else shape.mod(8)
    val k = s / 32
    val r = s / 2 - 1
    val cx = s / 2
    val cy = s / 2
    val amp = sh.wobble * .4f * k
    val seed = sh.seed + 3000 + (shp?.let { it * 17 } ?: 200)
    val heavy = if (kind == ToonSpeakerKind.Me) 3.1f else 2.3f
    val inner = wobbleAlongNormals(toonSpeakerBlobPoints(shp, cx, cy - .75f * k, r - heavy * k), amp * .8f, seed + 7)
    val layers = mutableListOf<ToonLayer>()
    if (kind == ToonSpeakerKind.Unnamed) {
        layers += smoothLayer(inner, c.surface)
        layers +=
            smoothLayer(
                wobbleAlongNormals(toonSpeakerBlobPoints(shp, cx, cy, r - .9f * k), amp, seed),
                c.ink,
                stroke = 1.9f * k,
                // The engine writes the dash to one decimal.
                dash = floatArrayOf(oneDecimal(3.6f * k), oneDecimal(2.8f * k)),
            )
    } else {
        val outer = wobbleAlongNormals(toonSpeakerBlobPoints(shp, cx, cy, r), amp, seed)
        val fill = if (kind == ToonSpeakerKind.Me) c.primary else accentsOf(tokens)[color.mod(3)]
        layers += smoothLayer(outer, c.ink, dx = 2 * k, dy = 2 * k)
        layers += smoothLayer(outer, c.ink)
        val rim = c.rim
        if (rim != null) {
            layers += smoothLayer(inner, if (kind == ToonSpeakerKind.Me) c.primaryHighlight else rim)
            layers += smoothLayer(inner, fill, dy = 1.3f * k)
        } else {
            layers += smoothLayer(inner, fill)
        }
    }
    val fs = (s * (if (letter.length > 1) .38f else .5f)).roundToInt().toFloat()
    val label =
        ToonLabel(
            letter,
            cx,
            cy + fs * .36f + (if (shp == 2) 2.5f * k else 0f),
            if (kind == ToonSpeakerKind.Unnamed) c.textMuted else c.ink,
            1f,
            letterStyle(fs),
        )
    return ToonScene(layers, listOf(label))
}

private fun oneDecimal(v: Float): Float = (v * 10).roundToInt() / 10f
