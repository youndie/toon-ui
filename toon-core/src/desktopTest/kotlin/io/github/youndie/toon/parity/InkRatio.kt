package io.github.youndie.toon.parity

import androidx.compose.ui.graphics.toArgb
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

/**
 * B-12's measure, shared by [InkParityTest] and the tests that hold a live component to its frames
 * (B-46): the pixels two images draw differently, over the pixels either of them inks, with the
 * floors measured on the committed Divider and TextFloor cells (research D3).
 */
internal object InkRatio {
    /** viddik's `designChannelTolerance` in toon-core/build.gradle.kts. */
    const val CHANNEL = 16

    private val snapshots = File("src/desktopTest/snapshots")

    private val stages =
        mapOf(
            "InkGouacheLight" to inkGouacheLight.colors.stage.toArgb(),
            "InkGouacheNight" to inkGouacheNight.colors.stage.toArgb(),
            "BrightCel" to brightCel.colors.stage.toArgb(),
        )

    fun differs(
        a: Int,
        b: Int,
    ): Boolean = (0..16 step 8).any { s -> abs((a shr s and 0xFF) - (b shr s and 0xFF)) > CHANNEL }

    /** Differing pixels over inked pixels, for two images [w] × [h] on a [stage]. */
    fun of(
        w: Int,
        h: Int,
        stage: Int,
        a: (Int, Int) -> Int,
        b: (Int, Int) -> Int,
    ): Double {
        var ink = 0
        var diff = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                val p = a(x, y)
                val q = b(x, y)
                if (differs(p, stage) || differs(q, stage)) ink++
                if (differs(p, q)) diff++
            }
        }
        return if (ink == 0) 0.0 else diff.toDouble() / ink
    }

    /** The ratio for one committed reference and its golden. */
    fun ofCell(name: String): Double {
        val ref: BufferedImage = ImageIO.read(File(snapshots, "design/$name"))
        val golden: BufferedImage = ImageIO.read(File(snapshots, name))
        val stage = stages.getValue(stages.keys.single { name.removeSuffix(".png").endsWith(it) })
        return of(ref.width, ref.height, stage, ref::getRGB, golden::getRGB)
    }

    fun cells(group: String?): List<String> =
        checkNotNull(File(snapshots, "design").list())
            .filter { it.endsWith(".png") && (group == null || it.startsWith("${group}_")) }
            .sorted()

    /** The floor for shapes: the Divider cells, the rasterisers' difference alone. */
    val shapes: Double by lazy { cells("Divider").maxOf(::ofCell) }

    /** The floor for cells with text: a label alone, inked by CoreText against Skia. */
    val text: Double by lazy { cells("TextFloor").maxOf(::ofCell) }
}
