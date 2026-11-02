package io.github.youndie.toon.component

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.theme.ToonTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-16: every MVP page FAB cell and landing frame against the engine's `fab` (and `dust`), the
 * landing curve against `fabAppear` at every millisecond, and the FAB over time.
 */
@OptIn(ExperimentalTestApi::class)
class ToonFabTest {
    @Test
    fun `every FAB cell and landing frame is the engine's`() {
        val cells = EngineFrames.cells("Fab")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                val look =
                    ToonFabLook(
                        p = o.f("p") ?: 0f,
                        sx = o.f("sx") ?: 1f,
                        sy = o.f("sy") ?: 1f,
                        disabled = o["disabled"]?.jsonPrimitive?.boolean ?: false,
                        focus = o["focus"]?.jsonPrimitive?.boolean ?: false,
                    )
                val scene =
                    if (o["land"] != null) {
                        val a = ToonFabAppear(o.f("op")!!, o.f("dy")!!, o.f("sx")!!, o.f("sy")!!, o.f("du")!!)
                        toonFabLandScene(cell.tokens, ToonIcons.Plus, "", 0f, look, a)
                    } else {
                        val label = o["label"]?.jsonPrimitive?.content ?: ""
                        toonFabScene(cell.tokens, ToonIcons.Plus, label, label.length * 17 * .55f, look)
                    }
                EngineFrames.compare(cell, scene)
            }
        println("FAB frames: ${cells.size} cells, worst coordinate $worst dp")
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the landing follows the engine's fabAppear`() {
        val c =
            Json
                .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText())
                .jsonObject
                .getValue("fabAppear(t)")
                .jsonObject

        fun col(name: String) = c.getValue(name).jsonArray.map { it.jsonPrimitive.float }
        val at = col("at")
        val fields =
            listOf<Pair<String, (ToonFabAppear) -> Float>>(
                "op" to { it.op },
                "dy" to { it.dy },
                "sx" to { it.sx },
                "sy" to { it.sy },
                "du" to { it.du },
            )
        val worst =
            fields.maxOf { (name, get) ->
                col(name).indices.maxOf {
                    abs(col(name)[it] - get(toonFabAppear(at[it])))
                }
            }
        println("fabAppear: worst $worst")
        assertTrue(worst <= 1e-5f, "fabAppear off the engine by $worst")
    }

    @Test
    fun `it hangs, falls and lands by the clock, late by its delay`() {
        val hang = toonFabFrameAt(0, landAgeMs = 60f, delayMs = 0, breathing = false, reduceMotion = false).appear
        assertEquals(toonFabAppear(60f).dy, hang.dy)
        val late = toonFabFrameAt(0, landAgeMs = 60f, delayMs = 100, breathing = false, reduceMotion = false).appear
        assertEquals(0f, late.op)
        assertEquals(-70f, late.dy)
    }

    @Test
    fun `under reduce motion the landed FAB fades in over 120 ms and does not breathe`() {
        val half = toonFabFrameAt(1250, landAgeMs = 60f, delayMs = 0, breathing = true, reduceMotion = true)
        assertEquals(.5f, half.appear.op, 1e-6f)
        assertEquals(0f, half.appear.dy)
        assertEquals(1f, half.look.br)
        assertTrue(half.appear.du < 0, "no dust")
        val breathing = toonFabFrameAt(1100, landAgeMs = null, delayMs = 0, breathing = true, reduceMotion = false)
        assertEquals(1.018f, breathing.look.br, 1e-6f)
    }

    @Test
    fun `width fits the label when extended and is 64 otherwise`() {
        assertEquals(64f, fabWidth(0f, extended = false))
        assertEquals(20 + 24 + 10 + 70f + 24, fabWidth(70f, extended = true))
    }

    @Test
    fun `a FAB clicks, a disabled one does not`() =
        runComposeUiTest {
            var clicks = 0
            // The FAB breathes while idle, so the clock never idles; advance it by hand.
            mainClock.autoAdvance = false
            setContent {
                ToonTheme {
                    Column {
                        ToonFab({ clicks++ }, contentDescription = "Add")
                        ToonFab({ clicks += 10 }, contentDescription = "Off", enabled = false)
                    }
                }
            }
            onNodeWithContentDescription("Add").performClick()
            onNodeWithContentDescription("Off").performClick()
            assertEquals(1, clicks)
        }
}
