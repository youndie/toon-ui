package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.fx.LocalToonFxEmitter
import io.github.youndie.toon.fx.NoToonFxHost
import io.github.youndie.toon.fx.ToonFxEvent
import io.github.youndie.toon.fx.toonShake
import io.github.youndie.toon.fx.toonStarsScene
import io.github.youndie.toon.motion.toonOscillation
import io.github.youndie.toon.motion.toonSpring
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

// A port of the engine's `field`, `fldFocus` and `shake` and the kit's `C.textfield` (B-19): a drawn
// frame over a real `BasicTextField` whose own caret is hidden. The frame thickens and hops on focus,
// the label flies out of the field, the caret is the engine's — a 3.2 dp block, on 500 ms and off
// 300, no fade — and an error shakes the field red with stars over its right edge.

/**
 * The design's text field: [value] typed into a hand-drawn frame [width] wide, [label] inside it until
 * it is focused or filled, then above. [error], when not empty, turns it red, shakes it once and
 * replaces [helper] below it.
 */
@Composable
public fun ToonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    helper: String = "",
    error: String = "",
    width: Dp = 342.dp,
    enabled: Boolean = true,
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val text = rememberToonText()
    val fx = LocalToonFxEmitter.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val clock = remember { ToonFieldClock() }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val w = width.value
    val hasError = error.isNotEmpty()

    LaunchedEffect(focused) { clock.focusChanged() }
    LaunchedEffect(hasError) {
        if (!hasError) return@LaunchedEffect
        if (fx !== NoToonFxHost && !reduce) {
            // A host draws the stars above everything; the field shakes on its own.
            fx.emit(
                ToonFxEvent.FieldError(
                    bounds,
                    at = Offset(bounds.left + w - 30, bounds.top + FIELD_TOP - 4),
                    spread = 26f,
                ),
            )
        }
        clock.errored()
    }
    LaunchedEffect(clock.starts, focused) {
        while (clock.running(focused && !reduce)) withFrameMillis { clock.tick(it) }
    }
    val moment = toonFieldFrameAt(clock.nowMs, focused, value.isNotEmpty(), clock.focusAge(), clock.errorAge(), reduce)
    val drawStars = fx === NoToonFxHost
    val valueStyle =
        TextStyle(
            fontFamily = Toon.fonts.body,
            fontWeight = FontWeight(500),
            fontSize = 16.sp,
            color = tokens.colors.text,
        )
    val baseline = remember(text) { text.measure("Pg", VALUE_STYLE).firstBaseline }

    Box(
        modifier
            .size(width, FIELD_BOX.dp)
            .onGloballyPositioned { bounds = it.boundsInRoot() },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val caretX =
                layout?.let { l ->
                    CARET_GAP +
                        l.getCursorRect(min(l.layoutInput.text.length, value.length)).left
                }
            val look =
                moment.look.copy(
                    label = label,
                    helper = if (hasError) error else helper,
                    error = hasError,
                    disabled = !enabled,
                    caretX = if (moment.caretOn) caretX else null,
                )
            var scene = toonFieldScene(tokens, w, look)
            if (drawStars &&
                moment.stars != null
            ) {
                scene += toonStarsScene(tokens.colors, moment.stars, w - 30, -4f, 26f)
            }
            drawScene(scene.placed(moment.shake, FIELD_TOP), text)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = valueStyle,
            cursorBrush = SolidColor(Color.Transparent),
            interactionSource = interaction,
            onTextLayout = { layout = it },
            modifier =
                Modifier
                    .offset(moment.shake.dp, FIELD_TOP.dp)
                    .size(width, FIELD_H.dp)
                    .semantics {
                        contentDescription = label
                        if (hasError) error(error)
                    },
            decorationBox = { inner ->
                Box(Modifier.padding(start = 20.dp, end = 20.dp, top = (VALUE_BASELINE - baseline).dp)) { inner() }
            },
        )
    }
}

/** The kit's box: 106 dp, the 56 dp field 22 down, room for the lifted label above and the helper below. */
private const val FIELD_BOX = 106f
private const val FIELD_TOP = 22f
private const val FIELD_H = 56f

/** The value: 16 sp, medium, on the field's baseline at 34; the caret 2 dp after the text (the kit's 22 + width). */
private val VALUE_STYLE = ToonTextStyle(500, 16f)
private const val VALUE_BASELINE = 34f
private const val CARET_GAP = 22f

/** Focus and blur run 400 ms in the kit, the error 500; the caret blinks on 500 of every 800. */
private const val FOCUS_MS = 400f
private const val ERROR_MS = 500f
private const val BLINK_MS = 800L
private const val BLINK_ON_MS = 500L

/** The engine's `fldFocus` at [t] ms: the label's lift, the outline's thickening, the frame's hop. */
internal fun toonFldFocus(
    t: Float,
    on: Boolean,
): ToonFieldLook {
    if (on) {
        return ToonFieldLook(
            focus = true,
            lift = 1 - toonSpring(t / 1000, .5f, 32f),
            thick = min(1f, t / 80),
            bounce = 1 + .035f * toonOscillation(t / 1000, .35f, 45f) * min(1f, t / 30),
        )
    }
    return ToonFieldLook(lift = toonSpring(t / 1000, .5f, 32f), thick = max(0f, 1 - t / 80))
}

/** What the field draws at a moment: its [look], the shake's offset, the stars' life, and the caret's blink. */
internal class ToonFieldMoment(
    val look: ToonFieldLook,
    val shake: Float,
    val stars: Float?,
    val caretOn: Boolean,
)

/**
 * The kit's `C.textfield.scene` at frame time [nowMs]: [focused], [filled], focus changed
 * [focusAgeMs] ago and the error [errorAgeMs] ago (null before either). Under reduce motion — the
 * Core page's words — focus is `instant`, the error `instant, red only` (no shake, no stars) and the
 * caret steady.
 */
internal fun toonFieldFrameAt(
    nowMs: Long,
    focused: Boolean,
    filled: Boolean,
    focusAgeMs: Float?,
    errorAgeMs: Float?,
    reduceMotion: Boolean,
): ToonFieldMoment {
    val tf = if (reduceMotion) focusAgeMs?.let { SETTLED_MS } else focusAgeMs
    var look =
        if (tf == null) {
            ToonFieldLook(focus = focused, lift = if (focused || filled) 1f else 0f, thick = if (focused) 1f else 0f)
        } else {
            toonFldFocus(tf, focused).copy(focus = focused)
        }
    if (filled) look = look.copy(lift = 1f)
    val te = if (reduceMotion) null else errorAgeMs
    val since = focusAgeMs?.toLong() ?: nowMs
    return ToonFieldMoment(
        look = look,
        shake = te?.let { toonShake(it) } ?: 0f,
        stars = te?.let { it / 350 }?.takeIf { it < 1 },
        caretOn = focused && (reduceMotion || since % BLINK_MS < BLINK_ON_MS),
    )
}

private const val SETTLED_MS = 1e6f

@Stable
private class ToonFieldClock {
    var nowMs by mutableLongStateOf(0L)
    var starts by mutableStateOf(0)
    private var focusAt: Long? = null
    private var errorAt: Long? = null
    private var first = true

    fun focusChanged() {
        if (first) {
            first = false
            return
        }
        focusAt = PENDING_START
        starts++
    }

    fun errored() {
        errorAt = PENDING_START
        starts++
    }

    fun tick(now: Long) {
        nowMs = now
        if (focusAt == PENDING_START) focusAt = now
        if (errorAt == PENDING_START) errorAt = now
    }

    private fun age(at: Long?): Float? = at?.let { if (it == PENDING_START) 0f else (nowMs - it).toFloat() }

    fun focusAge(): Float? = age(focusAt)

    fun errorAge(): Float? = age(errorAt)

    /** Frames are needed while focus or the error animate, and for the caret while focused. */
    fun running(blinking: Boolean): Boolean =
        blinking || (focusAge()?.let { it < FOCUS_MS } ?: false) || (errorAge()?.let { it < ERROR_MS } ?: false)
}

private const val PENDING_START = -1L

/** What one frame of the field draws: the engine's `field` options; [caretX] null hides the caret. */
internal data class ToonFieldLook(
    val label: String = "",
    val value: String = "",
    val helper: String = "",
    val focus: Boolean = false,
    val lift: Float = if (focus || value.isNotEmpty()) 1f else 0f,
    val thick: Float = if (focus) 1f else 0f,
    val bounce: Float = 1f,
    val error: Boolean = false,
    val disabled: Boolean = false,
    val caretX: Float? = null,
)

private val FIELD_RADII = ToonRadii(14f, 10f, 16f, 11f)

/** The engine's caret `x` for a value: it guesses 8.6 dp a character, 2 after a 20 dp inset. */
internal fun engineCaretX(value: String): Float = 20 + value.length * 8.6f + 2

/** The engine's number formatting, `f`: one decimal, none when whole. */
private fun f(v: Float): String {
    val r = round(v * 10) / 10
    return if (r == floor(r)) r.toInt().toString() else r.toString()
}

/** The engine's `field`: one frame [w] dp wide and 56 tall, from its top left. */
@OptIn(InternalToonApi::class)
internal fun toonFieldScene(
    tokens: ToonTokens,
    w: Float,
    o: ToonFieldLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val h = FIELD_H
    val bw = w * o.bounce
    val bh = h * (1 + (o.bounce - 1) * .6f)
    val x = (w - bw) / 2
    val y = (h - bh) / 2
    val rs = sh.radiusScale
    val r =
        ToonRadii(
            FIELD_RADII.topLeft * rs,
            FIELD_RADII.topRight * rs,
            FIELD_RADII.bottomRight * rs,
            FIELD_RADII.bottomLeft * rs,
        )
    val red = o.error
    val dis = o.disabled
    val k = 1 + .5f * (if (red) 1f else o.thick)
    val ins = sh.insets.let { ToonInsets(it.top * k, it.right * k, it.bottom * k, it.left * k) }
    val layers = mutableListOf<ToonLayer>()
    layers +=
        bodyLayers(
            shapePoints(x, y, bw, bh, r, ins, sh.wobble, sh.seed + 101),
            fill = if (dis) c.disabled else c.field,
            colors = c,
            shadow = 0f,
            inkAlpha = if (dis) .35f else 1f,
            inkColor = if (red) c.danger else c.ink,
            rim = c.rim?.let { c.fieldRim ?: it },
        )
    val lift = o.lift
    val labels = mutableListOf<ToonLabel>()
    labels +=
        ToonLabel(
            o.label,
            20 + (4 - 20) * lift,
            34 + (-10 - 34) * lift,
            if (lift > .5f) (if (red) c.dangerText else c.text) else c.textMuted,
            if (dis) .6f else 1f,
            ToonTextStyle(if (lift > .5f) 700 else 500, 16 - 3 * lift),
            ToonAnchor.Start,
        )
    if (o.value.isNotEmpty()) {
        labels +=
            ToonLabel(o.value, 20f, VALUE_BASELINE, c.text, 1f, VALUE_STYLE, ToonAnchor.Start)
    }
    if (o.caretX != null) {
        val cx = o.caretX
        val caret =
            Path().apply {
                moveTo(cx, 18f)
                relativeLineTo(3.2f, 0f)
                relativeLineTo(0f, 22f)
                relativeLineTo(-3.2f, 0f)
                close()
            }
        layers +=
            ToonLayer(
                FloatArray(0),
                ToonSegments.Line,
                true,
                c.primary,
                shape = caret,
                source = "M${f(cx)} 18 h3.2 v22 h-3.2 Z",
            )
    }
    if (o.helper.isNotEmpty()) {
        labels +=
            ToonLabel(o.helper, 4f, 80f, if (red) c.dangerText else c.textMuted, 1f, HELPER_STYLE, ToonAnchor.Start)
    }
    return ToonScene(layers, labels)
}

private val HELPER_STYLE = ToonTextStyle(600, 13f)
