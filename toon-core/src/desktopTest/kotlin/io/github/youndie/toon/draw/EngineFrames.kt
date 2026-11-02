package io.github.youndie.toon.draw

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.assertEquals

/**
 * B-14: a component's frames as the engine returns them for its cells on the spec pages
 * (`engine-goldens/components.json`), and the comparison every component's frame test makes against
 * them: the same layers in the same order, each with the engine's colour, opacity, stroke, offset and
 * scale, its path within the engine's 0.1 dp rounding; the same visible texts at the same places.
 */
internal object EngineFrames {
    private val all =
        Json
            .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/components.json")).readText())
            .jsonObject

    private val themes = mapOf("il" to inkGouacheLight, "in" to inkGouacheNight, "bc" to brightCel)

    /** The engine writes paths and offsets to 0.1 dp (research D11). */
    const val ROUNDING = 0.05f

    /** Half the rounding step plus Float error, as for `inkLine` and `star` in B-04. */
    const val TOLERANCE = ROUNDING + 2e-4f

    /** One cell: its name, the theme's tokens, the options it was drawn with, and the engine's frame. */
    class Cell(
        val name: String,
        val tokens: ToonTokens,
        val opts: JsonObject,
        val frame: JsonObject,
    )

    fun cells(group: String): List<Cell> =
        all.getValue(group).jsonObject.map { (name, f) ->
            val o = f.jsonObject
            Cell(name, themes.getValue(o.getValue("theme").jsonPrimitive.content), o.getValue("opts").jsonObject, o)
        }

    fun JsonObject.f(key: String): Float? = this[key]?.jsonPrimitive?.float

    private fun hex(c: Color) = "#%06X".format(c.toArgb() and 0xFFFFFF)

    private fun numbers(d: String) =
        Regex("-?\\d+(?:\\.\\d+)?")
            .findAll(d)
            .map { it.value.toFloat() }
            .toList()

    /** Every `translate` of a transform summed, and its `scale`: what `place` and `icon` write. */
    private fun transform(tf: String): Triple<Float, Float, Float> {
        var x = 0f
        var y = 0f
        Regex("translate\\(([-\\d.]+) ([-\\d.]+)\\)").findAll(tf).forEach {
            x += it.groupValues[1].toFloat()
            y += it.groupValues[2].toFloat()
        }
        val s =
            Regex("scale\\(([-\\d.]+)\\)")
                .find(tf)
                ?.groupValues
                ?.get(1)
                ?.toFloat() ?: 1f
        return Triple(x, y, s)
    }

    /** Asserts [scene] is [cell]'s frame and returns its worst path coordinate off the engine's. */
    fun compare(
        cell: Cell,
        scene: ToonScene,
    ): Float {
        val name = cell.name
        var worst = 0f
        val layers = cell.frame.getValue("L").jsonArray
        assertEquals(layers.size, scene.layers.size, "$name: layer count")
        layers.forEachIndexed { i, e ->
            val l = e.jsonObject
            val mine = scene.layers[i]
            val stroked = l.getValue("stroke").jsonPrimitive.content != "none"
            val (dx, dy, scale) = transform(l.getValue("tf").jsonPrimitive.content)
            assertEquals(dx, mine.dx, TOLERANCE, "$name layer $i: x offset")
            assertEquals(dy, mine.dy, TOLERANCE, "$name layer $i: y offset")
            assertEquals(scale, mine.scale, 5e-4f, "$name layer $i: scale")
            assertEquals(l.f("op")!!, mine.alpha, 1e-5f, "$name layer $i: opacity")
            val d = l.getValue("d").jsonPrimitive.content
            if (mine.source != null) {
                // A ready path: an icon. The engine's `d` is its source; without a DOM, an outline
                // is the centerline stroked at the weight's base width.
                assertEquals(d, mine.source, "$name layer $i: icon path")
                val colour =
                    l
                        .getValue(if (stroked) "stroke" else "fill")
                        .jsonPrimitive.content
                        .uppercase()
                assertEquals(colour, hex(mine.color), "$name layer $i: icon colour")
                if (stroked) assertEquals(l.f("sw")!!, mine.iconBase ?: -1f, 1e-6f, "$name layer $i: icon weight")
                return@forEachIndexed
            }
            val colour =
                l
                    .getValue(if (stroked) "stroke" else "fill")
                    .jsonPrimitive.content
                    .uppercase()
            assertEquals(colour, hex(mine.color), "$name layer $i: colour")
            assertEquals(stroked, mine.stroke > 0, "$name layer $i: stroked")
            if (stroked) assertEquals(l.f("sw")!!, mine.stroke, 1e-6f, "$name layer $i: stroke width")
            val dash = l["dash"]?.jsonPrimitive?.content?.takeIf { it != "none" }
            assertEquals(
                dash,
                mine.dash?.joinToString(" ") {
                    if (it % 1f ==
                        0f
                    ) {
                        it.toInt().toString()
                    } else {
                        it.toString()
                    }
                },
                "$name layer $i: dash",
            )
            // The engine writes the offset to two decimals: half a step, plus Float error.
            assertEquals(l.f("doff") ?: 0f, mine.dashOffset, 5e-3f + 1e-4f, "$name layer $i: dash offset")
            val expected = numbers(d)
            assertEquals(expected.size, mine.numbers.size, "$name layer $i: coordinates")
            for (k in expected.indices) worst = max(worst, abs(expected[k] - mine.numbers[k]))
        }
        val texts =
            cell.frame
                .getValue("X")
                .jsonArray
                .map { it.jsonObject }
                .filter { it.f("op")!! > .01f }
        val labels = scene.labels.filter { it.alpha > .01f }
        assertEquals(texts.size, labels.size, "$name: visible texts")
        texts.forEachIndexed { i, x ->
            val mine = labels[i]
            assertEquals(x.getValue("s").jsonPrimitive.content, mine.text, "$name text $i")
            assertEquals(x.f("x")!!, mine.x, 1e-4f, "$name text $i: x")
            assertEquals(x.f("y")!!, mine.baseline, 1e-4f, "$name text $i: baseline")
            assertEquals(
                x
                    .getValue("fill")
                    .jsonPrimitive.content
                    .uppercase(),
                hex(mine.color),
                "$name text $i: colour",
            )
            assertEquals(x.f("op")!!, mine.alpha, 1e-5f, "$name text $i: opacity")
            assertEquals(x.f("fw")!!.toInt(), mine.style.weight, "$name text $i: weight")
            assertEquals(x.f("fs")!!, mine.style.size, 1e-6f, "$name text $i: size")
            assertEquals(
                x.getValue("anchor").jsonPrimitive.content,
                mine.anchor.name.lowercase(),
                "$name text $i: anchor",
            )
        }
        return worst
    }
}
