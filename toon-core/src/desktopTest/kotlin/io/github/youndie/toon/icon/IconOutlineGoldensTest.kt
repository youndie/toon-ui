package io.github.youndie.toon.icon

import io.github.youndie.toon.InternalToonApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-11: the outline port against the engine's own `iconOutline`, computed in Chrome by
 * `scripts/design-references.mjs` (the engine samples a centerline through an SVG path element, so
 * only a DOM can run it). Every glyph, both weights: the same polygons, the same point counts, each
 * point within [TOLERANCE].
 */
@OptIn(InternalToonApi::class)
class IconOutlineGoldensTest {
    private val goldens =
        Json
            .parseToJsonElement(
                checkNotNull(javaClass.getResource("/design-goldens/icon-outlines.json")).readText(),
            ).jsonObject
            .getValue("outlines")
            .jsonObject

    /** The engine's `M x y L x y … Z` string, one array per polygon. */
    private fun polygons(d: String): List<DoubleArray> =
        d.split('M').filter { it.isNotBlank() }.map { p ->
            Regex("-?\\d+(?:\\.\\d+)?")
                .findAll(p)
                .map { it.value.toDouble() }
                .toList()
                .toDoubleArray()
        }

    private fun worst(): Double {
        var worst = 0.0
        for (glyph in ToonIcons.all) {
            val g = goldens.getValue(glyph.name).jsonObject
            for (weight in ToonIconWeight.entries) {
                val expected = polygons(g.getValue(weight.name.lowercase()).jsonPrimitive.content)
                val actual = iconOutlinePolygons(glyph, weight)
                assertEquals(expected.size, actual.size, "${glyph.name} $weight: polygon count")
                for ((i, e) in expected.withIndex()) {
                    assertEquals(e.size, actual[i].size, "${glyph.name} $weight polygon $i: point count")
                    for (j in e.indices) worst = max(worst, abs(e[j] - actual[i][j]))
                }
            }
        }
        return worst
    }

    @Test
    fun `every glyph in both weights matches the engine's outline`() {
        assertEquals(ToonIcons.all.map { it.name }.toSet(), goldens.keys, "glyphs against the goldens")
        val worst = worst()
        println("icon outlines: worst point $worst")
        assertTrue(worst <= TOLERANCE, "worst point $worst > $TOLERANCE")
    }

    private companion object {
        /**
         * The goldens are written to two decimals (±0.005), and the rest is the two arc conversions:
         * Compose's path parser turns an SVG arc into cubics, Chrome into conics. Measured worst
         * 0.0149 dp on 2026-11-02, a seventieth of a pixel at 24 dp.
         */
        const val TOLERANCE = 0.02
    }
}
