package io.github.youndie.toon.icon

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

// B-80: an icon's arcs as Chrome draws them. The SVG spec (F.6.5, F.6.6) scales an arc's radii up
// until they reach its end point and then puts the centre on the chord. Compose's path parser scales
// them by the chord over 1.99999, not 2, so such an arc keeps a centre a hair off the chord, and its
// large arc comes out long: the desktop `rec` glyph, two half-ellipses whose radii fall short by
// 0.03 %, measured 39.04 dp in Compose against the 38.96 its arcs integrate to and Chrome samples,
// one sample more per edge. Here each absolute `A` becomes cubics of at most 90°, computed in Double
// by the spec, before Compose parses the path.

private val SEGMENT = Regex("([MLCHVAZ])([^MLCHVAZ]*)")
private val NUMBER = Regex("-?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?")

/**
 * [d] with every arc written as cubic Béziers by the SVG spec's conversion, the rest unchanged. Only
 * the absolute commands the engine writes (`M L C H V A Z`) are read; a path with any other is
 * returned as it is.
 */
internal fun withSpecArcs(d: String): String {
    if ('A' !in d || d.any { it.isLetter() && it !in "MLCHVAZe" }) return d
    val out = StringBuilder()
    var x = 0.0
    var y = 0.0
    var startX = 0.0
    var startY = 0.0
    for (m in SEGMENT.findAll(d)) {
        val c = m.groupValues[1]
        val v = NUMBER.findAll(m.groupValues[2]).map { it.value.toDouble() }.toList()
        when (c) {
            "A" -> {
                for (i in 0 until v.size / 7) {
                    val (rx, ry, phi) = Triple(v[7 * i], v[7 * i + 1], v[7 * i + 2])
                    val large = v[7 * i + 3] != 0.0
                    val sweep = v[7 * i + 4] != 0.0
                    val x2 = v[7 * i + 5]
                    val y2 = v[7 * i + 6]
                    for (cubic in arcCubics(x, y, rx, ry, phi, large, sweep, x2, y2)) {
                        out.append("C").append(cubic.joinToString(" ") { it.toString() }).append(' ')
                    }
                    x = x2
                    y = y2
                }
            }

            else -> {
                out.append(m.value)
                when (c) {
                    "M" -> {
                        x = v[v.size - 2]
                        y = v[v.size - 1]
                        startX = v[0]
                        startY = v[1]
                    }

                    "L", "C" -> {
                        x = v[v.size - 2]
                        y = v[v.size - 1]
                    }

                    "H" -> {
                        x = v.last()
                    }

                    "V" -> {
                        y = v.last()
                    }

                    "Z" -> {
                        x = startX
                        y = startY
                    }
                }
            }
        }
    }
    return out.toString().trim()
}

/** The arc from ([x1], [y1]) to ([x2], [y2]) as cubics `[c1x, c1y, c2x, c2y, x, y]`, each at most 90°. */
internal fun arcCubics(
    x1: Double,
    y1: Double,
    rx0: Double,
    ry0: Double,
    phiDeg: Double,
    large: Boolean,
    sweep: Boolean,
    x2: Double,
    y2: Double,
): List<DoubleArray> {
    if (x1 == x2 && y1 == y2) return emptyList()
    var rx = abs(rx0)
    var ry = abs(ry0)
    // A zero radius is a straight line (F.6.2).
    if (rx == 0.0 || ry == 0.0) return listOf(doubleArrayOf(x1, y1, x2, y2, x2, y2))
    val phi = phiDeg * PI / 180
    val cp = cos(phi)
    val sp = sin(phi)
    val dx = (x1 - x2) / 2
    val dy = (y1 - y2) / 2
    val x1p = cp * dx + sp * dy
    val y1p = -sp * dx + cp * dy
    // F.6.6: radii that cannot reach the end point are scaled up until they just do.
    val lambda = x1p * x1p / (rx * rx) + y1p * y1p / (ry * ry)
    if (lambda > 1) {
        rx *= sqrt(lambda)
        ry *= sqrt(lambda)
    }
    val num = rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p
    val den = rx * rx * y1p * y1p + ry * ry * x1p * x1p
    val coef = (if (large == sweep) -1 else 1) * sqrt(max(0.0, num / den))
    val cxp = coef * rx * y1p / ry
    val cyp = -coef * ry * x1p / rx
    val cx = cp * cxp - sp * cyp + (x1 + x2) / 2
    val cy = sp * cxp + cp * cyp + (y1 + y2) / 2
    val theta1 = atan2((y1p - cyp) / ry, (x1p - cxp) / rx)
    var dTheta = atan2((-y1p - cyp) / ry, (-x1p - cxp) / rx) - theta1
    if (!sweep && dTheta > 0) dTheta -= 2 * PI
    if (sweep && dTheta < 0) dTheta += 2 * PI
    // A half turn computed a hair over 180° is still two quarters.
    val n = max(1, ceil(abs(dTheta) / (PI / 2) - 1e-6).toInt())
    val delta = dTheta / n
    val t = 4.0 / 3 * tan(delta / 4)

    fun px(a: Double) = cx + cp * rx * cos(a) - sp * ry * sin(a)

    fun py(a: Double) = cy + sp * rx * cos(a) + cp * ry * sin(a)

    fun dx(a: Double) = -cp * rx * sin(a) - sp * ry * cos(a)

    fun dy(a: Double) = -sp * rx * sin(a) + cp * ry * cos(a)
    return List(n) { i ->
        val a1 = theta1 + i * delta
        val a2 = a1 + delta
        // The last end is the arc's own end point, exactly, so the next command starts there.
        val ex = if (i == n - 1) x2 else px(a2)
        val ey = if (i == n - 1) y2 else py(a2)
        doubleArrayOf(px(a1) + t * dx(a1), py(a1) + t * dy(a1), px(a2) - t * dx(a2), py(a2) - t * dy(a2), ex, ey)
    }
}
