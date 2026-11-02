package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonAnchor
import io.github.youndie.toon.draw.ToonInsets
import io.github.youndie.toon.draw.ToonLabel
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonSegments
import io.github.youndie.toon.draw.ToonTextStyle
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.ringLayer
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.wobblePoints
import io.github.youndie.toon.interaction.rememberToonPressState
import io.github.youndie.toon.interaction.toonPressable
import io.github.youndie.toon.motion.toonOscillation
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonSize
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

// A port of the engine's `check` and `chkAnim` and the kit's `C.checkbox` (B-17): a lopsided 30 dp box
// in a 48 dp target whose tick is one drawn stroke, trimmed on from its start, its tip flying past the
// frame and flicking three ink specks; unchecking erases it from the tip and shivers the box.

/**
 * The design's checkbox: [label] beside a hand-drawn box. A click toggles it and reports the new
 * state to [onCheckedChange]; [checked] from outside sets it, as the kit's `checked` prop does.
 * [error] inks the box and the label in the danger colours.
 */
@Composable
public fun ToonCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    enabled: Boolean = true,
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val text = rememberToonText()
    val labelWidth =
        remember(label, text) { if (label.isEmpty()) 0f else text.measure(label, CHECK_LABEL).multiParagraph.width }
    var on by remember(checked) { mutableStateOf(checked) }
    val press = rememberToonPressState()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val clock = remember { ToonToggleClock() }
    val change by rememberUpdatedState(onCheckedChange)
    LaunchedEffect(clock.starts) { while (clock.running(TOGGLE_MS)) withFrameMillis { clock.tick(it) } }
    val look = toonCheckboxFrameAt(on, clock.age(), press.progress, reduce)
    val w = if (label.isEmpty()) ToonSize.TARGET_MIN else CHECK_LABEL_X + ceil(labelWidth) + 4

    Canvas(
        modifier
            .size(w.dp, ToonSize.TARGET_MIN.dp)
            .semantics {
                toggleableState = ToggleableState(on)
                if (label.isNotEmpty()) this.text = AnnotatedString(label)
            }.toonPressable(press, enabled = enabled, role = Role.Checkbox) {
                on = !on
                clock.start()
                change(on)
            }.focusable(enabled, interaction),
    ) {
        // The engine draws in dp (research D5); the canvas is pixels (B-47).

        inDp {
            val scene = toonCheckboxScene(tokens, label, look.copy(disabled = !enabled, focus = focused, error = error))
            if (look.fade != null && look.fade < 1f) {
                // Reduce motion's `fade 120`: the unchecked box fades into the checked one.
                drawScene(
                    toonCheckboxScene(
                        tokens,
                        label,
                        ToonCheckboxLook(on = false, disabled = !enabled, error = error),
                    ).placed(
                        0f,
                        0f,
                        1 - look.fade,
                    ),
                    text,
                )
                drawScene(scene.placed(0f, 0f, look.fade), text)
            } else {
                drawScene(scene, text)
            }
        }
    }
}

/** The label: 16 sp semibold in the theme's body face, from x 58. */
internal val CHECK_LABEL = ToonTextStyle(600, 16f)
private const val CHECK_LABEL_X = 58f

/** A check or uncheck runs 300 ms in the kit; under reduce motion a check fades in over 120. */
private const val TOGGLE_MS = 300f
private const val REDUCED_FADE_MS = 120f

/** The press squashes the box as a check's first 50 ms does, to 0.86 (the Core page's Pressed cell). */
private const val PRESS_SQUASH = .14f

/** The engine's `chkAnim`: the box's squash, the tick's trim and shiver, the specks' life, at [t] ms. */
internal fun toonChkAnim(
    t: Float,
    on: Boolean,
): ToonCheckboxLook {
    if (on) {
        val sq = if (t < 50) 1 - .14f * (t / 50) else 1 - .14f * toonOscillation((t - 50) / 1000, .35f, 40f)
        val u = c01((t - 50) / 120)
        return ToonCheckboxLook(on = true, trim = 1 - (1 - u).pow(3), sx = sq, sy = sq, sp = c01((t - 150) / 170))
    }
    val u = min(1f, t / 100)
    val dx = if (t > 100 && t < 160) 1.5f * sin((t - 100) / 60 * 2 * PI.toFloat()) else 0f
    return ToonCheckboxLook(on = t < 100, trim = 1 - u * u, dx = dx)
}

/**
 * The kit's `C.checkbox.scene` at a moment: [on], the toggle [toggleAgeMs] ago (null before any),
 * under press [p]. Under reduce motion — the Core page's words — a check is `fade 120` into the
 * checked box, an uncheck is `instant`.
 */
internal fun toonCheckboxFrameAt(
    on: Boolean,
    toggleAgeMs: Float?,
    p: Float,
    reduceMotion: Boolean,
): ToonCheckboxLook {
    val base =
        when {
            toggleAgeMs == null -> {
                ToonCheckboxLook(on = on)
            }

            reduceMotion -> {
                toonChkAnim(
                    SETTLED_MS,
                    on,
                ).copy(fade = if (on) min(1f, toggleAgeMs / REDUCED_FADE_MS) else null)
            }

            else -> {
                toonChkAnim(toggleAgeMs, on)
            }
        }
    val pressed = 1 - PRESS_SQUASH * c01(p)
    return base.copy(sx = min(base.sx, pressed), sy = min(base.sy, pressed))
}

private const val SETTLED_MS = 1e6f

private fun c01(v: Float): Float = max(0f, min(1f, v))

/** A toggle's clock, shared by the checkbox and the radio group: when the last toggle started. */
@Stable
internal class ToonToggleClock {
    var nowMs by mutableLongStateOf(0L)
    var starts by mutableStateOf(0)
    private var startedAt: Long? = null

    fun start() {
        startedAt = PENDING_START
        starts++
    }

    fun tick(now: Long) {
        nowMs = now
        if (startedAt == PENDING_START) startedAt = now
    }

    fun age(): Float? = startedAt?.let { if (it == PENDING_START) 0f else (nowMs - it).toFloat() }

    fun running(durationMs: Float): Boolean = age()?.let { it < durationMs } ?: false
}

private const val PENDING_START = -1L

/** What one frame of the checkbox draws: the engine's `check` options, and the reduce-motion fade. */
internal data class ToonCheckboxLook(
    val on: Boolean = false,
    val trim: Float = if (on) 1f else 0f,
    val sx: Float = 1f,
    val sy: Float = 1f,
    val dx: Float = 0f,
    val sp: Float = 0f,
    val disabled: Boolean = false,
    val focus: Boolean = false,
    val error: Boolean = false,
    val fade: Float? = null,
)

private val BOX_RADII = ToonRadii(8f, 5f, 9f, 6f)
private val RING_RADII = ToonRadii(12f, 9f, 13f, 10f)

/** The tick's stroke and its length: one polyline, drawn on by its dash. */
private const val TICK_LENGTH = 40.6f
private val SPECKS = listOf(floatArrayOf(1f, -.55f, 1.5f), floatArrayOf(.35f, -1f, 1.2f), floatArrayOf(1.1f, .35f, 1f))

/** The engine's `check`: one frame of the 48 dp target, the 30 dp box at (9, 9). */
@OptIn(InternalToonApi::class)
internal fun toonCheckboxScene(
    tokens: ToonTokens,
    label: String,
    o: ToonCheckboxLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val s = 30f
    val bx = 9f
    val by = 9f
    val bw = s * o.sx
    val bh = s * o.sy
    val x = bx + (s - bw) / 2 + o.dx
    val y = by + (s - bh)
    val dis = o.disabled
    val layers = mutableListOf<ToonLayer>()
    if (o.focus) layers += ringLayer(bx - 6, by - 6, s + 12, s + 12, RING_RADII, c, sh.wobble, sh.seed + 55)
    val ins = sh.insets.let { ToonInsets(it.top * .85f, it.right * .85f, it.bottom * .85f, it.left * .85f) }
    layers +=
        bodyLayers(
            shapePoints(x, y, bw, bh, BOX_RADII, ins, sh.wobble, sh.seed + 51, 3f),
            fill =
                if (dis) {
                    c.disabled
                } else if (o.on) {
                    c.primary
                } else {
                    c.surface
                },
            colors = c,
            shadow = if (dis) 0f else 2f,
            inkAlpha = if (dis) .35f else 1f,
            inkColor = if (o.error) c.danger else c.ink,
        )
    if (o.trim > .005f) {
        val dx = o.dx
        val tick = floatArrayOf(15 + dx, 24f, 21.5f + dx, 31f, 41 + dx, 7f)
        val doff = TICK_LENGTH * (1 - o.trim)
        val dash = floatArrayOf(TICK_LENGTH, 200f)
        layers +=
            ToonLayer(
                tick,
                ToonSegments.Line,
                false,
                c.surface,
                if (dis) .5f else 1f,
                stroke = 8.5f,
                dash = dash,
                dashOffset = doff,
            )
        layers +=
            ToonLayer(
                tick,
                ToonSegments.Line,
                false,
                c.ink,
                if (dis) .45f else 1f,
                stroke = 4.5f,
                dash = dash,
                dashOffset = doff,
            )
        if (o.sp > 0 && o.sp < 1) {
            val e2 = 1 - (1 - o.sp).pow(2)
            SPECKS.forEachIndexed { j, (vx, vy, r0) ->
                val px = 41 + dx + vx * (3 + 9 * e2)
                val py = 7 + vy * (3 + 9 * e2)
                val rr = r0 * (1 - o.sp * .6f)
                layers +=
                    smoothLayer(
                        wobblePoints(px - rr, py - rr, 2 * rr, 2 * rr, ToonRadii(rr, rr, rr, rr), .1f, 60 + j, 1.2f),
                        c.fxLine,
                        1 - o.sp,
                    )
            }
        }
    }
    val labels =
        if (label.isEmpty()) {
            emptyList()
        } else {
            listOf(
                ToonLabel(
                    label,
                    CHECK_LABEL_X,
                    30f,
                    if (o.error) c.dangerText else c.text,
                    if (dis) .6f else 1f,
                    CHECK_LABEL,
                    ToonAnchor.Start,
                ),
            )
        }
    return ToonScene(layers, labels)
}
