package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonAnchor
import io.github.youndie.toon.draw.ToonInsets
import io.github.youndie.toon.draw.ToonLabel
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonTextRenderer
import io.github.youndie.toon.draw.ToonTextStyle
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.ringLayer
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.spinnerPoints
import io.github.youndie.toon.interaction.hoverLifted
import io.github.youndie.toon.interaction.rememberToonHoverState
import io.github.youndie.toon.interaction.rememberToonPressState
import io.github.youndie.toon.interaction.toonHoverable
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos

// A port of the desktop extension's `recBtn` and `recPulse` and of `ToonRecordButton.dc.html` (B-75,
// research §1.8): the most noticeable control of a window that records. Idle it is a paper button with
// a red mark; recording it fills with `danger`, its mark breathes and the time runs beside the label;
// stopping it is grey and inert while a spinner turns. Its words are HTML over the drawing in the
// design, and here labels placed by that HTML's numbers.

/** The `state` prop of `ToonRecordButton.dc.html`. */
public enum class ToonRecordState {
    /** Not recording: the button starts. */
    Idle,

    /** Recording: the button stops, and shows the time. */
    Recording,

    /** Saving after a stop: inert until the caller moves on. */
    Stopping,
}

/**
 * The design's record button, 40 dp tall and as wide as its label: [recordLabel] while [state] is
 * idle, [stopLabel] and [elapsed] (in figures of one width) while recording, [savingLabel] while
 * stopping. A click, Enter or Space calls [onStart] when idle and [onStop] when recording; stopping,
 * it does nothing.
 *
 * A mouse over it lifts it, a press squashes it, keyboard focus rings it. Recording, its mark
 * breathes on a 1.2 s cycle, and stopping, a spinner turns; with reduced motion the mark stays at its
 * full size and the spinner still.
 */
@Composable
public fun ToonRecordButton(
    state: ToonRecordState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    elapsed: String = "00:00:00",
    recordLabel: String = "Record",
    stopLabel: String = "Stop",
    savingLabel: String = "Saving",
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val text = rememberToonText()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hover = rememberToonHoverState()
    val press = rememberToonPressState()
    val label =
        when (state) {
            ToonRecordState.Idle -> recordLabel
            ToonRecordState.Recording -> stopLabel
            ToonRecordState.Stopping -> savingLabel
        }
    val w = remember(text, label, state) { toonRecordButtonWidth(text, label, state) }
    val fire by rememberUpdatedState {
        when (state) {
            ToonRecordState.Idle -> onStart()
            ToonRecordState.Recording -> onStop()
            ToonRecordState.Stopping -> Unit
        }
    }
    // Ms into the current state, on the frame clock, while it moves: the pulse, the spinner.
    var age by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state, reduce) {
        age = 0L
        if (state == ToonRecordState.Idle || reduce) return@LaunchedEffect
        val start = withFrameMillis { it }
        while (true) withFrameMillis { age = it - start }
    }
    val inert = state == ToonRecordState.Stopping

    Box(
        modifier
            .size(w.dp, H.dp)
            .semantics {
                role = Role.Button
                contentDescription = label
                if (state == ToonRecordState.Recording) stateDescription = elapsed
                onClick {
                    fire()
                    !inert
                }
                if (inert) disabled()
            }.toonHoverable(hover)
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                        fire()
                        true
                    }

                    else -> {
                        false
                    }
                }
            }.focusable(interactionSource = interaction)
            .pointerInput(inert) {
                awaitEachGesture {
                    awaitFirstDown()
                    if (inert) return@awaitEachGesture
                    press.press()
                    val up = waitForUpOrCancellation()
                    if (press.release() && up != null) fire()
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // The engine draws in dp (research D5); the canvas is pixels (B-47).
            inDp {
                val look =
                    ToonRecordLook(
                        state = state,
                        hv = hover.value,
                        p = press.progress,
                        focus = focused,
                        pulse = if (reduce) 1f else toonRecPulse(age.toFloat()),
                        spinMs = age.toFloat(),
                    )
                val scene = toonRecordButtonScene(tokens, w, look)
                drawScene(scene + toonRecordButtonLabels(tokens, look, label, elapsed, text), text)
            }
        }
    }
}

/** The extension's `recPulse`: 0 to 1 and back on a 1.2 s cosine, the kit's idle breath. */
internal fun toonRecPulse(t: Float): Float = .5f - .5f * cos(2 * PI.toFloat() * t / PULSE_MS)

/** The design's `width()`: the 36 dp before the label, the label, the time while recording, 18 after. */
internal fun toonRecordButtonWidth(
    text: ToonTextRenderer,
    label: String,
    state: ToonRecordState,
): Float {
    val time = if (state == ToonRecordState.Recording) GAP + text.measure(TIME_TEMPLATE, TIME_STYLE).size.width else 0f
    return ceil(LABEL_X + text.measure(label, LABEL_STYLE).size.width + time + END_PAD)
}

/** What one frame draws: `recBtn`'s options. */
internal data class ToonRecordLook(
    val state: ToonRecordState = ToonRecordState.Idle,
    val hv: Float = 0f,
    val p: Float = 0f,
    val focus: Boolean = false,
    val pulse: Float = 0f,
    val spinMs: Float = 0f,
) {
    private val pressed get() = p.coerceIn(0f, 1f)

    /** The hover's lift, which the press takes back; a stopping button does not lift. */
    val lift: Float get() = if (state == ToonRecordState.Stopping) 0f else 2 * hv * (1 - pressed)

    /** How far the press drops what is drawn on the body: its squash and 1.2 dp more. */
    val drop: Float get() = H * .15f * pressed + 1.2f * pressed
}

/** The extension's `recBtn`: the button [w] dp wide and 40 tall, from its top left; no words. */
@OptIn(InternalToonApi::class)
internal fun toonRecordButtonScene(
    tokens: ToonTokens,
    w: Float,
    o: ToonRecordLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val p = o.p.coerceIn(0f, 1f)
    val bw = w * (1 + .12f * p)
    val bh = H * (1 - .15f * p)
    val x = (w - bw) / 2
    val y = H - bh
    val rs = sh.radiusScale
    val r =
        ToonRadii(
            BODY_RADII.topLeft * rs,
            BODY_RADII.topRight * rs,
            BODY_RADII.bottomRight * rs,
            BODY_RADII.bottomLeft * rs,
        )
    val st = o.state
    val layers = mutableListOf<ToonLayer>()
    if (o.focus) {
        layers +=
            ringLayer(
                -6f,
                -6f,
                w + 12,
                H + 12,
                ToonRadii(r.topLeft + 6, r.topRight + 6, r.bottomRight + 6, r.bottomLeft + 6),
                c,
                sh.wobble,
                sh.seed + 3301,
            )
    }
    // `liftBody`: the shadow drops with the press; a hover lifts the body and leaves the shadow.
    val k = if (st == ToonRecordState.Recording) 1.3f else 1f
    val ins = sh.insets.let { ToonInsets(it.top * k, it.right * k, it.bottom * k, it.left * k) }
    val shadow = if (st == ToonRecordState.Stopping) 0f else sh.shadow * (1 - p)
    val fill =
        when (st) {
            ToonRecordState.Recording -> c.danger
            ToonRecordState.Stopping -> c.disabled
            ToonRecordState.Idle -> c.surface
        }
    val body =
        ToonScene(
            bodyLayers(
                shapePoints(x, y, bw, bh, r, ins, sh.wobble, sh.seed + 3300),
                fill,
                c,
                shadow = if (shadow > .05f) shadow else 0f,
                rim = if (st == ToonRecordState.Idle) c.rim else null,
            ),
            emptyList(),
        )
    val lift = o.lift
    layers += if (lift != 0f) body.hoverLifted(lift, c.ink).layers else body.layers
    // The mark rides the body: lifted with it, dropped by the press.
    val cx = 20 - lift
    val cy = y + bh / 2 - lift + 1.2f * p
    if (st == ToonRecordState.Stopping) {
        val ph = o.spinMs / 1000 * 2 * PI.toFloat()
        layers += smoothLayer(spinnerPoints(cx, cy, SPIN_R, SPIN_R + 3, ph, 1f, 1f), c.ink)
        layers += smoothLayer(spinnerPoints(cx, cy, SPIN_R, SPIN_R, ph, 1f, 1f), c.primary)
    } else {
        val rr = if (st == ToonRecordState.Recording) 5.2f + 1.6f * o.pulse else 6f
        val mark =
            shapePoints(
                cx - rr,
                cy - rr,
                2 * rr,
                2 * rr,
                ToonRadii(rr, rr * .95f, rr, rr * .9f),
                MARK_INSETS,
                .35f,
                sh.seed + 3310,
                2f,
            )
        layers += smoothLayer(mark.ink, c.ink)
        layers += smoothLayer(mark.fill, if (st == ToonRecordState.Recording) c.surface else c.danger)
    }
    return ToonScene(layers, emptyList())
}

/**
 * The words where the design's HTML puts them: the label 36 dp in and the time 10 dp after it, centred
 * on the button as a flex row centres a line, and moved as the body moves — up and left by the lift,
 * down by the press.
 */
internal fun toonRecordButtonLabels(
    tokens: ToonTokens,
    o: ToonRecordLook,
    label: String,
    elapsed: String,
    text: ToonTextRenderer,
): ToonScene {
    val c = tokens.colors
    val colour =
        when (o.state) {
            ToonRecordState.Recording -> c.onDanger
            ToonRecordState.Stopping -> c.textMuted
            ToonRecordState.Idle -> c.text
        }
    val dx = -o.lift
    val dy = o.drop - o.lift
    val first = text.measure(label, LABEL_STYLE)
    val labels = mutableListOf<ToonLabel>()
    labels +=
        ToonLabel(
            label,
            LABEL_X + dx,
            (H - first.size.height) / 2 + first.firstBaseline + dy,
            colour,
            1f,
            LABEL_STYLE,
            ToonAnchor.Start,
        )
    if (o.state == ToonRecordState.Recording) {
        val time = text.measure(elapsed, TIME_STYLE)
        labels +=
            ToonLabel(
                elapsed,
                LABEL_X + first.size.width + GAP + dx,
                (H - time.size.height) / 2 + time.firstBaseline + dy,
                colour,
                1f,
                TIME_STYLE,
                ToonAnchor.Start,
            )
    }
    return ToonScene(emptyList(), labels)
}

/** The button: 40 dp tall (the pointer page's control), its radii and the mark's and spinner's sizes. */
private const val H = 40f
private val BODY_RADII = ToonRadii(16f, 11f, 18f, 12f)
private val MARK_INSETS = ToonInsets(1.4f, 1.6f, 2.2f, 1.4f)
private const val SPIN_R = 7f
private const val PULSE_MS = 1200f

/** The words: 15, bold, 36 dp in, the time 10 dp after the label and 18 dp before the end. */
private val LABEL_STYLE = ToonTextStyle(700, 15f)
private val TIME_STYLE = ToonTextStyle(700, 15f, tabular = true)
private const val TIME_TEMPLATE = "00:00:00"
private const val LABEL_X = 36f
private const val GAP = 10f
private const val END_PAD = 18f
