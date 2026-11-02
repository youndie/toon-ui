package io.github.youndie.toon.theme

import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-13: the vendored tiles wrap. A wrap's mean channel difference (last column against the first,
 * last row against the first) is held to the inner neighbours' times a factor measured once, on
 * 2026-11-02: every horizontal wrap and the paper tiles' vertical ones within 7 % of the inner
 * difference, the gouache tiles' vertical wrap at 1.91 times it (2.29 against 1.20 levels), which
 * the 2 × 2 stage golden shows (`Stage_Tiled_*`).
 */
class ToonStageSeamTest {
    private val tiles = File("src/commonMain/composeResources/drawable")

    private class Seams(
        val x: Double,
        val innerX: Double,
        val y: Double,
        val innerY: Double,
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
            (1 until h)
                .map {
                    rows(it - 1, it)
                }.average(),
        )
    }

    @Test
    fun `every tile wraps within its measured factor of the inner difference`() {
        for ((name, vertical) in listOf(
            "gouache_light" to 2.0,
            "gouache_night" to 2.0,
            "paper_light" to 1.1,
            "paper_night" to 1.1,
        )) {
            val s = seams(name)
            println(
                "$name: x wrap ${"%.2f".format(
                    s.x,
                )} inner ${"%.2f".format(s.innerX)}; y wrap ${"%.2f".format(s.y)} inner ${"%.2f".format(s.innerY)}",
            )
            assertTrue(s.x <= 1.1 * s.innerX, "$name: horizontal wrap ${s.x} against ${s.innerX}")
            assertTrue(s.y <= vertical * s.innerY, "$name: vertical wrap ${s.y} against ${s.innerY}")
        }
    }
}
