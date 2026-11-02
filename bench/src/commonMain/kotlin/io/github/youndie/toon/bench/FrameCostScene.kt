package io.github.youndie.toon.bench

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import io.github.youndie.toon.draw.ToonInsets
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.shapePoints
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

/**
 * The B-05 scene: ten cards falling in a 40 ms cascade on the engine's `cardAppear`, each landing
 * with two dust puffs of five clouds (`dust`). The numbers are toon-engine.js's `card`, `dust` and
 * `cardAppear` for Ink & Gouache light; the drawing is ink, offset shadow and flat fill, without text.
 */
enum class Arm {
    /** Geometry rebuilt from the engine's formula on every frame, as the sketch proposes. */
    Rebuild,

    /** Geometry cached per (size, radii, insets, seed, step), sizes quantised to a quarter dp. */
    Cache,

    /** [Rebuild] plus a 20 ms busy wait per frame: the positive control that jank is visible. */
    Control,
}

private val ink = Color(0xFF2B211C)
private val paper = Color(0xFFF4EBD6)
private val sticker = Color(0xFFE9B53A)
private val dustFill = Color(0xFFEFE6CF)
private val insets = ToonInsets(2f, 2.6f, 4f, 2.2f)
private val cardRadii =
    listOf(ToonRadii(16f, 10f, 18f, 12f), ToonRadii(11f, 17f, 13f, 19f), ToonRadii(18f, 12f, 15f, 10f))
private val tilts = listOf(-1.2f, 0f, 1.5f)
private val dustDirs =
    listOf(
        floatArrayOf(-1f, -.25f, 1f),
        floatArrayOf(-.55f, -.85f, .8f),
        floatArrayOf(.05f, -1f, .6f),
        floatArrayOf(.55f, -.8f, .85f),
        floatArrayOf(1f, -.3f, 1f),
    )

const val CARD_W = 342f
const val CARD_H = 84f
const val CARD_STEP = 80f
const val CARDS = 10

/** Replay length: the last card starts at 360 ms and its dust ends at 360 + 120 + 300. */
const val REPLAY_MS = 1000

private class ShapePaths(
    val ink: Path,
    val fill: Path,
)

private fun smooth(pts: FloatArray): Path =
    Path().apply {
        val n = pts.size / 2

        fun mx(
            i: Int,
            j: Int,
        ) = (pts[2 * i] + pts[2 * j]) / 2

        fun my(
            i: Int,
            j: Int,
        ) = (pts[2 * i + 1] + pts[2 * j + 1]) / 2
        moveTo(mx(n - 1, 0), my(n - 1, 0))
        for (i in 0 until n) {
            val j = (i + 1) % n
            quadraticTo(pts[2 * i], pts[2 * i + 1], mx(i, j), my(i, j))
        }
        close()
    }

/** One scene, one arm. Not thread-safe; one per drawing surface. */
class FrameCostScene(
    val arm: Arm,
) {
    private val cache = HashMap<String, ShapePaths>()
    var shapesBuilt = 0
        private set

    private fun q(v: Float) = round(v * 4) / 4

    private fun shape(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        r: ToonRadii,
        ins: ToonInsets,
        amp: Float,
        seed: Int,
        step: Float,
    ): ShapePaths {
        if (arm != Arm.Cache) return build(x, y, w, h, r, ins, amp, seed, step)
        val key = "${q(x)},${q(y)},${q(w)},${q(h)},$r,$ins,$amp,$seed,$step"
        return cache.getOrPut(key) { build(q(x), q(y), q(w), q(h), r, ins, amp, seed, step) }
    }

    private fun build(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        r: ToonRadii,
        ins: ToonInsets,
        amp: Float,
        seed: Int,
        step: Float,
    ): ShapePaths {
        shapesBuilt++
        val p = shapePoints(x, y, w, h, r, ins, amp, seed, step)
        return ShapePaths(smooth(p.ink), smooth(p.fill))
    }

    private fun osc(
        tSec: Float,
        z: Float,
        w: Float,
    ): Float {
        val wd = w * sqrt(1 - z * z)
        return exp(-z * w * tSec) * kotlin.math.cos(wd * tSec)
    }

    /** Draws the scene [tMs] after the cascade started; the scene's 0,0 is the top left of card 0. */
    fun draw(
        scope: DrawScope,
        tMs: Float,
    ) = with(scope) {
        if (arm == Arm.Control) busyWait(20)
        val px = density
        for (i in 0 until CARDS) {
            val t = tMs - 40 * i
            if (t < 0) continue
            val rot0 = tilts[i % 3]
            var op = 1f
            var dy = 0f
            var rot = rot0
            var sx = 1f
            var sy = 1f
            var du = -1f
            if (t < 120) {
                val u = t / 120
                op = min(1f, u * 3)
                dy = -24 * (1 - u * u)
                rot = rot0 + 2 * (1 - u)
            } else {
                val a = osc((t - 120) / 1000, .35f, 38f)
                sx = 1 + .06f * a
                sy = 1 - .1f * a
                du = (t - 120) / 300
            }
            translate(0f, (i * CARD_STEP + dy) * px) {
                val bw = CARD_W * sx
                val bh = CARD_H * sy
                rotate(rot, Offset(CARD_W / 2 * px, CARD_H / 2 * px)) {
                    scaleDp(px) {
                        val body =
                            shape(
                                (CARD_W - bw) / 2,
                                CARD_H - bh,
                                bw,
                                bh,
                                cardRadii[i % 3],
                                insets,
                                1.1f,
                                11 + i * 13,
                                6f,
                            )
                        translate(4f, 4f) { drawPath(body.ink, ink, alpha = op) }
                        drawPath(body.ink, ink, alpha = op)
                        drawPath(body.fill, paper, alpha = op)
                        val stickerShape =
                            shape(
                                CARD_W - 64 - 18,
                                (CARD_H - 40) / 2 - 2,
                                64f,
                                40f,
                                ToonRadii(14f, 9f, 15f, 10f),
                                ToonInsets(1.6f, 2.08f, 3.2f, 1.76f),
                                .88f,
                                11 + i * 17 + 5,
                                4f,
                            )
                        drawPath(stickerShape.ink, ink, alpha = op)
                        drawPath(stickerShape.fill, sticker, alpha = op)
                    }
                }
                if (du > 0 && du < 1) {
                    scaleDp(px) {
                        dust(du, 34f, 86f)
                        dust(du, CARD_W - 34, 86f)
                    }
                }
            }
        }
    }

    private fun DrawScope.dust(
        u: Float,
        cx: Float,
        cy: Float,
    ) {
        val s = 36f
        val e = 1 - (1 - u).pow(2.2f)
        val fr = min(3, floor(u * 4).toInt())
        val op = 1 - u.pow(1.6f)
        dustDirs.forEachIndexed { j, d ->
            val r = (3 + 9 * sqrt(u)) * d[2] * (s / 40 * .5f + .5f)
            val x = cx + d[0] * s * e
            val y = cy + d[1] * s * .55f * e
            val g =
                shape(
                    x - r,
                    y - r,
                    2 * r,
                    2 * r,
                    ToonRadii(r, r * .9f, r, r * .85f),
                    ToonInsets(1.2f, 1.4f, 1.9f, 1.2f),
                    .7f,
                    700 + j * 7 + fr * 13,
                    3f,
                )
            drawPath(g.ink, ink, alpha = op)
            drawPath(g.fill, dustFill, alpha = op)
        }
    }
}

private inline fun DrawScope.scaleDp(
    px: Float,
    block: DrawScope.() -> Unit,
) {
    drawContext.transform.scale(px, px, Offset.Zero)
    block()
    drawContext.transform.scale(1 / px, 1 / px, Offset.Zero)
}

internal expect fun nowNanos(): Long

private fun busyWait(ms: Int) {
    val end = nowNanos() + ms * 1_000_000L
    while (nowNanos() < end) {
        // spin: the control must hold the UI thread, not yield it
    }
}
