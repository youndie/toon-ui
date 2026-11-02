package io.github.youndie.toon.fx

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-12: the iris and shake curves against the engine's, sampled every millisecond (`curves.json`). */
class ToonFxCurvesTest {
    private val curves =
        Json
            .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText())
            .jsonObject

    private fun column(
        curve: JsonObject,
        name: String,
    ) = curve.getValue(name).jsonArray.map { it.jsonPrimitive.float }

    @Test
    fun `the iris closes, holds and opens as the engine's irisAt`() {
        val c = curves.getValue("irisAt(t)").jsonObject
        val at = column(c, "at")
        val k = column(c, "k")
        val scene = c.getValue("scene").jsonArray.map { it.jsonPrimitive.int }
        var worst = 0f
        for (i in at.indices) {
            val f = toonIrisAt(at[i])
            worst = max(worst, abs(f.k - k[i]))
            assertEquals(scene[i], f.scene, "scene at ${at[i]} ms")
        }
        println("irisAt: worst k $worst")
        assertTrue(worst <= TOLERANCE, "worst k $worst")
    }

    @Test
    fun `the struck element shakes as the engine's shake`() {
        val c = curves.getValue("shake(t)").jsonObject
        val at = column(c, "at")
        val value = column(c, "value")
        var worst = 0f
        for (i in at.indices) worst = max(worst, abs(toonShake(at[i]) - value[i]))
        println("shake: worst $worst dp")
        assertTrue(worst <= TOLERANCE, "worst offset $worst dp")
    }

    private companion object {
        /**
         * The goldens' six decimals (±5·10⁻⁷) and Float arithmetic. Measured worst on 2026-11-02:
         * 6.0·10⁻⁷ for the iris's k, 2.2·10⁻⁶ dp for the shake, whose amplitude is 6 dp.
         */
        const val TOLERANCE = 1e-5f
    }
}
