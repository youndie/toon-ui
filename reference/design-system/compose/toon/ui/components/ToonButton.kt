package toon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import toon.ui.draw.*
import toon.ui.motion.*
import toon.ui.theme.*

// Reference implementations on compose.foundation + compose.ui only (no material / material3).
// Times come from withFrameMillis (multiplatform), never System.nanoTime.
// Pattern for every component: one time source, closed-form curves from ToonCurves.kt, geometry rebuilt per frame
// from ToonGeometry.kt (cheap: ~130 points), touch target fixed at ≥ 48 dp while the drawing animates inside it.

/** Frame clock in ms that only ticks while `running` is true. */
@Composable
fun rememberClock(running: Boolean): State<Float> {
    val t = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) { if (!running) return@LaunchedEffect; val start = withFrameMillis { it }; while (true) withFrameMillis { t.floatValue = (it - start).toFloat() } }
    return t
}

private fun DrawScope.ink(s: InkShape, t: ToonTheme, fill: androidx.compose.ui.graphics.Color, shadow: Float, inkAlpha: Float = 1f) {
    val c = t.colors
    if (shadow > 0f) translate(shadow, shadow) { drawPath(s.ink, c.ink, alpha = inkAlpha) }
    drawPath(s.ink, c.ink, alpha = inkAlpha)
    val rim = c.rim
    if (rim != null) { drawPath(s.fill, rim); translate(0f, 1.6f * density) { drawPath(s.fill, fill) } } else drawPath(s.fill, fill)
}

enum class ToonButtonVariant { Filled, Outlined, Text }

@Composable
fun ToonButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, width: Dp = 200.dp, variant: ToonButtonVariant = ToonButtonVariant.Filled, enabled: Boolean = true, idle: Boolean = false) {
    val theme = Toon.theme; val reduce = LocalReduceMotion.current; val d = LocalDensity.current.density
    var pressedAt by remember { mutableStateOf<Long?>(null) }; var releasedAt by remember { mutableStateOf<Long?>(null) }; var pFrom by remember { mutableFloatStateOf(0f) }
    var p by remember { mutableFloatStateOf(0f) }; var now by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { while (true) withFrameMillis { ms ->
        now = ms.toFloat()
        p = when { reduce -> if (pressedAt != null) 1f else 0f
            pressedAt != null -> pressCurve((ms - pressedAt!!).toFloat(), pFrom)
            releasedAt != null -> { val t = (ms - releasedAt!!).toFloat(); if (t > 500) 0f else releaseCurve(t) }
            else -> 0f }
    } }
    Box(modifier.width(width).height(52.dp)
        .semantics { role = Role.Button }
        .pointerInput(enabled) { if (!enabled) return@pointerInput; awaitEachGesture {
            awaitFirstDown(); pFrom = p; pressedAt = now.toLong()
            val up = waitForUpOrCancellation(); pressedAt = null; releasedAt = now.toLong()
            if (up != null) onClick()   // action fires on release, never waits for the animation
        } }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = theme.shape; val c = theme.colors
            val r = s.buttonRadii * (s.radiusScale * d); val ins = s.insets * d
            val br = if (idle && !reduce && enabled) breath(now) else 1f
            val seed = if (idle && !reduce) boilSeed(s.seed + 900, now) else s.seed + 900
            val g = squashed(size.width, size.height, p, r, ins, s.wobble * d, seed, br)
            if (variant != ToonButtonVariant.Text) ink(g, theme,
                if (!enabled) c.disabled else if (variant == ToonButtonVariant.Filled) c.primary else c.surface,
                if (!enabled) 0f else (if (variant == ToonButtonVariant.Filled) 1f else .6f) * s.shadow.toPx() * (1 - p.coerceIn(0f, 1f)),
                if (enabled) 1f else .35f)
        }
        // label translates with the squashed shape, never scales
        val sq = squashOf(p); val dy = (52 * (1 - sq.sy) / 2 + 2 * p.coerceIn(0f, 1f))
        BasicText(label, Modifier.offset { IntOffset(0, (dy * d).roundToInt()) }, style = theme.type.title.copy(color = if (variant == ToonButtonVariant.Filled) theme.colors.onPrimary else theme.colors.text))
    }
}

@Composable
fun ToonSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val theme = Toon.theme; val reduce = LocalReduceMotion.current; val d = LocalDensity.current.density
    var animFrom by remember { mutableStateOf<Int?>(null) }; var key by remember { mutableIntStateOf(0) }
    val clock = rememberClock(animFrom != null && !reduce)
    LaunchedEffect(key) { if (key > 0) { kotlinx.coroutines.delay(480); animFrom = null } }
    Box(modifier.size(60.dp, 48.dp)
        .semantics { role = Role.Switch; stateDescription = if (checked) "On" else "Off" }
        .pointerInput(enabled, checked) { if (!enabled) return@pointerInput; awaitEachGesture { awaitFirstDown(); if (waitForUpOrCancellation() != null) { animFrom = if (checked) 1 else 0; key++; onCheckedChange(!checked) } } }) {
        Canvas(Modifier.fillMaxSize()) {
            val s = theme.shape; val c = theme.colors
            val pose = animFrom?.let { if (reduce) null else switchPose(clock.value, it, if (checked) 1 else 0) }
            val pos = pose?.pos ?: if (checked) 1f else 0f; val trackOn = pose?.trackOn ?: checked
            val track = inkShape(0f, 7 * d, 60 * d, 34 * d, ToonShape.Radii(17f, 15f, 17f, 14f) * d, s.insets * (.75f * d), s.wobble * .7f * d, s.seed + 201, 4 * d)
            val wave = pose?.wave
            ink(track, theme, if (!enabled) c.disabled else if (wave != null && wave < 1f) (if (trackOn) c.trackOff else c.trackOn) else if (trackOn) c.trackOn else c.trackOff, if (enabled) (s.shadow.toPx() - d) else 0f, if (enabled) 1f else .35f)
            // (colour wave + echo ghosts: see toon-engine.js sw(); draw the wave as a growing pill from the impact edge)
            val thumb = inkShape(0f, 0f, 26 * d, 26 * d, ToonShape.Radii(13f, 12f, 13f, 11f) * d, ToonShape.Insets(1.8f, 2.2f, 3f, 1.8f) * d, s.wobble * .5f * d, s.seed + 301, 3 * d)
            val ox = when (pose?.originRight) { true -> 26 * d; false -> 0f; null -> 13 * d }
            withTransform({ translate((4 + pos * 26) * d, 11 * d); scale(pose?.sx ?: 1f, pose?.sy ?: 1f, Offset(ox, 13 * d)) }) {
                drawPath(thumb.ink, c.ink, alpha = if (enabled) 1f else .35f); drawPath(thumb.fill, if (enabled) c.thumb else c.disabled)
            }
        }
    }
}

// Small helper so the file has no dependency on BoxScope receivers in the snippets above.
private fun Modifier.fillMaxSize(): Modifier = this.then(androidx.compose.foundation.layout.fillMaxSize().let { Modifier.then(it) })
