package io.github.youndie.toon.theme

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-13: the stage is textured in Ink & Gouache, repeating every 256 dp at any density, and flat in Bright Cel. */
@OptIn(ExperimentalTestApi::class)
class ToonStageTest {
    private fun stage(
        tokens: ToonTokens,
        density: Float,
    ): PixelMap {
        lateinit var px: PixelMap
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(density)) {
                    ToonTheme(tokens) { ToonStage(Modifier.size(300.dp).testTag("stage")) }
                }
            }
            waitForIdle()
            px = onNodeWithTag("stage").captureToImage().toPixelMap()
        }
        return px
    }

    @Test
    fun `the gouache stage repeats every 256 dp, at densities 1 and 2`() {
        for (tokens in listOf(inkGouacheLight, inkGouacheNight)) {
            for (d in listOf(1f, 2f)) {
                val px = stage(tokens, d)
                val period = (256 * d).toInt()
                val colours = mutableSetOf<Int>()
                var mismatched = 0
                for (y in 0 until period step 3) {
                    for (x in 0 until period step 3) colours += px[x, y].toArgb()
                }
                var halfway = 0
                for (y in 0 until (40 * d).toInt()) {
                    for (x in 0 until (40 * d).toInt()) {
                        if (px[x, y].toArgb() != px[x + period, y].toArgb()) mismatched++
                        if (px[x, y].toArgb() != px[x, y + period].toArgb()) mismatched++
                        if (px[x, y].toArgb() != px[x + period / 2, y].toArgb()) halfway++
                    }
                }
                // Not at half the period: a tile drawn at 256 px would repeat at 512 px too.
                assertTrue(halfway > 0, "density $d: the tile repeats at ${period / 2} px")
                // The tile's palette is small (8 colours on a 6 px grid of the PNG); the flat stage has one.
                assertTrue(colours.size >= 4, "density $d: a texture, not a flat colour (${colours.size} colours)")
                assertEquals(0, mismatched, "density $d: the tile repeats at $period px")
            }
        }
    }

    @Test
    fun `the Bright Cel stage is its flat colour`() {
        val px = stage(brightCel, 1f)
        for (y in 0 until 300 step 7) {
            for (x in 0 until 300 step 7) assertEquals(brightCel.colors.stage.toArgb(), px[x, y].toArgb())
        }
    }
}
