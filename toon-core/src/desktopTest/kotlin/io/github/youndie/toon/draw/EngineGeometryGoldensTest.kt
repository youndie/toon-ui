package io.github.youndie.toon.draw

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
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

/**
 * B-04: the geometry port reproduces the engine's own output (research D3). The goldens are what
 * `scripts/engine-goldens.mjs` recorded while the engine's renderers drew, so every outline here is
 * one a component really asks for, with its radii, insets and seed.
 */
class EngineGeometryGoldensTest {
    private fun golden(name: String): JsonObject =
        Json
            .parseToJsonElement(
                checkNotNull(javaClass.getResource("/engine-goldens/$name")) { "missing golden $name" }.readText(),
            ).jsonObject

    private val themes = listOf("il", "in", "bc")

    private fun JsonElement.f() = jsonPrimitive.float

    private fun radii(e: JsonElement) = e.jsonArray.map { it.f() }.let { ToonRadii(it[0], it[1], it[2], it[3]) }

    /** Numbers of an SVG path string, in order: the engine's `M`/`L`/`Z` polygons. */
    private fun numbers(d: String): FloatArray =
        Regex("-?\\d+(?:\\.\\d+)?")
            .findAll(d)
            .map { it.value.toFloat() }
            .toList()
            .toFloatArray()

    private fun compare(
        what: String,
        expected: FloatArray,
        actual: FloatArray,
    ): Float {
        assertEquals(expected.size, actual.size, "$what: point count")
        var worst = 0f
        for (i in expected.indices) worst = max(worst, abs(expected[i] - actual[i]))
        return worst
    }

    private class Worst {
        var wob = 0f
        var path = 0f
        var count = 0
    }

    private fun measure(): Worst {
        val worst = Worst()
        for (theme in themes) {
            for ((scene, kinds) in golden("geometry-$theme.json")) {
                val k = kinds.jsonObject
                k["wob"]?.jsonArray?.forEach { g ->
                    val a = g.jsonObject.getValue("args").jsonArray
                    val step = a.getOrNull(7)?.takeIf { it != JsonNull }?.f() ?: 6f
                    val actual =
                        wobblePoints(
                            a[0].f(),
                            a[1].f(),
                            a[2].f(),
                            a[3].f(),
                            radii(a[4]),
                            a[5].f(),
                            a[6].jsonPrimitive.int,
                            step,
                        )
                    val expected =
                        g.jsonObject
                            .getValue("pts")
                            .jsonArray
                            .flatMap { p ->
                                p.jsonArray.map { it.f() }
                            }.toFloatArray()
                    worst.wob = max(worst.wob, compare("$theme $scene wob $a", expected, actual))
                    worst.count++
                }
                k["inkLine"]?.jsonArray?.forEach { g ->
                    val a = g.jsonObject.getValue("args").jsonArray
                    val sag = a.getOrNull(5)?.takeIf { it != JsonNull }?.f() ?: 0f
                    val actual = inkLinePoints(a[0].f(), a[1].f(), a[2].f(), a[3].f(), a[4].jsonPrimitive.int, sag)
                    worst.path =
                        max(
                            worst.path,
                            compare(
                                "$theme $scene inkLine $a",
                                numbers(
                                    g.jsonObject
                                        .getValue("d")
                                        .jsonPrimitive.content,
                                ),
                                actual,
                            ),
                        )
                    worst.count++
                }
                k["star"]?.jsonArray?.forEach { g ->
                    val a = g.jsonObject.getValue("args").jsonArray
                    val actual = starPoints(a[0].f(), a[1].f(), a[2].jsonPrimitive.int)
                    worst.path =
                        max(
                            worst.path,
                            compare(
                                "$theme $scene star $a",
                                numbers(
                                    g.jsonObject
                                        .getValue("d")
                                        .jsonPrimitive.content,
                                ),
                                actual,
                            ),
                        )
                    worst.count++
                }
                k["burst"]?.jsonArray?.forEach { g ->
                    val a = g.jsonObject.getValue("args").jsonArray
                    val kk = a.getOrNull(4)?.takeIf { it != JsonNull }?.f() ?: 0.76f
                    val actual = burstPoints(a[0].f(), a[1].f(), a[2].jsonPrimitive.int, a[3].jsonPrimitive.int, kk)
                    worst.path =
                        max(
                            worst.path,
                            compare(
                                "$theme $scene burst $a",
                                numbers(
                                    g.jsonObject
                                        .getValue("d")
                                        .jsonPrimitive.content,
                                ),
                                actual,
                            ),
                        )
                    worst.count++
                }
            }
        }
        return worst
    }

    @Test
    fun `every outline the engine drew is reproduced within the measured tolerance`() {
        val worst = measure()
        println("B-04 geometry: ${worst.count} goldens, worst wob ${worst.wob}, worst path ${worst.path}")
        assertTrue(worst.count > 0, "no goldens were read")
        assertTrue(worst.wob <= WOB_TOLERANCE, "wob points off by ${worst.wob} px, over $WOB_TOLERANCE")
        assertTrue(worst.path <= PATH_TOLERANCE, "path points off by ${worst.path} px, over $PATH_TOLERANCE")
    }

    @Test
    fun `the random sequence matches the engine`() {
        val r = ToonRandom(11)
        // node: r = new ToonEngine().rng(11); [r(), r(), r()]  (research §1.2's run)
        assertEquals(0.5115870486479253, r.next())
        assertEquals(0.5299464082345366, r.next())
        assertEquals(0.6081185641232878, r.next())
    }

    private companion object {
        // Measured once in B-04 and not to be raised to get green (research D11). Over 270 goldens
        // the worst wob point was 6.1e-5 px off: Float against the engine's doubles, plus the
        // goldens' six decimals. The inkLine, star and burst goldens are the engine's path strings,
        // rounded to 0.1 px, so their bound is half that step plus the same float error; the worst
        // was 0.04987.
        const val WOB_TOLERANCE = 2e-4f
        const val PATH_TOLERANCE = 0.05f + WOB_TOLERANCE
    }
}
