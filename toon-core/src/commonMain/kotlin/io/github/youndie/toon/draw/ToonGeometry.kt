package io.github.youndie.toon.draw

import io.github.youndie.toon.InternalToonApi
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// A port of the shape half of toon-engine.js: rng, rr, wob, shape, inkLine, star, burst (B-04).
// Each function keeps the engine's name in its KDoc and its arithmetic in the same order, because
// the goldens are the engine's own output (research D3). Units are the engine's px, which are dp
// (D5). Points are flat [x0, y0, x1, y1, …] arrays in Float (D11).

/**
 * The engine's `rng`: mulberry32 seeded with [seed], the same sequence in every runtime. Kept in
 * 32-bit integer arithmetic, so the integers match the engine bit for bit; only the final
 * division is a [Double].
 */
@InternalToonApi
public class ToonRandom(
    seed: Int,
) {
    private var a = seed

    public fun next(): Double {
        a += 0x6D2B79F5
        var t = (a xor (a ushr 15)) * (1 or a)
        t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
        return (t xor (t ushr 14)).toUInt().toDouble() / 4294967296.0
    }
}

/** Four corner radii, clockwise from the top left, as the engine's `r` arrays are. */
@InternalToonApi
public data class ToonRadii(
    public val topLeft: Float,
    public val topRight: Float,
    public val bottomRight: Float,
    public val bottomLeft: Float,
)

/** Fill insets per side, as the engine's `ins` arrays are: `[top, right, bottom, left]`. */
@InternalToonApi
public data class ToonInsets(
    public val top: Float,
    public val right: Float,
    public val bottom: Float,
    public val left: Float,
)

/** An ink outline and the fill inside it; the ink between them is the variable-weight line. */
@InternalToonApi
public class ToonShapePoints(
    public val ink: FloatArray,
    public val fill: FloatArray,
)

private const val PI_F = PI.toFloat()

/** The engine's `rr`: points every [step] along a rectangle with four independent radii. */
@InternalToonApi
public fun roundRectPoints(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    r: ToonRadii,
    step: Float,
): FloatArray {
    val out = ArrayList<Float>(512)

    fun seg(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
    ) {
        val n = max(1, (hypot(x1 - x0, y1 - y0) / step).roundToInt())
        for (i in 0 until n) {
            val t = i.toFloat() / n
            out += x0 + (x1 - x0) * t
            out += y0 + (y1 - y0) * t
        }
    }

    fun arc(
        cx: Float,
        cy: Float,
        rad: Float,
        a0: Float,
        a1: Float,
    ) {
        val n = max(2, (abs(a1 - a0) * rad / step).roundToInt())
        for (i in 0 until n) {
            val a = a0 + (a1 - a0) * i / n
            out += cx + rad * cos(a)
            out += cy + rad * sin(a)
        }
    }
    val m = max(1f, min(w, h) / 2)
    val tl = r.topLeft.coerceIn(1f, max(1f, m))
    val tr = r.topRight.coerceIn(1f, max(1f, m))
    val br = r.bottomRight.coerceIn(1f, max(1f, m))
    val bl = r.bottomLeft.coerceIn(1f, max(1f, m))
    seg(x + tl, y, x + w - tr, y)
    arc(x + w - tr, y + tr, tr, -PI_F / 2, 0f)
    seg(x + w, y + tr, x + w, y + h - br)
    arc(x + w - br, y + h - br, br, 0f, PI_F / 2)
    seg(x + w - br, y + h, x + bl, y + h)
    arc(x + bl, y + h - bl, bl, PI_F / 2, PI_F)
    seg(x, y + h - bl, x, y + tl)
    arc(x + tl, y + tl, tl, PI_F, 1.5f * PI_F)
    return out.toFloatArray()
}

/**
 * The engine's `wob`: each point of [roundRectPoints] pushed along its normal by a three-harmonic
 * offset whose phases come from [seed]. Low harmonics, so the line looks drawn, not noisy.
 */
@InternalToonApi
public fun wobblePoints(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    r: ToonRadii,
    amp: Float,
    seed: Int,
    step: Float = 6f,
): FloatArray {
    val rnd = ToonRandom(seed)
    val ph0 = (rnd.next() * 6.28).toFloat()
    val ph1 = (rnd.next() * 6.28).toFloat()
    val ph2 = (rnd.next() * 6.28).toFloat()
    val pts = roundRectPoints(x, y, w, h, r, step)
    val n = pts.size / 2
    val out = FloatArray(pts.size)
    for (i in 0 until n) {
        val a = (i - 1 + n) % n
        val b = (i + 1) % n
        var nx = pts[2 * b + 1] - pts[2 * a + 1]
        var ny = -(pts[2 * b] - pts[2 * a])
        val l = hypot(nx, ny).takeIf { it != 0f } ?: 1f
        nx /= l
        ny /= l
        val s = i.toFloat() / n * 6.2832f
        val d = amp * (0.6f * sin(3 * s + ph0) + 0.3f * sin(7 * s + ph1) + 0.22f * sin(13 * s + ph2))
        out[2 * i] = pts[2 * i] + nx * d
        out[2 * i + 1] = pts[2 * i + 1] + ny * d
    }
    return out
}

/**
 * The engine's `shape`: the outline, and a fill inset by [ins] per side with radii shrunk by the
 * mean of the top and left insets, wobbling at 0.8 of [amp] from the next seed but six.
 */
@InternalToonApi
public fun shapePoints(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    r: ToonRadii,
    ins: ToonInsets,
    amp: Float,
    seed: Int,
    step: Float = 6f,
): ToonShapePoints {
    val shrink = (ins.top + ins.left) / 2
    val ri =
        ToonRadii(
            max(2f, r.topLeft - shrink),
            max(2f, r.topRight - shrink),
            max(2f, r.bottomRight - shrink),
            max(2f, r.bottomLeft - shrink),
        )
    return ToonShapePoints(
        ink = wobblePoints(x, y, w, h, r, amp, seed, step),
        fill =
            wobblePoints(
                x + ins.left,
                y + ins.top,
                max(1f, w - ins.left - ins.right),
                max(1f, h - ins.top - ins.bottom),
                ri,
                amp * 0.8f,
                seed + 7,
                step,
            ),
    )
}

/**
 * The engine's `inkLine`: a tapering stroke from [x0] to [x1], thickest in the middle, as a closed
 * outline — the top edge left to right, then the bottom edge back. [sag] bows it downward.
 */
@InternalToonApi
public fun inkLinePoints(
    x0: Float,
    x1: Float,
    y: Float,
    th: Float,
    seed: Int,
    sag: Float = 0f,
): FloatArray {
    val ph = (ToonRandom(seed).next() * 6).toFloat()
    val n = 28
    val top = FloatArray(2 * (n + 1))
    val bot = FloatArray(2 * (n + 1))
    for (i in 0..n) {
        val u = i.toFloat() / n
        val x = x0 + (x1 - x0) * u
        val t = th * (0.25f + 0.75f * sin(PI_F * u)) * (1 + 0.2f * sin(9 * u + ph))
        val wave = 0.8f * sin(5 * u + ph) + sag * sin(PI_F * u)
        top[2 * i] = x
        top[2 * i + 1] = y + wave - t * 0.4f
        bot[2 * i] = x
        bot[2 * i + 1] = y + wave + t * 0.6f
    }
    val out = FloatArray(top.size + bot.size)
    top.copyInto(out)
    for (i in 0..n) {
        out[top.size + 2 * i] = bot[2 * (n - i)]
        out[top.size + 2 * i + 1] = bot[2 * (n - i) + 1]
    }
    return out
}

/** The engine's `star`: ten points about the origin, outer radius [outer], inner [inner], jittered by [seed]. */
@InternalToonApi
public fun starPoints(
    outer: Float,
    inner: Float,
    seed: Int,
): FloatArray {
    val rnd = ToonRandom(seed)
    val out = FloatArray(20)
    for (i in 0 until 10) {
        val a = -PI_F / 2 + i * PI_F / 5
        val rr = (if (i % 2 == 1) inner else outer) * (0.88f + 0.24f * rnd.next().toFloat())
        out[2 * i] = cos(a) * rr
        out[2 * i + 1] = sin(a) * rr
    }
    return out
}

/** The engine's `burst`: a [spikes]-pointed jagged ellipse about the origin, inner points at [k] of the outer. */
@InternalToonApi
public fun burstPoints(
    rx: Float,
    ry: Float,
    spikes: Int,
    seed: Int,
    k: Float = 0.76f,
): FloatArray {
    val rnd = ToonRandom(seed)
    val out = FloatArray(4 * spikes)
    for (i in 0 until 2 * spikes) {
        val a = -PI_F / 2 + i * PI_F / spikes
        val outerPoint = i % 2 == 0
        val j = 0.9f + 0.2f * rnd.next().toFloat()
        out[2 * i] = cos(a) * (if (outerPoint) rx else rx * k) * j
        out[2 * i + 1] = sin(a) * (if (outerPoint) ry else ry * k) * j
    }
    return out
}
