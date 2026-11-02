package io.github.youndie.toon.type

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.youndie.toon.draw.ToonTextRenderer
import io.github.youndie.toon.draw.ToonTextStyle
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-35: what the backing faces promise, measured on the faces themselves at 100 sp: a Cyrillic
 * letter stands as tall as its Latin twin in the design's face (`х` and `x`), and its stem is as
 * heavy (`о` and `o`).
 */
@OptIn(ExperimentalTestApi::class)
class ToonCyrillicMetricsTest {
    /** The ink rows and the widest dark run at mid-height of [s], drawn alone in black. */
    private fun ink(
        text: ToonTextRenderer,
        s: String,
        style: ToonTextStyle,
    ): Pair<Int, Int> {
        val layout = text.measure(s, style)
        val img = ImageBitmap(layout.size.width + 4, layout.size.height + 4)
        CanvasDrawScope().draw(
            Density(1f),
            LayoutDirection.Ltr,
            Canvas(img),
            Size(img.width.toFloat(), img.height.toFloat()),
        ) {
            drawText(layout, Color.Black)
        }
        val px = img.toPixelMap()
        val rows = (0 until img.height).filter { y -> (0 until img.width).any { px[it, y].alpha > .5f } }
        val mid = (rows.first() + rows.last()) / 2
        var run = 0
        var best = 0
        for (x in 0 until img.width) {
            run = if (px[x, mid].alpha > .5f) run + 1 else 0
            best = maxOf(best, run)
        }
        return rows.size to best
    }

    private fun check(tokens: ToonTokens) {
        lateinit var text: ToonTextRenderer
        runComposeUiTest {
            setContent { ToonTheme(tokens) { rememberToonText().also { SideEffect { text = it } } } }
            waitForIdle()
        }
        for ((role, style) in listOf(
            "display" to ToonTextStyle(400, 100f, display = true),
            "body 500" to ToonTextStyle(500, 100f),
            "body 700" to ToonTextStyle(700, 100f),
        )) {
            val (lx, _) = ink(text, "x", style)
            val (cx, _) = ink(text, "х", style)
            val (_, lo) = ink(text, "o", style)
            val (_, co) = ink(text, "о", style)
            println("${tokens.displayFamily} $role: x-height $lx / $cx, stem $lo / $co")
            // One row either way is where an edge falls between pixels.
            assertTrue(abs(lx - cx) <= 1, "${tokens.displayFamily} $role: x-height $lx, Cyrillic $cx")
            // Read as one weight: the display faces measured 7 and 11 % apart, the body faces under 10.
            assertTrue(abs(lo - co) <= .12f * lo, "${tokens.displayFamily} $role: stem $lo, Cyrillic $co")
        }
    }

    @Test
    fun `in Ink and Gouache a Cyrillic letter stands as tall and as heavy as the Latin`() = check(inkGouacheLight)

    @Test
    fun `in Bright Cel a Cyrillic letter stands as tall and as heavy as the Latin`() = check(brightCel)
}
