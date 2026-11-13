package io.github.youndie.toon.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.inkGouacheLight
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** B-88: a segmented control whose labels do not fit equal segments sizes its segments by them. */
@OptIn(ExperimentalTestApi::class)
class ToonSegmentedLabelsTest {
    /** The issue's labels, measured on desktop at the selected weight: «Все», «Красные», «С PR», «Ждут меня». */
    private val issue = listOf(26f, 63f, 33f, 78f)

    @Test
    fun `labels that fit keep the design's equal segments`() {
        assertNull(toonSegmentedLayout(300f, listOf(28f, 40f, 46f)).edges, "Day, Week, Month at 300")
        assertNull(toonSegmentedLayout(328f, listOf(58f, 58f, 58f, 58f)).edges, "every label exactly fills its share")
    }

    @Test
    fun `segments hold their labels, share the rest, and fill the width`() {
        for (w in listOf(200f, 240f, 280f, 328f, 360f)) {
            for (widths in listOf(issue, listOf(20f, 200f), listOf(90f, 90f, 90f, 90f, 90f))) {
                val layout = toonSegmentedLayout(w, widths)
                val e = layout.edges ?: continue
                assertEquals(widths.size + 1, e.size)
                assertEquals(0f, e.first())
                assertEquals(w, e.last())
                for (i in widths.indices) {
                    assertTrue(e[i + 1] > e[i], "$w $widths: segment $i is empty")
                }
                assertTrue(layout.labelScale in .8f..1f, "$w $widths: labels set at ${layout.labelScale}")
                val fits = widths.sumOf { (it * layout.labelScale + 24f).toDouble() } <= w + .01
                if (fits) {
                    for (i in widths.indices) {
                        assertTrue(
                            e[i + 1] - e[i] >= widths[i] * layout.labelScale + 24f - .01f,
                            "$w $widths: label $i out of its segment",
                        )
                    }
                }
            }
        }
        // Past the floor, the short labels keep their segments whole and only the long one is cut.
        val squeezed = toonSegmentedLayout(260f, listOf(26f, 220f, 33f))
        val se = squeezed.edges!!
        assertEquals(.8f, squeezed.labelScale)
        assertTrue(se[1] - se[0] >= 26f * .8f + 24f - .01f, "«Все» squeezed to ${se[1] - se[0]}")
        assertTrue(se[3] - se[2] >= 33f * .8f + 24f - .01f, "«Ждут» squeezed to ${se[3] - se[2]}")
        val layout = toonSegmentedLayout(328f, issue)
        assertEquals(1f, layout.labelScale, "the issue's labels fit at their size once the segments follow them")
        assertEquals(3, layout.slotAt(328f, 4, 300f))
        assertEquals(0, layout.slotAt(328f, 4, 1f))
    }

    @Test
    fun `the drop runs between segments of their own widths without a jump`() {
        val e = toonSegmentedLayout(328f, issue).edges!!
        val start = toonSegSwitch(0f, 0, 3, 82f, e)
        val end = toonSegSwitch(5_000f, 0, 3, 82f, e)
        assertTrue(abs(start.ia!! - (e[0] + 5)) < .5f && abs(start.ib!! - (e[1] - 5)) < .5f, "starts on segment 0")
        assertTrue(abs(end.ia!! - (e[3] + 5)) < .5f && abs(end.ib!! - (e[4] - 5)) < .5f, "ends on segment 3")
        var last = start
        for (t in 1..700) {
            val m = toonSegSwitch(t.toFloat(), 0, 3, 82f, e)
            assertTrue(abs(m.ia!! - last.ia!!) < 6f && abs(m.ib!! - last.ib!!) < 6f, "a jump at $t ms")
            last = m
        }
        val rest =
            toonSegmentedScene(
                inkGouacheLight,
                328f,
                listOf("a", "b", "c", "d"),
                ToonSegmentedLook(sel = 3),
                toonSegmentedLayout(328f, issue),
            )
        assertEquals((e[3] + e[4]) / 2, rest.labels[3].x)
        assertEquals(e[4] - e[3] - 24f + 1f, rest.labels[3].maxWidth)
    }

    @Test
    fun `the issue's control draws every label inside its segment and a tap picks the segment it lands on`() {
        val c = inkGouacheLight.colors

        fun near(a: Color) =
            abs(a.red - c.text.red) <= .12f && abs(a.green - c.text.green) <= .12f && abs(a.blue - c.text.blue) <= .12f
        val labels = listOf("Все", "Красные", "С PR", "Ждут меня")
        runComposeUiTest {
            var chosen: Int? = null
            setContent {
                ToonTheme {
                    ToonSegmented(labels, Modifier.testTag("seg"), width = 328.dp, onChange = {
                        chosen =
                            it
                    })
                }
            }
            val image = onNodeWithTag("seg").captureToImage().toPixelMap()
            // Between the last label and the control's right outline: no letter.
            var ink = 0
            for (y in 14..34) for (x in 316..321) if (near(image[x, y])) ink++
            assertEquals(0, ink, "«Ждут меня» drawn into the outline")
            val e = toonSegmentedLayout(328f, issue).edges!!
            onNodeWithTag("seg").performTouchInput { click(Offset((e[3] + 4) * density, 24 * density)) }
            assertNotNull(chosen)
            assertEquals(3, chosen, "a tap 4 dp into the last segment")
        }
    }
}
