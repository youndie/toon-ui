package io.github.youndie.toon.fx

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonInsets
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonRandom
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.polygonPath
import io.github.youndie.toon.draw.roundRectPoints
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.smoothPath
import io.github.youndie.toon.draw.starPoints
import io.github.youndie.toon.motion.toonSpring
import io.github.youndie.toon.tokens.ToonColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

// A port of the engine's FX library (B-12): `dust`, `stars`, `speed`, `flash`, `vibe`, `irisAt` with
// `irisScene`'s wipe, `hole` and `sweat`, plus the `shake` curve. Each
// function draws one frame from its life `u` in [0, 1) and draws nothing outside it, as the engine
// returns an empty scene there. Arguments are the engine's, in dp; the arithmetic keeps its order.
// `spr` lives with the other shared curves in motion/ToonCurves.kt.

private fun c01(v: Float): Float = max(0f, min(1f, v))

/** The engine's `shake`: the struck element's sideways offset in dp at [t] ms; zero from 400 ms. */
internal fun toonShake(t: Float): Float = if (t < 400) 6 * sin(2 * PI.toFloat() * t / 120) * exp(-t / 180) else 0f

/** One frame of the iris: the radius as a share [k] of the full one, and which [scene] shows. */
internal class ToonIrisFrame(
    val k: Float,
    val scene: Int,
)

/** The engine's `irisAt`: closes ease-in in 220 ms, holds 80 on ink, opens with an overshoot. */
internal fun toonIrisAt(t: Float): ToonIrisFrame {
    if (t < 220) {
        val u = t / 220
        return ToonIrisFrame(1 - u * u * u, 0)
    }
    if (t < 300) return ToonIrisFrame(0f, 1)
    val u = c01((t - 300) / 300)
    return ToonIrisFrame(min(1.04f, 1 - toonSpring(u * .3f, .55f, 22f)), 1)
}

/** The engine's `vibe` offset: the buzzing element's x in dp, flipping every 40 ms of 320. */
internal fun toonVibeOffset(u: Float): Float {
    if (u <= 0 || u >= 1) return 0f
    val ph = floor(u * 8).toInt() % 2
    return (if (ph == 1) 2.2f else -2.2f) * (1 - u)
}

private val DUST_DIRS =
    listOf(
        floatArrayOf(-1f, -.25f, 1f),
        floatArrayOf(-.55f, -.85f, .8f),
        floatArrayOf(.05f, -1f, .6f),
        floatArrayOf(.55f, -.8f, .85f),
        floatArrayOf(1f, -.3f, 1f),
    )

/**
 * The engine's `dust` as frame layers: puffs fanning out sideways and up from ([cx], [cy]) over spread
 * [s], one per row of [dirs] (x, y, size), five by default. [k] scales the puffs and [gap] pushes each
 * side's out, the options the navigation bar's landing passes (`o.k`, `o.gap`, `o.dirs`).
 */
@OptIn(InternalToonApi::class)
internal fun toonDustScene(
    colors: ToonColors,
    u: Float,
    cx: Float,
    cy: Float,
    s: Float = 40f,
    k: Float = 1f,
    gap: Float = 0f,
    dirs: List<FloatArray> = DUST_DIRS,
): ToonScene {
    if (u <= 0 || u >= 1) return ToonScene.Empty
    val e = 1 - (1 - u).pow(2.2f)
    val fr = min(3, floor(u * 4).toInt())
    val layers = mutableListOf<ToonLayer>()
    dirs.forEachIndexed { j, (dx, dy, size) ->
        val r = (3 + 9 * sqrt(u)) * size * (s / 40 * .5f + .5f) * k
        val op = 1 - u.pow(1.6f)
        val px = cx + sign(dx) * gap + dx * s * e
        val py = cy + dy * s * .55f * e
        val g =
            shapePoints(
                px - r,
                py - r,
                2 * r,
                2 * r,
                ToonRadii(r, r * .9f, r, r * .85f),
                ToonInsets(1.2f, 1.4f, 1.9f, 1.2f),
                .7f,
                700 + j * 7 + fr * 13,
                3f,
            )
        layers += smoothLayer(g.ink, colors.ink, op)
        layers += smoothLayer(g.fill, colors.fxDust, op)
    }
    return ToonScene(layers, emptyList())
}

/** The engine's `dust`, drawn: [toonDustScene]. */
internal fun DrawScope.drawToonDust(
    colors: ToonColors,
    u: Float,
    cx: Float,
    cy: Float,
    s: Float = 40f,
    k: Float = 1f,
    gap: Float = 0f,
    dirs: List<FloatArray> = DUST_DIRS,
) = drawScene(toonDustScene(colors, u, cx, cy, s, k, gap, dirs))

/** The engine's `stars`: three stars popping on an arc of radius [s] over ([cx], [cy]), spinning out. */
@OptIn(InternalToonApi::class)
internal fun DrawScope.drawToonStars(
    colors: ToonColors,
    u: Float,
    cx: Float,
    cy: Float,
    s: Float = 34f,
) {
    if (u <= 0 || u >= 1) return
    val fr = min(3, floor(u * 4).toInt())
    floatArrayOf(-150f, -90f, -30f).forEachIndexed { j, a ->
        val ar = a * PI.toFloat() / 180
        val e = 1 - (1 - u).pow(3)
        val rr = s * (.35f + .65f * e)
        val px = cx + cos(ar) * rr * 1.3f
        val py = cy + sin(ar) * rr
        val sc = if (u < .25f) (u / .25f) * 1.15f else 1.15f - .5f * (u - .25f) / .75f
        val op = if (u < .6f) 1f else 1 - (u - .6f) / .4f
        translate(px, py) {
            rotate(200 * u * (if (j % 2 == 1) -1 else 1), Offset.Zero) {
                drawPath(
                    polygonPath(starPoints(9 * sc + 2.8f, 4.2f * sc + 2.4f, 40 + j + fr * 5)),
                    colors.ink,
                    alpha = op,
                )
                drawPath(polygonPath(starPoints(9 * sc, 4.2f * sc, 40 + j + fr * 5)), colors.warning, alpha = op)
            }
        }
    }
}

/** The engine's `speed`: five tapering strokes trailing left from [x], over [y]…[y] + [h]. */
internal fun DrawScope.drawToonSpeed(
    colors: ToonColors,
    u: Float,
    x: Float,
    y: Float,
    h: Float,
) {
    if (u < 0 || u >= 1) return
    floatArrayOf(.15f, .32f, .5f, .68f, .85f).forEachIndexed { j, k ->
        val len = (50 + 40 * ((j * 7) % 3)) * (1 - u)
        val yy = y + h * k
        val x0 = x - 10
        drawPath(polygonPath(floatArrayOf(x0, yy - 1.8f, x0 - len, yy, x0, yy + 1.8f)), colors.fxLine, alpha = 1 - u)
    }
}

private val FLASH_KEYS =
    arrayOf(
        floatArrayOf(0f, .25f, .55f),
        floatArrayOf(.18f, 1.15f, .42f),
        floatArrayOf(.4f, 1f, .6f),
        floatArrayOf(.7f, .72f, .78f),
        floatArrayOf(1f, .2f, .92f),
    )

/** The engine's `lerpK`: [keys] rows of (u, values…), linear between the two rows around [u]. */
private fun lerpK(
    keys: Array<FloatArray>,
    u: Float,
): FloatArray {
    var i = 0
    while (i < keys.size - 2 && u > keys[i + 1][0]) i++
    val a = keys[i]
    val b = keys[i + 1]
    val v = c01((u - a[0]) / ((b[0] - a[0]).takeIf { it != 0f } ?: 1f))
    return FloatArray(a.size) { j -> a[j] + (b[j] - a[j]) * v }
}

/** The jitter of the flash's 24 points: one fixed sequence, seed 77, so every frame shares a grid. */
private val FLASH_JITTER = ToonRandom(77).let { rnd -> FloatArray(24) { .82f + .36f * rnd.next().toFloat() } }

/**
 * The engine's `flash`: a jagged burst of 12 spikes about ([cx], [cy]), radius [s], with a paper-white
 * core for the first half of its life. [opacity] multiplies the engine's own fade (reduce motion).
 */
internal fun DrawScope.drawToonFlash(
    colors: ToonColors,
    u: Float,
    cx: Float,
    cy: Float,
    s: Float = 44f,
    opacity: Float = 1f,
) {
    if (u <= 0 || u >= 1) return
    val n = 12
    val (_, sc, kr) = lerpK(FLASH_KEYS, u)
    val op = (if (u > .7f) 1 - (u - .7f) / .3f else 1f) * opacity
    val rot = u * .35f

    fun pts(
        add: Float,
        mul: Float,
    ) = FloatArray(4 * n).also { p ->
        for (i in 0 until 2 * n) {
            val a = rot - PI.toFloat() / 2 + i * PI.toFloat() / n
            val r = ((if (i % 2 == 1) kr else 1f) * s * sc * FLASH_JITTER[i]) * mul + add
            p[2 * i] = cx + cos(a) * r
            p[2 * i + 1] = cy + sin(a) * r
        }
    }
    drawPath(polygonPath(pts(3.5f, 1f)), colors.ink, alpha = op)
    drawPath(polygonPath(pts(0f, 1f)), colors.warning, alpha = op)
    if (u < .5f) drawPath(polygonPath(pts(0f, .5f)), colors.surface, alpha = op * (1 - u / .5f))
}

/** The engine's `vibe` arcs: two pairs of buzz strokes either side of the element at [x], [y], [w], [h]. */
internal fun DrawScope.drawToonVibe(
    colors: ToonColors,
    u: Float,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
) {
    if (u <= 0 || u >= 1) return
    val ph = floor(u * 8).toInt() % 2
    val op = 1 - u * u
    val cy = y + h / 2
    val stroke = Stroke(width = 2.6f, cap = StrokeCap.Round)
    floatArrayOf(1f, .6f).forEachIndexed { k, sz ->
        val off = 8 + k * 7 + (if (ph == 1) 2 else 0)
        val hh = h * .34f * sz
        val left =
            Path().apply {
                moveTo(x - off, cy - hh)
                quadraticTo(x - off - 4, cy, x - off, cy + hh)
            }
        val right =
            Path().apply {
                moveTo(x + w + off, cy - hh)
                quadraticTo(x + w + off + 4, cy, x + w + off, cy + hh)
            }
        drawPath(left, colors.fxLine, alpha = op, style = stroke)
        drawPath(right, colors.fxLine, alpha = op, style = stroke)
    }
}

/**
 * The engine's iris wipe from `irisScene`: ink over a [w] × [h] screen with a hole of radius [k] of the
 * full one about ([cx], [cy]), and two faint rings following its edge. The scenes either side of it
 * are the screens' own (B-44).
 */
internal fun DrawScope.drawToonIris(
    colors: ToonColors,
    k: Float,
    w: Float,
    h: Float,
    cx: Float,
    cy: Float,
) {
    val rMax = hypot(max(cx, w - cx), max(cy, h - cy)) + 8
    val r = max(0f, rMax * k)
    if (r >= rMax) return
    val mask =
        Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(0f, 0f, w, h))
            addOval(Rect(Offset(cx, cy), r))
        }
    drawPath(mask, colors.ink)
    floatArrayOf(6f, 14f).forEachIndexed { j, d ->
        drawCircle(
            colors.rim ?: colors.surface,
            radius = r + d,
            center = Offset(cx, cy),
            alpha = (if (j == 1) .35f else .6f) * c01(r / 20),
            style = Stroke(width = if (j == 1) .8f else 1.4f),
        )
    }
}

/** The hole's silhouette at one frame: the cracked [ink] polygon and the paper [edge] 2.6 dp outside it. */
internal class ToonHoleOutline(
    val ink: FloatArray,
    val edge: FloatArray,
)

/**
 * The silhouette of the engine's `hole` at life [u]: the element's outline at [x], [y], [w], [h] with
 * corner radii [r], cracked every seventh point by [seed], scaled about its centre.
 */
@OptIn(InternalToonApi::class)
internal fun toonHoleOutline(
    u: Float,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    r: ToonRadii,
    seed: Int,
): ToonHoleOutline {
    val rnd = ToonRandom(seed)
    val pts = roundRectPoints(x, y, w, h, r, 4f)
    val n = pts.size / 2
    val cx = x + w / 2
    val cy = y + h / 2
    val sc = if (u < .15f) 1f else 1 - .55f * ((u - .15f) / .85f).pow(2)
    val p = FloatArray(2 * n)
    for (i in 0 until n) {
        val dx = pts[2 * i] - cx
        val dy = pts[2 * i + 1] - cy
        val l = hypot(dx, dy).takeIf { it != 0f } ?: 1f
        val spike =
            when (i % 7) {
                3 -> 4 + 5 * rnd.next().toFloat()
                2, 4 -> 1.2f
                else -> -.6f * rnd.next().toFloat()
            }
        p[2 * i] = cx + (dx + dx / l * spike) * sc
        p[2 * i + 1] = cy + (dy + dy / l * spike) * sc
    }
    val p2 =
        FloatArray(2 * n).also { q ->
            for (i in 0 until n) {
                val dx = p[2 * i] - cx
                val dy = p[2 * i + 1] - cy
                val l = hypot(dx, dy).takeIf { it != 0f } ?: 1f
                q[2 * i] = p[2 * i] + dx / l * 2.6f
                q[2 * i + 1] = p[2 * i + 1] + dy / l * 2.6f
            }
        }
    return ToonHoleOutline(p, p2)
}

/**
 * The engine's `hole`: [toonHoleOutline] holding then shrinking to 45 % and fading, while four chips
 * fly off.
 */
internal fun DrawScope.drawToonHole(
    colors: ToonColors,
    u: Float,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    r: ToonRadii,
    seed: Int = 5,
) {
    if (u < 0 || u >= 1) return
    val cx = x + w / 2
    val cy = y + h / 2
    val op = if (u < .6f) 1f else 1 - (u - .6f) / .4f
    val outline = toonHoleOutline(u, x, y, w, h, r, seed)
    drawPath(polygonPath(outline.edge), colors.holeEdge, alpha = op)
    drawPath(polygonPath(outline.ink), colors.ink, alpha = op)
    val e = 1 - (1 - c01(u * 1.4f)).pow(2)
    HOLE_CHIPS.forEachIndexed { j, (dx, dy) ->
        val px = cx + dx * (w / 2 + 6 + 34 * e)
        val py = cy + dy * (h / 2 + 4 + 26 * e) + 30 * e * e
        val rot = (if (j % 2 == 1) 1 else -1) * 260 * u
        val s2 = 4 + j % 2 * 1.5f
        val chip = polygonPath(floatArrayOf(-s2, -s2 * .6f, s2, -s2 * .9f, s2 * .5f, s2))
        translate(px, py) {
            rotate(rot, Offset.Zero) {
                scale(1.5f, Offset.Zero) { drawPath(chip, colors.ink, alpha = op * (1 - u)) }
                drawPath(chip, colors.holeEdge, alpha = op * (1 - u))
            }
        }
    }
}

private val HOLE_CHIPS = listOf(-.9f to -.6f, .8f to -.8f, 1f to .3f, -1f to .4f)

private val SWEAT_DROP =
    PathParser()
        .parsePathString(
            "M0 -7 C2 -3 5 0 5 3 C5 6 2.6 8 0 8 C-2.6 8 -5 6 -5 3 C-5 0 -2 -3 0 -7 Z",
        ).toPath()
private val SWEAT_SHINE = PathParser().parsePathString("M-1.8 1.2 A1.2 1.8 -20 1 1 -1.79 1.2 Z").toPath()
private val SWEAT_DROPS =
    listOf(floatArrayOf(.12f, -1f, 0f, 1f), floatArrayOf(.88f, 1f, 0f, .9f), floatArrayOf(.98f, 1f, .18f, .75f))

/** The engine's `sweat`: three drops springing off the top of the element at [x], [y], width [w]. */
internal fun DrawScope.drawToonSweat(
    colors: ToonColors,
    u: Float,
    x: Float,
    y: Float,
    w: Float,
) {
    if (u <= 0 || u >= 1) return
    for ((at, dir, dl, k) in SWEAT_DROPS) {
        val v = c01((u - dl) / (1 - dl))
        if (v <= 0 || v >= 1) continue
        val sz = (if (v < .2f) v / .2f else 1f) * k
        val px = x + w * at + dir * 20 * v
        val py = y - 6 - 16 * sin(PI.toFloat() * min(1f, v * 1.1f)) + 30 * max(0f, v - .45f).pow(2) * 4
        val rot = dir * (35 - 70 * v)
        val st = 1 + .45f * max(0f, v - .5f)
        val op = if (v > .8f) 1 - (v - .8f) / .2f else 1f
        translate(px, py) {
            rotate(rot, Offset.Zero) {
                scale(sz / sqrt(st), sz * st, Offset.Zero) {
                    scale(1.32f, Offset.Zero) { drawPath(SWEAT_DROP, colors.ink, alpha = op) }
                    drawPath(SWEAT_DROP, colors.fxSweat, alpha = op)
                    drawPath(SWEAT_SHINE, colors.surface, alpha = op * .9f)
                }
            }
        }
    }
}
