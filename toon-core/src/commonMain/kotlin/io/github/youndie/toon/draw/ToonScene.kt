package io.github.youndie.toon.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.tokens.ToonColors

// A component's frame as the engine returns it (B-14): layers of paths (`L`) drawn in order, then
// text (`X`) on top, as `toSvg` writes them. Building the frame as data before drawing it is what
// lets a test hold a component to the engine number by number — every coordinate, colour, opacity
// and offset of every layer — where a pixel diff loses a 0.4 dp shadow in a large fill.

/** How a layer's [ToonLayer.numbers] join: `M` then quadratic `Q` pairs, or `M` then `L` points. */
internal enum class ToonSegments { Quad, Line }

/**
 * One path of a frame, as the engine's `L`: its coordinates in the order the engine writes its `d`,
 * the [color] it fills (or strokes, when [stroke] is above 0, with round caps and joins as `toSvg`
 * draws every stroke), [alpha], and the translation [dx], [dy] of its `transform`.
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
) {
    val path: Path by lazy {
        Path().apply {
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
}

/** One text of a frame, as the engine's `X` with `anchor: middle`: centred on [x], on [baseline]. */
internal class ToonLabel(
    val x: Float,
    val baseline: Float,
    val color: Color,
    val alpha: Float,
)

/** A frame: [layers] in order, then [labels] over them. */
internal class ToonScene(
    val layers: List<ToonLayer>,
    val labels: List<ToonLabel>,
)

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

/** Draws [scene]: its layers in order, then [text] at each of its labels. */
internal fun DrawScope.drawScene(
    scene: ToonScene,
    text: TextLayoutResult?,
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
                            PathEffect.dashPathEffect(it)
                        },
                )
            } else {
                Fill
            }
        if (l.dx == 0f && l.dy == 0f) {
            drawPath(l.path, l.color, alpha = l.alpha, style = style)
        } else {
            translate(l.dx, l.dy) { drawPath(l.path, l.color, alpha = l.alpha, style = style) }
        }
    }
    if (text == null) return
    for (x in scene.labels) {
        drawText(
            text,
            x.color,
            Offset(x.x - text.multiParagraph.width / 2, x.baseline - text.firstBaseline),
            alpha = x.alpha,
        )
    }
}
