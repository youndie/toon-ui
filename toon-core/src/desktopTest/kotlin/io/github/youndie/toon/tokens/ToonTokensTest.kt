package io.github.youndie.toon.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * B-07: the generated tokens hold exactly what the engine's THEMES hold. A second reading: the
 * engine's own THEMES (engine-goldens/themes.json, written by running it) against the Kotlin values,
 * every colour and every shape number, with the key-to-name table written out again here rather than
 * shared with the generator.
 */
class ToonTokensTest {
    private val themes: JsonObject =
        Json
            .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/themes.json")).readText())
            .jsonObject

    private fun hex(c: Color) = "#%06X".format(c.toArgb() and 0xFFFFFF)

    private fun colours(t: ToonTokens): Map<String, Color?> =
        with(t.colors) {
            mapOf(
                "ink" to ink,
                "bg" to stage,
                "paper" to surface,
                "surface2" to surfacePressed,
                "text" to text,
                "muted" to textMuted,
                "primary" to primary,
                "onPrimary" to onPrimary,
                "danger" to danger,
                "onDanger" to onDanger,
                "dangerText" to dangerText,
                "success" to success,
                "successLight" to successLight,
                "warn" to warning,
                "focus" to focus,
                "off" to trackOff,
                "disabledFill" to disabled,
                "dust" to fxDust,
                "btnHi" to primaryHighlight,
                "chipSel" to chipSelected,
                "field" to field,
                "on" to trackOn,
                "thumb" to thumb,
                "sweat" to fxSweat,
                "fxLine" to fxLine,
                "holeEdge" to holeEdge,
                "nail" to nail,
                "rim" to rim,
                "fieldRim" to fieldRim,
            )
        }

    private fun check(
        key: String,
        t: ToonTokens,
    ) {
        val e = themes.getValue(key).jsonObject
        colours(t).forEach { (k, c) ->
            val expected =
                e
                    .getValue(k)
                    .takeIf { it != JsonNull }
                    ?.jsonPrimitive
                    ?.content
            assertEquals(expected, c?.let(::hex), "$key.$k")
        }
        assertEquals(
            e.getValue("cardColors").jsonArray.map {
                it.jsonPrimitive.content
            },
            t.colors.cardFills.map(::hex),
            "$key.cardColors",
        )
        assertEquals(
            e.getValue("sColors").jsonArray.map {
                it.jsonPrimitive.content
            },
            t.colors.stickers.map(::hex),
            "$key.sColors",
        )
        with(t.shape) {
            assertEquals(e.getValue("amp").jsonPrimitive.float, wobble, "$key.amp")
            assertEquals(
                e.getValue("ins").jsonArray.map {
                    it.jsonPrimitive.float
                },
                listOf(insets.top, insets.right, insets.bottom, insets.left),
                "$key.ins",
            )
            assertEquals(
                e.getValue("btnR").jsonArray.map { it.jsonPrimitive.float },
                listOf(buttonRadii.topLeft, buttonRadii.topRight, buttonRadii.bottomRight, buttonRadii.bottomLeft),
                "$key.btnR",
            )
            assertEquals(e.getValue("rs").jsonPrimitive.float, radiusScale, "$key.rs")
            assertEquals(e.getValue("sh").jsonPrimitive.float, shadow, "$key.sh")
            assertEquals(e.getValue("rots").jsonArray.map { it.jsonPrimitive.float }, tilts, "$key.rots")
            assertEquals(e.getValue("seed").jsonPrimitive.int, seed, "$key.seed")
        }
        assertEquals(e.getValue("display").jsonPrimitive.content, t.displayFamily, "$key.display")
        assertEquals(e.getValue("body").jsonPrimitive.content, t.bodyFamily, "$key.body")
    }

    @Test
    fun `Ink and Gouache light matches the engine`() = check("il", inkGouacheLight)

    @Test
    fun `Ink and Gouache night matches the engine`() = check("in", inkGouacheNight)

    @Test
    fun `Bright Cel matches the engine`() = check("bc", brightCel)

    @Test
    fun `every engine key is covered by the test's table`() {
        val covered =
            colours(inkGouacheLight).keys +
                listOf("cardColors", "sColors", "amp", "ins", "btnR", "rs", "sh", "rots", "seed", "display", "body")
        val presentational = setOf("key", "label", "sub")
        val engine = themes.getValue("il").jsonObject.keys - presentational
        assertEquals(emptySet(), engine - covered, "THEMES keys with no token")
    }
}
