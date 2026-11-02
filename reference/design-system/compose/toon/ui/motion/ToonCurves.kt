package toon.ui.motion

import kotlin.math.*

// Closed-form curves from toon-engine.js. Each takes elapsed ms and returns draw params,
// so a component reads one clock (withFrameMillis / Animatable<Float> of time) and draws.

fun osc(tMs: Float, zeta: Float, omega: Float): Float { val t = tMs / 1000; val wd = omega * sqrt(1 - zeta * zeta); return exp(-zeta * omega * t) * cos(wd * t) }
fun spr(tMs: Float, zeta: Float, omega: Float): Float { val t = tMs / 1000; val wd = omega * sqrt(1 - zeta * zeta); return exp(-zeta * omega * t) * (cos(wd * t) + zeta / sqrt(1 - zeta * zeta) * sin(wd * t)) }
private fun c01(v: Float) = v.coerceIn(0f, 1f)

/** Button press 80 ms ease-out-cubic to p = 1; release = spring(ζ 0.35, ω 45) from 1 → overshoots to −0.31 (scaleY 1.08). */
fun pressCurve(tMs: Float, from: Float) = from + (1 - from) * (1 - (1 - c01(tMs / 80)).pow(3))
fun releaseCurve(tMs: Float) = spr(tMs, .35f, 45f)
data class Squash(val sx: Float, val sy: Float)
fun squashOf(p: Float) = Squash(if (p > 0) 1 + .12f * p else 1 + .1f * p, if (p > 0) 1 - .15f * p else 1 - .26f * p)

data class SwitchPose(val pos: Float, val sx: Float, val sy: Float, val originRight: Boolean?, val trackOn: Boolean, val echo: Float, val wave: Float?)
/** Switch 320 ms: anticipation 40 → smear flight 100 → slap squash (ζ 0.35, ω 40); colour wave from impact 170 ms. */
fun switchPose(t: Float, from: Int, to: Int): SwitchPose {
    val d = (to - from).toFloat()
    if (t < 40) { val u = t / 40; return SwitchPose(from - d * .12f * u, 1 - .12f * u, 1 + .08f * u, null, from == 1, 0f, null) }
    if (t < 140) { val u = (t - 40) / 100; val e = u * u * (3 - 2 * u); val s = 1 + .5f * sin(PI.toFloat() * u); return SwitchPose(from - d * .12f + d * 1.12f * e, s, 1 / sqrt(s), null, from == 1, d * sin(PI.toFloat() * u), null) }
    val a = osc(t - 140, .35f, 40f); return SwitchPose(to.toFloat(), 1 - .28f * a, 1 + .2f * a, d > 0, to == 1, 0f, c01((t - 140) / 170))
}

data class CheckPose(val trim: Float, val box: Float, val dx: Float, val specks: Float, val on: Boolean)
fun checkPose(t: Float, on: Boolean): CheckPose = if (on) {
    val sq = if (t < 50) 1 - .14f * (t / 50) else 1 - .14f * osc(t - 50, .35f, 40f); val u = c01((t - 50) / 120)
    CheckPose(1 - (1 - u).pow(3), sq, 0f, c01((t - 150) / 170), true)
} else { val u = min(1f, t / 100); CheckPose(1 - u * u, 1f, if (t in 100f..160f) 1.5f * sin((t - 100) / 60 * 2 * PI.toFloat()) else 0f, 0f, t < 100) }

data class DropPose(val dotY: Float, val sx: Float, val sy: Float, val splash: Float)
/** Radio 340 ms: hold 60 above → fall 90 (stretched) → splat 1.45× wide, ζ 0.32 wobble, droplets 220 ms. */
fun radioDrop(t: Float): DropPose = when {
    t < 60 -> DropPose(-24f, .9f, 1.12f, -1f)
    t < 150 -> { val u = (t - 60) / 90; DropPose(-24 * (1 - u * u), .9f - .12f * u, 1.12f + .2f * u, -1f) }
    else -> { val a = osc(t - 150, .32f, 38f); DropPose(0f, 1 + .45f * a, 1 - .4f * a, (t - 150) / 220) }
}

data class FallPose(val alpha: Float, val dy: Float, val rot: Float, val sx: Float, val sy: Float, val dust: Float)
/** Card appear 380 ms (+40 ms per index): fall 24 dp with 2° turn, land squash, dust 300 ms. */
fun cardAppear(t: Float, rot: Float): FallPose = when {
    t < 0 -> FallPose(0f, -24f, rot + 2, 1f, 1f, -1f)
    t < 120 -> { val u = t / 120; FallPose(min(1f, u * 3), -24 * (1 - u * u), rot + 2 * (1 - u), 1f, 1f, -1f) }
    else -> { val a = osc(t - 120, .35f, 38f); FallPose(1f, 0f, rot, 1 + .06f * a, 1 - .1f * a, (t - 120) / 300) }
}
/** FAB 520 ms: snap-hold 90 ms in the air → fall 140 → land, dust. */
fun fabLand(t: Float): FallPose = when {
    t < 0 -> FallPose(0f, -70f, 0f, 1f, 1f, -1f)
    t < 90 -> FallPose(c01(t / 40), -70 + 3 * sin(t / 90 * PI.toFloat()), 0f, 1f, 1f, -1f)
    t < 230 -> { val u = (t - 90) / 140; FallPose(1f, -70 * (1 - u * u), 0f, 1 - .06f * u, 1 + .1f * u, -1f) }
    else -> { val a = osc(t - 230, .35f, 40f); FallPose(1f, 0f, 0f, 1 + .16f * a, 1 - .2f * a, (t - 230) / 300) }
}

/** Snackbar: show = spring ζ 0.48 ω 24 from +90 dp (16 dp overshoot) + two ±3° swings; hide = anticipate −8 then fall. */
fun snackShowY(t: Float) = 90 * spr(t, .48f, 24f)
fun snackSwing(t: Float) = if (t > 160) 3 * sin(2 * PI.toFloat() * (t - 160) / 150) * exp(-(t - 160) / 200) else 0f
fun snackHideY(t: Float) = if (t < 70) -8 * (1 - (1 - t / 70).pow(2)) else { val u = min(1f, (t - 70) / 170); -8 + 130 * u * u }

/** TextField: error shake ±6 dp, 3 decaying swings in 400 ms; focus label lift spring ζ 0.5 ω 32. */
fun errorShake(t: Float) = if (t < 400) 6 * sin(2 * PI.toFloat() * t / 120) * exp(-t / 180) else 0f
fun labelLift(t: Float, focused: Boolean) = if (focused) 1 - spr(t, .5f, 32f) else spr(t, .5f, 32f)

/** Dialog open 400: iris radius ease-in-out 260, scale 0.6 → 1.1 (120 ms) → spring to 1. Close 300: 1.05 then collapse, stars at 170. */
fun dialogOpenScale(t: Float) = if (t < 120) .6f + .5f * (t / 120) else 1 + .1f * osc(t - 120, .4f, 40f)
fun dialogCloseScale(t: Float) = if (t < 80) 1 + .05f * t / 80 else { val u = c01((t - 80) / 100); 1.05f * (1 - u * u) }

/** Indeterminate linear progress (1200 ms loop): head lunges twice per cycle, tail lags 20 %. Arch = 11 dp × compression^0.8. */
fun worm(t: Float): Pair<Float, Float> { val u = ((t % 1200) + 1200) % 1200 / 1200; fun j(v: Float) = v - sin(4 * PI.toFloat() * v) / (4 * PI.toFloat()) * .9f; return (-.15f + 1.35f * j(max(0f, u - .2f))) to (-.15f + 1.35f * j(u)) }

/** TopAppBar sway (pendulum ζ 0.25 ω 14) from an angular kick v0 in °/s. */
fun sway(t: Float, v0: Float): Float { val w = 14f; val z = .25f; val wd = w * sqrt(1 - z * z); return v0 / wd * exp(-z * w * t / 1000) * sin(wd * t / 1000) }

/** Badge: pop to 1.3 in 90 ms then spring; number appears 42 ms (one 24 fps frame) later. */
fun badgeScale(t: Float) = if (t < 90) 1.3f * (1 - (1 - t / 90).pow(2)) else 1 + .3f * osc(t - 90, .4f, 45f)

/** Idle: breathing 1.5 % over 2500 ms + boiling outline (3 seeds at 10 fps). Max LocalIdleBudget elements per screen. */
fun breath(t: Float) = 1 + .0075f * (1 - cos(2 * PI.toFloat() * t / 2500))
fun boilSeed(base: Int, t: Float) = base + ((t / 100).toInt() % 3) * 31
