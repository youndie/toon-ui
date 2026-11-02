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
import io.github.youndie.toon.parity.toonTabsSwitchScene
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
 * B-28: every MVP page tabs cell and its switch strip against the engine's frames, `tabsSwitch` at
 * every millisecond, the content offset and reduce motion, and switching by tap and by keyboard.
 */
@OptIn(ExperimentalTestApi::class)
class ToonTabsTest {
    private val labels = listOf("Today", "Weekend", "Someday")

    private fun JsonObject.int(k: String): Int? = this[k]?.takeIf { it !is JsonNull }?.jsonPrimitive?.int

    @Test
    fun `every tabs cell and switch frame is the engine's`() {
        val cells = EngineFrames.cells("Tabs")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                val scene =
                    if (o.getValue("kind").jsonPrimitive.content == "switch") {
                        toonTabsSwitchScene(cell.tokens, o.f("t")!!)
                    } else {
                        toonTabsScene(
                            cell.tokens,
                            342f,
                            labels,
                            ToonTabsLook(
                                o.int("sel")!!,
                                pressed = o.int("pressed"),
                                focus = o.int("focus"),
                                disabled = o.int("disabled"),
                            ),
                        )
                    }
                EngineFrames.compare(cell, scene)
            }
        println("tabs frames: ${cells.size} cells, worst coordinate $worst dp")
        assertEquals(30, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the switch follows the engine's curve at every millisecond`() {
        val curve =
            Json
                .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText())
                .jsonObject
                .getValue("tabsSwitch(t, 0, 1, 114)")
                .jsonObject

        fun col(name: String) = curve.getValue(name).jsonArray.map { it.jsonPrimitive.content.toFloat() }
        val at = col("at")
        var worst = 0f
        for ((name, get) in listOf<Pair<String, (ToonTabsMotion) -> Float>>(
            "ia" to { it.ia!! },
            "ib" to { it.ib!! },
            "cx" to { it.cx },
            "sel" to { it.sel!!.toFloat() },
        )) {
            val c = col(name)
            for (i in at.indices) worst = maxOf(worst, abs(c[i] - get(toonTabsSwitch(at[i], 0, 1, 114f))))
        }
        println("tabsSwitch curve: worst $worst")
        assertTrue(worst <= 2e-5f, "tabsSwitch off the engine by $worst")
    }

    @Test
    fun `the switch and the content's slide play for 700 ms, and not at all under reduce motion`() {
        assertEquals(toonTabsSwitch(699f, 0, 2, 114f), toonTabsFrameAt(699f, 0, 2, 114f, false))
        assertEquals(ToonTabsMotion(), toonTabsFrameAt(700f, 0, 2, 114f, false))
        assertEquals(ToonTabsMotion(), toonTabsFrameAt(90f, 0, 2, 114f, true))
        // The content starts 40 dp toward the new tab and springs back.
        assertEquals(40f, toonTabsSwitch(0f, 0, 2, 114f).cx, 1e-5f)
        assertEquals(-40f, toonTabsSwitch(0f, 2, 0, 114f).cx, 1e-5f)
        val state = ToonTabsState(0)
        state.slot = 114f
        state.select(1)
        state.tick(1000)
        state.tick(1050)
        assertEquals(toonTabsSwitch(50f, 0, 1, 114f).cx, state.contentOffset.value, 1e-5f)
        state.reduceMotion = true
        assertEquals(0f, state.contentOffset.value)
    }

    @Test
    fun `a release on another tab switches once, and the keyboard does too`() {
        runComposeUiTest {
            val heard = mutableListOf<Int>()
            setContent {
                ToonTheme {
                    ToonTabs(labels, Modifier.testTag("tabs"), onChange = { heard += it })
                }
            }
            val tabs = onNodeWithTag("tabs")
            tabs.performTouchInput { click(Offset(290f, 26f)) }
            tabs.performTouchInput { click(Offset(290f, 26f)) }
            assertEquals(listOf(2), heard)
            tabs.performSemanticsAction(SemanticsActions.RequestFocus)
            tabs.performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.Enter)
            }
            assertEquals(listOf(2, 1), heard)
        }
    }
}
