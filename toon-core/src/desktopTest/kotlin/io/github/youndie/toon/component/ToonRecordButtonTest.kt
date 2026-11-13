package io.github.youndie.toon.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.draw.ToonTextRenderer
import io.github.youndie.toon.draw.ToonTextStyle
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.inkGouacheLight
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.awt.image.BufferedImage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-75: the record button against the desktop extension's `recBtn` (`engine-goldens/desktop.json`,
 * group `RecordButton`), its pulse against `recPulse`, and what `ToonRecordButton.dc.html` does with a
 * click and the keys.
 */
@OptIn(ExperimentalTestApi::class)
class ToonRecordButtonTest {
    @Test
    fun `the three states, the pulse, the spinner, hover, focus and the press are the extension's frames`() {
        val cells = EngineFrames.cells("RecordButton")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                EngineFrames.compare(
                    cell,
                    toonRecordButtonScene(
                        cell.tokens,
                        o.f("w")!!,
                        ToonRecordLook(
                            state =
                                ToonRecordState.valueOf(
                                    o
                                        .getValue("state")
                                        .jsonPrimitive.content
                                        .replaceFirstChar { it.uppercase() },
                                ),
                            hv = o.f("hv")!!,
                            p = o.f("p")!!,
                            focus = o["focus"]?.jsonPrimitive?.booleanOrNull ?: false,
                            pulse = o.f("pulse")!!,
                            spinMs = o.f("spin")!!,
                        ),
                    ),
                )
            }
        println("record button frames: ${cells.size} cells, worst coordinate $worst dp")
        assertEquals(39, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the pulse is recPulse, the kit's idle breath over 1,2 s`() {
        for (t in 0..2_400 step 50) {
            assertEquals(.5f - .5f * cos(2 * PI.toFloat() * t / 1200f), toonRecPulse(t.toFloat()), 1e-6f, "at $t ms")
        }
        assertEquals(0f, toonRecPulse(0f), 1e-6f)
        assertEquals(1f, toonRecPulse(600f), 1e-6f)
    }

    /** The mark's ink outline, the scene's layer after the body's. */
    private fun markRadius(look: ToonRecordLook): Float {
        val layers = toonRecordButtonScene(inkGouacheLight, 186f, look).layers
        val ink = layers[layers.size - 2].numbers
        val xs = ink.filterIndexed { i, _ -> i % 2 == 0 }
        return (xs.max() - xs.min()) / 2
    }

    @Test
    fun `recording, the mark follows the pulse frame by frame, and holds its full size with reduced motion`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var reduce by mutableStateOf(false)
            var drawn = 0f
            setContent {
                ToonTheme(inkGouacheLight, reduceMotion = reduce) {
                    ToonRecordButton(ToonRecordState.Recording, {}, {}, Modifier.testTag("rec"), elapsed = "00:00:01")
                }
            }

            // The mark is drawn from a pulse read off the frame clock: a bigger mark, a wider red dot.
            fun markWidth(): Int {
                val shot = onNodeWithTag("rec").captureToImage().toAwtImage()
                // The paper dot on the red body, its middle row.
                val y = shot.height / 2
                return (0 until 40).count { x ->
                    (shot.getRGB(x, y) and 0xFFFFFF) ==
                        (
                            inkGouacheLight.colors.surface.value
                                .toLong() ushr 32
                        ).toInt() and 0xFFFFFF
                }
            }
            mainClock.advanceTimeByFrame()
            val widths =
                (0 until 8).map {
                    mainClock.advanceTimeBy(160)
                    markWidth()
                }
            assertTrue(widths.distinct().size > 2, "the mark breathes: $widths")
            reduce = true
            mainClock.advanceTimeBy(200)
            val held = markWidth()
            mainClock.advanceTimeBy(600)
            assertEquals(held, markWidth(), "reduced motion holds the mark")
            assertTrue(held >= widths.max() - 1, "at its full size: $held against $widths")
            drawn = held.toFloat()
            assertTrue(drawn > 0f)
        }

    @Test
    fun `the mark's size is the pulse's`() {
        val small = markRadius(ToonRecordLook(ToonRecordState.Recording, pulse = 0f))
        val full = markRadius(ToonRecordLook(ToonRecordState.Recording, pulse = 1f))
        val idle = markRadius(ToonRecordLook(ToonRecordState.Idle))
        assertTrue(full - small > 1.4f, "5.2 dp to 6.8: $small, $full")
        assertTrue(idle > small && idle < full, "idle 6 dp: $idle")
    }

    @Test
    fun `a click, Enter or Space start when idle and stop when recording, and saving does nothing`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var state by mutableStateOf(ToonRecordState.Idle)
            val calls = mutableListOf<String>()
            setContent {
                ToonTheme {
                    ToonRecordButton(state, { calls += "start" }, { calls += "stop" }, Modifier.testTag("rec"))
                }
            }
            mainClock.advanceTimeBy(100)
            val button = onNodeWithTag("rec")
            button.performMouseInput { click(center) }
            mainClock.advanceTimeBy(500)
            button.requestFocus()
            button.performKeyInput { pressKey(Key.Enter) }
            button.performKeyInput { pressKey(Key.Spacebar) }
            state = ToonRecordState.Recording
            mainClock.advanceTimeBy(100)
            button.performKeyInput { pressKey(Key.Enter) }
            button.performMouseInput { click(center) }
            mainClock.advanceTimeBy(500)
            state = ToonRecordState.Stopping
            mainClock.advanceTimeBy(100)
            button.performKeyInput { pressKey(Key.Enter) }
            button.performMouseInput { click(center) }
            mainClock.advanceTimeBy(500)
            assertEquals(listOf("start", "start", "start", "stop", "stop"), calls)
        }

    @Test
    fun `it is 40 dp tall, as wide as its label, and read as its label with the time`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var state by mutableStateOf(ToonRecordState.Idle)
            setContent {
                ToonTheme { ToonRecordButton(state, {}, {}, Modifier.testTag("rec"), elapsed = "00:12:07") }
            }
            mainClock.advanceTimeBy(100)
            val node = onNodeWithTag("rec")
            node.assertHeightIsEqualTo(40.dp)
            val idle = node.fetchSemanticsNode().size.width
            assertEquals(
                listOf("Record"),
                node.fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription),
            )
            state = ToonRecordState.Recording
            mainClock.advanceTimeBy(100)
            val recording = node.fetchSemanticsNode()
            assertEquals(listOf("Stop"), recording.config.getOrNull(SemanticsProperties.ContentDescription))
            assertEquals("00:12:07", recording.config.getOrNull(SemanticsProperties.StateDescription))
            // The time's room: 10 dp and eight figures more than the label alone.
            assertTrue(recording.size.width > idle + 50, "recording ${recording.size.width} px, idle $idle px")
            state = ToonRecordState.Stopping
            mainClock.advanceTimeBy(100)
            assertTrue(node.fetchSemanticsNode().config.contains(SemanticsProperties.Disabled), "saving is inert")
        }

    @Test
    fun `the words ride the body, lifted by the hover and dropped by the press`() {
        val o = ToonRecordLook(ToonRecordState.Idle, hv = 1f)
        assertEquals(2f, o.lift)
        assertEquals(0f, o.drop)
        val pressed = ToonRecordLook(ToonRecordState.Idle, hv = 1f, p = 1f)
        assertEquals(0f, pressed.lift, "the press takes the lift back")
        assertEquals(40 * .15f + 1.2f, pressed.drop, 1e-5f)
        assertEquals(0f, ToonRecordLook(ToonRecordState.Stopping, hv = 1f).lift, "saving does not lift")
    }

    @Test
    fun `the label stands 36 dp in and the time 10 dp after it, moved with the body`() =
        runComposeUiTest {
            var text: ToonTextRenderer? = null
            setContent { ToonTheme(inkGouacheLight) { text = rememberToonText() } }
            waitForIdle()
            val t = text!!

            fun labels(look: ToonRecordLook) =
                toonRecordButtonLabels(inkGouacheLight, look, "Stop", "00:12:07", t).labels
            val rest = labels(ToonRecordLook(ToonRecordState.Recording))
            assertEquals(2, rest.size)
            assertEquals(36f, rest[0].x)
            assertEquals(36f + t.measure("Stop", ToonTextStyle(700, 15f)).size.width + 10f, rest[1].x)
            assertEquals(inkGouacheLight.colors.onDanger, rest[0].color)
            val lifted = labels(ToonRecordLook(ToonRecordState.Recording, hv = 1f))
            assertEquals(rest[0].x - 2f, lifted[0].x, 1e-5f)
            assertEquals(rest[0].baseline - 2f, lifted[0].baseline, 1e-5f)
            val pressed = labels(ToonRecordLook(ToonRecordState.Recording, hv = 1f, p = 1f))
            assertEquals(rest[0].x, pressed[0].x, 1e-5f)
            assertEquals(rest[0].baseline + 7.2f, pressed[0].baseline, 1e-5f)
            assertEquals(1, labels(ToonRecordLook(ToonRecordState.Idle)).size, "no time while idle")
        }

    private fun BufferedImage.differs(o: BufferedImage): Int {
        var n = 0
        for (y in 0 until height) for (x in 0 until width) if (getRGB(x, y) != o.getRGB(x, y)) n++
        return n
    }

    @Test
    fun `keyboard focus rings it`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                ToonTheme(inkGouacheLight) {
                    // The ring stands 6 dp outside the button: captured round it.
                    Box(Modifier.testTag("frame").padding(10.dp)) {
                        ToonRecordButton(ToonRecordState.Idle, {}, {}, Modifier.testTag("rec"))
                    }
                }
            }
            mainClock.advanceTimeBy(100)

            fun shot() = onNodeWithTag("frame").captureToImage().toAwtImage()
            val rest = shot()
            onNodeWithTag("rec").requestFocus()
            mainClock.advanceTimeBy(100)
            assertTrue(shot().differs(rest) > 100, "the ring drew")
        }
}
