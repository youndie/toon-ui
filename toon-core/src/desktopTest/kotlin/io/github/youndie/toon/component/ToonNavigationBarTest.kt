package io.github.youndie.toon.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.parity.NavFrames
import io.github.youndie.toon.theme.ToonTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-27: every MVP page navigation bar cell and its select strip against the engine's frames,
 * `navSelect` at every millisecond, reduce motion, and selection by tap and by keyboard.
 */
@OptIn(ExperimentalTestApi::class)
class ToonNavigationBarTest {
    private fun JsonObject.int(k: String): Int? = this[k]?.takeIf { it !is JsonNull }?.jsonPrimitive?.int

    @Test
    fun `every navigation bar cell and select frame is the engine's`() {
        val cells = EngineFrames.cells("NavigationBar")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                val look =
                    if (o.getValue("kind").jsonPrimitive.content == "select") {
                        ToonNavLook(sel = 2, motion = toonNavSelect(o.f("t")!!, 0, 2, 97.5f))
                    } else {
                        ToonNavLook(
                            sel = o.int("sel")!!,
                            pressed = o.int("pressed"),
                            focus = o.int("focus"),
                            disabled = o.int("disabled"),
                            badge = o.int("badge")?.let { it to ToonBadgeLook(o.int("n").toString()) },
                        )
                    }
                EngineFrames.compare(cell, toonNavBarScene(cell.tokens, 390f, NavFrames.ITEMS, look))
            }
        println("navigation bar frames: ${cells.size} cells, worst coordinate $worst dp")
        assertEquals(30, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the select follows the engine's curve at every millisecond`() {
        val curve =
            Json
                .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText())
                .jsonObject
                .getValue("navSelect(t, 0, 2, 97.5)")
                .jsonObject

        fun col(name: String) = curve.getValue(name).jsonArray.map { it.jsonPrimitive.content.toFloat() }
        val at = col("at")
        var worst = 0f
        for ((name, get) in listOf<Pair<String, (ToonNavMotion) -> Float>>(
            "ia" to { it.ia!! },
            "ib" to { it.ib!! },
            "iy" to { it.iy },
            "isx" to { it.isx },
            "isy" to { it.isy },
            "du" to { it.du },
        )) {
            val c = col(name)
            for (i in at.indices) worst = maxOf(worst, abs(c[i] - get(toonNavSelect(at[i], 0, 2, 97.5f))))
        }
        println("navSelect curve: worst $worst")
        // The indicator's end reaches 276, where a Float's step is 3·10⁻⁵: two steps, as the dialog's.
        assertTrue(worst <= 6e-5f, "navSelect off the engine by $worst")
    }

    @Test
    fun `the selection plays for 700 ms, and not at all under reduce motion`() {
        assertEquals(toonNavSelect(699f, 0, 2, 97.5f), toonNavFrameAt(699f, 0, 2, 97.5f, false))
        assertEquals(ToonNavMotion(), toonNavFrameAt(700f, 0, 2, 97.5f, false))
        assertEquals(ToonNavMotion(), toonNavFrameAt(90f, 0, 2, 97.5f, true))
        assertEquals(ToonNavMotion(), toonNavFrameAt(null, 0, 2, 97.5f, false))
    }

    @Test
    fun `a slot is a quarter of the bar, clamped to the items`() {
        assertEquals(0, toonNavSlot(390f, 4, 0f))
        assertEquals(1, toonNavSlot(390f, 4, 97.6f))
        assertEquals(0, toonNavSlot(390f, 4, 97.4f))
        assertEquals(3, toonNavSlot(390f, 4, 400f))
    }

    @Test
    fun `a release on another item selects it once, and the keyboard does too`() {
        runComposeUiTest {
            val heard = mutableListOf<Int>()
            setContent {
                ToonTheme {
                    ToonNavigationBar(NavFrames.ITEMS, Modifier.testTag("nav"), onChange = { heard += it })
                }
            }
            val nav = onNodeWithTag("nav")
            nav.performTouchInput { click(Offset(250f, 40f)) }
            nav.performTouchInput { click(Offset(250f, 40f)) }
            assertEquals(listOf(2), heard, "the second tap is on the selected item")
            nav.performSemanticsAction(SemanticsActions.RequestFocus)
            nav.performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.Enter)
            }
            assertEquals(listOf(2, 1), heard, "focus starts on the first selection and moves right")
        }
    }
}
