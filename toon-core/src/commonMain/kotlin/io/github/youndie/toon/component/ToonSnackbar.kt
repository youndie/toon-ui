package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
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
import io.github.youndie.toon.draw.ToonTextStyle
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.wobblePoints
import io.github.youndie.toon.motion.toonSpring
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

// A port of the engine's `snack`, `snkShow` and `snkHide` and the kit's `C.snackbar` (B-23): a sign
// hanging from a nail on two strings that flies up from below, overshoots and swings ±3° twice about
// the nail; it leaves by lifting 8 dp, then dropping with a smear and speed lines trailing above.

/** The `type` prop of `ToonSnackbar.dc.html`: the sign's colour. */
public enum class ToonSnackbarType {
    Info,
    Success,
    Error,
}

/**
 * The design's snackbar: [text] on a sign [width] wide, and [action] at its end when not empty, which
 * a tap there reports to [onAction]. It flies in when [shown] turns true and drops out when it turns
 * false.
 */
@Composable
public fun ToonSnackbar(
    text: String,
    modifier: Modifier = Modifier,
    action: String = "",
    type: ToonSnackbarType = ToonSnackbarType.Info,
    shown: Boolean = true,
    width: Dp = 342.dp,
    onAction: () -> Unit = {},
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val texts = rememberToonText()
    val clock = remember { ToonSnackClock(shown) }
    val act by rememberUpdatedState(onAction)
    val w = width.value
    LaunchedEffect(shown) { clock.changed(shown) }
    LaunchedEffect(clock.starts) { while (clock.running()) withFrameMillis { clock.tick(it) } }

    Canvas(
        modifier
            .size(width, SNACK_H.dp)
            .semantics {
                this.text = AnnotatedString(text)
                liveRegion = LiveRegionMode.Polite
                if (action.isNotEmpty()) {
                    customActions =
                        listOf(CustomAccessibilityAction(action) { act().let { true } })
                }
            }.pointerInput(action, w) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val up = waitForUpOrCancellation()
                    // The kit's action target: the last 110 dp of the sign.
                    if (up != null && clock.shown && action.isNotEmpty() &&
                        down.position.x / density > w - ACTION_TARGET
                    ) {
                        act()
                    }
                }
            },
    ) {
        // The engine draws in dp (research D5); the canvas is pixels (B-47).

        inDp {
            val moment = toonSnackbarFrameAt(clock.shown, clock.age(), reduce) ?: return@Canvas
            drawScene(toonSnackbarScene(tokens, w, text, action, type, moment), texts)
        }
    }
}

private const val SNACK_H = 56f
private const val ACTION_TARGET = 110f
private const val HOOK_Y = -26f

/** The show runs 600 ms in the kit, the hide 400 (drawn for 260); under reduce motion both fade in 120. */
private const val SHOW_MS = 600f
private const val HIDE_MS = 400f
private const val HIDE_DRAWN_MS = 260f
private const val REDUCED_FADE_MS = 120f

/** What the snackbar draws at a moment: its look, its offset and swing about the nail, and its fade. */
internal class ToonSnackMoment(
    val look: ToonSnackLook,
    val dy: Float,
    val rot: Float,
    val fade: Float?,
)

/** The kit's `C.snackbar.scene`: the sign at [moment], moved and swung about the nail at (w / 2, −26). */
internal fun toonSnackbarScene(
    tokens: ToonTokens,
    w: Float,
    text: String,
    action: String,
    type: ToonSnackbarType,
    moment: ToonSnackMoment,
): ToonScene =
    toonSnackScene(tokens, w, text, action, type, moment.look).placed(
        0f,
        moment.dy,
        moment.fade ?: 1f,
        moment.rot,
        w / 2,
        HOOK_Y,
    )

/** The engine's `snkShow` at [t] ms: the fly-up from 90 dp below on a spring, then the swing. */
internal fun toonSnkShow(t: Float): ToonSnackMotion =
    ToonSnackMotion(
        dy = 90 * toonSpring(t / 1000, .48f, 24f),
        rot = if (t > 160) 3 * sin(2 * PI.toFloat() * (t - 160) / 150) * exp(-(t - 160) / 200) else 0f,
        sy = 1f,
    )

/** The engine's `snkHide` at [t] ms: an 8 dp lift, then the drop with a vertical smear. */
internal fun toonSnkHide(t: Float): ToonSnackMotion {
    if (t < 70) {
        val u = t / 70
        return ToonSnackMotion(-8 * (1 - (1 - u).pow(2)), 0f, 1f)
    }
    val u = min(1f, (t - 70) / 170)
    return ToonSnackMotion(-8 + 130 * u * u, 0f, 1 + .3f * sin(PI.toFloat() * u))
}

internal class ToonSnackMotion(
    val dy: Float,
    val rot: Float,
    val sy: Float,
)

/**
 * The kit's `C.snackbar.scene` at a moment: [shown], the last change [ageMs] ago (null for one shown
 * from the start). Null when nothing is drawn. Under reduce motion — the Core page's `fade 120` for
 * both — the sign fades in at rest, and fades out where it hangs.
 */
internal fun toonSnackbarFrameAt(
    shown: Boolean,
    ageMs: Float?,
    reduceMotion: Boolean,
): ToonSnackMoment? {
    if (reduceMotion && ageMs != null) {
        val u = min(1f, ageMs / REDUCED_FADE_MS)
        if (!shown && u >= 1) return null
        return ToonSnackMoment(ToonSnackLook(), 0f, 0f, if (shown) u else 1 - u)
    }
    if (shown) {
        val a = ageMs?.let { toonSnkShow(it) } ?: ToonSnackMotion(0f, 0f, 1f)
        return ToonSnackMoment(ToonSnackLook(sy = a.sy), a.dy, a.rot, null)
    }
    if (ageMs == null || ageMs > HIDE_DRAWN_MS) return null
    val a = toonSnkHide(ageMs)
    return ToonSnackMoment(
        ToonSnackLook(
            sy = a.sy,
            speedU =
                if (ageMs >
                    70
                ) {
                    (ageMs - 70) / 190
                } else {
                    -1f
                },
        ),
        a.dy,
        a.rot,
        null,
    )
}

@Stable
private class ToonSnackClock(
    initial: Boolean,
) {
    var nowMs by mutableLongStateOf(0L)
    var starts by mutableStateOf(0)
    var shown by mutableStateOf(initial)
    private var at: Long? = if (initial) PENDING_START else null

    fun changed(now: Boolean) {
        if (now == shown) return
        shown = now
        at = PENDING_START
        starts++
    }

    fun tick(now: Long) {
        nowMs = now
        if (at == PENDING_START) at = now
    }

    fun age(): Float? = at?.let { if (it == PENDING_START) 0f else (nowMs - it).toFloat() }

    fun running(): Boolean = age()?.let { it < (if (shown) SHOW_MS else HIDE_MS) } ?: false
}

private const val PENDING_START = -1L

/** What one frame of the sign draws: the engine's `snack` options. */
internal data class ToonSnackLook(
    val sy: Float = 1f,
    val speedU: Float = -1f,
    val rope: Boolean = true,
)

private val SIGN_RADII = ToonRadii(10f, 14f, 9f, 13f)
private val NAIL_INSETS = ToonInsets(1.3f, 1.3f, 1.8f, 1.3f)
private val TEXT = ToonTextStyle(700, 16f)
private val ACTION = ToonTextStyle(800, 16f)

/** The engine's `snack`: one frame [w] dp wide and 56 tall from its top left, the nail at (w / 2, −26). */
@OptIn(InternalToonApi::class)
internal fun toonSnackScene(
    tokens: ToonTokens,
    w: Float,
    text: String,
    action: String,
    type: ToonSnackbarType,
    o: ToonSnackLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val bh = SNACK_H * o.sy
    val (fill, ink) =
        when (type) {
            ToonSnackbarType.Info -> c.warning to c.ink
            ToonSnackbarType.Success -> c.successLight to c.ink
            ToonSnackbarType.Error -> c.danger to c.onDanger
        }
    val rope = c.fxLine
    val hx = w / 2
    val hy = HOOK_Y
    val layers = mutableListOf<ToonLayer>()
    if (o.rope) {
        layers +=
            ToonLayer(floatArrayOf(14f, 12f, hx * .5f, hy * .2f, hx, hy), ToonSegments.Quad, false, rope, stroke = 1.8f)
        layers +=
            ToonLayer(
                floatArrayOf(w - 14, 12f, w - hx * .5f, hy * .2f, hx, hy),
                ToonSegments.Quad,
                false,
                rope,
                stroke = 1.8f,
            )
        val g = shapePoints(hx - 4, hy - 4, 8f, 8f, ToonRadii(4f, 4f, 4f, 4f), NAIL_INSETS, .3f, sh.seed + 640, 2f)
        layers += smoothLayer(g.ink, c.ink)
        layers += smoothLayer(g.fill, c.nail)
    }
    val su = o.speedU
    if (su >= 0 && su < 1) {
        floatArrayOf(.22f, .4f, .6f, .78f).forEachIndexed { j, k ->
            val x = w * k
            val len = (26 + 14 * (j % 2)) * (1 - su)
            layers +=
                ToonLayer(
                    floatArrayOf(x - 1.7f, -6f, x, -6 - len, x + 1.7f, -6f),
                    ToonSegments.Line,
                    true,
                    rope,
                    1 - su,
                )
        }
    }
    val rs = sh.radiusScale
    val r =
        ToonRadii(
            SIGN_RADII.topLeft * rs,
            SIGN_RADII.topRight * rs,
            SIGN_RADII.bottomRight * rs,
            SIGN_RADII.bottomLeft * rs,
        )
    layers +=
        bodyLayers(shapePoints(0f, 0f, w, bh, r, sh.insets, sh.wobble, sh.seed + 601), fill, c, sh.shadow, rim = null)
    listOf(14f to 12f, w - 14 to 12f).forEachIndexed { j, (cx, cy) ->
        layers +=
            smoothLayer(
                wobblePoints(cx - 3, cy - 3, 6f, 6f, ToonRadii(3f, 3f, 3f, 3f), .3f, sh.seed + 650 + j, 2f),
                c.ink,
            )
    }
    val labels = mutableListOf(ToonLabel(text, 22f, bh / 2 + 6, ink, 1f, TEXT, ToonAnchor.Start))
    if (action.isNotEmpty()) {
        labels += ToonLabel(action, w - 22, bh / 2 + 6, ink, 1f, ACTION, ToonAnchor.End)
        // The engine guesses the action's width at 9 dp a letter.
        val lw = action.length * 9f
        val x1 = w - 22
        val yy = bh / 2 + 12
        layers +=
            ToonLayer(
                floatArrayOf(
                    x1 - lw,
                    yy,
                    x1 - lw * .75f,
                    yy + 2.4f,
                    x1 - lw / 2,
                    yy,
                    x1 - lw * .25f,
                    yy - 2.4f,
                    x1,
                    yy,
                ),
                ToonSegments.Quad,
                false,
                ink,
                stroke = 2.6f,
            )
    }
    return ToonScene(layers, labels)
}
