package io.github.youndie.toon.theme

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-86: the textured stage drew different pixels on two x86 machines. At 1 and 2 px a dp every pixel
 * of the stage is exactly one texel of the bundled tile, which CI's `stage-x86` job also checks on a
 * Linux x86 runner; and `stageTexture = false` draws it flat, the guarantee for a consumer's goldens.
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
                        // At 1 px a dp a pixel's centre lies on the line between texels 2x and 2x + 1; the Mac
                        // and the x86 runners tried all take 2x.
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

    private fun bitmap(name: String) =
        org.jetbrains.skia.Image
            .makeFromEncoded(File("src/commonMain/composeResources/drawable/$name.png").readBytes())
            .toComposeImageBitmap()

    /**
     * One stage frame of 260 × 120 dp at [density], drawn the way the stage draws itself, under a canvas moved by [nudge] px
     * and with [perspective] as the matrix's perspective term in y, as viddik's capture matrix has it (#71).
     */
    private fun frame(
        tile: ImageBitmap,
        density: Float,
        nudge: Float = 0f,
        perspective: Float = 0f,
    ): IntArray {
        val w = (260 * density).toInt()
        val h = (120 * density).toInt()
        val image = ImageBitmap(w, h)
        val canvas = Canvas(image)
        if (perspective != 0f) canvas.concat(Matrix().apply { values[Matrix.Perspective1] = perspective })
        CanvasDrawScope().draw(Density(density), LayoutDirection.Ltr, canvas, Size(w.toFloat(), h.toFloat())) {
            translate(nudge, nudge) { drawToonStage(inkGouacheLight, tile) }
        }
        val pixels = IntArray(w * h)
        image.readPixels(pixels)
        return pixels
    }

    @Test
    fun `no pixel's centre falls between two texels, so a nudge of a thousandth moves nothing`() {
        // B-98: the tie #71 found is a centre on the line between two texels. Nudged a thousandth of a pixel either way, a
        // tie goes to the texels either side of it on any CPU; a centre off the line keeps its texel.
        val tile = bitmap("gouache_light")
        for (density in listOf(1f, 2f, 2.75f)) {
            val left = frame(tile, density, nudge = -1e-3f)
            val right = frame(tile, density, nudge = 1e-3f)
            val differ = left.indices.count { left[it] != right[it] }
            assertEquals(
                0,
                differ,
                "at $density px a dp, $differ of ${left.size} pixels change with a nudge of 0.002 px",
            )
        }
    }

    @Test
    fun `under viddik's capture matrix the stage draws what it draws without it`() {
        // The consumer's frame: its goldens are captured under a perspective term of 1e-9 (#71).
        val tile = bitmap("gouache_light")
        for (density in listOf(1f, 2f)) {
            val plain = frame(tile, density)
            val captured = frame(tile, density, perspective = 1e-9f)
            val differ = plain.indices.count { plain[it] != captured[it] }
            assertEquals(0, differ, "at $density px a dp, $differ pixels differ under the capture matrix")
        }
    }

    @Test
    fun `the shift keeps every centre at least 0_008 texel off a line at common densities`() {
        val densities = (4..16).map { it / 4f } + listOf(2.625f, 2.75f)
        var worst = 1f
        for (density in densities) {
            val k = density / STAGE_TILE_PX_PER_DP
            for (x in 0 until 3000) {
                val u = (x + .5f) / k - STAGE_TILE_SHIFT
                val f = u - kotlin.math.floor(u)
                worst = minOf(worst, f, 1 - f)
            }
        }
        assertTrue(worst >= .008f, "the nearest centre is $worst texel from a line")
    }
}
