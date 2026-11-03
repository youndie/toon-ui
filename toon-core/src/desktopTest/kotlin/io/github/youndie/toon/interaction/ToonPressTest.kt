package io.github.youndie.toon.interaction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.theme.ToonTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-10: the press machine against the kit's own, replayed over the timelines
 * scripts/engine-goldens.mjs ran toon-kit.js through (`engine-goldens/press.json`).
 */
@OptIn(ExperimentalTestApi::class)
class ToonPressTest {
    private val timelines =
        Json.parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/press.json")).readText()).jsonObject

    @Test
    fun `every timeline matches the kit's press machine`() {
        var worst = 0.0
        for ((name, ops) in timelines) {
            val m = ToonPressMachine()
            ops.jsonArray.forEachIndexed { i, e ->
                val o = e.jsonObject
                when (o.getValue("op").jsonPrimitive.content) {
                    "press" -> {
                        m.press()
                    }

                    "release" -> {
                        assertEquals(
                            o.getValue("released").jsonPrimitive.boolean,
                            m.release(),
                            "$name #$i released",
                        )
                    }

                    else -> {
                        m.step(
                            o
                                .getValue("ms")
                                .takeIf { it != JsonNull }!!
                                .jsonPrimitive.double,
                            reduceMotion = false,
                        )
                    }
                }
                worst = maxOf(worst, abs(o.getValue("p").jsonPrimitive.double - m.progress))
                assertEquals(o.getValue("mode").jsonPrimitive.content, m.mode.name.lowercase(), "$name #$i mode")
            }
        }
        // The goldens carry six decimals; the arithmetic is the kit's, in the same order, in Double.
        assertTrue(worst <= 1e-6, "worst squash off the kit by $worst")
    }

    @Test
    fun `a release 40 ms in springs from where the press got to, without a jump`() {
        val m = ToonPressMachine()
        m.press()
        repeat(4) { m.step(10.0, reduceMotion = false) }
        val atRelease = m.progress
        m.release()
        m.step(10.0, reduceMotion = false)
        assertTrue(atRelease < 1.0 && m.progress < atRelease + 0.01, "jumped from $atRelease to ${m.progress}")
    }

    @Test
    fun `under reduced motion the press pose is instant and the release snaps back`() {
        val m = ToonPressMachine()
        m.press()
        m.step(1.0, reduceMotion = true)
        assertEquals(1.0, m.progress)
        m.release()
        m.step(1.0, reduceMotion = true)
        assertEquals(0.0, m.progress)
        assertEquals(ToonPressMachine.Mode.Rest, m.mode)
    }

    @Test
    fun `the click fires on release, a cancelled gesture does not click, and the target is 48 dp`() =
        runComposeUiTest {
            var clicks = 0
            setContent {
                ToonTheme(reduceMotion = true) {
                    val state = rememberToonPressState()
                    Box(Modifier.testTag("t").toonPressable(state) { clicks++ }) { Box(Modifier.size(10.dp)) }
                }
            }
            val node = onNodeWithTag("t")
            node.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            node.performTouchInput { down(center) }
            waitForIdle()
            assertEquals(0, clicks)
            node.performTouchInput { up() }
            waitForIdle()
            assertEquals(1, clicks)
            node.performTouchInput {
                down(center)
                cancel()
            }
            waitForIdle()
            assertEquals(1, clicks)
        }

    @Test
    fun `a release on the frame the press loop ends still springs back to rest`() =
        runComposeUiTest {
            // B-53: the loop that steps the machine ends once the press is fully down. A release that
            // lands before the next recomposition must still start the spring, or the press sticks.
            mainClock.autoAdvance = false
            lateinit var state: ToonPressState
            setContent {
                state = rememberToonPressState()
                Box(Modifier.size(10.dp))
            }
            mainClock.advanceTimeByFrame()
            runOnUiThread { state.press() }
            var frames = 0
            while (state.progress < 1f && frames++ < 60) mainClock.advanceTimeByFrame()
            assertEquals(1f, state.progress, "fully pressed")
            runOnUiThread { state.release() }
            mainClock.advanceTimeBy(1_500)
            assertEquals(0f, state.progress, "back at rest")
            assertEquals(ToonPressMachine.Mode.Rest, state.machine.mode)
        }
}
