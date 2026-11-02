package io.github.youndie.toon.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.down
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.up
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.parity.MenuFrames
import io.github.youndie.toon.theme.ToonTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B-37: every v1 page menu cell and its unroll and roll-up strips against the engine's frames,
 * `menuUnroll` at every millisecond both ways, reduce motion, and choosing an item by a release, by
 * the keyboard and by a screen reader.
 */
@OptIn(ExperimentalTestApi::class)
class ToonMenuTest {
    @Test
    fun `every menu cell and strip frame is the engine's`() {
        val cells = EngineFrames.cells("Menu")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts

                fun index(k: String) = o[k]?.jsonPrimitive?.intOrNull
                val scene =
                    when (o.getValue("kind").jsonPrimitive.content) {
                        "unroll" -> {
                            MenuFrames.strip(cell.tokens, true, o.f("t")!!)
                        }

                        "rollUp" -> {
                            MenuFrames.strip(cell.tokens, false, o.f("t")!!)
                        }

                        else -> {
                            toonMenuScene(
                                cell.tokens,
                                200f,
                                MenuFrames.items,
                                ToonMenuLook(
                                    o.f("u") ?: 1f,
                                    index("pressed"),
                                    focus = index("focus"),
                                    disabled = index("disabled"),
                                ),
                            )
                        }
                    }
                EngineFrames.compare(cell, scene)
            }
        println("menu frames: ${cells.size} cells, worst coordinate $worst dp")
        assertEquals(42, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the paper follows the engine's curve at every millisecond, unrolled and rolled up`() {
        val curves =
            Json
                .parseToJsonElement(
                    checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText(),
                ).jsonObject
        var worst = 0f
        for (open in listOf(true, false)) {
            val c = curves.getValue("menuUnroll(t, $open)").jsonObject
            val at = c.getValue("at").jsonArray.map { it.jsonPrimitive.content.toFloat() }
            val v = c.getValue("value").jsonArray.map { it.jsonPrimitive.content.toFloat() }
            for (i in at.indices) worst = maxOf(worst, abs(v[i] - toonMenuUnroll(at[i], open)))
        }
        println("menuUnroll: worst $worst")
        assertTrue(worst <= 1e-5f, "menuUnroll off the engine by $worst")
    }

    @Test
    fun `the scroll shows nothing shut, and under reduce motion fades unrolled in 120 ms`() {
        assertNull(toonMenuFrameAt(false, null, false))
        assertEquals(ToonMenuMoment(1f), toonMenuFrameAt(true, null, false))
        assertEquals(ToonMenuMoment(toonMenuUnroll(60f, true)), toonMenuFrameAt(true, 60f, false))
        assertEquals(ToonMenuMoment(toonMenuUnroll(60f, false)), toonMenuFrameAt(false, 60f, false))
        // Rolled up past 160 ms, the paper is gone; at the start of an unroll it has not dropped yet.
        assertNull(toonMenuFrameAt(false, 160f, false))
        assertNull(toonMenuFrameAt(true, 0f, false))
        assertEquals(ToonMenuMoment(1f, .5f), toonMenuFrameAt(true, 60f, true))
        assertEquals(ToonMenuMoment(1f, .5f), toonMenuFrameAt(false, 60f, true))
        assertNull(toonMenuFrameAt(false, 120f, true))
        assertEquals(ToonMenuMoment(1f, 1f), toonMenuFrameAt(true, 500f, true))
    }

    @Test
    fun `an item is the 44 dp row under the point, 14 dp down`() {
        assertEquals(0, toonMenuItemAt(3, 14f))
        assertEquals(0, toonMenuItemAt(3, 57.9f))
        assertEquals(1, toonMenuItemAt(3, 58f))
        assertEquals(2, toonMenuItemAt(3, 145f))
        assertNull(toonMenuItemAt(3, 13f))
        assertNull(toonMenuItemAt(3, 146f))
        assertEquals(171f, toonMenuHeight(3))
    }

    @Test
    fun `an item fires on release, not on press, and a shut menu does not fire`() {
        runComposeUiTest {
            val heard = mutableListOf<Int>()
            var open by mutableStateOf(true)
            setContent {
                ToonTheme {
                    ToonMenu(MenuFrames.items, Modifier.testTag("menu"), open = open, onSelect = {
                        heard +=
                            it
                    })
                }
            }
            val menu = onNodeWithTag("menu")
            menu.performTouchInput { down(Offset(100f, 80f)) }
            assertEquals(emptyList(), heard, "a press alone chooses nothing")
            menu.performTouchInput { up() }
            assertEquals(listOf(1), heard)
            menu.performTouchInput { click(Offset(100f, 130f)) }
            assertEquals(listOf(1, 2), heard)
            menu.performTouchInput { click(Offset(100f, 6f)) }
            assertEquals(listOf(1, 2), heard, "above the items is nothing")
            open = false
            waitForIdle()
            menu.performTouchInput { click(Offset(100f, 30f)) }
            assertEquals(listOf(1, 2), heard, "rolled up, it does not fire")
        }
    }

    @Test
    fun `the keyboard walks the items and chooses, and a screen reader has one action per item`() {
        runComposeUiTest {
            val heard = mutableListOf<Int>()
            setContent {
                ToonTheme {
                    ToonMenu(
                        MenuFrames.items,
                        Modifier.testTag("menu"),
                        onSelect = { heard += it },
                    )
                }
            }
            val menu = onNodeWithTag("menu")
            menu.performSemanticsAction(SemanticsActions.RequestFocus)
            menu.performKeyInput {
                pressKey(Key.Enter)
                pressKey(Key.DirectionDown)
                pressKey(Key.DirectionDown)
                pressKey(Key.DirectionDown)
                pressKey(Key.Spacebar)
                pressKey(Key.DirectionUp)
                pressKey(Key.Enter)
            }
            assertEquals(listOf(0, 2, 1), heard, "focus starts on the first item and stops at the last")
            val actions =
                menu
                    .fetchSemanticsNode()
                    .config
                    .getOrNull(SemanticsActions.CustomActions)
                    .orEmpty()
            assertEquals(listOf("Edit", "Share", "Delete"), actions.map { it.label })
            actions[2].action()
            assertEquals(listOf(0, 2, 1, 2), heard)
        }
    }
}
