package io.github.youndie.toon.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.inkGouacheLight
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-72: the scrollbar against the desktop extension's `scrollThumb` (`engine-goldens/desktop.json`,
 * group `Scrollbar`), and what `ToonScrollbar.dc.html` does with a pointer, bound to Compose's scroll
 * states.
 */
@OptIn(ExperimentalTestApi::class)
class ToonScrollbarTest {
    @Test
    fun `rest, hover and drag, a least thumb and a held one are the extension's frames layer by layer`() {
        val cells = EngineFrames.cells("Scrollbar")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                EngineFrames.compare(
                    cell,
                    toonScrollbarScene(
                        cell.tokens,
                        o.f("h")!!,
                        o.f("a")!!,
                        o.f("b")!!,
                        ToonScrollbarState.entries[o.f("st")!!.toInt()],
                    ),
                )
            }
        println("scrollbar frames: ${cells.size} cells, worst coordinate $worst dp")
        // The pointer page's three states, the page's default, the least thumb and the held one.
        assertEquals(18, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the thumb is at least 28 dp and held inside its track`() {
        assertEquals(44f to 99f, toonScrollThumbSpan(220f, .2f, .45f))
        assertEquals(110f to 138f, toonScrollThumbSpan(220f, .5f, .505f), "the least thumb")
        // At the end of a long list the thumb would hang past the track; it stands at its end.
        assertEquals(192f to 220f, toonScrollThumbSpan(220f, .97f, .995f))
        assertEquals(40f, toonScrollTrack(12f), "a track is at least 40 dp")
    }

    /** A 600 dp tall area of [n] rows 56 dp tall, with the scrollbar over its end edge. */
    @Composable
    private fun Rows(
        state: LazyListState,
        n: Int = ROWS,
    ) = Box(Modifier.size(400.dp, 600.dp)) {
        LazyColumn(Modifier.fillMaxSize(), state) { items(n) { Box(Modifier.fillMaxWidth().height(ROW.dp)) } }
        ToonScrollbar(state, Modifier.align(Alignment.TopEnd).testTag("bar"))
    }

    private fun LazyListState.offsetDp() = firstVisibleItemIndex * ROW + firstVisibleItemScrollOffset.toFloat()

    @Test
    fun `dragging the thumb scrolls a 1,500 row list in step with the pointer, without a jump`() =
        runComposeUiTest {
            val list = LazyListState()
            setContent { ToonTheme { Rows(list) } }
            // The track is 600 - 16 dp; the thumb's 28 dp stand for a 600 dp view of 84,000.
            val h = 584f
            val perDp = 84_000f / h
            val bar = onNodeWithTag("bar")
            bar.performMouseInput {
                moveTo(Offset(10f, 8f + 14f))
                press()
            }
            waitForIdle()
            assertEquals(0f, list.offsetDp(), "grabbing the thumb scrolls nothing")
            val steps = mutableListOf<Float>()
            var was = list.offsetDp()
            repeat(120) {
                bar.performMouseInput { moveBy(Offset(0f, 2f)) }
                waitForIdle()
                steps += list.offsetDp() - was
                was = list.offsetDp()
            }
            bar.performMouseInput { release() }
            println("drag steps: ${steps.min()}..${steps.max()} dp for 2 dp of pointer, ${2 * perDp} expected")
            assertTrue(steps.all { abs(it - 2 * perDp) <= 1.5f }, "every step ${2 * perDp} dp: ${steps.distinct()}")
            assertEquals(240 * perDp, list.offsetDp(), 120 * 1.5f)
        }

    @Test
    fun `dragging past the end of the track leaves the list at its end, and back up runs smooth`() =
        runComposeUiTest {
            val list = LazyListState()
            setContent { ToonTheme { Rows(list) } }
            val bar = onNodeWithTag("bar")
            bar.performMouseInput {
                moveTo(Offset(10f, 22f))
                press()
            }
            bar.performMouseInput { moveBy(Offset(0f, 700f)) }
            waitForIdle()
            assertEquals(ROWS * ROW - 600f, list.offsetDp(), 1f)
            var was = list.offsetDp()
            val back = mutableListOf<Float>()
            repeat(40) {
                bar.performMouseInput { moveBy(Offset(0f, -2f)) }
                waitForIdle()
                back += was - list.offsetDp()
                was = list.offsetDp()
            }
            bar.performMouseInput { release() }
            // The pointer went past the end: the list waits at its end until the pointer is back over
            // the track, then follows it.
            assertTrue(
                back.all { it >= 0f && it <= 2 * 84_000f / 584 + 1.5f },
                "no step back jumps: ${back.distinct()}",
            )
        }

    @Test
    fun `over items of very different heights the thumb stays under the pointer, down and back`() =
        runComposeUiTest {
            // B-93: 400 items alternating 20 and 300 dp, so the items in view are rarely their average.
            val list = LazyListState()
            setContent {
                ToonTheme {
                    Box(Modifier.size(400.dp, 600.dp)) {
                        LazyColumn(Modifier.fillMaxSize(), list) {
                            items(400) { Box(Modifier.fillMaxWidth().height(if (it % 2 == 0) 20.dp else 300.dp)) }
                        }
                        ToonScrollbar(list, Modifier.align(Alignment.TopEnd).testTag("bar"))
                    }
                }
            }
            val h = 584f
            val span = toonScrollSpan(list)

            // The thumb's top on its track, as the bar draws it.
            fun thumbTop() =
                runOnIdle {
                    val (a, b) = span.fractions()
                    toonScrollThumbSpan(h, a, b).first
                }
            val bar = onNodeWithTag("bar")
            bar.performMouseInput {
                moveTo(Offset(10f, 8f + 14f))
                press()
            }
            waitForIdle()
            val off = mutableListOf<Float>()
            var travel = 0f
            repeat(100) {
                bar.performMouseInput { moveBy(Offset(0f, 5f)) }
                travel += 5f
                off += thumbTop() - travel
            }
            assertTrue(list.firstVisibleItemIndex > 300, "the drag went most of the way: ${list.firstVisibleItemIndex}")
            repeat(100) {
                bar.performMouseInput { moveBy(Offset(0f, -5f)) }
                travel -= 5f
                off += thumbTop() - travel
            }
            bar.performMouseInput { release() }
            waitForIdle()
            println("thumb off the pointer: ${off.min()}..${off.max()} dp")
            assertTrue(
                off.all { abs(it) <= 1f },
                "the thumb's top within a dp of the pointer's travel: ${off.min()}..${off.max()}",
            )
            assertEquals(0, list.firstVisibleItemIndex, "back at the start")
            assertEquals(0, list.firstVisibleItemScrollOffset, "back at the start")
        }

    @Test
    fun `dragging the thumb of a scroll state moves its value by the pointer's share of the track`() =
        runComposeUiTest {
            val scroll = ScrollState(0)
            setContent {
                ToonTheme {
                    Box(Modifier.size(400.dp, 600.dp)) {
                        Column(Modifier.fillMaxSize().verticalScroll(scroll)) { Box(Modifier.size(10.dp, 3_000.dp)) }
                        ToonScrollbar(scroll, Modifier.align(Alignment.TopEnd).testTag("bar"))
                    }
                }
            }
            val bar = onNodeWithTag("bar")
            bar.performMouseInput {
                moveTo(Offset(10f, 8f + 14f))
                press()
            }
            repeat(50) {
                bar.performMouseInput { moveBy(Offset(0f, 2f)) }
                waitForIdle()
                // 3,000 dp of content over 584 dp of track, a whole pixel at a time.
                assertEquals((it + 1) * 2 * 3_000f / 584, scroll.value.toFloat(), .5f)
            }
            bar.performMouseInput { release() }
        }

    @Test
    fun `a press beside the thumb pages by nine tenths of the view`() =
        runComposeUiTest {
            val scroll = ScrollState(0)
            setContent {
                ToonTheme {
                    Box(Modifier.size(400.dp, 600.dp)) {
                        Column(Modifier.fillMaxSize().verticalScroll(scroll)) { Box(Modifier.size(10.dp, 3_000.dp)) }
                        ToonScrollbar(scroll, Modifier.align(Alignment.TopEnd).testTag("bar"))
                    }
                }
            }
            val bar = onNodeWithTag("bar")
            // The thumb covers 600 / 3,000 of the 584 dp track from its top; a press at 400 dp is below.
            bar.performMouseInput { click(Offset(10f, 8f + 400f)) }
            waitForIdle()
            assertEquals(540, scroll.value)
            bar.performMouseInput { click(Offset(10f, 8f + 400f)) }
            waitForIdle()
            assertEquals(1_080, scroll.value)
            // Above the thumb, which now starts at 1,080 / 3,000 of the track.
            bar.performMouseInput { click(Offset(10f, 8f + 50f)) }
            waitForIdle()
            assertEquals(540, scroll.value)
        }

    @Test
    fun `a scroll state's span is its value over its content`() =
        runComposeUiTest {
            val scroll = ScrollState(0)
            val list = LazyListState()
            setContent {
                ToonTheme {
                    Box(Modifier.size(400.dp, 600.dp)) {
                        Column(Modifier.fillMaxSize().verticalScroll(scroll)) { Box(Modifier.size(10.dp, 3_000.dp)) }
                    }
                    Box(Modifier.size(400.dp, 600.dp)) {
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            list,
                        ) { items(ROWS) { Box(Modifier.fillMaxWidth().height(ROW.dp)) } }
                    }
                }
            }
            runOnIdle { scroll.dispatchRawDelta(1_200f) }
            runOnIdle { list.dispatchRawDelta(42_000f) }
            waitForIdle()
            val s = toonScrollSpan(scroll).fractions()
            assertEquals(1_200f / 3_000f, s.first, 1e-6f)
            assertEquals(1_800f / 3_000f, s.second, 1e-6f)
            val l = toonScrollSpan(list).fractions()
            assertEquals(42_000f / 84_000f, l.first, 1e-6f)
            assertEquals(42_600f / 84_000f, l.second, 1e-6f)
        }

    @Test
    fun `while the content fits it draws nothing and takes no press`() =
        runComposeUiTest {
            val scroll = ScrollState(0)
            var clicks = 0
            setContent {
                ToonTheme {
                    Box(Modifier.size(400.dp, 600.dp)) {
                        Column(
                            Modifier.fillMaxSize().verticalScroll(scroll).clickable { clicks++ },
                        ) { Box(Modifier.size(10.dp, 300.dp)) }
                        ToonScrollbar(scroll, Modifier.align(Alignment.TopEnd).testTag("bar"))
                    }
                }
            }
            val shot = onNodeWithTag("bar").captureToImage().toAwtImage()
            assertEquals(0, shot.inked().size, "nothing drawn")
            onNodeWithTag("bar").performMouseInput { click(Offset(10f, 30f)) }
            waitForIdle()
            assertEquals(1, clicks, "the press reached the content under it")
        }

    /** The columns holding a pixel that is not fully transparent. */
    private fun BufferedImage.inked(): Set<Int> =
        buildSet { for (y in 0 until height) for (x in 0 until width) if ((getRGB(x, y) ushr 24) > 0) add(x) }

    @Test
    fun `the thumb keeps 8 dp from the window's edge, and is grabbed beside an 8 dp resize strip`() =
        runComposeUiTest {
            val list = LazyListState()
            setContent {
                ToonTheme(inkGouacheLight) {
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            list,
                        ) { items(ROWS) { Box(Modifier.fillMaxWidth().height(ROW.dp)) } }
                        ToonScrollbar(list, Modifier.align(Alignment.TopEnd).testTag("bar"))
                        // An undecorated window's resize strip: 8 dp over the content that takes every press.
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .width(8.dp)
                                .fillMaxHeight()
                                .testTag("strip")
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            awaitPointerEvent().changes.forEach {
                                                it
                                                    .consume()
                                            }
                                        }
                                    }
                                },
                        )
                    }
                }
            }
            val bar = onNodeWithTag("bar")
            // The thumb's body ends 8 dp before the bar's end, which is the window's: its ink reaches
            // into the column at 12 dp only by its outline's wobble, 0.45 dp at most.
            val rest = bar.captureToImage().toAwtImage()
            assertTrue(rest.inked().isNotEmpty())
            assertTrue(rest.inked().max() <= 20 - 8, "the thumb's ink reaches column ${rest.inked().max()} of 20")
            val window = onRoot().captureToImage().toAwtImage().width
            // Grabbed in the middle of its width, 11 dp from the edge, it drags; the strip takes nothing from it.
            bar.performMouseInput {
                moveTo(Offset(9f, 8f + 14f))
                press()
                moveBy(Offset(0f, 50f))
            }
            waitForIdle()
            val held = bar.captureToImage().toAwtImage()
            bar.performMouseInput { release() }
            assertTrue(list.offsetDp() > 0f, "the drag scrolled the list")
            assertTrue(held.inked().max() <= 20 - 8, "held, the thumb's ink reaches column ${held.inked().max()} of 20")
            assertEquals(
                window - 20,
                bar
                    .fetchSemanticsNode()
                    .boundsInRoot.left
                    .toInt(),
                "the bar stands at the window's end",
            )
        }

    private companion object {
        const val ROWS = 1_500
        const val ROW = 56f
    }
}
