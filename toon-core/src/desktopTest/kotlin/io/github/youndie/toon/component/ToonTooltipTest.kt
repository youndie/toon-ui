package io.github.youndie.toon.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.parity.TipFrames
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
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
 * B-40: every v1 page tooltip cell and its inflate strip against the engine's frames, `tipInflate` at
 * every millisecond both ways, reduce motion, and what a screen reader hears.
 */
@OptIn(ExperimentalTestApi::class)
class ToonTooltipTest {
    /** Without a DOM the engine measures text as length × size × 0.55 (research §1.2). */
    private fun nodeWidth(text: String) = text.length * 14 * .55f

    @Test
    fun `every tooltip cell and inflate frame is the engine's, drawn as designed`() {
        val cells = EngineFrames.cells("Tooltip", asDesigned = true)
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                val scene =
                    if (o.getValue("kind").jsonPrimitive.content == "inflate") {
                        TipFrames.inflate(cell.tokens, o.f("t")!!, nodeWidth(TipFrames.TEXT), asDesigned = true)
                    } else {
                        val text = o.getValue("text").jsonPrimitive.content
                        TipFrames.cell(cell.tokens, text, o.f("s") ?: 1f, nodeWidth(text), asDesigned = true)
                    }
                EngineFrames.compare(cell, scene)
            }
        println("tooltip frames: ${cells.size} cells, worst coordinate $worst dp")
        assertEquals(24, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `no ink crosses the joint between the balloon's paper and the tail's, in any theme`() {
        // B-60: the engine's tail paper stops at y −15.5, and Bright Cel's balloon paper ends at −15.7,
        // so a 0.2 dp hairline of the balloon's ink ran across the tail's top. Drawn 8× with no text,
        // the columns over the tail's middle are paper from inside the balloon down into the tail.
        val k = 8
        for ((theme, tokens) in listOf(
            "InkGouacheLight" to inkGouacheLight,
            "InkGouacheNight" to inkGouacheNight,
            "BrightCel" to brightCel,
        )) {
            val w = toonTooltipWidth(100f)
            val img = ImageBitmap((w * k).toInt(), 46 * k)
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(img), Size(w * k, 46f * k)) {
                drawRect(tokens.colors.stage)
                scale(k.toFloat(), k.toFloat(), Offset.Zero) {
                    drawScene(toonTooltipScene(tokens, "", 100f, 1f).placed(w / 2, 46f))
                }
            }
            val px = img.toPixelMap()
            val paper = tokens.colors.surface
            for (x in -1..3) {
                for (row in (46 - 20) * k until (46 - 10) * k) {
                    val c = px[((w / 2 + x) * k).toInt(), row]
                    val off = maxOf(abs(c.red - paper.red), abs(c.green - paper.green), abs(c.blue - paper.blue))
                    assertTrue(
                        off < .1f,
                        "$theme: ink at x $x dp, y ${row.toFloat() / k - 46} dp ($c)",
                    )
                }
            }
        }
    }

    @Test
    fun `the balloon follows the engine's curve at every millisecond, up and down`() {
        val curves =
            Json
                .parseToJsonElement(
                    checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText(),
                ).jsonObject
        var worst = 0f
        for (show in listOf(true, false)) {
            val c = curves.getValue("tipInflate(t, $show)").jsonObject
            val at = c.getValue("at").jsonArray.map { it.jsonPrimitive.content.toFloat() }
            val v = c.getValue("value").jsonArray.map { it.jsonPrimitive.content.toFloat() }
            for (i in at.indices) worst = maxOf(worst, abs(v[i] - toonTipInflate(at[i], show)))
        }
        println("tipInflate: worst $worst")
        assertTrue(worst <= 1e-5f, "tipInflate off the engine by $worst")
    }

    @Test
    fun `a show or a hide plays 500 ms, and under reduce motion the balloon fades at its size in 120`() {
        assertEquals(ToonTooltipMoment(1f), toonTooltipFrameAt(true, null, false))
        assertNull(toonTooltipFrameAt(false, null, false))
        assertEquals(ToonTooltipMoment(toonTipInflate(60f, true)), toonTooltipFrameAt(true, 60f, false))
        assertEquals(ToonTooltipMoment(toonTipInflate(60f, false)), toonTooltipFrameAt(false, 60f, false))
        // Under 0.02 nothing is drawn: the first frame of a show, and a hide from 118 ms.
        assertNull(toonTooltipFrameAt(true, 0f, false))
        assertNull(toonTooltipFrameAt(false, 118f, false))
        assertEquals(
            ToonTooltipMoment(toonTipInflate(115f, false)),
            toonTooltipFrameAt(false, 115f, false),
            "0.04 is drawn",
        )
        assertEquals(ToonTooltipMoment(1f, .5f), toonTooltipFrameAt(true, 60f, true))
        assertEquals(ToonTooltipMoment(1f, .5f), toonTooltipFrameAt(false, 60f, true))
        assertNull(toonTooltipFrameAt(false, 120f, true))
        val clock = ToonTooltipClock(true)
        clock.tick(1000)
        clock.tick(1499)
        assertEquals(499f, clock.age())
        clock.tick(1500)
        assertNull(clock.age())
        clock.changed(false)
        clock.tick(2000)
        clock.tick(2499)
        assertEquals(499f, clock.age())
        clock.tick(2500)
        assertNull(clock.age())
    }

    @Test
    fun `the text shows past 0_85 of the balloon, and the box is the balloon's width`() {
        val tokens = inkGouacheLight
        assertEquals(0f, toonTooltipScene(tokens, "Hi", 20f, .85f).labels.single().alpha)
        assertEquals(.5f, toonTooltipScene(tokens, "Hi", 20f, .925f).labels.single().alpha, 1e-5f)
        assertEquals(1f, toonTooltipScene(tokens, "Hi", 20f, 1.3f).labels.single().alpha)
        assertTrue(toonTooltipScene(tokens, "Hi", 20f, .019f).layers.isEmpty())
        assertEquals(48f, toonTooltipWidth(20f))
    }

    @Test
    fun `a screen reader hears the text while it shows, politely, and nothing once hidden`() {
        runComposeUiTest {
            var show by mutableStateOf(true)
            setContent { ToonTheme { ToonTooltip("Tap to add a plan", Modifier.testTag("tip"), show = show) } }
            val c = onNodeWithTag("tip").fetchSemanticsNode().config
            assertEquals(listOf("Tap to add a plan"), c.getOrNull(SemanticsProperties.ContentDescription))
            assertEquals(
                androidx.compose.ui.semantics.LiveRegionMode.Polite,
                c.getOrNull(SemanticsProperties.LiveRegion),
            )
            show = false
            waitForIdle()
            assertNull(
                onNodeWithTag("tip").fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription),
            )
        }
    }
}
