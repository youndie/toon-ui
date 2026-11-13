package io.github.youndie.toon.theme

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
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
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * B-86: the textured stage drew different pixels on two x86 machines. Each pixel's centre sat on the
 * line between two texels at 1 px a dp, and nearest sampling picked by rounding. The stage now samples
 * texel centres, so every pixel is exactly one texel of the bundled tile; and `stageTexture = false`
 * draws it flat.
 */
@OptIn(ExperimentalTestApi::class)
class ToonStageSamplingTest {
    private fun tile(name: String) = ImageIO.read(File("src/commonMain/composeResources/drawable/$name.png"))

    private fun stage(
        tokens: ToonTokens,
        density: Float,
        texture: Boolean = true,
    ): androidx.compose.ui.graphics.PixelMap {
        var map: androidx.compose.ui.graphics.PixelMap? = null
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(density)) {
                    ToonTheme(
                        tokens,
                        stageTexture = texture,
                    ) { ToonStage(Modifier.size(260.dp, 120.dp).testTag("stage")) }
                }
            }
            waitForIdle()
            map = onNodeWithTag("stage").captureToImage().toPixelMap()
        }
        return map!!
    }

    @Test
    fun `every pixel of the stage is one texel of the tile, at 1 and 2 px a dp`() {
        for ((tokens, name) in listOf(inkGouacheLight to "gouache_light", inkGouacheNight to "gouache_night")) {
            val tile = tile(name)
            for (density in listOf(1f, 2f)) {
                val map = stage(tokens, density)
                val k = density / 2
                var off = 0
                for (y in 0 until map.height) {
                    for (x in 0 until map.width) {
                        // The texel whose centre the pixel's centre samples: x / k + 0.5 lies inside texel floor(x / k).
                        val tx = Math.floorMod((x / k).toInt(), tile.width)
                        val ty = Math.floorMod((y / k).toInt(), tile.height)
                        if (map[x, y].toArgb() != (tile.getRGB(tx, ty) or (0xFF shl 24))) off++
                    }
                }
                assertEquals(
                    0,
                    off,
                    "${tokens.name} at $density px a dp: $off of ${map.width * map.height} pixels are not their texel",
                )
            }
        }
    }

    @Test
    fun `stageTexture false draws the stage in its flat colour`() {
        for (tokens in listOf(inkGouacheLight, inkGouacheNight)) {
            val map = stage(tokens, 1f, texture = false)
            val stage = tokens.colors.stage.toArgb()
            var off = 0
            for (y in 0 until map.height) for (x in 0 until map.width) if (map[x, y].toArgb() != stage) off++
            assertEquals(0, off, "${tokens.name}: the flat stage has $off textured pixels")
        }
    }
}
