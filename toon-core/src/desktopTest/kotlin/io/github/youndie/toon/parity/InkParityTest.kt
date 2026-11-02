package io.github.youndie.toon.parity

import androidx.compose.ui.graphics.toArgb
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-12: parity measured against the ink, not the cell. viddik's percentage is of the whole cell, and
 * an effect covers a small part of its 260 × 150 cell, so a wrong effect stays under the Divider's
 * 1.54 % floor: stars spinning one way measured 0.56 %, sweat drops without their stretch 0.50 %.
 * Here a cell's differing pixels are counted against the pixels either image inks (differs from
 * the stage), and the floor is that same ratio over the Divider cells, the rasterisers' difference
 * alone (research D3). Both sides are committed files: the Chrome reference and the viddik golden.
 */
class InkParityTest {
    private val snapshots = File("src/desktopTest/snapshots")

    private val stages =
        mapOf(
            "InkGouacheLight" to inkGouacheLight.colors.stage.toArgb(),
            "InkGouacheNight" to inkGouacheNight.colors.stage.toArgb(),
            "BrightCel" to brightCel.colors.stage.toArgb(),
        )

    private fun differs(
        a: Int,
        b: Int,
    ): Boolean = (0..16 step 8).any { s -> abs((a shr s and 0xFF) - (b shr s and 0xFF)) > CHANNEL }

    /** Differing pixels over inked pixels, for one reference and its golden. */
    private fun inkRatio(name: String): Double {
        val ref: BufferedImage = ImageIO.read(File(snapshots, "design/$name"))
        val golden: BufferedImage = ImageIO.read(File(snapshots, name))
        val stage = stages.getValue(stages.keys.single { name.removeSuffix(".png").endsWith(it) })
        var ink = 0
        var diff = 0
        for (y in 0 until ref.height) {
            for (x in 0 until ref.width) {
                val r = ref.getRGB(x, y)
                val g = golden.getRGB(x, y)
                if (differs(r, stage) || differs(g, stage)) ink++
                if (differs(r, g)) diff++
            }
        }
        return if (ink == 0) 0.0 else diff.toDouble() / ink
    }

    private fun cells(group: String) =
        checkNotNull(File(snapshots, "design").list())
            .filter { it.startsWith("${group}_") && it.endsWith(".png") }
            .sorted()

    @Test
    fun `every effect frame is within the divider's ink floor`() {
        val floor = cells("Divider").maxOf(::inkRatio)
        val fx = cells("Fx").associateWith(::inkRatio)
        val worst = fx.maxBy { it.value }
        println("ink floor (Divider): ${pct(floor)}; worst FX: ${worst.key} ${pct(worst.value)}")
        val over = fx.filterValues { it > floor }
        assertTrue(over.isEmpty(), "over the ink floor ${pct(floor)}: ${over.mapValues { pct(it.value) }}")
    }

    private fun pct(v: Double) = "%.2f %%".format(v * 100)

    private companion object {
        /** viddik's `designChannelTolerance` in toon-core/build.gradle.kts. */
        const val CHANNEL = 16
    }
}
