package io.github.youndie.toon.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.parity.SnackFrames
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.inkGouacheLight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B-23: every Core page snackbar cell and its show and hide strips against the engine's frames,
 * `snkShow` and `snkHide` at every millisecond, reduce motion, and the action's tap target.
 */
@OptIn(ExperimentalTestApi::class)
class ToonSnackbarTest {
    @Test
    fun `every snackbar cell and strip frame is the engine's`() {
        val cells = EngineFrames.cells("Snackbar")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                val scene =
                    when (o["kind"]?.jsonPrimitive?.content) {
                        "show" -> {
                            SnackFrames.strip(cell.tokens, true, o.f("t")!!)
                        }

                        "hide" -> {
                            SnackFrames.strip(cell.tokens, false, o.f("t")!!)
                        }

                        else -> {
                            val type =
                                ToonSnackbarType.valueOf(
                                    o
                                        .getValue("type")
                                        .jsonPrimitive.content
                                        .replaceFirstChar { it.uppercase() },
                                )
                            toonSnackScene(
                                cell.tokens,
                                342f,
                                o.getValue("text").jsonPrimitive.content,
                                o["action"]?.jsonPrimitive?.content ?: "",
                                type,
                                ToonSnackLook(),
                            )
                        }
                    }
                EngineFrames.compare(cell, scene)
            }
        println("snackbar frames: ${cells.size} cells, worst coordinate $worst dp")
        assertTrue(cells.size == 39, "cells: ${cells.size}")
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the show and the hide follow the engine's curves at every millisecond`() {
        val curves =
            Json
                .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText())
                .jsonObject

        fun col(
            curve: String,
            name: String,
        ) = curves
            .getValue(curve)
            .jsonObject
            .getValue(name)
            .jsonArray
            .map { it.jsonPrimitive.content.toFloat() }
        var worst = 0f
        for ((curve, f) in listOf<Pair<String, (Float) -> ToonSnackMotion>>(
            "snkShow(t)" to ::toonSnkShow,
            "snkHide(t)" to ::toonSnkHide,
        )) {
            val at = col(curve, "at")
            for ((name, get) in listOf<Pair<String, (ToonSnackMotion) -> Float>>(
                "dy" to { it.dy },
                "rot" to { it.rot },
                "sy" to { it.sy },
            )) {
                val c = col(curve, name)
                for (i in at.indices) worst = maxOf(worst, abs(c[i] - get(f(at[i]))))
            }
        }
        println("snackbar curves: worst $worst")
        // The drop reaches 122 dp, where a Float's step is 8·10⁻⁶: six decimals and Float, as elsewhere.
        assertTrue(worst <= 3e-5f, "snackbar curves off the engine by $worst")
    }

    @Test
    fun `the kit draws the hide for 260 ms with speed lines after 70, and nothing then`() {
        assertEquals(-1f, toonSnackbarFrameAt(false, 70f, false)!!.look.speedU)
        assertEquals(30f / 190, toonSnackbarFrameAt(false, 100f, false)!!.look.speedU, 1e-6f)
        assertTrue(toonSnackbarFrameAt(false, 260f, false) != null)
        assertNull(toonSnackbarFrameAt(false, 261f, false))
        assertNull(toonSnackbarFrameAt(false, null, false), "hidden from the start draws nothing")
        val still = toonSnackbarFrameAt(true, null, false)!!
        assertEquals(0f, still.dy)
        assertEquals(0f, still.rot)
    }

    @Test
    fun `under reduce motion both the show and the hide fade in 120 ms at rest`() {
        for (t in listOf(0f, 30f, 60f, 119f)) {
            val show = toonSnackbarFrameAt(true, t, true)!!
            assertEquals(t / 120, show.fade!!, 1e-6f)
            assertEquals(0f, show.dy)
            assertEquals(0f, show.rot)
            val hide = toonSnackbarFrameAt(false, t, true)!!
            assertEquals(1 - t / 120, hide.fade!!, 1e-6f)
            assertEquals(0f, hide.dy)
            assertEquals(1f, hide.look.sy)
            assertEquals(-1f, hide.look.speedU)
        }
        assertEquals(1f, toonSnackbarFrameAt(true, 500f, true)!!.fade)
        assertNull(toonSnackbarFrameAt(false, 120f, true))

        // The fade is the scene's opacity: every layer and text carries it.
        fun scene(t: Float) =
            toonSnackbarScene(
                inkGouacheLight,
                342f,
                "Plan added",
                "Undo",
                ToonSnackbarType.Info,
                toonSnackbarFrameAt(true, t, true)!!,
            )
        val half = scene(60f)
        val full = scene(500f)
        assertEquals(full.layers.map { it.alpha * .5f }, half.layers.map { it.alpha })
        assertTrue(half.labels.all { abs(it.alpha - .5f) < 1e-6f })
    }

    @Test
    fun `only a tap on the last 110 dp fires the action, and only while shown`() {
        runComposeUiTest {
            var actions = 0
            var shown by mutableStateOf(true)
            setContent {
                ToonTheme {
                    ToonSnackbar(
                        "Plan added",
                        Modifier.testTag("snack"),
                        action = "Undo",
                        shown = shown,
                        onAction = { actions++ },
                    )
                }
            }
            val snack = onNodeWithTag("snack")
            snack.performTouchInput { click(Offset(100f * density, 28f * density)) }
            assertEquals(0, actions, "a tap on the text")
            snack.performTouchInput { click(Offset(300f * density, 28f * density)) }
            assertEquals(1, actions, "a tap on the action")
            shown = false
            waitForIdle()
            snack.performTouchInput { click(Offset(300f * density, 28f * density)) }
            assertEquals(1, actions, "a tap after the hide")
        }
    }

    @Test
    fun `a sign without an action ignores taps`() {
        runComposeUiTest {
            var actions = 0
            setContent {
                ToonTheme {
                    ToonSnackbar(
                        "Saved for Saturday",
                        Modifier.testTag("snack"),
                        type = ToonSnackbarType.Success,
                        onAction = { actions++ },
                    )
                }
            }
            onNodeWithTag("snack").performTouchInput { click(Offset(300f * density, 28f * density)) }
            assertEquals(0, actions)
        }
    }
}

/** B-23: the host hangs one sign at a time, in the order asked, and reports how each left. */
@OptIn(ExperimentalTestApi::class)
class ToonSnackbarHostTest {
    @Test
    fun `a second sign waits until the first has dropped out`() {
        runComposeUiTest {
            val state = ToonSnackbarHostState()
            val results = mutableListOf<Pair<String, ToonSnackbarResult>>()
            lateinit var scope: CoroutineScope
            setContent {
                scope = rememberCoroutineScope()
                ToonTheme { ToonSnackbarHost(state, Modifier.testTag("host")) }
            }
            mainClock.autoAdvance = false
            scope.launch { results += "Plan added" to state.show("Plan added", "Undo") }
            scope.launch { results += "Saved" to state.show("Saved", type = ToonSnackbarType.Success) }
            mainClock.advanceTimeBy(1_000)
            onNodeWithText("Plan added").assertExists()
            onNodeWithText("Saved").assertDoesNotExist()
            onNodeWithTag("host").performTouchInput { click(Offset(300f * density, 28f * density)) }
            // The hide is drawn for 260 ms before the next sign flies in.
            mainClock.advanceTimeBy(200)
            onNodeWithText("Saved").assertDoesNotExist()
            mainClock.advanceTimeBy(200)
            onNodeWithText("Saved").assertExists()
            assertEquals(listOf("Plan added" to ToonSnackbarResult.ActionPerformed), results)
            // A short sign drops on its own after 4 s.
            mainClock.advanceTimeBy(3_500)
            onNodeWithText("Saved").assertExists()
            mainClock.advanceTimeBy(1_000)
            onNodeWithText("Saved").assertDoesNotExist()
            assertEquals("Saved" to ToonSnackbarResult.Dismissed, results.last())
        }
    }

    @Test
    fun `dismiss drops an indefinite sign`() {
        runComposeUiTest {
            val state = ToonSnackbarHostState()
            var result: ToonSnackbarResult? = null
            lateinit var scope: CoroutineScope
            setContent {
                scope = rememberCoroutineScope()
                ToonTheme { ToonSnackbarHost(state) }
            }
            mainClock.autoAdvance = false
            scope.launch {
                result =
                    state.show("Couldn't save", "Retry", ToonSnackbarType.Error, ToonSnackbarDuration.Indefinite)
            }
            mainClock.advanceTimeBy(60_000)
            onNodeWithText("Couldn't save").assertExists()
            state.dismiss()
            mainClock.advanceTimeBy(400)
            onNodeWithText("Couldn't save").assertDoesNotExist()
            assertEquals(ToonSnackbarResult.Dismissed, result)
        }
    }
}
