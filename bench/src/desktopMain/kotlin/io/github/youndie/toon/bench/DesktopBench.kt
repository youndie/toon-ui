package io.github.youndie.toon.bench

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import java.io.File

/**
 * B-05, desktop half: the scene drawn into a Pixel 6a-sized raster (1080 × 2400 px at 2.625),
 * frame by frame at 60 Hz steps, timing each frame's draw call. CPU raster: it times the geometry
 * and Skia's path filling, with no GPU and no compositor in the loop.
 *
 * Protocol (pre-registered in the B-05 item): 5 warm-up replays per arm, then 20 rounds in ABBA
 * order of Rebuild and Cache, then 5 replays of Control. One CSV row per frame.
 */
fun main(args: Array<String>) {
    val out = File(args.firstOrNull() ?: "desktop-frames.csv")
    out.parentFile?.mkdirs()
    val density = Density(2.625f)
    val bitmap = ImageBitmap(1080, 2400)
    val canvas = Canvas(bitmap)
    val drawScope = CanvasDrawScope()
    val size = Size(1080f, 2400f)
    val frames = (0..(REPLAY_MS * 60 / 1000)).map { it * 1000f / 60 }
    val rows = StringBuilder("arm,replay,frame,t_ms,draw_ns,shapes_built\n")

    fun replay(
        arm: Arm,
        index: Int,
        record: Boolean,
    ) {
        val scene = FrameCostScene(arm)
        frames.forEachIndexed { f, t ->
            val before = scene.shapesBuilt
            val start = System.nanoTime()
            drawScope.draw(density, LayoutDirection.Ltr, canvas, size) {
                translate(16f * density.density, 40f * density.density) { scene.draw(this, t) }
            }
            val ns = System.nanoTime() - start
            if (record) rows.append("$arm,$index,$f,${"%.2f".format(t)},$ns,${scene.shapesBuilt - before}\n")
        }
    }

    repeat(5) {
        replay(Arm.Rebuild, it, false)
        replay(Arm.Cache, it, false)
    }
    for (round in 0 until 20) {
        val order = if (round % 2 == 0) listOf(Arm.Rebuild, Arm.Cache) else listOf(Arm.Cache, Arm.Rebuild)
        order.forEach { replay(it, round, true) }
    }
    repeat(5) { replay(Arm.Control, it, true) }
    out.writeText(rows.toString())
    println("B-05 desktop: ${rows.lines().size - 2} frames written to $out")
}
