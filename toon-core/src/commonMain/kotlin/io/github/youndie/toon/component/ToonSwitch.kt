package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonAnchor
import io.github.youndie.toon.draw.ToonInsets
import io.github.youndie.toon.draw.ToonLabel
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonSegments
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.ringLayer
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.smoothNumbers
import io.github.youndie.toon.draw.wobblePoints
import io.github.youndie.toon.interaction.rememberToonPressState
import io.github.youndie.toon.interaction.toonPressable
import io.github.youndie.toon.motion.toonOscillation
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonSize
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// A port of the engine's `sw` and `swAnim` and the kit's `C.switch` (B-18): the thumb pulls back for
// 40 ms, flies with a 1.5× smear and two fading echoes, slaps the far edge and squashes against it,
// and the new track colour spreads from the impact edge as a wave.

/**
 * The design's switch: [label] on the left, the 60 × 34 track on the right of [width] (0 fits the
 * label). A click toggles it and reports the new state to [onCheckedChange]; [checked] from outside
 * sets it, as the kit's `checked` prop does.
 */
@Composable
public fun ToonSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    width: Dp = 0.dp,
    enabled: Boolean = true,
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val text = rememberToonText()
    val labelWidth =
        remember(label, text) { if (label.isEmpty()) 0f else text.measure(label, CHECK_LABEL).multiParagraph.width }
    val w = if (width > 0.dp) width.value else (if (label.isEmpty()) 0f else ceil(labelWidth) + 16) + TRACK_W
    var on by remember(checked) { mutableStateOf(checked) }
    val press = rememberToonPressState()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val clock = remember { ToonToggleClock() }
    val change by rememberUpdatedState(onCheckedChange)
    LaunchedEffect(clock.starts) { while (clock.running(TOGGLE_MS)) withFrameMillis { clock.tick(it) } }
    val look = toonSwitchFrameAt(on, clock.age(), press.progress, reduce)

    Canvas(
        modifier
            .size(w.dp, ToonSize.TARGET_MIN.dp)
            .semantics {
                toggleableState = ToggleableState(on)
                if (label.isNotEmpty()) this.text = AnnotatedString(label)
            }.toonPressable(press, enabled = enabled, role = Role.Switch) {
                on = !on
                clock.start()
                change(on)
            }.focusable(enabled, interaction),
    ) {
        // The engine draws in dp (research D5); the canvas is pixels (B-47).

        inDp {
            drawScene(toonSwitchRowScene(tokens, label, w, look.copy(disabled = !enabled, focus = focused)), text)
        }
    }
}

private const val TRACK_W = 60f

/** The kit runs a toggle for 520 ms and its curve for 480. */
private const val TOGGLE_MS = 520f
private const val ANIM_MS = 480f

/** A press flattens the thumb onto its bottom, as the Core page's Pressed cell draws it. */
private const val PRESS_SX = .18f
private const val PRESS_SY = .16f

/** Where the thumb squashes from: its centre, or the edge it slapped. */
internal enum class ToonSwitchOrigin { Centre, Left, Right }

/** The engine's `swAnim` at [t] ms, toggling from [from] to [to] (0 off, 1 on). */
internal fun toonSwAnim(
    t: Float,
    from: Int,
    to: Int,
): ToonSwitchLook {
    val d = (to - from).toFloat()
    if (t < 40) {
        val u = t / 40
        return ToonSwitchLook(
            on = to == 1,
            pos = from - d * .12f * u,
            sx = 1 - .12f * u,
            sy = 1 + .08f * u,
            trackOn =
                from == 1,
        )
    }
    if (t < 140) {
        val u = (t - 40) / 100
        val e = u * u * (3 - 2 * u)
        val s = 1 + .5f * sin(PI.toFloat() * u)
        return ToonSwitchLook(
            on = to == 1,
            pos = from - d * .12f + d * 1.12f * e,
            sx = s,
            sy = 1 / sqrt(s),
            trackOn = from == 1,
            echo = d * sin(PI.toFloat() * u),
        )
    }
    val a = toonOscillation((t - 140) / 1000, .35f, 40f)
    return ToonSwitchLook(
        on = to == 1,
        pos = to.toFloat(),
        sx = 1 - .28f * a,
        sy = 1 + .2f * a,
        origin = if (d > 0) ToonSwitchOrigin.Right else ToonSwitchOrigin.Left,
        trackOn = to == 1,
        wave = c01((t - 140) / 170),
    )
}

/**
 * The kit's `C.switch.scene` at a moment: [on], the toggle [toggleAgeMs] ago (null before any), under
 * press [p]. Under reduce motion — the Core page's `instant` — the thumb is at its end at once.
 */
internal fun toonSwitchFrameAt(
    on: Boolean,
    toggleAgeMs: Float?,
    p: Float,
    reduceMotion: Boolean,
): ToonSwitchLook {
    if (!reduceMotion && toggleAgeMs != null && toggleAgeMs < ANIM_MS) {
        return toonSwAnim(toggleAgeMs, if (on) 0 else 1, if (on) 1 else 0)
    }
    val q = c01(p)
    return ToonSwitchLook(on = on, sx = 1 + PRESS_SX * q, sy = 1 - PRESS_SY * q, pressed = q > 0)
}

private fun c01(v: Float): Float = max(0f, min(1f, v))

/** What one frame of the switch draws: the engine's `sw` options. */
internal data class ToonSwitchLook(
    val on: Boolean = false,
    val pos: Float = if (on) 1f else 0f,
    val sx: Float = 1f,
    val sy: Float = 1f,
    val origin: ToonSwitchOrigin = ToonSwitchOrigin.Centre,
    val trackOn: Boolean = on,
    val echo: Float = 0f,
    val wave: Float? = null,
    val pressed: Boolean = false,
    val disabled: Boolean = false,
    val focus: Boolean = false,
)

private val TRACK_RADII = ToonRadii(17f, 15f, 17f, 14f)
private val THUMB_RADII = ToonRadii(13f, 12f, 13f, 11f)
private val THUMB_INSETS = ToonInsets(1.8f, 2.2f, 3f, 1.8f)
private val FOCUS_RADII = ToonRadii(22f, 20f, 22f, 19f)

/** The kit's row: [label] from x 0, the switch at the right of [w]. */
internal fun toonSwitchRowScene(
    tokens: ToonTokens,
    label: String,
    w: Float,
    o: ToonSwitchLook,
): ToonScene {
    val sw = toonSwitchScene(tokens, o).placed(w - TRACK_W, 0f)
    if (label.isEmpty()) return sw
    val c = tokens.colors
    return sw +
        ToonScene(
            emptyList(),
            listOf(ToonLabel(label, 0f, 30f, c.text, if (o.disabled) .6f else 1f, CHECK_LABEL, ToonAnchor.Start)),
        )
}

/** The engine's `sw`: one frame, the track at (0, 7), the thumb 26 dp travelling 26. */
@OptIn(InternalToonApi::class)
internal fun toonSwitchScene(
    tokens: ToonTokens,
    o: ToonSwitchLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val dis = o.disabled
    val layers = mutableListOf<ToonLayer>()
    if (o.focus) layers += ringLayer(-6f, 1f, 72f, 46f, FOCUS_RADII, c, sh.wobble, sh.seed + 205)
    val tOn = o.trackOn
    val wv = o.wave?.takeIf { !dis && it < 1 }
    val waving = wv != null
    val tIns = sh.insets.let { ToonInsets(it.top * .75f, it.right * .75f, it.bottom * .75f, it.left * .75f) }
    layers +=
        bodyLayers(
            shapePoints(0f, 7f, TRACK_W, 34f, TRACK_RADII, tIns, sh.wobble * .7f, sh.seed + 201, 4f),
            fill =
                if (dis) {
                    c.disabled
                } else if (if (waving) !tOn else tOn) {
                    c.trackOn
                } else {
                    c.trackOff
                },
            colors = c,
            shadow = if (dis) 0f else sh.shadow - 1,
            inkAlpha = if (dis) .35f else 1f,
        )
    if (wv != null) {
        val fx = tIns.left + .5f
        val fy = 7 + tIns.top + .5f
        val fw = TRACK_W - tIns.right - tIns.left - 1
        val fh = 34 - tIns.top - tIns.bottom - 1
        val ww = max(3f, fw * (1 - (1 - wv).pow(2)))
        val x0 = if (o.origin == ToonSwitchOrigin.Right) fx + fw - ww else fx
        val r = fh / 2
        layers +=
            smoothLayer(
                wobblePoints(x0, fy, ww, fh, ToonRadii(r, r, r, r), .7f, sh.seed + 207 + floor(wv * 4).toInt(), 3f),
                if (tOn) c.trackOn else c.trackOff,
            )
    }
    val tx = 4 + o.pos * 26
    val ty = 11f
    val ox =
        when (o.origin) {
            ToonSwitchOrigin.Right -> 26f
            ToonSwitchOrigin.Left -> 0f
            ToonSwitchOrigin.Centre -> 13f
        }
    val oy = if (o.pressed) 26f else 13f
    val g = shapePoints(0f, 0f, 26f, 26f, THUMB_RADII, THUMB_INSETS, sh.wobble * .5f, sh.seed + 301, 3f)
    val fill = smoothNumbers(g.fill)
    if (o.echo != 0f && !dis) {
        listOf(2 to .14f, 1 to .3f).forEach { (k, op) ->
            val gx = 4 + (o.pos - o.echo * .16f * k) * 26
            layers +=
                ToonLayer(
                    fill,
                    ToonSegments.Quad,
                    true,
                    c.thumb,
                    op * abs(o.echo),
                    gx,
                    ty,
                    scale = o.sx * .96f,
                    scaleY =
                        o.sy * .96f,
                    pivotX = 13f,
                    pivotY = 13f,
                )
        }
    }
    layers +=
        ToonLayer(
            smoothNumbers(g.ink),
            ToonSegments.Quad,
            true,
            c.ink,
            if (dis) .35f else 1f,
            tx,
            ty,
            scale = o.sx,
            scaleY = o.sy,
            pivotX = ox,
            pivotY = oy,
        )
    layers +=
        ToonLayer(
            fill,
            ToonSegments.Quad,
            true,
            if (dis) c.disabled else c.thumb,
            1f,
            tx,
            ty,
            scale = o.sx,
            scaleY = o.sy,
            pivotX = ox,
            pivotY = oy,
        )
    return ToonScene(layers, emptyList())
}
