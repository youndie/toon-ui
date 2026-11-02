package toon.ui.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import kotlin.math.*
import toon.ui.theme.ToonShape

// 1:1 port of rr / wob / shape / inkLine / iconOutline from toon-engine.js. Units are dp × density (px).

/** mulberry32 — must match the JS rng exactly so seeds produce the same drawing on every platform. */
class Mulberry32(seed: Int) {
    private var a = seed
    fun next(): Float {
        a += 0x6D2B79F5
        var t = (a xor (a ushr 15)) * (1 or a)
        t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t
        return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toFloat() / 4294967296f
    }
}

/** Points along a rounded rect with four independent radii, clockwise from the top-left straight. */
fun roundRectPoints(x: Float, y: Float, w: Float, h: Float, r: ToonShape.Radii, step: Float): List<Offset> {
    val pts = ArrayList<Offset>(256)
    fun seg(x0: Float, y0: Float, x1: Float, y1: Float) { val n = max(1, (hypot(x1 - x0, y1 - y0) / step).roundToInt()); for (i in 0 until n) { val t = i.toFloat() / n; pts += Offset(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t) } }
    fun arc(cx: Float, cy: Float, rad: Float, a0: Float, a1: Float) { val n = max(2, (abs(a1 - a0) * rad / step).roundToInt()); for (i in 0 until n) { val a = a0 + (a1 - a0) * i / n; pts += Offset(cx + rad * cos(a), cy + rad * sin(a)) } }
    val m = max(1f, min(w, h) / 2)
    val tl = r.tl.coerceIn(1f, m); val tr = r.tr.coerceIn(1f, m); val br = r.br.coerceIn(1f, m); val bl = r.bl.coerceIn(1f, m)
    val P = PI.toFloat()
    seg(x + tl, y, x + w - tr, y); arc(x + w - tr, y + tr, tr, -P / 2, 0f)
    seg(x + w, y + tr, x + w, y + h - br); arc(x + w - br, y + h - br, br, 0f, P / 2)
    seg(x + w - br, y + h, x + bl, y + h); arc(x + bl, y + h - bl, bl, P / 2, P)
    seg(x, y + h - bl, x, y + tl); arc(x + tl, y + tl, tl, P, 1.5f * P)
    return pts
}

/** Closed quadratic-midpoint path through the points (same smoothing as JS `smooth`). */
fun smoothClosed(pts: List<Offset>): Path = Path().apply {
    val n = pts.size; fun mid(a: Offset, b: Offset) = Offset((a.x + b.x) / 2, (a.y + b.y) / 2)
    val m0 = mid(pts[n - 1], pts[0]); moveTo(m0.x, m0.y)
    for (i in 0 until n) { val nx = mid(pts[i], pts[(i + 1) % n]); quadraticBezierTo(pts[i].x, pts[i].y, nx.x, nx.y) }
    close()
}

/** The wobble: each point is pushed along its normal by a 3-harmonic periodic offset. Low frequency → hand-drawn, not noisy. */
fun wobblePath(x: Float, y: Float, w: Float, h: Float, r: ToonShape.Radii, amp: Float, seed: Int, step: Float = 6f): Path {
    val rnd = Mulberry32(seed); val ph = FloatArray(3) { rnd.next() * 6.28f }
    val pts = roundRectPoints(x, y, w, h, r, step); val n = pts.size
    val out = pts.mapIndexed { i, p ->
        val a = pts[(i - 1 + n) % n]; val b = pts[(i + 1) % n]
        var nx = b.y - a.y; var ny = -(b.x - a.x); val l = hypot(nx, ny).takeIf { it > 0 } ?: 1f; nx /= l; ny /= l
        val s = i.toFloat() / n * 6.2832f
        val d = amp * (0.6f * sin(3 * s + ph[0]) + 0.3f * sin(7 * s + ph[1]) + 0.22f * sin(13 * s + ph[2]))
        Offset(p.x + nx * d, p.y + ny * d)
    }
    return smoothClosed(out)
}

/** Outline + fill pair. The fill is inset by different amounts per side, so the ink shows as a variable-weight line. */
class InkShape(val ink: Path, val fill: Path)

fun inkShape(x: Float, y: Float, w: Float, h: Float, r: ToonShape.Radii, ins: ToonShape.Insets, amp: Float, seed: Int, step: Float = 6f): InkShape {
    val inset = (ins.top + ins.left) / 2
    val ri = ToonShape.Radii(max(2f, r.tl - inset), max(2f, r.tr - inset), max(2f, r.br - inset), max(2f, r.bl - inset))
    return InkShape(
        wobblePath(x, y, w, h, r, amp, seed, step),
        wobblePath(x + ins.left, y + ins.top, max(1f, w - ins.left - ins.right), max(1f, h - ins.top - ins.bottom), ri, amp * 0.8f, seed + 7, step),
    )
}

/**
 * Squash keeps the stroke weight: rebuild the shape on squashed geometry (anchored to the bottom edge)
 * instead of scaling the layer, and thicken the bottom inset up to 1.3×.
 */
fun squashed(w: Float, h: Float, p: Float, r: ToonShape.Radii, ins: ToonShape.Insets, amp: Float, seed: Int, breath: Float = 1f): InkShape {
    val sy = (if (p > 0) 1 - .15f * p else 1 - .26f * p) * breath
    val sx = (if (p > 0) 1 + .12f * p else 1 + .1f * p) * breath
    val bw = w * sx; val bh = h * sy
    return inkShape((w - bw) / 2, h - bh, bw, bh, r, ins.copy(bottom = ins.bottom * (1 + .3f * p.coerceIn(0f, 1f))), amp, seed)
}

/** Tapering ink stroke (Divider, Tabs shelf, ground lines). `sag` bows it downward for the pressed List item. */
fun inkLine(x0: Float, x1: Float, y: Float, th: Float, seed: Int, sag: Float = 0f): Path {
    val ph = Mulberry32(seed).next() * 6; val n = 28
    val top = ArrayList<Offset>(); val bot = ArrayList<Offset>()
    for (i in 0..n) { val u = i.toFloat() / n; val x = x0 + (x1 - x0) * u
        val t = th * (.25f + .75f * sin(PI.toFloat() * u)) * (1 + .2f * sin(9 * u + ph)); val w = .8f * sin(5 * u + ph) + sag * sin(PI.toFloat() * u)
        top += Offset(x, y + w - t * .4f); bot += Offset(x, y + w + t * .6f) }
    return Path().apply { moveTo(top[0].x, top[0].y); top.drop(1).forEach { lineTo(it.x, it.y) }; bot.asReversed().forEach { lineTo(it.x, it.y) }; close() }
}

/**
 * Icon renderer: one centerline per subpath in, filled variable-width outline out.
 * width(y, tangent) = base · (0.72 + 0.42·low + 0.3·horizontal²) — thicker low in the 24 grid and on horizontals.
 * Cache the result per (icon, size, weight); it's static.
 */
fun iconOutline(centerline: Path, base: Float, closed: Boolean, grid: Float = 24f, step: Float = .3f): Path {
    val pm = PathMeasure().apply { setPath(centerline, false) }
    val len = pm.length; val out = Path()
    if (len < .5f) { val c = pm.getPosition(0f); out.addOval(androidx.compose.ui.geometry.Rect(c.x - base * 1.05f, c.y - base * .95f, c.x + base * 1.05f, c.y + base * .95f)); return out }
    val n = max(10, ceil(len / step).toInt()); val pts = (0..n).map { pm.getPosition(len * it / n) }.let { if (closed) it.dropLast(1) else it }
    val N = pts.size; val left = ArrayList<Offset>(N); val right = ArrayList<Offset>(N); val wid = FloatArray(N); val tan = ArrayList<Offset>(N)
    for (i in 0 until N) {
        val a = if (closed) pts[(i - 1 + N) % N] else pts[max(0, i - 1)]; val b = if (closed) pts[(i + 1) % N] else pts[min(N - 1, i + 1)]
        var tx = b.x - a.x; var ty = b.y - a.y; val l = hypot(tx, ty).takeIf { it > 0 } ?: 1f; tx /= l; ty /= l
        val low = ((pts[i].y / grid * 24 - 4) / 16).coerceIn(0f, 1f)
        val taper = if (closed) 1f else min(1f, .6f + .4f * min(i, N - 1 - i) / 6f)
        val w = base * (.72f + .42f * low + .3f * tx * tx) / 2 * taper
        left += Offset(pts[i].x - ty * w, pts[i].y + tx * w); right += Offset(pts[i].x + ty * w, pts[i].y - tx * w); wid[i] = w; tan += Offset(tx, ty)
    }
    fun poly(p: List<Offset>) { out.moveTo(p[0].x, p[0].y); p.drop(1).forEach { out.lineTo(it.x, it.y) }; out.close() }
    if (closed) { poly(left); poly(right.asReversed()); return out }
    fun cap(i: Int, sgn: Float) = (1 until 8).map { j -> val a = j / 8f * PI.toFloat(); val t = tan[i]; val nx = -t.y; val ny = t.x
        Offset(pts[i].x + wid[i] * (cos(a) * nx * sgn + sin(a) * t.x * sgn), pts[i].y + wid[i] * (cos(a) * ny * sgn + sin(a) * t.y * sgn)) }
    poly(left + cap(N - 1, 1f) + right.asReversed() + cap(0, -1f))
    return out
}
