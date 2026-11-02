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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B-17: every Core page checkbox cell and MVP page radio cell, with their strips, against the
 * engine's `check` and `radio` frames; `chkAnim` and `radioSelect` against the engine's at every
 * millisecond; and the two over time and under a click.
 */
@OptIn(ExperimentalTestApi::class)
class ToonCheckboxAndRadioTest {
    private fun JsonObject.flag(key: String) = this[key]?.jsonPrimitive?.boolean ?: false

    @Test
    fun `every checkbox cell is the engine's frame`() {
        val cells = EngineFrames.cells("Checkbox")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                val on = o.flag("on")
                val look =
                    ToonCheckboxLook(
                        on = on,
                        trim = o.f("trim") ?: if (on) 1f else 0f,
                        sx = o.f("sx") ?: 1f,
                        sy = o.f("sy") ?: 1f,
                        dx = o.f("dx") ?: 0f,
                        sp = o.f("sp") ?: 0f,
                        disabled = o.flag("disabled"),
                        focus = o.flag("focus"),
                        error = o.flag("error"),
                    )
                EngineFrames.compare(
                    cell,
                    toonCheckboxScene(cell.tokens, o["label"]?.jsonPrimitive?.content ?: "", look),
                )
            }
        println("checkbox frames: ${cells.size} cells, worst coordinate $worst dp")
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `every radio cell is the engine's frame`() {
        val cells = EngineFrames.cells("Radio")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                val sel = o.flag("sel")
                val look =
                    ToonRadioLook(
                        selected = sel,
                        ds = o.f("ds") ?: if (sel) 1f else 0f,
                        dotY = o.f("dotY") ?: 0f,
                        dsx = o.f("dsx") ?: 1f,
                        dsy = o.f("dsy") ?: 1f,
                        sp = o.f("sp") ?: -1f,
                        sq = o.f("sq") ?: 1f,
                        disabled = o.flag("disabled"),
                        focus = o.flag("focus"),
                    )
                EngineFrames.compare(cell, toonRadioScene(cell.tokens, o["label"]?.jsonPrimitive?.content ?: "", look))
            }
        println("radio frames: ${cells.size} cells, worst coordinate $worst dp")
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    private val curves =
        Json
            .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText())
            .jsonObject

    private fun worst(
        curve: String,
        fields: List<Pair<String, (Float) -> Float>>,
    ): Float {
        val c = curves.getValue(curve).jsonObject
        val at = c.getValue("at").jsonArray.map { it.jsonPrimitive.float }
        return fields.maxOf { (name, get) ->
            val col =
                c.getValue(name).jsonArray.map {
                    it.jsonPrimitive.content.let { v ->
                        v.toFloatOrNull()
                            ?: if (v == "true") 1f else 0f
                    }
                }
            at.indices.maxOf { abs(col[it] - get(at[it])) }
        }
    }

    @Test
    fun `the tick, the shiver and the drop follow the engine's curves`() {
        fun chk(on: Boolean): List<Pair<String, (Float) -> Float>> =
            listOf<Pair<String, (Float) -> Float>>(
                "on" to { t -> if (toonChkAnim(t, on).on) 1f else 0f },
                "trim" to { t -> toonChkAnim(t, on).trim },
                "sx" to { t -> toonChkAnim(t, on).sx },
                "sy" to { t -> toonChkAnim(t, on).sy },
                "dx" to { t -> toonChkAnim(t, on).dx },
            ) + if (on) listOf("sp" to { t: Float -> toonChkAnim(t, on).sp }) else emptyList()
        val check = worst("chkAnim(t, true)", chk(true))
        val uncheck = worst("chkAnim(t, false)", chk(false))
        val drop =
            worst(
                "radioSelect(t)",
                listOf<Pair<String, (Float) -> Float>>(
                    "ds" to { t -> toonRadioSelect(t).ds },
                    "dotY" to { t -> toonRadioSelect(t).dotY },
                    "dsx" to { t -> toonRadioSelect(t).dsx },
                    "dsy" to { t -> toonRadioSelect(t).dsy },
                    "sp" to { t -> toonRadioSelect(t).sp },
                ),
            )
        println("chkAnim on $check, off $uncheck; radioSelect $drop")
        assertTrue(maxOf(check, uncheck, drop) <= 1e-5f, "curves off the engine by ${maxOf(check, uncheck, drop)}")
    }

    @Test
    fun `under reduce motion a check fades in over 120 ms, an uncheck and a drop are instant`() {
        val check = toonCheckboxFrameAt(on = true, toggleAgeMs = 60f, p = 0f, reduceMotion = true)
        assertEquals(.5f, check.fade!!, 1e-6f)
        assertEquals(1f, check.trim)
        val uncheck = toonCheckboxFrameAt(on = false, toggleAgeMs = 60f, p = 0f, reduceMotion = true)
        assertNull(uncheck.fade)
        assertEquals(0f, uncheck.trim)
        val drop = toonRadioFrameAt(1, selectedIndex = 1, previousIndex = 0, ageMs = 60f, p = 0f, reduceMotion = true)
        assertEquals(0f, drop.dotY)
        assertEquals(
            0f,
            toonRadioFrameAt(0, selectedIndex = 1, previousIndex = 0, ageMs = 60f, p = 0f, reduceMotion = true).ds,
        )
    }

    @Test
    fun `the old dot deflates in 120 ms while the new one falls`() {
        assertEquals(.5f, toonRadioFrameAt(0, 1, 0, ageMs = 60f, p = 0f, reduceMotion = false).ds, 1e-6f)
        assertEquals(
            toonRadioSelect(60f).dotY,
            toonRadioFrameAt(1, 1, 0, ageMs = 60f, p = 0f, reduceMotion = false).dotY,
        )
    }

    @Test
    fun `a press squashes the box and the ring to 0_86`() {
        assertEquals(.86f, toonCheckboxFrameAt(on = false, toggleAgeMs = null, p = 1f, reduceMotion = false).sx, 1e-6f)
        assertEquals(.86f, toonRadioFrameAt(0, 0, -1, ageMs = null, p = 1f, reduceMotion = false).sq, 1e-6f)
    }

    @Test
    fun `a click checks and selects`() =
        runComposeUiTest {
            val checks = mutableListOf<Boolean>()
            val picks = mutableListOf<Int>()
            setContent {
                ToonTheme {
                    androidx.compose.foundation.layout.Column {
                        ToonCheckbox(false, { checks += it }, "Bring snacks")
                        ToonRadioGroup(listOf("Morning", "Noon", "Evening"), 0, { picks += it })
                    }
                }
            }
            onNodeWithText("Bring snacks").assertHeightIsEqualTo(48.dp).performClick()
            onNodeWithText(
                "Bring snacks",
            ).assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            onNodeWithText("Noon").assertHeightIsEqualTo(52.dp).performClick()
            onNodeWithText("Noon").assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            onNodeWithText("Noon").performClick()
            assertEquals(listOf(true), checks)
            assertEquals(listOf(1), picks)
        }
}
