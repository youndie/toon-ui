package io.github.youndie.toon.component

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.theme.ToonTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-18: every Core page switch cell and toggle frame against the engine's `sw`, `swAnim` against the
 * engine's at every millisecond in both directions — echo and wave included — and the switch under
 * a click and reduce motion.
 */
@OptIn(ExperimentalTestApi::class)
class ToonSwitchTest {
    private fun JsonObject.flag(key: String) = this[key]?.jsonPrimitive?.booleanOrNull ?: false

    private val origins =
        mapOf(
            "c" to ToonSwitchOrigin.Centre,
            "l" to ToonSwitchOrigin.Left,
            "r" to ToonSwitchOrigin.Right,
        )

    @Test
    fun `every switch cell and toggle frame is the engine's`() {
        val cells = EngineFrames.cells("Switch")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                val on = o.flag("on")
                val look =
                    ToonSwitchLook(
                        on = on,
                        pos = o.f("pos") ?: if (on) 1f else 0f,
                        sx = o.f("sx") ?: 1f,
                        sy = o.f("sy") ?: 1f,
                        origin = origins.getValue(o["origin"]?.jsonPrimitive?.content ?: "c"),
                        trackOn = o["trackOn"]?.jsonPrimitive?.boolean ?: on,
                        echo = o.f("echo") ?: 0f,
                        wave = o.f("wave"),
                        pressed = o.flag("pressed"),
                        disabled = o.flag("disabled"),
                        focus = o.flag("focus"),
                    )
                EngineFrames.compare(cell, toonSwitchScene(cell.tokens, look))
            }
        println("switch frames: ${cells.size} cells, worst coordinate $worst dp")
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    private val curves =
        Json
            .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText())
            .jsonObject

    private fun worst(
        from: Int,
        to: Int,
    ): Float {
        val c = curves.getValue("swAnim(t, $from, $to)").jsonObject
        val at = c.getValue("at").jsonArray.map { it.jsonPrimitive.float }
        var worst = 0f
        for (i in at.indices) {
            val mine = toonSwAnim(at[i], from, to)

            fun num(name: String) =
                c.getValue(name).jsonArray[i].let { if (it is JsonNull) null else it.jsonPrimitive.float }
            worst = maxOf(worst, abs(num("pos")!! - mine.pos), abs(num("sx")!! - mine.sx), abs(num("sy")!! - mine.sy))
            worst = maxOf(worst, abs((num("echo") ?: 0f) - mine.echo))
            val wave = num("wave")
            assertEquals(wave == null, mine.wave == null, "wave at ${at[i]} ms")
            if (wave != null) worst = maxOf(worst, abs(wave - mine.wave!!))
            assertEquals(
                origins.getValue(
                    c
                        .getValue("origin")
                        .jsonArray[i]
                        .jsonPrimitive.content,
                ),
                mine.origin,
                "origin at ${at[i]} ms",
            )
            assertEquals(
                c
                    .getValue("trackOn")
                    .jsonArray[i]
                    .jsonPrimitive.boolean,
                mine.trackOn,
                "track at ${at[i]} ms",
            )
        }
        return worst
    }

    @Test
    fun `the strip's values match the engine's swAnim at every millisecond, both ways`() {
        val on = worst(0, 1)
        val off = worst(1, 0)
        println("swAnim on $on, off $off")
        assertTrue(maxOf(on, off) <= 1e-5f, "swAnim off the engine by ${maxOf(on, off)}")
    }

    @Test
    fun `under reduce motion the thumb is at its end at once`() {
        val look = toonSwitchFrameAt(on = true, toggleAgeMs = 40f, p = 0f, reduceMotion = true)
        assertEquals(1f, look.pos)
        assertEquals(1f, look.sx)
        assertEquals(null, look.wave)
        assertEquals(
            toonSwAnim(40f, 0, 1).pos,
            toonSwitchFrameAt(on = true, toggleAgeMs = 40f, p = 0f, reduceMotion = false).pos,
        )
    }

    @Test
    fun `a press flattens the thumb onto its bottom as the Pressed cell`() {
        val look = toonSwitchFrameAt(on = true, toggleAgeMs = null, p = 1f, reduceMotion = false)
        assertEquals(1.18f, look.sx, 1e-6f)
        assertEquals(.84f, look.sy, 1e-6f)
        assertTrue(look.pressed)
    }

    @Test
    fun `a click toggles it`() =
        runComposeUiTest {
            val changes = mutableListOf<Boolean>()
            setContent { ToonTheme { ToonSwitch(true, { changes += it }, "Remind me at noon") } }
            val node = onNodeWithText("Remind me at noon").assertHeightIsEqualTo(48.dp)
            node.performClick()
            node.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
            assertEquals(listOf(false), changes)
        }
}
