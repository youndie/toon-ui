package io.github.youndie.toon.motion

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

// The engine's shared curves (D7): closed forms of elapsed time, read by several components.

/** The engine's `spr`: a damped spring's displacement at [t] seconds, damping [z], frequency [w]. */
internal fun toonSpring(
    t: Float,
    z: Float,
    w: Float,
): Float {
    val wd = w * sqrt(1 - z * z)
    return exp(-z * w * t) * (cos(wd * t) + z / sqrt(1 - z * z) * sin(wd * t))
}

/** The engine's `osc`: a damped oscillation at [t] seconds, damping [z], frequency [w]; 1 at rest. */
internal fun toonOscillation(
    t: Float,
    z: Float,
    w: Float,
): Float {
    val wd = w * sqrt(1 - z * z)
    return exp(-z * w * t) * cos(wd * t)
}

/** A squash: horizontal and vertical scale. */
internal class ToonSquash(
    val sx: Float,
    val sy: Float,
)

/**
 * The engine's `squashOf`: press [p] from −1 (release overshoot) to 1 (fully pressed) as a squash
 * onto the bottom edge, wider and lower when pressed, taller and narrower on the overshoot.
 */
internal fun toonSquashOf(p: Float): ToonSquash =
    ToonSquash(
        sx = if (p > 0) 1 + .12f * p else 1 + .1f * p,
        sy = if (p > 0) 1 - .15f * p else 1 - .26f * p,
    )
