package io.github.youndie.toon.draw

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonColors
import io.github.youndie.toon.type.ToonFontFamilies
import io.github.youndie.toon.type.withBacking
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

// A component's frame as the engine returns it (B-14): layers of paths (`L`) drawn in order, then
// text (`X`) on top, as `toSvg` writes them. Building the frame as data before drawing it is what
// lets a test hold a component to the engine number by number — every coordinate, colour, opacity
// and offset of every layer — where a pixel diff loses a 0.4 dp shadow in a large fill.

/**
 * How a layer's [ToonLayer.numbers] join: `M` then quadratic `Q` pairs, or `M` then `L` points. An
 * [Iris] is the engine's `iris`, a rectangle with a round hole filled even-odd, kept as its `d` writes
 * it: `M0 0`, `H` w, `V` h, `H0`, then `M` the circle's right point and two arcs `A r r 0 1 0 x y`.
 * An [Arc] is `M x y` and one circular arc `A r r 0 large sweep x y`, as the circular progress draws.
 * [Pairs] is a run of separate strokes, `M x y L x y` each, as the slider's rope hatching is drawn.
 */
internal enum class ToonSegments { Quad, Line, Iris, Arc, Pairs }

/** A 2D affine transform as SVG writes it, `[a c e; b d f]`: what `place` and `scaleAbout` prefix. */
internal class ToonAffine(
    val a: Float = 1f,
    val b: Float = 0f,
    val c: Float = 0f,
    val d: Float = 1f,
    val e: Float = 0f,
    val f: Float = 0f,
) {
    val isIdentity: Boolean get() = a == 1f && b == 0f && c == 0f && d == 1f && e == 0f && f == 0f

    operator fun times(o: ToonAffine): ToonAffine =
        ToonAffine(
            a * o.a + c * o.b,
            b * o.a + d * o.b,
            a * o.c + c * o.d,
            b * o.c + d * o.d,
            a * o.e + c * o.f + e,
            b * o.e + d * o.f + f,
        )

    fun toMatrix(): Matrix =
        Matrix().apply {
            values[Matrix.ScaleX] = a
            values[Matrix.SkewY] = b
            values[Matrix.SkewX] = c
            values[Matrix.ScaleY] = d
            values[Matrix.TranslateX] = e
            values[Matrix.TranslateY] = f
        }

    companion object {
        val Identity: ToonAffine = ToonAffine()

        fun translate(
            x: Float,
            y: Float,
        ): ToonAffine = ToonAffine(e = x, f = y)

        fun scale(
            x: Float,
            y: Float,
        ): ToonAffine = ToonAffine(a = x, d = y)

        fun rotate(degrees: Float): ToonAffine {
            val r = degrees * PI.toFloat() / 180
            return ToonAffine(cos(r), sin(r), -sin(r), cos(r))
        }
    }
}

/**
 * One path of a frame, as the engine's `L`: its coordinates in the order the engine writes its `d`,
 * the [color] it fills (or strokes, when [stroke] is above 0, with round caps and joins as `toSvg`
 * draws every stroke, dashed by [dash] from [dashOffset]), [alpha], and its `transform`: a translation
 * [dx], [dy], then a [rotation] in degrees, then a [scale] (by [scale] and [scaleY], about the
 * pivot [pivotX], [pivotY]).
 *
 * A path the engine does not compute from numbers — an icon — comes as a ready [shape], with the
 * engine's own `d` for it as [source]. Without a DOM the engine strokes an icon's centerline
 * [iconBase] wide instead of filling its outline (research §1.2), which is what a frame golden holds.
 *
 * [outer] is the transform `place` and `scaleAbout` write before the layer's own; [outerPairs],
 * [outerSlack] and [outerTurn] carry what the engine's rounding of them allows the comparison.
 */
internal class ToonLayer(
    val numbers: FloatArray,
    val segments: ToonSegments,
    val closed: Boolean,
    val color: Color,
    val alpha: Float = 1f,
    val dx: Float = 0f,
    val dy: Float = 0f,
    val stroke: Float = 0f,
    val dash: FloatArray? = null,
    val scale: Float = 1f,
    val shape: Path? = null,
    val source: String? = null,
    val iconBase: Float? = null,
    val dashOffset: Float = 0f,
    val scaleY: Float = scale,
    val pivotX: Float = 0f,
    val pivotY: Float = 0f,
    val rotation: Float = 0f,
    val outer: ToonAffine = ToonAffine.Identity,
    val outerPairs: Int = 0,
    val outerSlack: Float = 0f,
    val outerTurn: Float = 0f,
) {
    val path: Path by lazy {
        if (segments == ToonSegments.Iris) return@lazy irisPath(numbers)
        if (segments == ToonSegments.Arc) return@lazy arcPath(numbers)
        if (segments == ToonSegments.Pairs) {
            return@lazy Path().apply {
                var i = 0
                while (i + 4 <= numbers.size) {
                    moveTo(numbers[i], numbers[i + 1])
                    lineTo(numbers[i + 2], numbers[i + 3])
                    i += 4
                }
            }
        }
        shape ?: Path().apply {
            moveTo(numbers[0], numbers[1])
            val step = if (segments == ToonSegments.Quad) 4 else 2
            var i = 2
            while (i + step <= numbers.size) {
                if (segments == ToonSegments.Quad) {
                    quadraticTo(numbers[i], numbers[i + 1], numbers[i + 2], numbers[i + 3])
                } else {
                    lineTo(numbers[i], numbers[i + 1])
                }
                i += step
            }
            if (closed) close()
        }
    }

    /** This layer translated by ([x], [y]) first and faded by [opacity], as the engine's `place` does. */
    fun moved(
        x: Float,
        y: Float,
        opacity: Float = 1f,
    ): ToonLayer =
        if (outer.isIdentity) {
            copy(alpha = alpha * opacity, dx = dx + x, dy = dy + y)
        } else {
            copy(alpha = alpha * opacity, outer = ToonAffine.translate(x, y) * outer)
        }

    fun copy(
        alpha: Float = this.alpha,
        dx: Float = this.dx,
        dy: Float = this.dy,
        outer: ToonAffine = this.outer,
        outerPairs: Int = this.outerPairs,
        outerSlack: Float = this.outerSlack,
        outerTurn: Float = this.outerTurn,
    ): ToonLayer =
        ToonLayer(
            numbers,
            segments,
            closed,
            color,
            alpha,
            dx,
            dy,
            stroke,
            dash,
            scale,
            shape,
            source,
            iconBase,
            dashOffset,
            scaleY,
            pivotX,
            pivotY,
            rotation,
            outer,
            outerPairs,
            outerSlack,
            outerTurn,
        )
}

/** The weight and size of a frame's text, in the theme's body face or its display face. */
internal class ToonTextStyle(
    val weight: Int,
    val size: Float,
    /** The theme's display face (`T.display`) instead of its body face. */
    val display: Boolean = false,
)

/** Where a text sits on its x, as the engine's `X` anchors: from its start, centred, or to its end. */
internal enum class ToonAnchor { Start, Middle, End }

/** One text of a frame, as the engine's `X`: [text] on [baseline], [anchor]ed at [x]. */
internal class ToonLabel(
    val text: String,
    val x: Float,
    val baseline: Float,
    val color: Color,
    val alpha: Float,
    val style: ToonTextStyle,
    val anchor: ToonAnchor = ToonAnchor.Middle,
)

/** A frame: [layers] in order, then [labels] over them. */
internal class ToonScene(
    val layers: List<ToonLayer>,
    val labels: List<ToonLabel>,
) {
    /**
     * The engine's `place`: everything moved by ([x], [y]), the layers turned [rotation] degrees about
     * ([px], [py]) — the engine moves texts and never turns them — and faded by [opacity].
     */
    fun placed(
        x: Float,
        y: Float,
        opacity: Float = 1f,
        rotation: Float = 0f,
        px: Float = 0f,
        py: Float = 0f,
    ): ToonScene {
        val layers =
            if (rotation == 0f) {
                layers.map { it.moved(x, y, opacity) }
            } else {
                // `translate(dx dy) rotate(r px py)`, the angle written to two decimals.
                val turn =
                    ToonAffine.translate(x + px, y + py) * ToonAffine.rotate(rotation) * ToonAffine.translate(-px, -py)
                layers.map {
                    it.copy(
                        alpha = it.alpha * opacity,
                        outer = turn * it.outer,
                        outerSlack = it.outerSlack + ROTATE_ROUNDING * max(abs(px), abs(py)) + ROTATE_PIVOT_ROUNDING,
                        outerTurn = it.outerTurn + ROTATE_ROUNDING,
                    )
                }
            }
        return ToonScene(
            layers,
            labels.map {
                ToonLabel(
                    it.text,
                    it.x + x,
                    it.baseline + y,
                    it.color,
                    it.alpha * opacity,
                    it.style,
                    it.anchor,
                )
            },
        )
    }

    /** The engine's `scaleAbout`: the layers, not the texts, scaled by [sx], [sy] about ([px], [py]). */
    fun scaledAbout(
        px: Float,
        py: Float,
        sx: Float,
        sy: Float,
    ): ToonScene {
        if (sx == 1f && sy == 1f) return this
        val m = ToonAffine.translate(px, py) * ToonAffine.scale(sx, sy) * ToonAffine.translate(-px, -py)
        val pairSlack = max(pairRounding(px, sx), pairRounding(py, sy))
        return ToonScene(
            layers.map {
                it.copy(
                    outer = m * it.outer,
                    outerPairs = it.outerPairs + 1,
                    outerSlack = it.outerSlack + SCALE_ROUNDING * max(abs(px), abs(py)) + pairSlack,
                    outerTurn = it.outerTurn + SCALE_ROUNDING,
                )
            },
            labels,
        )
    }

    /**
     * The layers, not the texts, under the engine's `matrix(a b c d 0 0) rotate(r 0 py)`, as the
     * drawer prefixes its door: [a] and [b] written to three decimals, [rotation] to two.
     */
    fun skewedAbout(
        a: Float,
        b: Float,
        rotation: Float,
        py: Float,
    ): ToonScene {
        val m =
            ToonAffine(a = a, b = b) * ToonAffine.translate(0f, py) * ToonAffine.rotate(rotation) *
                ToonAffine.translate(0f, -py)
        return ToonScene(
            layers.map {
                it.copy(
                    outer = m * it.outer,
                    outerSlack = it.outerSlack + (ROTATE_ROUNDING + MATRIX_ROUNDING) * abs(py) + ROTATE_PIVOT_ROUNDING,
                    outerTurn = it.outerTurn + ROTATE_ROUNDING + MATRIX_ROUNDING,
                )
            },
            labels,
        )
    }

    /**
     * What the engine's `f` does to the pair about a pivot [p] scaled by [s]: it writes `translate(f(p))`
     * and `translate(f(-p))`, and `Math.round` takes a half up, so off the 0.1 grid the two ends round
     * apart (146.25 → 146.3 and −146.2) and the second is scaled.
     */
    private fun pairRounding(
        p: Float,
        s: Float,
    ): Float {
        fun f(v: Float) = floor(v * 10 + .5f) / 10
        return abs(f(p) - p) + abs(s) * abs(f(-p) + p)
    }

    /** The engine's `cat`: [other]'s layers after these, its labels after these. */
    operator fun plus(other: ToonScene): ToonScene = ToonScene(layers + other.layers, labels + other.labels)

    companion object {
        val Empty: ToonScene = ToonScene(emptyList(), emptyList())

        /** `place` writes an angle to two decimals, 0.005°, and its pivot to one, 0.05 dp. */
        private const val ROTATE_ROUNDING = 8.73e-5f
        private const val ROTATE_PIVOT_ROUNDING = .005f

        /** `scaleAbout` writes a scale to three decimals. */
        private const val SCALE_ROUNDING = 5e-4f

        /** `matrix` written to three decimals. */
        private const val MATRIX_ROUNDING = 5e-4f
    }
}

/** The engine's `smooth` as numbers: the midpoint of the last and first point, then each point and the next midpoint. */
@OptIn(InternalToonApi::class)
internal fun smoothNumbers(pts: FloatArray): FloatArray {
    val n = pts.size / 2
    val out = FloatArray(2 + 4 * n)
    out[0] = (pts[2 * (n - 1)] + pts[0]) / 2
    out[1] = (pts[2 * (n - 1) + 1] + pts[1]) / 2
    for (i in 0 until n) {
        val j = (i + 1) % n
        out[2 + 4 * i] = pts[2 * i]
        out[3 + 4 * i] = pts[2 * i + 1]
        out[4 + 4 * i] = (pts[2 * i] + pts[2 * j]) / 2
        out[5 + 4 * i] = (pts[2 * i + 1] + pts[2 * j + 1]) / 2
    }
    return out
}

/** A closed `smooth` curve through [pts] as a layer. */
internal fun smoothLayer(
    pts: FloatArray,
    color: Color,
    alpha: Float = 1f,
    dx: Float = 0f,
    dy: Float = 0f,
    stroke: Float = 0f,
    dash: FloatArray? = null,
): ToonLayer = ToonLayer(smoothNumbers(pts), ToonSegments.Quad, true, color, alpha, dx, dy, stroke, dash)

/** The engine's `body` as layers: shadow, outline, then the fill over the rim where there is one. */
@OptIn(InternalToonApi::class)
internal fun bodyLayers(
    shape: ToonShapePoints,
    fill: Color,
    colors: ToonColors,
    shadow: Float,
    inkAlpha: Float = 1f,
    inkColor: Color = colors.ink,
    rim: Color? = colors.rim,
): List<ToonLayer> =
    buildList {
        if (shadow != 0f) add(smoothLayer(shape.ink, colors.ink, inkAlpha, shadow, shadow))
        add(smoothLayer(shape.ink, inkColor, inkAlpha))
        if (rim != null) add(smoothLayer(shape.fill, rim))
        add(smoothLayer(shape.fill, fill, dy = if (rim != null) RIM_DROP else 0f))
    }

/** The engine's `ring` as a layer: a dashed wobble 2.5 dp wide in the theme's focus colour. */
@OptIn(InternalToonApi::class)
internal fun ringLayer(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    radii: ToonRadii,
    colors: ToonColors,
    wobble: Float,
    seed: Int,
): ToonLayer = smoothLayer(wobblePoints(x, y, w, h, radii, wobble, seed), colors.focus, stroke = 2.5f, dash = RING_DASH)

private val RING_DASH = floatArrayOf(7f, 5f)

/**
 * Lays out a frame's texts in the theme's body or display face, Cyrillic in the face that backs it
 * (B-35); [measurer] caches the layouts.
 */
@Stable
internal class ToonTextRenderer(
    private val measurer: TextMeasurer,
    private val fonts: ToonFontFamilies,
    private val fontScale: Float = 1f,
) {
    fun measure(
        text: String,
        style: ToonTextStyle,
    ): TextLayoutResult =
        measurer.measure(
            withBacking(
                text,
                if (style.display) fonts.displayCyrillic else fonts.bodyCyrillic,
                style.size,
                style.weight,
            ),
            TextStyle(
                fontFamily = if (style.display) fonts.display else fonts.body,
                fontWeight = FontWeight(style.weight),
                fontSize = style.size.sp,
            ),
            // Laid out in dp, as the frame is drawn ([inDp]); the user's font scale still applies.
            density = Density(1f, fontScale),
        )
}

/** The [ToonTextRenderer] of the nearest `ToonTheme`. */
@Composable
internal fun rememberToonText(): ToonTextRenderer {
    val measurer = rememberTextMeasurer()
    val fonts = Toon.fonts
    val fontScale = LocalDensity.current.fontScale
    return remember(measurer, fonts, fontScale) { ToonTextRenderer(measurer, fonts, fontScale) }
}

/**
 * Draws [scene]: its layers in order, then each label laid out by [text], skipping what the
 * engine's `toSvg` skips, a text at opacity 0.01 or less.
 */
internal fun DrawScope.drawScene(
    scene: ToonScene,
    text: ToonTextRenderer? = null,
) {
    for (l in scene.layers) {
        val style =
            if (l.stroke > 0) {
                Stroke(
                    width = l.stroke,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect =
                        l.dash?.let {
                            PathEffect.dashPathEffect(it, l.dashOffset)
                        },
                )
            } else {
                Fill
            }
        val draw: DrawScope.() -> Unit = {
            translate(l.dx, l.dy) {
                rotate(l.rotation, Offset.Zero) {
                    if (l.scale == 1f && l.scaleY == 1f) {
                        drawPath(l.path, l.color, alpha = l.alpha, style = style)
                    } else {
                        scale(
                            l.scale,
                            l.scaleY,
                            Offset(l.pivotX, l.pivotY),
                        ) { drawPath(l.path, l.color, alpha = l.alpha, style = style) }
                    }
                }
            }
        }
        if (l.outer.isIdentity) draw() else withTransform({ transform(l.outer.toMatrix()) }, draw)
    }
    if (text == null) return
    for (x in scene.labels) {
        if (x.alpha <= .01f) continue
        val layout = text.measure(x.text, x.style)
        val left =
            when (x.anchor) {
                ToonAnchor.Start -> x.x
                ToonAnchor.Middle -> x.x - layout.multiParagraph.width / 2
                ToonAnchor.End -> x.x - layout.multiParagraph.width
            }
        drawText(layout, x.color, Offset(left, x.baseline - layout.firstBaseline), alpha = x.alpha)
    }
}

/** The engine's `iris` with the window [w] × [h] and a hole of radius [r] about ([cx], [cy]), as its `d`. */
internal fun irisLayer(
    w: Float,
    h: Float,
    cx: Float,
    cy: Float,
    r: Float,
    color: Color,
    alpha: Float,
): ToonLayer =
    ToonLayer(
        floatArrayOf(
            0f,
            0f,
            w,
            0f,
            w,
            h,
            0f,
            h,
            cx + r,
            cy,
            r,
            r,
            0f,
            1f,
            0f,
            cx - r,
            cy,
            r,
            r,
            0f,
            1f,
            0f,
            cx + r,
            cy,
        ),
        ToonSegments.Iris,
        true,
        color,
        alpha,
    )

private fun irisPath(n: FloatArray): Path =
    Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(Rect(0f, 0f, n[4], n[5]))
        val r = n[10]
        val cx = n[8] - r
        if (r > 0f) addOval(Rect(cx - r, n[9] - r, cx + r, n[9] + r))
    }

/**
 * One SVG arc of a circle, `M x0 y0 A r r 0 large sweep x1 y1`, from [n] in that order: its centre
 * found from the two ends and the flags, as SVG's implementation notes do it (F.6.5).
 */
private fun arcPath(n: FloatArray): Path {
    val x0 = n[0].toDouble()
    val y0 = n[1].toDouble()
    var r = n[2].toDouble()
    val large = n[5] != 0f
    val sweep = n[6] != 0f
    val x1 = n[7].toDouble()
    val y1 = n[8].toDouble()
    val hx = (x0 - x1) / 2
    val hy = (y0 - y1) / 2
    val d2 = hx * hx + hy * hy
    if (r * r < d2) r = sqrt(d2)
    val coef = sqrt(max(0.0, (r * r - d2) / d2)) * (if (large == sweep) -1 else 1)
    val cx = coef * hy + (x0 + x1) / 2
    val cy = -coef * hx + (y0 + y1) / 2
    val start = atan2(y0 - cy, x0 - cx)
    var delta = atan2(y1 - cy, x1 - cx) - start
    if (sweep && delta < 0) delta += 2 * PI
    if (!sweep && delta > 0) delta -= 2 * PI
    return Path().apply {
        moveTo(n[0], n[1])
        arcTo(
            Rect((cx - r).toFloat(), (cy - r).toFloat(), (cx + r).toFloat(), (cy + r).toFloat()),
            (start * 180 / PI).toFloat(),
            (delta * 180 / PI).toFloat(),
            false,
        )
    }
}

/**
 * Draws [block] in dp: the engine's numbers are dp (research D5) and a canvas is pixels, so a frame
 * is scaled by the density once, here (B-47). Text measured by [ToonTextRenderer] is in dp too.
 */
internal inline fun DrawScope.inDp(block: DrawScope.() -> Unit) = scale(density, density, Offset.Zero, block)

/** The draw scope's size in dp, to use inside [inDp]. */
internal val DrawScope.sizeDp: Size get() = Size(size.width / density, size.height / density)
