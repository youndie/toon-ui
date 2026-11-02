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

    private data class Quad(
        val what: String,
        val golden: Float,
        val mine: Float,
        val tolerance: Float,
    )

    fun JsonObject.f(key: String): Float? = this[key]?.jsonPrimitive?.float

    private fun hex(c: Color) = "#%06X".format(c.toArgb() and 0xFFFFFF)

    /**
     * A path's coordinates as absolute numbers in the order the port keeps them: the start, then each
     * segment's control point (for `Q`) and end. The engine writes `M`, `L`, `Q` and `Z` absolute, and
     * the text field's caret in relative `h` and `v`, which this resolves.
     */
    private fun numbers(d: String): List<Float> {
        val out = mutableListOf<Float>()
        var x = 0f
        var y = 0f
        Regex("([MLQHVhvlZz])([^MLQHVhvlZz]*)").findAll(d).forEach { m ->
            val v = Regex("-?\\d+(?:\\.\\d+)?").findAll(m.groupValues[2]).map { it.value.toFloat() }.toList()
            when (m.groupValues[1]) {
                "M", "L" -> {
                    v.chunked(2).forEach { (a, b) ->
                        x = a
                        y = b
                        out += listOf(a, b)
                    }
                }

                "Q" -> {
                    v.chunked(4).forEach { q ->
                        out += q
                        x = q[2]
                        y = q[3]
                    }
                }

                "H" -> {
                    v.forEach {
                        x = it
                        out += listOf(x, y)
                    }
                }

                "V" -> {
                    v.forEach {
                        y = it
                        out += listOf(x, y)
                    }
                }

                "h" -> {
                    v.forEach {
                        x += it
                        out += listOf(x, y)
                    }
                }

                "v" -> {
                    v.forEach {
                        y += it
                        out += listOf(x, y)
                    }
                }

                "l" -> {
                    v.chunked(2).forEach { (a, b) ->
                        x += a
                        y += b
                        out += listOf(x, y)
                    }
                }
            }
        }
        return out
    }

    /** An affine transform as SVG writes it, `[a c e; b d f]`. */
    private class Affine(
        val a: Float = 1f,
        val b: Float = 0f,
        val c: Float = 0f,
        val d: Float = 1f,
        val e: Float = 0f,
        val f: Float = 0f,
    ) {
        operator fun times(o: Affine) =
            Affine(
                a * o.a + c * o.b,
                b * o.a + d * o.b,
                a * o.c + c * o.d,
                b * o.c + d * o.d,
                a * o.e + c * o.f + e,
                b * o.e + d * o.f + f,
            )
    }

    /** The engine's `transform`: its `translate`, `scale` and `rotate` operations, composed in order. */
    private fun transform(tf: String): Affine =
        Regex("(translate|scale|rotate)\\(([^)]*)\\)").findAll(tf).fold(Affine()) { m, op ->
            val v =
                op.groupValues[2]
                    .trim()
                    .split(Regex("[ ,]+"))
                    .map { it.toFloat() }
            m *
                when (op.groupValues[1]) {
                    "translate" -> {
                        Affine(e = v[0], f = v.getOrElse(1) { 0f })
                    }

                    "scale" -> {
                        Affine(a = v[0], d = v.getOrElse(1) { v[0] })
                    }

                    else -> {
                        val r = Math.toRadians(v[0].toDouble())
                        val (cx, cy) = if (v.size == 3) v[1] to v[2] else 0f to 0f
                        val cos = kotlin.math.cos(r).toFloat()
                        val sin = kotlin.math.sin(r).toFloat()
                        Affine(e = cx, f = cy) * Affine(cos, sin, -sin, cos) * Affine(e = -cx, f = -cy)
                    }
                }
        }

    /** A layer's own transform: its translation, its rotation, then its scale about its pivot. */
    private fun ToonLayer.affine(): Affine =
        Affine(e = dx, f = dy) * rotationOf(rotation) * Affine(e = pivotX, f = pivotY) * Affine(a = scale, d = scaleY) *
            Affine(e = -pivotX, f = -pivotY)

    private fun rotationOf(degrees: Float): Affine {
        val r = Math.toRadians(degrees.toDouble())
        val cos = kotlin.math.cos(r).toFloat()
        val sin = kotlin.math.sin(r).toFloat()
        return Affine(cos, sin, -sin, cos)
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
            val tfText = l.getValue("tf").jsonPrimitive.content
            val tf = transform(tfText)
            // Each translation the engine writes is rounded to 0.1 (`place` adds one to a layer's own);
            // the pair about a pivot is whole numbers.
            val rounded =
                Regex("translate\\(").findAll(tfText).count() - (if (mine.pivotX != 0f || mine.pivotY != 0f) 2 else 0)
            val offset = ROUNDING * max(1, rounded) + 2e-4f
            val own = mine.affine()
            // The engine writes a scale to three decimals and an offset to one; a pivot carries the
            // scale's rounding into the offset, so only a layer with one gets that much more.
            val carried = 5e-4f * maxOf(kotlin.math.abs(mine.pivotX), kotlin.math.abs(mine.pivotY))
            // It writes an angle to 0.1°: sin(0.05°) more in the matrix, for a layer that turns.
            val turned = if (mine.rotation != 0f) 8.73e-4f else 0f
            for ((what, g, m, tol) in listOf(
                Quad("scale x", tf.a, own.a, 5e-4f + turned),
                Quad("skew y", tf.b, own.b, 5e-4f + turned),
                Quad("skew x", tf.c, own.c, 5e-4f + turned),
                Quad("scale y", tf.d, own.d, 5e-4f + turned),
                Quad("x offset", tf.e, own.e, offset + carried),
                Quad("y offset", tf.f, own.f, offset + carried),
            )) {
                assertEquals(g, m, tol, "$name layer $i: $what")
            }
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
