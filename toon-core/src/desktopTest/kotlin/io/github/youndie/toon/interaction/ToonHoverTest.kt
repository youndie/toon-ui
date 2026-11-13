package io.github.youndie.toon.interaction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.ToonLabel
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonTextStyle
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.inkGouacheLight
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-69: the pointer rule's hover — its numbers, its ramp, the lift, and who may set it; B-79: the
 * same for a component whose slots share one canvas, and the plate a slot draws.
 */
@OptIn(ExperimentalTestApi::class)
class ToonHoverTest {
    private val tokens =
        Json
            .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/manifest.json")).readText())
            .jsonObject
            .getValue("desktopTokens")
            .jsonObject

    private fun token(
        group: String,
        name: String,
    ) = tokens
        .getValue(group)
        .jsonObject
        .getValue(name)
        .jsonPrimitive.float

    @Test
    fun `the hover motion and the pointer targets are the extension's`() {
        assertEquals(token("motion", "hoverIn"), HOVER_IN_MS)
        assertEquals(token("motion", "hoverOut"), HOVER_OUT_MS)
        assertEquals(token("motion", "lift"), HOVER_LIFT)
        assertEquals(token("pointer", "min"), ToonPointerTarget.MIN)
        assertEquals(token("pointer", "icon"), ToonPointerTarget.ICON)
        assertEquals(token("pointer", "control"), ToonPointerTarget.CONTROL)
        assertEquals(token("pointer", "row"), ToonPointerTarget.ROW)
        assertEquals(token("pointer", "navRow"), ToonPointerTarget.NAV_ROW)
    }

    @Test
    fun `hover comes in over 80 ms and goes out over 120 on the press curve`() {
        assertEquals(0f, toonHoverAt(0f, on = true, ageMs = 0f, reduceMotion = false))
        // `hoverRamp` at a quarter of the way: 1 − 0.75³.
        assertEquals(1 - .75f * .75f * .75f, toonHoverAt(0f, on = true, ageMs = 20f, reduceMotion = false), 1e-6f)
        assertEquals(1f, toonHoverAt(0f, on = true, ageMs = 80f, reduceMotion = false))
        assertTrue(toonHoverAt(1f, on = false, ageMs = 80f, reduceMotion = false) > 0f, "still going out at 80 ms")
        assertEquals(0f, toonHoverAt(1f, on = false, ageMs = 120f, reduceMotion = false))
        // Reduce motion: instant either way.
        assertEquals(1f, toonHoverAt(0f, on = true, ageMs = 0f, reduceMotion = true))
        assertEquals(0f, toonHoverAt(1f, on = false, ageMs = 0f, reduceMotion = true))
    }

    @Test
    fun `a pointer that leaves half way goes out from where it got to, without a jump`() {
        val at = toonHoverAt(0f, on = true, ageMs = 30f, reduceMotion = false)
        assertEquals(at, toonHoverAt(at, on = false, ageMs = 0f, reduceMotion = false))
        var last = at
        for (ms in 1..120) {
            val v = toonHoverAt(at, on = false, ageMs = ms.toFloat(), reduceMotion = false)
            assertTrue(v <= last && last - v < .04f, "step at $ms ms: $last → $v")
            last = v
        }
    }

    @Test
    fun `the lift moves everything but the hard shadow`() {
        val c = inkGouacheLight.colors
        val pts = floatArrayOf(0f, 0f, 10f, 0f, 10f, 10f, 0f, 10f)
        val scene =
            ToonScene(
                listOf(
                    smoothLayer(pts, c.ink, dx = 4f, dy = 4f),
                    smoothLayer(pts, c.ink),
                    smoothLayer(pts, c.primary, dy = 1.5f),
                ),
                listOf(ToonLabel("A", 5f, 7f, c.text, 1f, ToonTextStyle(700, 16f))),
            )
        val lifted = scene.hoverLifted(2f, c.ink)
        assertEquals(listOf(4f, -2f, -2f), lifted.layers.map { it.dx })
        assertEquals(listOf(4f, -2f, -.5f), lifted.layers.map { it.dy })
        assertEquals(3f to 5f, lifted.labels.single().let { it.x to it.baseline })
    }

    @Test
    fun `a mouse over the element sets the hover, and its leaving lets it go`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            lateinit var state: ToonHoverState
            setContent {
                ToonTheme {
                    state = rememberToonHoverState()
                    Box(Modifier.size(100.dp).testTag("h").toonHoverable(state))
                }
            }
            onNodeWithTag("h").performMouseInput { moveTo(center) }
            // The enter relaunches the ramp's loop on the next frame, which starts it; one frame later it moves.
            mainClock.advanceTimeBy(48)
            val early = state.value
            assertTrue(early > 0f && early < 1f, "under way after three frames: $early")
            mainClock.advanceTimeBy(100)
            assertEquals(1f, state.value)
            onNodeWithTag("h").performMouseInput { moveTo(Offset(-50f, -50f)) }
            mainClock.advanceTimeBy(160)
            assertEquals(0f, state.value)
        }

    @Test
    fun `a finger never sets the hover`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            lateinit var state: ToonHoverState
            setContent {
                ToonTheme {
                    state = rememberToonHoverState()
                    Box(Modifier.size(100.dp).testTag("h").toonHoverable(state))
                }
            }
            onNodeWithTag("h").performTouchInput {
                down(center)
                moveBy(Offset(5f, 5f))
            }
            mainClock.advanceTimeBy(200)
            assertEquals(0f, state.value, "pressed by a finger")
            onNodeWithTag("h").performTouchInput { up() }
            mainClock.advanceTimeBy(200)
            assertEquals(0f, state.value, "let go")
        }

    @Test
    fun `a disabled element shows no hover`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            lateinit var state: ToonHoverState
            setContent {
                ToonTheme {
                    state = rememberToonHoverState()
                    Box(Modifier.size(100.dp).testTag("h").toonHoverable(state, enabled = false))
                }
            }
            onNodeWithTag("h").performMouseInput { moveTo(center) }
            mainClock.advanceTimeBy(200)
            assertEquals(0f, state.value)
        }

    @Test
    fun `a label beside a control stays on the table while the control lifts`() {
        val c = inkGouacheLight.colors
        val pts = floatArrayOf(0f, 0f, 10f, 0f, 10f, 10f, 0f, 10f)
        val scene =
            ToonScene(
                listOf(smoothLayer(pts, c.ink, dx = 2f, dy = 2f), smoothLayer(pts, c.ink)),
                listOf(ToonLabel("A", 58f, 30f, c.text, 1f, ToonTextStyle(600, 16f))),
            )
        val lifted = scene.hoverLifted(2f, c.ink, labelsToo = false)
        assertEquals(listOf(2f, -2f), lifted.layers.map { it.dx })
        assertEquals(58f to 30f, lifted.labels.single().let { it.x to it.baseline })
    }

    @Test
    fun `a slot draws its press plate whole when pressed, at the hover otherwise, and never when selected`() {
        assertEquals(1f, toonSlotPlate(selected = false, pressed = true, disabled = false, hv = 0f))
        assertEquals(1f, toonSlotPlate(selected = false, pressed = true, disabled = false, hv = .3f))
        assertEquals(.3f, toonSlotPlate(selected = false, pressed = false, disabled = false, hv = .3f))
        assertEquals(0f, toonSlotPlate(selected = true, pressed = false, disabled = false, hv = 1f))
        assertEquals(0f, toonSlotPlate(selected = true, pressed = true, disabled = false, hv = 1f))
        assertEquals(0f, toonSlotPlate(selected = false, pressed = false, disabled = true, hv = 1f))
        assertEquals(0f, toonSlotPlate(selected = false, pressed = false, disabled = false, hv = 0f))
    }

    /** Two slots across 100 dp, the left half 0 and the right half 1, read after each step of [steps]. */
    private fun slots(
        enabled: Boolean,
        steps: ComposeUiTest.(lit: () -> List<Float>) -> Unit,
    ) = runComposeUiTest {
        mainClock.autoAdvance = false
        lateinit var states: List<ToonHoverState>
        setContent {
            ToonTheme {
                states = rememberToonHoverStates(2)
                Box(
                    Modifier.size(100.dp).testTag("h").toonSlotHoverable(states, enabled, null) { x, _ ->
                        if (x < 50f) 0 else 1
                    },
                )
            }
        }
        steps { states.map { it.value } }
    }

    @Test
    fun `a mouse lights the slot under it alone, moves the light with it, and takes it away on leaving`() =
        slots(true) { lit ->
            onNodeWithTag("h").performMouseInput { moveTo(Offset(20f, 50f)) }
            mainClock.advanceTimeBy(200)
            assertEquals(listOf(1f, 0f), lit())
            onNodeWithTag("h").performMouseInput { moveTo(Offset(80f, 50f)) }
            mainClock.advanceTimeBy(200)
            assertEquals(listOf(0f, 1f), lit())
            onNodeWithTag("h").performMouseInput { moveTo(Offset(-50f, -50f)) }
            mainClock.advanceTimeBy(200)
            assertEquals(listOf(0f, 0f), lit())
        }

    @Test
    fun `a finger never lights a slot`() =
        slots(true) { lit ->
            onNodeWithTag("h").performTouchInput {
                down(Offset(20f, 50f))
                moveTo(Offset(80f, 50f))
            }
            mainClock.advanceTimeBy(200)
            assertEquals(listOf(0f, 0f), lit(), "pressed and dragged by a finger")
            onNodeWithTag("h").performTouchInput { up() }
            mainClock.advanceTimeBy(200)
            assertEquals(listOf(0f, 0f), lit(), "let go")
        }

    @Test
    fun `a disabled component lights no slot`() =
        slots(false) { lit ->
            onNodeWithTag("h").performMouseInput { moveTo(Offset(20f, 50f)) }
            mainClock.advanceTimeBy(200)
            assertEquals(listOf(0f, 0f), lit())
        }
}
