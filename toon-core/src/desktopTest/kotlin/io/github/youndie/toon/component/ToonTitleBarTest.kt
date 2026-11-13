package io.github.youndie.toon.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-76: the title bar's dashed slots against the desktop extension's `trafficSlots`
 * (`engine-goldens/desktop.json`, group `TitleBar`), and its HTML words and record slot where
 * `ToonTitleBar.dc.html` puts them.
 */
@OptIn(ExperimentalTestApi::class)
class ToonTitleBarTest {
    @Test
    fun `the traffic slots are the extension's frames layer by layer in three themes`() {
        val cells = EngineFrames.cells("TitleBar")
        val worst = cells.maxOf { EngineFrames.compare(it, toonTitleBarSlotsScene(it.tokens)) }
        println("title bar frames: ${cells.size} cells, worst coordinate $worst dp")
        assertEquals(3, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `at 1280 and 960 the title starts at 96 and is centred, and the record button sits 20 from the end`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val themes = listOf(inkGouacheLight, inkGouacheNight, brightCel)
            val widths = listOf(1280, 960)
            setContent {
                Column {
                    themes.forEachIndexed { t, tokens ->
                        ToonTheme(tokens) {
                            widths.forEach { w ->
                                ToonTitleBar(
                                    "Weekly planning, Thursday",
                                    Modifier.requiredWidth(w.dp).testTag("bar$t$w"),
                                    trafficSlots = true,
                                ) {
                                    ToonRecordButton(
                                        ToonRecordState.Recording,
                                        {},
                                        {},
                                        Modifier.testTag("rec$t$w"),
                                        elapsed = "00:12:07",
                                    )
                                }
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val titles = onAllNodes(hasHeading()).fetchSemanticsNodes().map { it.boundsInRoot }
            assertEquals(6, titles.size)
            themes.indices.forEach { t ->
                widths.forEachIndexed { k, w ->
                    val bar = onNodeWithTag("bar$t$w").fetchSemanticsNode().boundsInRoot
                    onNodeWithTag("bar$t$w").assertWidthIsEqualTo(w.dp).assertHeightIsEqualTo(52.dp)
                    val title = titles[t * widths.size + k]
                    assertEquals(96f, title.left - bar.left, .5f, "the title's start at $w")
                    assertEquals(
                        (52 - title.height) / 2,
                        title.top - bar.top,
                        .5f,
                        "the title centred on the bar at $w",
                    )
                    val rec = onNodeWithTag("rec$t$w").fetchSemanticsNode().boundsInRoot
                    assertEquals(20f, bar.right - rec.right, .5f, "the button's end at $w")
                    assertEquals(8f, rec.top - bar.top, .5f, "the button's top, clear of the resize strip, at $w")
                    assertEquals(40f, rec.height, .5f)
                }
            }
        }

    @Test
    fun `a long title ends 220 dp before the end, and before wider actions`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val long = "A meeting with a title long enough to run the whole width of a narrow window and on"
            setContent {
                ToonTheme {
                    Column {
                        ToonTitleBar(long, Modifier.width(700.dp).testTag("design"))
                        ToonTitleBar(long, Modifier.width(700.dp).testTag("wide")) {
                            Box(Modifier.size(300.dp, 40.dp))
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val (design, wide) = onAllNodes(hasHeading()).fetchSemanticsNodes().map { it.boundsInRoot }
            assertEquals(700f - 220f - 96f, design.width, .5f, "the design's title box")
            assertEquals(700f - 20f - 300f - 10f - 96f, wide.width, .5f, "10 dp before a 300 dp row")
        }

    @Test
    fun `a row taller than 44 dp grows the bar, and nothing bounds it to 1280`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                ToonTheme {
                    Column {
                        ToonTitleBar(
                            "",
                            Modifier.width(900.dp).testTag("tall"),
                        ) { Box(Modifier.size(48.dp).testTag("btn")) }
                        ToonTitleBar(
                            "",
                            Modifier.width(900.dp).testTag("small"),
                        ) { Box(Modifier.size(32.dp).testTag("small btn")) }
                        Box(Modifier.wrapContentWidth(unbounded = true)) {
                            ToonTitleBar("Free", Modifier.testTag("free"))
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            onNodeWithTag("tall").assertHeightIsEqualTo(56.dp)
            val tall = onNodeWithTag("tall").fetchSemanticsNode().boundsInRoot
            assertEquals(8f, onNodeWithTag("btn").fetchSemanticsNode().boundsInRoot.top - tall.top, .5f)
            val small = onNodeWithTag("small").fetchSemanticsNode().boundsInRoot
            onNodeWithTag("small").assertHeightIsEqualTo(52.dp)
            assertEquals(
                10f,
                onNodeWithTag("small btn").fetchSemanticsNode().boundsInRoot.top - small.top,
                .5f,
                "centred",
            )
            onNodeWithTag("free").assertWidthIsEqualTo(1280.dp)
        }

    @Test
    fun `the slots are drawn only when asked, and the bar draws no background`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val stage = brightCel.colors.stage
            setContent {
                ToonTheme(brightCel) {
                    Column(Modifier.background(stage)) {
                        ToonTitleBar("", Modifier.width(600.dp).testTag("off"))
                        ToonTitleBar("", Modifier.width(600.dp).testTag("on"), trafficSlots = true)
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val off = onNodeWithTag("off").captureToImage().toAwtImage()
            val on = onNodeWithTag("on").captureToImage().toAwtImage()
            val bg = stage.toArgb()
            val drawn = { img: java.awt.image.BufferedImage ->
                (0 until 80).sumOf { x ->
                    (0 until 52).count { y ->
                        img.getRGB(x, y) !=
                            bg
                    }
                }
            }
            assertEquals(
                0,
                (0 until 600).sumOf { x ->
                    (0 until 52).count { y -> off.getRGB(x, y) != bg }
                },
                "nothing drawn",
            )
            assertTrue(drawn(on) > 60, "three dashed rings: ${drawn(on)} px")
            // Off the rings, still the stage: no background.
            assertEquals(bg, on.getRGB(40, 4))
            assertEquals(bg, on.getRGB(300, 26))
        }

    @Test
    fun `the record button in the slot is the record button`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var started = 0
            setContent {
                ToonTheme {
                    ToonTitleBar("Planning", Modifier.width(960.dp)) {
                        ToonRecordButton(ToonRecordState.Idle, { started++ }, {})
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            onNodeWithContentDescription("Record").performClick()
            mainClock.advanceTimeByFrame()
            assertEquals(1, started)
        }

    @Test
    fun `the box at density 2 is the design's numbers in pixels`() {
        val b = toonTitleBarBox(w = 2560f, start = 192f, rowW = 372f, rowH = 80f, density = 2f)
        assertEquals(2560f - 40f - 372f, b.actionsX)
        assertEquals(16f, b.actionsY)
        assertEquals(2560f - 440f - 192f, b.titleWidth)
        assertEquals(104f, b.height)
        assertTrue(
            abs(toonTitleBarBox(100f, 96f, 0f, 0f, 1f).titleWidth) == 0f,
            "no room is no title, not a negative one",
        )
    }

    private fun hasHeading() = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
}
