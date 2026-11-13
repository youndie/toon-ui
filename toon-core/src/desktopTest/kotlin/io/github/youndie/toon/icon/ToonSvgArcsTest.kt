package io.github.youndie.toon.icon

import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-80: an icon's arcs converted by the SVG spec, as Chrome draws them, before Compose parses the
 * path — held directly here, and through every glyph's outline by `IconOutlineGoldensTest`.
 */
class ToonSvgArcsTest {
    private fun length(d: String): Double {
        val m = PathMeasure()
        m.setPath(
            PathParser().parsePathString(d).toPath().apply { transform(Matrix().apply { scale(1024f, 1024f) }) },
            false,
        )
        return m.length / 1024.0
    }

    @Test
    fun `a path without arcs is left as it is`() {
        val d = "M4.6 19.4 H19.4 M12 4.2 V15 M7.4 10.7 L12 15.3 L16.6 10.6"
        assertEquals(d, withSpecArcs(d))
    }

    @Test
    fun `an arc whose radii fall short has its centre on the chord, as the spec and Chrome put it`() {
        // The desktop `rec` glyph: two half-ellipses 6.3 by 6.1 between points 12.6 apart and 0.2 off level.
        val rec = ToonIcons.Rec.centerline
        // Integrated by the spec's construction: radii scaled by √λ, the centre at the chord's middle.
        val lambda = 6.3 * 6.3 / (6.3 * 6.3) + 0.1 * 0.1 / (6.1 * 6.1)
        val a = 6.3 * sqrt(lambda)
        val b = 6.1 * sqrt(lambda)
        val h = ((a - b) / (a + b)).let { it * it }
        val ramanujan = PI * (a + b) * (1 + 3 * h / (10 + sqrt(4 - 3 * h)))
        val raw = length(rec)
        val spec = length(withSpecArcs(rec))
        println("rec: Compose's own arcs $raw dp, the spec's $spec dp, the ellipse $ramanujan dp")
        // A quarter of an ellipse as one cubic is a hundredth of a percent long; a sample is 0.3 dp.
        assertEquals(ramanujan, spec, .01)
        assertEquals(ceil(ramanujan / .3), ceil(spec / .3), "the samples the engine takes")
        // Compose's own arcs gave the outline one more sample per edge than Chrome's.
        assertTrue(ceil(raw / .3) > ceil(ramanujan / .3), "the parser's arcs are long here: $raw")
    }

    @Test
    fun `radii far too short are scaled up until they reach, a half circle on the chord`() {
        // Radius 2 between points 10 apart: the spec draws a half circle of radius 5.
        val d = "M0 0 A2 2 0 0 1 10 0"
        assertEquals(PI * 5, length(withSpecArcs(d)), .01)
        val mid = arcCubics(0.0, 0.0, 2.0, 2.0, 0.0, false, true, 10.0, 0.0).first()
        // The first quarter ends at the top of the circle about (5, 0).
        assertEquals(5.0, mid[4], 1e-9)
        assertEquals(-5.0, mid[5], 1e-9)
    }

    @Test
    fun `a quarter circle is the classic cubic`() {
        val c = arcCubics(12.0, 6.0, 6.0, 6.0, 0.0, false, true, 18.0, 12.0).single()
        val k = 4.0 / 3 * (sqrt(2.0) - 1) * 6
        val expected = doubleArrayOf(12 + k, 6.0, 18.0, 12 - k, 18.0, 12.0)
        expected.indices.forEach { assertEquals(expected[it], c[it], 1e-9, "coordinate $it") }
    }

    @Test
    fun `a half turn takes two cubics and ends where the arc ends`() {
        val cubics = arcCubics(18.3, 12.1, 6.3, 6.1, 0.0, true, true, 5.7, 11.9)
        assertEquals(2, cubics.size)
        assertEquals(5.7, cubics.last()[4])
        assertEquals(11.9, cubics.last()[5])
    }
}
