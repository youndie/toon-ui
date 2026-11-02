package io.github.youndie.toon.icon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonRandom
import io.github.youndie.toon.theme.Toon
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// A port of the engine's icon half (B-11): `sampleSub`, `iconWidth`, `iconOutline` and `icon`. An
// icon is one centerline on the 24 dp grid, and its ink is computed from it: sampled every 0.3 dp,
// widened by a weight that grows towards the bottom, across the stroke and with a 6 % swell along
// it, tapered at open ends and capped round. The arithmetic keeps the engine's order, in Double;
// only the sampled positions are Float, because PathMeasure's are.

/** The two ink weights: the engine's `base`, the stroke width in dp at the 24 dp grid. */
public enum class ToonIconWeight(
    internal val base: Double,
) {
    Regular(2.1),
    Bold(2.7),
}

/**
 * One glyph of [ToonIcons]: its [centerline] and its cel [fill] as the engine's SVG path strings on
 * the 24 dp grid. The ink outline is computed from the centerline, never stored.
 */
@Immutable
public class ToonIconGlyph internal constructor(
    public val name: String,
    public val centerline: String,
    public val fill: String?,
) {
    override fun toString(): String = "ToonIconGlyph($name)"
}

/**
 * The engine's `icon`: [icon] at [size], its outline in [weight] and [color], over its cel fill in the
 * theme's `chipSel` colour when [filled] and the glyph has one. [contentDescription] null marks the
 * icon decorative.
 */
@Composable
public fun ToonIcon(
    icon: ToonIconGlyph,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    weight: ToonIconWeight = ToonIconWeight.Regular,
    filled: Boolean = false,
    color: Color = Toon.colors.text,
) {
    val outline = remember(icon, weight) { iconOutlinePath(icon, weight) }
    val fill = remember(icon, filled) { if (filled) icon.fill?.let(::parsePath) else null }
    val fillColor = Toon.colors.chipSelected
    val described =
        if (contentDescription == null) {
            modifier
        } else {
            modifier.semantics {
                this.contentDescription = contentDescription
                role = Role.Image
            }
        }
    Canvas(described.size(size)) {
        scale(this.size.width / GRID, pivot = Offset.Zero) {
            if (fill != null) drawPath(fill, fillColor)
            drawPath(outline, color)
        }
    }
}

/** The grid an icon is drawn on, in dp: the engine's `size / 24`. */
private const val GRID = 24f

/** The engine's sampling step along a centerline, in dp. */
private const val STEP = 0.3

private fun parsePath(d: String): Path = PathParser().parsePathString(d).toPath()

private fun c01(v: Double): Double = max(0.0, min(1.0, v))

/** The engine's `iconWidth`: the full ink width at height [y], tangent x [tx], arc position [s]. */
internal fun iconWidth(
    y: Double,
    tx: Double,
    base: Double,
    s: Double,
    ph: Double,
): Double = base * (0.72 + 0.42 * c01((y - 4) / 16) + 0.3 * tx * tx) * (1 + .06 * sin(s * 1.3 + ph))

/** One subpath of a centerline sampled as the engine's `sampleSub` does: points, or a dot. */
private class ToonIconSample(
    val pts: DoubleArray,
    val closed: Boolean,
    val dot: Offset?,
)

private val CLOSED = Regex("[zZ]\\s*$")

/**
 * How much a subpath is enlarged before it is measured. Skia's PathMeasure flattens a curve to within
 * about half a unit, which on the 24 dp grid is a percent of an icon's length: the search lens
 * measured 43.1 dp against the 43.54 the SVG arc integrates to, and Chrome's 43.5 — two samples short.
 */
private const val MEASURE_SCALE = 1024f

private fun sampleSub(d: String): ToonIconSample {
    val measure = PathMeasure()
    measure.setPath(parsePath(d).apply { transform(Matrix().apply { scale(MEASURE_SCALE, MEASURE_SCALE) }) }, false)
    val length = measure.length.toDouble() / MEASURE_SCALE
    val closed = CLOSED.containsMatchIn(d.trim())
    if (length < .5) return ToonIconSample(DoubleArray(0), closed, measure.getPosition(0f) / MEASURE_SCALE)
    val n = max(10, ceil(length / STEP).toInt())
    val count = if (closed) n else n + 1
    val pts = DoubleArray(2 * count)
    for (i in 0 until count) {
        val q = measure.getPosition((length * MEASURE_SCALE * i / n).toFloat())
        pts[2 * i] = q.x.toDouble() / MEASURE_SCALE
        pts[2 * i + 1] = q.y.toDouble() / MEASURE_SCALE
    }
    return ToonIconSample(pts, closed, null)
}

/**
 * The engine's `iconOutline`: the polygons whose non-zero fill is the ink of [glyph] in [weight], on
 * the 24 dp grid, as flat `[x0, y0, x1, y1, …]` arrays. A closed subpath gives two polygons, its
 * outer edge and its inner edge reversed; an open one gives one, with round caps; a dot gives an
 * ellipse of 16 points.
 */
@InternalToonApi
public fun iconOutlinePolygons(
    glyph: ToonIconGlyph,
    weight: ToonIconWeight,
): List<DoubleArray> {
    val base = weight.base
    val ph = ToonRandom(glyph.name.length * 31 + (base * 10).roundToInt()).next() * 6
    val out = mutableListOf<DoubleArray>()
    glyph.centerline
        .split(Regex("(?=M)"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEach { sub ->
            val s = sampleSub(sub)
            if (s.dot != null) {
                val r = base * 1.0
                out +=
                    DoubleArray(32).also { p ->
                        for (i in 0 until 16) {
                            val a = i / 16.0 * 2 * PI
                            p[2 * i] = s.dot.x + cos(a) * r * 1.05
                            p[2 * i + 1] = s.dot.y + sin(a) * r * .95
                        }
                    }
                return@forEach
            }
            out += outlineOf(s, base, ph)
        }
    return out
}

private fun outlineOf(
    s: ToonIconSample,
    base: Double,
    ph: Double,
): List<DoubleArray> {
    val pts = s.pts
    val n = pts.size / 2
    val left = DoubleArray(2 * n)
    val right = DoubleArray(2 * n)
    val w = DoubleArray(n)
    val t = DoubleArray(2 * n)
    for (i in 0 until n) {
        val a = if (s.closed) (i - 1 + n) % n else max(0, i - 1)
        val b = if (s.closed) (i + 1) % n else min(n - 1, i + 1)
        var tx = pts[2 * b] - pts[2 * a]
        var ty = pts[2 * b + 1] - pts[2 * a + 1]
        val l = hypot(tx, ty).takeIf { it != 0.0 } ?: 1.0
        tx /= l
        ty /= l
        val k = if (s.closed) 1.0 else min(1.0, .6 + .4 * min(i, n - 1 - i) / 6)
        val wi = iconWidth(pts[2 * i + 1], tx, base, i * .3, ph) / 2 * k
        val nx = -ty
        val ny = tx
        left[2 * i] = pts[2 * i] + nx * wi
        left[2 * i + 1] = pts[2 * i + 1] + ny * wi
        right[2 * i] = pts[2 * i] - nx * wi
        right[2 * i + 1] = pts[2 * i + 1] - ny * wi
        w[i] = wi
        t[2 * i] = tx
        t[2 * i + 1] = ty
    }
    if (s.closed) return listOf(left, reversed(right))

    fun cap(
        i: Int,
        sgn: Int,
    ): DoubleArray {
        val cx = pts[2 * i]
        val cy = pts[2 * i + 1]
        val tx = t[2 * i]
        val ty = t[2 * i + 1]
        val nx = -ty
        val ny = tx
        val r = w[i]
        return DoubleArray(14).also { p ->
            for (j in 1 until 8) {
                val a = j / 8.0 * PI
                p[2 * (j - 1)] = cx + r * (cos(a) * nx * sgn + sin(a) * tx * sgn)
                p[2 * (j - 1) + 1] = cy + r * (cos(a) * ny * sgn + sin(a) * ty * sgn)
            }
        }
    }
    return listOf(left + cap(n - 1, 1) + reversed(right) + cap(0, -1))
}

private fun reversed(pts: DoubleArray): DoubleArray {
    val n = pts.size / 2
    return DoubleArray(pts.size).also { r ->
        for (i in 0 until n) {
            r[2 * i] = pts[2 * (n - 1 - i)]
            r[2 * i + 1] = pts[2 * (n - 1 - i) + 1]
        }
    }
}

/** [iconOutlinePolygons] as one path, the engine's `M … Z` per polygon under the non-zero rule. */
@OptIn(InternalToonApi::class)
internal fun iconOutlinePath(
    glyph: ToonIconGlyph,
    weight: ToonIconWeight,
): Path =
    Path().apply {
        for (p in iconOutlinePolygons(glyph, weight)) {
            moveTo(p[0].toFloat(), p[1].toFloat())
            for (i in 1 until p.size / 2) lineTo(p[2 * i].toFloat(), p[2 * i + 1].toFloat())
            close()
        }
    }
