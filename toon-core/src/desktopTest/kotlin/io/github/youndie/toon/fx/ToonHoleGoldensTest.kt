package io.github.youndie.toon.fx

import io.github.youndie.toon.draw.ToonRadii
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-12: the hole's cracked silhouette against the engine's, every 0.05 of its life (`fx.json`). A
 * pixel diff cannot see its crack pattern: the fill is large and a different seed measured under the
 * ink floor, so the polygons are compared point by point.
 */
class ToonHoleGoldensTest {
    private fun numbers(d: String): FloatArray =
        Regex("-?\\d+(?:\\.\\d+)?")
            .findAll(d)
            .map { it.value.toFloat() }
            .toList()
            .toFloatArray()

    @Test
    fun `the hole's ink and paper edge match the engine's over its life`() {
        val frames =
            Json
                .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/fx.json")).readText())
                .jsonObject
                .getValue("hole")
                .jsonArray
        var worst = 0f
        for (f in frames) {
            val o = f.jsonObject
            val u = o.getValue("u").jsonPrimitive.float
            val actual = toonHoleOutline(u, 90f, 62f, 80f, 48f, ToonRadii(12f, 8f, 13f, 9f), 5)
            for ((name, mine) in listOf("ink" to actual.ink, "edge" to actual.edge)) {
                val expected = numbers(o.getValue(name).jsonPrimitive.content)
                assertEquals(expected.size, mine.size, "$name at u $u: point count")
                for (i in expected.indices) worst = max(worst, abs(expected[i] - mine[i]))
            }
        }
        println("hole: worst point $worst dp")
        assertTrue(worst <= TOLERANCE, "worst point $worst dp")
    }

    private companion object {
        /** The engine writes the hole's path to 0.1 dp, as `inkLine` and `star` (research D11). */
        const val TOLERANCE = 0.05f + 2e-4f
    }
}
