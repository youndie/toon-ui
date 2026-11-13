package io.github.youndie.toon.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.brightCel
import kotlinx.serialization.json.jsonPrimitive
import java.awt.image.BufferedImage
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-74: the level meter against the desktop extension's `levelMeter` (`engine-goldens/desktop.json`,
 * group `LevelMeter`), its motion against `livelevel`, and its header's words.
 */
@OptIn(ExperimentalTestApi::class)
class ToonLevelMeterTest {
    @Test
    fun `live, silent and clipping strips at three widths are the extension's frames layer by layer`() {
        val cells = EngineFrames.cells("LevelMeter")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                val w = o.f("w")!!
                val state =
                    ToonLevelState.valueOf(
                        o
                            .getValue("state")
                            .jsonPrimitive.content
                            .replaceFirstChar { it.uppercase() },
                    )
                EngineFrames.compare(
                    cell,
                    toonLevelMeterScene(cell.tokens, w, state, toonLevelLit(w, state, o.f("level")!!)),
                )
            }
        println("level meter frames: ${cells.size} cells, worst coordinate $worst dp")
        assertEquals(24, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `livelevel is the extension's three sines round 0,42, held between 0 and 1`() {
        for (t in 0..20_000 step 37) {
            for (seed in 0..3) {
                val s = t / 1000.0 + seed * 7.3
                val expected =
                    (
                        .42 + .22 *
                            sin(
                                s * 5.1,
                            ) + .14 * sin(s * 13.7 + 1) + .08 * sin(s * 29 + 2)
                    ).coerceIn(0.0, 1.0)
                assertEquals(expected, toonLiveLevel(t.toDouble(), seed), 1e-12, "at $t ms, seed $seed")
            }
        }
        assertEquals(8, toonLevelBlocks(60f), "at least eight blocks")
        assertEquals(24, toonLevelBlocks(320f))
        assertEquals(0, toonLevelLit(320f, ToonLevelState.Silent, 1f), "silent lights nothing")
    }

    /** Lit blocks in a strip [w] dp wide, read off its fills at mid-height. */
    private fun BufferedImage.lit(w: Float): Int {
        val n = toonLevelBlocks(w)
        val bw = (w - (n - 1) * 3) / n
        val c = brightCel.colors
        val litFills = setOf(c.success, c.warning, c.danger).map { it.toArgb() }
        return (0 until n).count { i -> getRGB(((i * (bw + 3)) + bw / 2).toInt(), 10) in litFills }
    }

    @Test
    fun `with animate the strip follows livelevel frame for frame, and reduced motion holds the level`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var reduce by mutableStateOf(false)
            setContent {
                ToonTheme(brightCel, reduceMotion = reduce) {
                    ToonLevelMeter("Mic", Modifier.testTag("meter"), level = .7f, width = 320.dp)
                }
            }
            mainClock.advanceTimeByFrame()

            fun strip() =
                onNodeWithTag("meter").captureToImage().toAwtImage().let {
                    it.getSubimage(0, it.height - 20, it.width, 20)
                }
            val seen =
                (0 until 60).map {
                    mainClock.advanceTimeByFrame()
                    strip().lit(320f)
                }
            // Frame by frame, from the frame the strip began to move: the drawn age is a whole number of frames.
            val frame = 16
            val matches =
                (0..3).filter { lag ->
                    seen.indices.all { k ->
                        seen[k] ==
                            toonLevelLit(
                                320f,
                                ToonLevelState.Live,
                                toonLiveLevel(((k + lag) * frame).toDouble(), 0).toFloat(),
                            )
                    }
                }
            println("live strip: ${seen.joinToString(" ")}; aligned at a lag of $matches frames")
            assertTrue(seen.distinct().size > 3, "the strip moves: $seen")
            assertTrue(matches.isNotEmpty(), "the strip follows livelevel: $seen")
            reduce = true
            mainClock.advanceTimeByFrame()
            val held =
                (0 until 10).map {
                    mainClock.advanceTimeBy(100)
                    strip().lit(320f)
                }
            assertEquals(
                List(10) { toonLevelLit(320f, ToonLevelState.Live, .7f) },
                held,
                "reduced motion holds the given level",
            )
        }

    @Test
    fun `a clipping strip leaves its last 24 dp to the burst`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                ToonTheme(brightCel) {
                    Box(Modifier.background(brightCel.colors.stage)) {
                        ToonLevelMeter("Mic", Modifier.testTag("meter"), state = ToonLevelState.Clip, width = 320.dp)
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val shot = onNodeWithTag("meter").captureToImage().toAwtImage()
            val strip = shot.getSubimage(0, shot.height - 20, shot.width, 20)
            val stage = brightCel.colors.stage.toArgb()
            // The strip ends at 296 dp and the burst starts near 300: between them, the stage.
            for (x in 297..298) {
                for (y in 13..16) {
                    assertEquals(
                        stage,
                        strip.getRGB(x, y),
                        "($x, $y) is between the strip and the burst",
                    )
                }
            }
            assertTrue(
                (300 until 320).any {
                    strip.getRGB(it, 10) == brightCel.colors.danger.toArgb()
                },
                "the burst is drawn in the room",
            )
        }

    @Test
    fun `silence and clipping are said in words, not only in the strip`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var state by mutableStateOf(ToonLevelState.Live)
            setContent { ToonTheme { ToonLevelMeter("Microphone", source = "Built-in", state = state) } }
            mainClock.advanceTimeByFrame()
            onNodeWithText("Microphone").assertIsDisplayed()
            onNodeWithText("Built-in").assertIsDisplayed()
            assertEquals(0, onAllNodes(hasText("Silence")).fetchSemanticsNodes().size)
            state = ToonLevelState.Silent
            mainClock.advanceTimeByFrame()
            onNodeWithText("Silence").assertIsDisplayed()
            state = ToonLevelState.Clip
            mainClock.advanceTimeByFrame()
            onNodeWithText("Too loud").assertIsDisplayed()
        }

    @Test
    fun `a long source gives way, the status stays whole and inside, and the width is held to its range`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                ToonTheme {
                    Column {
                        ToonLevelMeter(
                            "Microphone",
                            Modifier.testTag("meter"),
                            source = "An external USB microphone with a very long name indeed",
                            state = ToonLevelState.Clip,
                            width = 100.dp,
                        )
                        // A label as long as the row: the label gives way before the status does.
                        ToonLevelMeter(
                            "System sound from the speakers",
                            Modifier.testTag("long"),
                            state = ToonLevelState.Silent,
                            width = 160.dp,
                        )
                        // The words alone, unconstrained, for their whole widths.
                        Box(Modifier.wrapContentWidth(unbounded = true)) {
                            ToonLevelMeter(
                                "",
                                source = "",
                                state = ToonLevelState.Silent,
                                width = 600.dp,
                                modifier = Modifier.testTag("free"),
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            onNodeWithTag("meter").assertWidthIsEqualTo(160.dp)
            val status = onNodeWithText("Too loud").fetchSemanticsNode().boundsInRoot
            assertTrue(status.right <= 160f + .5f, "the status ends at ${status.right} of 160")
            val silences = onAllNodes(hasText("Silence")).fetchSemanticsNodes().map { it.boundsInRoot }
            assertEquals(2, silences.size)
            assertEquals(silences[1].width, silences[0].width, .5f, "the status is whole next to a long label")
            assertTrue(silences[0].right <= 160f + .5f, "and inside: ${silences[0].right}")
        }
}
