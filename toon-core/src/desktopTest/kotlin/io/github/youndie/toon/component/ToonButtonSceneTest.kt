package io.github.youndie.toon.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
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
 * B-14: every cell of the Core page's button, as the port builds it, against the frame the engine's
 * `btn` returns for it (`engine-goldens/components.json`): the same layers in the same order, each
 * with the engine's colour, opacity, stroke and offset, and its path within the engine's 0.1 dp
 * rounding; the label at the engine's place, colour and opacity.
 */
class ToonButtonSceneTest {
    private val frames =
        Json
            .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/components.json")).readText())
            .jsonObject
            .getValue("Button")
            .jsonObject

    private val themes = mapOf("il" to inkGouacheLight, "in" to inkGouacheNight, "bc" to brightCel)

    private fun JsonObject.f(key: String) = this[key]?.jsonPrimitive?.float

    private fun look(o: JsonObject) =
        ToonButtonLook(
            p = o.f("p") ?: 0f,
            pl = o.f("pl") ?: o.f("p") ?: 0f,
            br = o.f("br") ?: 1f,
            seed = o["seed"]?.jsonPrimitive?.int ?: 0,
            disabled = o["disabled"]?.jsonPrimitive?.boolean ?: false,
            focus = o["focus"]?.jsonPrimitive?.boolean ?: false,
            lt = o.f("lt"),
            spin = o.f("spin") ?: 0f,
        )

    private fun hex(c: Color) = "#%06X".format(c.toArgb() and 0xFFFFFF)

    private fun numbers(d: String) =
        Regex("-?\\d+(?:\\.\\d+)?")
            .findAll(d)
            .map { it.value.toFloat() }
            .toList()

    private fun translate(tf: String): Pair<Float, Float> =
        Regex("translate\\(([-\\d.]+) ([-\\d.]+)\\)").find(tf)?.destructured?.let { (x, y) ->
            x.toFloat() to y.toFloat()
        }
            ?: (0f to 0f)

    /** The worst path coordinate off the engine over every cell; fails at the first structural difference. */
    private fun compareAll(): Float {
        var worst = 0f
        for ((cell, frame) in frames) {
            val f = frame.jsonObject
            val tokens: ToonTokens = themes.getValue(f.getValue("theme").jsonPrimitive.content)
            val o = f.getValue("opts").jsonObject
            val variant =
                ToonButtonVariant.valueOf(
                    o
                        .getValue("v")
                        .jsonPrimitive.content
                        .replaceFirstChar { it.uppercase() },
                )
            val scene: ToonScene = toonButtonScene(tokens, variant, 150f, LABEL_WIDTH, look(o))
            val layers = f.getValue("L").jsonArray
            assertEquals(layers.size, scene.layers.size, "$cell: layer count")
            layers.forEachIndexed { i, e ->
                val l = e.jsonObject
                val mine = scene.layers[i]
                val stroked = l.getValue("stroke").jsonPrimitive.content != "none"
                val colour =
                    l
                        .getValue(if (stroked) "stroke" else "fill")
                        .jsonPrimitive.content
                        .uppercase()
                assertEquals(colour, hex(mine.color), "$cell layer $i: colour")
                assertEquals(stroked, mine.stroke > 0, "$cell layer $i: stroked")
                if (stroked) assertEquals(l.f("sw")!!, mine.stroke, 1e-6f, "$cell layer $i: stroke width")
                assertEquals(l.f("op")!!, mine.alpha, 1e-5f, "$cell layer $i: opacity")
                val (dx, dy) = translate(l.getValue("tf").jsonPrimitive.content)
                assertEquals(dx, mine.dx, TOLERANCE, "$cell layer $i: x offset")
                assertEquals(dy, mine.dy, TOLERANCE, "$cell layer $i: y offset")
                val expected = numbers(l.getValue("d").jsonPrimitive.content)
                assertEquals(expected.size, mine.numbers.size, "$cell layer $i: coordinates")
                for (k in expected.indices) worst = max(worst, abs(expected[k] - mine.numbers[k]))
            }
            val texts = f.getValue("X").jsonArray.filter { it.jsonObject.f("op")!! > .01f }
            assertEquals(texts.size, scene.labels.size, "$cell: labels")
            texts.forEachIndexed { i, e ->
                val x = e.jsonObject
                val mine = scene.labels[i]
                assertEquals(x.f("x")!!, mine.x, 1e-4f, "$cell label: x")
                assertEquals(x.f("y")!!, mine.baseline, 1e-4f, "$cell label: baseline")
                assertEquals(
                    x
                        .getValue("fill")
                        .jsonPrimitive.content
                        .uppercase(),
                    hex(mine.color),
                    "$cell label: colour",
                )
                assertEquals(x.f("op")!!, mine.alpha, 1e-5f, "$cell label: opacity")
            }
        }
        return worst
    }

    @Test
    fun `every cell is the engine's frame layer by layer`() {
        val worst = compareAll()
        println("button frames: ${frames.size} cells, worst coordinate $worst dp")
        assertTrue(worst <= TOLERANCE, "worst coordinate $worst dp")
    }

    private companion object {
        /** The engine writes paths and offsets to 0.1 dp (research D11). */
        const val ROUNDING = 0.05f

        /** Half the rounding step plus Float error, as for `inkLine` and `star` in B-04. */
        const val TOLERANCE = ROUNDING + 2e-4f

        /** `tw` without a DOM: "Add plan".length × 18 × 0.55, the width the goldens were drawn with. */
        const val LABEL_WIDTH = 8 * 18 * .55f
    }
}
