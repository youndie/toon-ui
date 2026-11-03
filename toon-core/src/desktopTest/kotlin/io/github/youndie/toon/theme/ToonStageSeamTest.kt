package io.github.youndie.toon.theme

import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-59: the tiles wrap without a seam. The design's tiles (B-13) did not: the gouache tiles' last
 * row stood 1.91 times the inner difference from the first (2.29 against 1.20 levels), a line in
 * the stage every 256 dp, and paper night's columns 1.1 times. The bundled tiles are healed from
 * them: the wrap is a pair of the design tile's own neighbours, and no column or row anywhere, the
 * wraps included, stands out from the median of its neighbours.
 */
class ToonStageSeamTest {
    private val tiles = File("src/commonMain/composeResources/drawable")

    private class Seams(
        val x: Double,
        val innerX: Double,
        val y: Double,
        val innerY: Double,
        val columns: List<Double>,
        val rows: List<Double>,
    )

    private fun seams(name: String): Seams {
        val img = ImageIO.read(File(tiles, "$name.png"))
        val w = img.width
        val h = img.height

        fun diff(
            a: Int,
            b: Int,
        ) = (0..16 step 8).sumOf { s -> abs((a shr s and 0xFF) - (b shr s and 0xFF)) } / 3.0

        fun cols(
            x0: Int,
            x1: Int,
        ) = (0 until h).sumOf { diff(img.getRGB(x0, it), img.getRGB(x1, it)) } / h

        fun rows(
            y0: Int,
            y1: Int,
        ) = (0 until w).sumOf { diff(img.getRGB(it, y0), img.getRGB(it, y1)) } / w
        return Seams(
            cols(w - 1, 0),
            (1 until w).map { cols(it - 1, it) }.average(),
            rows(h - 1, 0),
            (1 until h).map { rows(it - 1, it) }.average(),
            (0 until w).map { cols(it, (it + 1) % w) },
            (0 until h).map { rows(it, (it + 1) % h) },
        )
    }

    @Test
    fun `no line of any tile stands out, the wraps included`() {
        for (name in listOf("gouache_light", "gouache_night", "paper_light", "paper_night")) {
            val s = seams(name)
            println("$name: x wrap %.2f inner %.2f; y wrap %.2f inner %.2f".format(s.x, s.innerX, s.y, s.innerY))
            for ((axis, lines) in listOf("column" to s.columns, "row" to s.rows)) {
                val median = lines.sorted()[lines.size / 2]
                val worst = lines.indices.maxBy { lines[it] }
                assertTrue(
                    lines[worst] <= LINE * median,
                    "$name: $axis $worst against its neighbour ${lines[worst]}, the median $median",
                )
            }
        }
    }

    @Test
    fun `every tile keeps the design tile's paint and is made from it`() {
        // B-59: the heal cuts and never mixes, so the posterised colours stay the design's own.
        val design = File("../reference/design-system/assets/textures")
        for (name in listOf("gouache_light", "gouache_night", "paper_light", "paper_night")) {
            fun colours(f: File) =
                ImageIO.read(f).let { img ->
                    (0 until img.width * img.height).map { img.getRGB(it % img.width, it / img.width) }.toSet()
                }
            val ours = colours(File(tiles, "$name.png"))
            val theirs = colours(File(design, "${name.replace('_', '-')}.png"))
            assertTrue(ours.isNotEmpty() && theirs.containsAll(ours), "$name: colours not in the design's tile")
            assertEquals(448, ImageIO.read(File(tiles, "$name.png")).width, "$name: 512 px less the 64 folded")
        }
    }

    private companion object {
        /** The healed tiles' worst line is 1.51 times the median; the design's gouache seam was 1.93. */
        const val LINE = 1.6
    }
}
