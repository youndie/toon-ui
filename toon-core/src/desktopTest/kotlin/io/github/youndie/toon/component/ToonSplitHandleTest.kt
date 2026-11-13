package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.sizeDp
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.inkGouacheLight
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-73: the split handle against the desktop extension's `splitGrip` (`engine-goldens/desktop.json`,
 * group `SplitHandle`), and what `ToonSplitHandle.dc.html` does with a pointer and the keys.
 */
@OptIn(ExperimentalTestApi::class)
class ToonSplitHandleTest {
    @Test
    fun `rest, hover, drag and focus at the page's height and the editor's ends are the extension's frames`() {
        val cells = EngineFrames.cells("SplitHandle")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                EngineFrames.compare(
                    cell,
                    toonSplitHandleScene(
                        cell.tokens,
                        o.f("h")!!,
                        ToonSplitHandleState.entries[o.f("st")!!.toInt()],
                        o.getValue("focus").jsonPrimitive.boolean,
                    ),
                )
            }
        println("split handle frames: ${cells.size} cells, worst coordinate $worst dp")
        // Nine looks in three themes.
        assertEquals(27, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    /** What the handle reported, in order: `start`, each `drag` offset, `end`. */
    private class Calls {
        val log = mutableListOf<String>()

        fun start() {
            log += "start"
        }

        fun drag(dx: Float) {
            log += "drag ${"%.1f".format(dx)}"
        }

        fun end() {
            log += "end"
        }
    }

    @Test
    fun `a drag reports the pointer's distance from the press, though the handle moves with the sidebar`() =
        runComposeUiTest {
            val calls = Calls()
            var sidebar by mutableFloatStateOf(SIDEBAR)
            var from = 0f
            setContent {
                ToonTheme(inkGouacheLight) {
                    Row(Modifier.size(800.dp, 400.dp)) {
                        Box(Modifier.width(sidebar.dp).fillMaxHeight())
                        ToonSplitHandle(
                            onDragStart = {
                                calls.start()
                                from = sidebar
                            },
                            onDrag = {
                                calls.drag(it)
                                sidebar = from + it
                            },
                            onDragEnd = calls::end,
                            modifier = Modifier.testTag("handle"),
                        )
                    }
                }
            }
            val handle = onNodeWithTag("handle")
            handle.performMouseInput {
                moveTo(Offset(8f, 200f))
                press()
            }
            waitForIdle()
            assertEquals(listOf("start"), calls.log, "a press starts the drag, before any move")
            repeat(3) {
                handle.performMouseInput { moveBy(Offset(10f, 3f)) }
                waitForIdle()
            }
            handle.performMouseInput { moveBy(Offset(-25f, 0f)) }
            waitForIdle()
            handle.performMouseInput { release() }
            waitForIdle()
            assertEquals(listOf("start", "drag 10.0", "drag 20.0", "drag 30.0", "drag 5.0", "end"), calls.log)
            assertEquals(SIDEBAR + 5f, sidebar, "the sidebar followed the pointer")
            assertEquals(
                SIDEBAR,
                handle
                    .fetchSemanticsNode()
                    .boundsInRoot.left - 5f,
                "the handle stands after it",
            )
        }

    @Test
    fun `a finger drags it too`() =
        runComposeUiTest {
            val calls = Calls()
            setContent {
                ToonTheme {
                    ToonSplitHandle(calls::drag, Modifier.testTag("handle").height(200.dp), calls::start, calls::end)
                }
            }
            onNodeWithTag("handle").performTouchInput {
                down(Offset(8f, 100f))
                moveBy(Offset(-12f, 0f))
                up()
            }
            waitForIdle()
            assertEquals(listOf("start", "drag -12.0", "end"), calls.log)
        }

    @Test
    fun `with keyboard focus, left and right move it by 8 dp, a whole drag each, and nothing else does`() =
        runComposeUiTest {
            val calls = Calls()
            setContent {
                ToonTheme {
                    ToonSplitHandle(calls::drag, Modifier.testTag("handle").height(200.dp), calls::start, calls::end)
                }
            }
            val handle = onNodeWithTag("handle")
            handle.requestFocus()
            handle.performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionUp)
                pressKey(Key.Enter)
                pressKey(Key.DirectionLeft)
                pressKey(Key.DirectionLeft)
            }
            waitForIdle()
            assertEquals(
                listOf("start", "drag 8.0", "end", "start", "drag -8.0", "end", "start", "drag -8.0", "end"),
                calls.log,
            )
        }

    @Test
    fun `it is its label to assistive technology, with the keys' two moves as actions`() =
        runComposeUiTest {
            val calls = Calls()
            setContent {
                ToonTheme {
                    ToonSplitHandle(
                        calls::drag,
                        Modifier.testTag("handle").height(200.dp),
                        calls::start,
                        calls::end,
                        label = "Sidebar width",
                    )
                }
            }
            val node = onNodeWithTag("handle").fetchSemanticsNode()
            assertEquals(listOf("Sidebar width"), node.config.getOrNull(SemanticsProperties.ContentDescription))
            val actions = node.config[SemanticsActions.CustomActions]
            assertEquals(listOf("Move left", "Move right"), actions.map { it.label })
            runOnIdle {
                actions[1].action()
                actions[0].action()
            }
            assertEquals(listOf("start", "drag 8.0", "end", "start", "drag -8.0", "end"), calls.log)
        }

    @Test
    fun `it is 16 dp wide and as tall as it is let be, at least 80 dp`() =
        runComposeUiTest {
            setContent {
                ToonTheme {
                    Row(Modifier.height(300.dp)) { ToonSplitHandle({}, Modifier.testTag("filled")) }
                    // In a column that scrolls, nothing bounds its height: it takes the editor's least.
                    Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                        ToonSplitHandle({}, Modifier.testTag("unbounded"))
                    }
                }
            }
            onNodeWithTag("filled").assertWidthIsEqualTo(16.dp).assertHeightIsEqualTo(300.dp)
            onNodeWithTag("unbounded").assertWidthIsEqualTo(16.dp).assertHeightIsEqualTo(80.dp)
        }

    /** The scene for [st] and [focus], drawn as the handle draws it, for a picture to compare with. */
    @Composable
    private fun Reference(
        st: ToonSplitHandleState,
        focus: Boolean,
    ) {
        val tokens = Toon.tokens
        Box(Modifier.testTag("frame").padding(FRAME_PAD.dp)) {
            Canvas(
                Modifier.size(16.dp, H.dp),
            ) { inDp { drawScene(toonSplitHandleScene(tokens, sizeDp.height, st, focus)) } }
        }
    }

    private fun BufferedImage.differs(o: BufferedImage): Int {
        var n = 0
        for (y in 0 until height) for (x in 0 until width) if (getRGB(x, y) != o.getRGB(x, y)) n++
        return n
    }

    @Test
    fun `a mouse lifts the grip, a press holds it, keyboard focus only rings it, a finger never lifts it`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var look by mutableStateOf<Pair<ToonSplitHandleState, Boolean>?>(null)
            setContent {
                ToonTheme(inkGouacheLight) {
                    look?.let { Reference(it.first, it.second) }
                        ?: Box(Modifier.testTag("frame").padding(FRAME_PAD.dp)) {
                            ToonSplitHandle({}, Modifier.testTag("handle").height(H.dp))
                        }
                }
            }
            mainClock.advanceTimeBy(100)

            fun shot() = onNodeWithTag("frame").captureToImage().toAwtImage()

            fun expected(
                st: ToonSplitHandleState,
                focus: Boolean = false,
            ): BufferedImage {
                look = st to focus
                mainClock.advanceTimeBy(50)
                return shot().also {
                    look = null
                    mainClock.advanceTimeBy(50)
                }
            }
            val rest = expected(ToonSplitHandleState.Rest)
            val hover = expected(ToonSplitHandleState.Hover)
            val held = expected(ToonSplitHandleState.Drag)
            val ring = expected(ToonSplitHandleState.Rest, focus = true)
            assertTrue(rest.differs(hover) > 100 && hover.differs(held) > 100 && rest.differs(ring) > 100)

            assertEquals(0, shot().differs(rest), "at rest")
            onNodeWithTag("handle").performTouchInput { down(Offset(8f, 60f)) }
            mainClock.advanceTimeBy(200)
            assertEquals(0, shot().differs(held), "a finger holds it")
            onNodeWithTag("handle").performTouchInput { up() }
            mainClock.advanceTimeBy(200)
            assertEquals(0, shot().differs(rest), "and leaves it at rest, never hovered")

            onNodeWithTag("handle").performMouseInput { moveTo(Offset(8f, 60f)) }
            mainClock.advanceTimeBy(200)
            assertEquals(0, shot().differs(hover), "a mouse over it")
            onNodeWithTag("handle").performMouseInput { press() }
            mainClock.advanceTimeBy(200)
            assertEquals(0, shot().differs(held), "held")
            onNodeWithTag("handle").performMouseInput { release() }
            mainClock.advanceTimeBy(200)
            assertEquals(0, shot().differs(hover), "released under the mouse, and the press took no focus")
            onNodeWithTag("handle").performMouseInput { moveTo(Offset(200f, 60f)) }
            mainClock.advanceTimeBy(200)
            assertEquals(0, shot().differs(rest), "the mouse gone")

            onNodeWithTag("handle").requestFocus()
            mainClock.advanceTimeBy(200)
            assertEquals(0, shot().differs(ring), "keyboard focus rings the grip at rest")
        }

    private companion object {
        const val SIDEBAR = 300f
        const val H = 120f

        /** Room round the handle for its ring, 2 dp out at the sides. */
        const val FRAME_PAD = 4f
    }
}
