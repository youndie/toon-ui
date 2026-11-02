package io.github.youndie.toon.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.parity.SegFrames
import io.github.youndie.toon.theme.ToonTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-41: every v1 page segmented cell and its switch strip against the engine's frames, `segSwitch`
 * at every millisecond, reduce motion, and switching by a release, by the keyboard and by a screen
 * reader — and not at all when disabled.
 */
@OptIn(ExperimentalTestApi::class)
class ToonSegmentedTest {
    private val labels = listOf("Day", "Week", "Month")

    @Test
    fun `every segmented cell and switch frame is the engine's`() {
        val cells = EngineFrames.cells("Segmented") + EngineFrames.cells("SegmentedProbe")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts

                fun index(k: String) = o[k]?.jsonPrimitive?.intOrNull
                val scene =
                    if (o.getValue("kind").jsonPrimitive.content == "switch") {
                        SegFrames.switch(cell.tokens, o.f("t")!!)
                    } else {
                        toonSegmentedScene(
                            cell.tokens,
                            260f,
                            labels,
                            ToonSegmentedLook(
                                sel = index("sel")!!,
                                pressed = index("pressed"),
                                focus = index("focus"),
                                disabled = o["disabled"]?.jsonPrimitive?.booleanOrNull ?: false,
                            ),
                        )
                    }
                EngineFrames.compare(cell, scene)
            }
        println("segmented frames: ${cells.size} cells, worst coordinate $worst dp")
        assertEquals(33, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the drop follows the engine's curve at every millisecond`() {
        val c =
            Json
                .parseToJsonElement(
                    checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText(),
                ).jsonObject
                .getValue("segSwitch(t, 0, 2, 86.7)")
                .jsonObject

        fun col(k: String) = c.getValue(k).jsonArray.map { it.jsonPrimitive.content.toFloat() }
        val at = col("at")
        val ia = col("ia")
        val ib = col("ib")
        val sq = col("sq")
        val sel = col("sel")
        var worst = 0f
        for (i in at.indices) {
            val m = toonSegSwitch(at[i], 0, 2, 86.7f)
            worst = maxOf(worst, abs(ia[i] - m.ia!!), abs(ib[i] - m.ib!!), abs(sq[i] - m.sq))
            assertEquals(sel[i].toInt(), m.sel, "sel at ${at[i]} ms")
        }
        println("segSwitch: worst $worst")
        // The drop's head overshoots to 283, where a Float's step is 3·10⁻⁵: two steps of it.
        val bound = 2 * Math.ulp(ib.max())
        assertTrue(worst <= bound, "segSwitch off the engine by $worst, over $bound")
    }

    @Test
    fun `a switch plays 700 ms, and under reduce motion the drop moves at once`() {
        assertEquals(toonSegSwitch(160f, 0, 2, 100f), toonSegmentedFrameAt(160f, 0, 2, 100f, false))
        assertEquals(toonSegSwitch(699f, 0, 2, 100f), toonSegmentedFrameAt(699f, 0, 2, 100f, false))
        assertEquals(ToonSegmentedMotion(), toonSegmentedFrameAt(700f, 0, 2, 100f, false))
        assertEquals(ToonSegmentedMotion(), toonSegmentedFrameAt(160f, 0, 2, 100f, true))
        assertEquals(ToonSegmentedMotion(), toonSegmentedFrameAt(null, 0, 2, 100f, false))
    }

    @Test
    fun `a release on another segment switches once, the keyboard too, and a screen reader`() {
        runComposeUiTest {
            val heard = mutableListOf<Int>()
            setContent { ToonTheme { ToonSegmented(labels, Modifier.testTag("seg"), onChange = { heard += it }) } }
            val seg = onNodeWithTag("seg")
            seg.performTouchInput { click(Offset(250f, 22f)) }
            seg.performTouchInput { click(Offset(250f, 22f)) }
            assertEquals(listOf(2), heard)
            seg.performSemanticsAction(SemanticsActions.RequestFocus)
            seg.performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.Enter)
            }
            assertEquals(listOf(2, 1), heard, "focus starts on the first selection and moves right")
            assertEquals("Week", seg.fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription))
            val actions =
                seg
                    .fetchSemanticsNode()
                    .config
                    .getOrNull(SemanticsActions.CustomActions)
                    .orEmpty()
            assertEquals(labels, actions.map { it.label })
            actions[0].action()
            assertEquals(listOf(2, 1, 0), heard)
        }
    }

    @Test
    fun `the focus ring is drawn only while the segmented button has the focus`() {
        runComposeUiTest {
            setContent { ToonTheme(reduceMotion = true) { ToonSegmented(labels, Modifier.testTag("seg")) } }
            val seg = onNodeWithTag("seg")
            val before =
                seg.captureToImage().toPixelMap().let { m ->
                    IntArray(m.width * m.height) {
                        m[
                            it % m.width, it /
                                m.width,
                        ].toArgb()
                    }
                }
            seg.performSemanticsAction(SemanticsActions.RequestFocus)
            waitForIdle()
            val after =
                seg.captureToImage().toPixelMap().let { m ->
                    IntArray(m.width * m.height) {
                        m[
                            it % m.width, it /
                                m.width,
                        ].toArgb()
                    }
                }
            assertTrue(!before.contentEquals(after), "focusing changed nothing drawn")
        }
    }

    @Test
    fun `disabled, it switches by nothing and says so`() {
        runComposeUiTest {
            val heard = mutableListOf<Int>()
            setContent {
                ToonTheme {
                    ToonSegmented(labels, Modifier.testTag("seg"), enabled = false, onChange = {
                        heard +=
                            it
                    })
                }
            }
            val seg = onNodeWithTag("seg")
            seg.performTouchInput { click(Offset(250f, 22f)) }
            assertEquals(emptyList(), heard)
            val config = seg.fetchSemanticsNode().config
            assertTrue(config.contains(SemanticsProperties.Disabled))
            assertEquals(null, config.getOrNull(SemanticsActions.CustomActions))
        }
    }
}
