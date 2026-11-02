package io.github.youndie.toon.motion

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-14: the shared curves against the engine's (`curves.json`): the squash a press maps to, which
 * the Core page's press and release strips read at their timestamps, and the two springs.
 */
class ToonCurvesTest {
    private val curves =
        Json
            .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText())
            .jsonObject

    private fun column(
        curve: String,
        name: String,
    ) = curves
        .getValue(curve)
        .jsonObject
        .getValue(name)
        .jsonArray
        .map { it.jsonPrimitive.float }

    private fun worst(
        expected: List<Float>,
        actual: (Int) -> Float,
    ): Float = expected.indices.maxOf { abs(expected[it] - actual(it)) }

    @Test
    fun `a press maps to the engine's squash`() {
        val at = column("squashOf(p)", "at")
        val sx = worst(column("squashOf(p)", "sx")) { toonSquashOf(at[it]).sx }
        val sy = worst(column("squashOf(p)", "sy")) { toonSquashOf(at[it]).sy }
        println("squashOf: worst sx $sx, sy $sy")
        assertTrue(max(sx, sy) <= TOLERANCE, "squash off the engine by ${max(sx, sy)}")
    }

    @Test
    fun `the springs follow the engine's spr and osc`() {
        val at = column("spr(t s, 0.35, 45)", "at")
        val spr = worst(column("spr(t s, 0.35, 45)", "value")) { toonSpring(at[it] / 1000, .35f, 45f) }
        val at2 = column("osc(t s, 0.35, 40)", "at")
        val osc = worst(column("osc(t s, 0.35, 40)", "value")) { toonOscillation(at2[it] / 1000, .35f, 40f) }
        println("spr: worst $spr; osc: worst $osc")
        assertTrue(max(spr, osc) <= TOLERANCE, "spring off the engine by ${max(spr, osc)}")
    }

    private companion object {
        /** The goldens' six decimals and Float arithmetic, as for the FX curves (B-12). */
        const val TOLERANCE = 1e-5f
    }
}
