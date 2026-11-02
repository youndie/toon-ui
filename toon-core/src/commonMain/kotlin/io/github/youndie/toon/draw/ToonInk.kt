package io.github.youndie.toon.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import io.github.youndie.toon.tokens.ToonColors
import io.github.youndie.toon.tokens.ToonShape

// The drawing half of the engine's primitives (B-09): `smooth`, `body`, `ring` and `place`. Every
// renderer in toon-engine.js draws through these, and so does every component here. Coordinates are
// the engine's px, which are dp (D5); a component scales the draw scope by its density once, then
// works in the engine's numbers. No geometry is cached: B-05 measured it at 2.4 ms of a 15 ms frame,
// and a size-keyed cache missed on every animated frame (research H1).

/** The engine's `smooth`: a closed curve of quadratic segments through the midpoints of [pts]. */
internal fun smoothPath(pts: FloatArray): Path =
    Path().apply {
        val n = pts.size / 2
        moveTo((pts[2 * (n - 1)] + pts[0]) / 2, (pts[2 * (n - 1) + 1] + pts[1]) / 2)
        for (i in 0 until n) {
            val j = (i + 1) % n
            quadraticTo(
                pts[2 * i],
                pts[2 * i + 1],
                (pts[2 * i] + pts[2 * j]) / 2,
                (pts[2 * i + 1] + pts[2 * j + 1]) / 2,
            )
        }
        close()
    }

/** A closed polygon through [pts]: how the engine writes `inkLine`, `star` and `burst` (M, L…, Z). */
internal fun polygonPath(pts: FloatArray): Path =
    Path().apply {
        moveTo(pts[0], pts[1])
        for (i in 1 until pts.size / 2) lineTo(pts[2 * i], pts[2 * i + 1])
        close()
    }

/** A shape's outline and fill as paths, built once per frame the shape changes. */
internal class ToonInkPaths(
    val ink: Path,
    val fill: Path,
)

internal fun ToonShapePoints.toPaths(): ToonInkPaths = ToonInkPaths(smoothPath(ink), smoothPath(fill))

/** How far the night theme's fill is shifted down so that its rim shows along the top edge, in dp. */
private const val RIM_DROP = 1.6f

/**
 * The engine's `body`: the outline offset by [shadow] as the hard shadow, the outline in [inkColor],
 * then — where the theme has a rim and [rim] is not null — the fill in the rim colour and the fill
 * again [RIM_DROP] lower, else the fill alone. [inkAlpha] fades ink and shadow together, as a
 * disabled component does.
 */
internal fun DrawScope.drawInkBody(
    paths: ToonInkPaths,
    fill: Color,
    colors: ToonColors,
    shadow: Float,
    inkAlpha: Float = 1f,
    inkColor: Color = colors.ink,
    rim: Color? = colors.rim,
) {
    if (shadow != 0f) translate(shadow, shadow) { drawPath(paths.ink, colors.ink, alpha = inkAlpha) }
    drawPath(paths.ink, inkColor, alpha = inkAlpha)
    if (rim != null) {
        drawPath(paths.fill, rim)
        translate(0f, RIM_DROP) { drawPath(paths.fill, fill) }
    } else {
        drawPath(paths.fill, fill)
    }
}

/** The engine's `body` from a box: builds the shape from the theme's insets and wobble, then draws it. */
internal fun DrawScope.drawInkBody(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    radii: ToonRadii,
    seed: Int,
    fill: Color,
    colors: ToonColors,
    shape: ToonShape,
    shadow: Float = shape.shadow,
    inkAlpha: Float = 1f,
    rim: Color? = colors.rim,
) = drawInkBody(
    shapePoints(x, y, w, h, radii, shape.insets, shape.wobble, seed).toPaths(),
    fill,
    colors,
    shadow,
    inkAlpha,
    rim = rim,
)

/** The engine's `ring`: the focus ring, a dashed wobble 2.5 dp wide in the theme's focus colour. */
internal fun DrawScope.drawFocusRing(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    radii: ToonRadii,
    colors: ToonColors,
    shape: ToonShape,
    seed: Int,
) {
    drawPath(
        smoothPath(wobblePoints(x, y, w, h, radii, shape.wobble, seed)),
        colors.focus,
        style =
            Stroke(
                width = 2.5f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 5f)),
            ),
    )
}

/** The engine's `place`: translate by ([dx], [dy]), then rotate [degrees] about ([px], [py]). */
internal inline fun DrawScope.place(
    dx: Float,
    dy: Float,
    degrees: Float = 0f,
    px: Float = 0f,
    py: Float = 0f,
    block: DrawScope.() -> Unit,
) = translate(dx, dy) {
    if (degrees == 0f) block() else rotate(degrees, Offset(px, py)) { block() }
}
