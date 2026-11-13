package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
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
import io.github.youndie.toon.draw.ToonTextStyle
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.ringLayer
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.fx.LocalToonFxEmitter
import io.github.youndie.toon.fx.NoToonFxHost
import io.github.youndie.toon.fx.ToonFxEvent
import io.github.youndie.toon.fx.toonDustScene
import io.github.youndie.toon.fx.toonHoleScene
import io.github.youndie.toon.fx.toonSpeedScene
import io.github.youndie.toon.interaction.HOVER_LIFT
import io.github.youndie.toon.interaction.hoverLifted
import io.github.youndie.toon.interaction.rememberToonHoverState
import io.github.youndie.toon.interaction.toonHoverable
import io.github.youndie.toon.motion.toonOscillation
import io.github.youndie.toon.motion.toonSpring
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

// A port of the engine's `card`, `cardAppear` and `cardDismiss` and the kit's `C.card` (B-21): a sheet
// of paper tilted ±1–2° with a hard shadow and a time sticker. It drops in with a squash and dust at
// both bottom corners (40 ms later per card in a list), leans toward the finger when pressed, and a
// swipe past 80 dp shoots it off with speed lines, punching its cracked silhouette into the paper.

/**
 * The design's card: [title], [meta] and the [time] sticker on a sheet [width] wide, coloured and
 * shaped by [index] (one of three). [appear] drops it in [delay] ms late — a list passes 40 ms per
 * card for the cascade. [dismissible] lets a swipe past 80 dp throw it away and call [onDismiss].
 */
@Composable
public fun ToonCard(
    title: String,
    modifier: Modifier = Modifier,
    meta: String = "",
    time: String = "",
    index: Int = 0,
    tilt: Boolean = true,
    appear: Boolean = false,
    delay: Int = 0,
    width: Dp = 342.dp,
    selected: Boolean = false,
    enabled: Boolean = true,
    dismissible: Boolean = false,
    onDismiss: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    val state = remember { ToonCardState(appear) }
    ToonCard(
        state,
        title,
        modifier,
        meta,
        time,
        index,
        tilt,
        delay,
        width,
        selected,
        enabled,
        dismissible,
        onDismiss,
        onClick,
    )
}

/** The card over a [state] its caller holds: a [ToonCardList] reads each card's dismiss to close up the cards below. */
@Composable
internal fun ToonCard(
    state: ToonCardState,
    title: String,
    modifier: Modifier,
    meta: String,
    time: String,
    index: Int,
    tilt: Boolean,
    delay: Int,
    width: Dp,
    selected: Boolean,
    enabled: Boolean,
    dismissible: Boolean,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    hole: Boolean = true,
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val text = rememberToonText()
    // B-83: the time measured, so its sticker can grow to hold it.
    val timeWidth = remember(time, text) { if (time.isEmpty()) 0f else text.measure(time, TIME).multiParagraph.width }
    val fx = LocalToonFxEmitter.current
    val w = width.value
    val i = index.mod(3)
    val rot0 = if (tilt) tokens.shape.tilts[i] else 0f
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val click by rememberUpdatedState(onClick)
    val dismissed by rememberUpdatedState(onDismiss)
    val hover = rememberToonHoverState()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    LaunchedEffect(state.starts) {
        var last = -1L
        while (state.running(delay)) {
            withFrameMillis { now ->
                state.step(now, if (last < 0) 0f else (now - last).toFloat())
                last = now
            }
        }
    }
    LaunchedEffect(state.dismissStarts) {
        if (state.dismissStarts == 0) return@LaunchedEffect
        // A swiped card first springs back to its slot (B-67): the throw, and the hole, start that much later.
        val lead = state.throwShift.toLong()
        var told = false
        var waited = 0L
        val start = withFrameMillis { it }
        while (!reduce && waited < GONE_AFTER_MS + lead) {
            if (!told && waited >= lead) {
                told = true
                // A host draws the hole above everything; the card leaves it to the host.
                if (hole && fx !== NoToonFxHost) fx.emit(ToonFxEvent.CardGone(bounds, cardRadii(tokens)))
            }
            waited = withFrameMillis { it } - start
        }
        state.gone = true
        dismissed()
    }

    // In a list the cards below close up over the slot at once, so a hole there is only covered up (B-66).
    val holeLocal = hole && fx === NoToonFxHost
    Canvas(
        modifier
            .size(width, CARD_H.dp)
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .semantics {
                this.text = AnnotatedString(listOf(title, meta, time).filter { it.isNotEmpty() }.joinToString(", "))
                role = Role.Button
                if (selected) this.selected = true
                if (!enabled) disabled()
                onClick {
                    if (enabled) click()
                    enabled
                }
                // A swipe has no equivalent a screen reader can perform; the action throws the card the
                // way a swipe to the right does (B-30).
                if (dismissible && enabled) {
                    customActions =
                        listOf(
                            CustomAccessibilityAction("Dismiss") {
                                if (!state.gone && !state.dismissing) state.throwAway()
                                true
                            },
                        )
                }
            }.pointerInput(enabled, dismissible, w) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (state.gone || state.dismissing) return@awaitEachGesture
                    state.press(down.position.x / density, w)
                    var moved = false
                    while (true) {
                        val ev = awaitPointerEvent()
                        val change = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val d = change.position.x / density - down.position.x / density
                        if (dismissible && abs(d) > MOVE_SLOP) moved = true
                        if (moved) {
                            state.drag(d)
                            change.consume()
                        }
                    }
                    if (state.release(dismissible)) return@awaitEachGesture
                    if (!moved) click()
                }
            }.toonHoverable(hover, enabled)
            .focusable(enabled, interaction)
            .onPreviewKeyEvent { e ->
                // The pointer page's rule 3: the card takes keyboard focus, and Enter or Space presses it
                // at its middle as a tap there does (B-69).
                if (!enabled || e.key !in ACTIVATE || state.gone || state.dismissing) return@onPreviewKeyEvent false
                when (e.type) {
                    KeyEventType.KeyDown -> if (!state.held) state.press(w / 2, w)
                    KeyEventType.KeyUp -> if (state.held && !state.release(false)) click()
                }
                true
            },
    ) {
        // The engine draws in dp (research D5); the canvas is pixels (B-47).
        inDp {
            val scene =
                toonCardFrame(
                    tokens,
                    i,
                    w,
                    title,
                    meta,
                    time,
                    rot0,
                    state,
                    delay,
                    reduce,
                    selected,
                    !enabled,
                    holeLocal,
                    focus = focused,
                    hv = hover.value,
                    timeWidth = timeWidth,
                )
            if (scene.fade != null) {
                // Reduce motion's `fade 120`: the card fades in on appear and out on dismiss.
                drawScene(scene.scene.placed(0f, 0f, scene.fade), text)
            } else {
                drawScene(scene.scene, text)
            }
        }
    }
}

private const val CARD_H = 84f
private const val MOVE_SLOP = 6f
private const val DISMISS_PAST = 80f

/** The appear runs 700 ms in the kit (plus its delay), the dismiss 520; the card is gone 200 ms in. */
private const val APPEAR_MS = 700f
private const val DISMISS_MS = 520f
private const val GONE_AFTER_MS = 200L

/** Under reduce motion the appear and the dismiss cross-fade in 120 ms (the Core page's `fade 120`). */
private const val REDUCED_FADE_MS = 120f

/** The card's radii per index, the hole's for the one dismissed: the engine's `[11, 17, 13, 19]`. */
private val CARD_RADII =
    listOf(ToonRadii(16f, 10f, 18f, 12f), ToonRadii(11f, 17f, 13f, 19f), ToonRadii(18f, 12f, 15f, 10f))
private val HOLE_RADII = ToonRadii(11f, 17f, 13f, 19f)

private fun ToonRadii.times(k: Float) = ToonRadii(topLeft * k, topRight * k, bottomRight * k, bottomLeft * k)

private fun cardRadii(tokens: ToonTokens) = HOLE_RADII.times(tokens.shape.radiusScale)

/** The kit's press, drag and dismiss state, stepped on the frame clock. */
@Stable
internal class ToonCardState(
    appear: Boolean,
) {
    var nowMs by mutableLongStateOf(0L)
    var starts by mutableStateOf(0)
    var dismissStarts by mutableStateOf(0)
    var press by mutableFloatStateOf(0f)
    var target = 0f
    var lean: Float? = null
    var drag by mutableFloatStateOf(0f)
    private var dragV = 0f

    /** A press is down: a key's release clicks only after its press (B-69). */
    var held = false
        private set
    var gone by mutableStateOf(false)
    var dir = 1f
    var dragAtDismiss = 0f
    var appearAt: Long? = if (appear) PENDING_START else null
    var dismissAt: Long? by mutableStateOf(null)
    val dismissing: Boolean get() = dismissAt != null

    fun press(
        x: Float,
        w: Float,
    ) {
        held = true
        lean = max(-1f, min(1f, (x / w - .5f) * 2))
        target = 1f
        starts++
    }

    fun drag(d: Float) {
        target = 0f
        drag = d
    }

    /** The release: true when it threw the card away. */
    fun release(dismissible: Boolean): Boolean {
        held = false
        target = 0f
        starts++
        if (dismissible && abs(drag) > DISMISS_PAST) {
            dir = sign(drag)
            dragAtDismiss = drag
            drag = 0f
            dismissAt = PENDING_START
            dismissStarts++
            return true
        }
        return false
    }

    /** Thrown with no drag, by a reader's action: the design's throw to the right, from the slot. */
    fun throwAway() {
        held = false
        target = 0f
        drag = 0f
        dir = 1f
        dragAtDismiss = 0f
        dismissAt = PENDING_START
        dismissStarts++
        starts++
    }

    /**
     * How much later than the design's a swiped card's throw runs: it springs back to its slot first,
     * [RETURN_MS], in place of the engine's 70 ms wind-up (B-67). A reader's throw runs on time.
     */
    val throwShift: Float get() = if (dragAtDismiss != 0f) RETURN_MS - WIND_UP_MS else 0f

    /** The kit's `step`: the press eases toward its target; a let-go drag springs back (ω 30, ζ 0.55). */
    fun step(
        now: Long,
        ms: Float,
    ) {
        nowMs = now
        if (appearAt == PENDING_START) appearAt = now
        if (dismissAt == PENDING_START) dismissAt = now
        press += (target - press) * min(1f, ms / 35)
        if (!held && drag != 0f) {
            val w = SPRING_W
            val z = SPRING_Z
            val h = ms / 1000
            dragV += (-w * w * drag - 2 * z * w * dragV) * h
            drag += dragV * h
            if (abs(drag) < .3f && abs(dragV) < 3) {
                drag = 0f
                dragV = 0f
            }
        }
    }

    fun age(at: Long?): Float? = nowMs.let { now -> at?.let { if (it == PENDING_START) 0f else (now - it).toFloat() } }

    fun running(delayMs: Int): Boolean =
        abs(target - press) > .01f || drag != 0f || held ||
            (age(appearAt)?.let { it < APPEAR_MS + delayMs } ?: false) ||
            (age(dismissAt)?.let { it - throwShift < DISMISS_MS } ?: false)

    /**
     * How far the cards below have closed up over this one, in card steps: the engine's `cardDismiss`
     * `n`, 0 until 200 ms into the throw and −1 once settled. Under reduce motion the page's `fade 120`
     * moves nothing; the list takes the slot away when the fade is over.
     */
    fun closeUp(reduceMotion: Boolean): Float =
        if (reduceMotion || !dismissing) 0f else age(dismissAt)?.let { toonCardDismiss(it - throwShift).n } ?: 0f

    /** True once the throw has played out — 520 ms, or the 120 ms fade under reduce motion — and the slot can go. */
    fun closedUp(reduceMotion: Boolean): Boolean =
        // A card not thrown reads no time, so a list is not recomposed on every frame of a press.
        dismissing &&
            age(dismissAt)?.let { if (reduceMotion) it >= REDUCED_FADE_MS else it - throwShift >= DISMISS_MS } ?: false
}

private const val PENDING_START = -1L

/** A card frame and, under reduce motion, its cross-fade. */
internal class ToonCardMoment(
    val scene: ToonScene,
    val fade: Float?,
)

/** The kit's `C.card.scene`: the card placed by its appear, press and drag, or thrown off by its dismiss. */
internal fun toonCardFrame(
    tokens: ToonTokens,
    i: Int,
    w: Float,
    title: String,
    meta: String,
    time: String,
    rot0: Float,
    state: ToonCardState,
    delayMs: Int,
    reduceMotion: Boolean,
    selected: Boolean,
    disabled: Boolean,
    holeLocal: Boolean,
    focus: Boolean = false,
    hv: Float = 0f,
    timeWidth: Float = 0f,
): ToonCardMoment {
    val c = tokens.colors
    val ta = state.age(state.appearAt)
    val td = state.age(state.dismissAt)
    if (td != null) {
        if (reduceMotion) {
            val card =
                toonCardScene(
                    tokens,
                    i,
                    w,
                    title,
                    meta,
                    time,
                    ToonCardLook(selected = selected, disabled = disabled),
                    timeWidth,
                )
            return ToonCardMoment(card.placed(0f, 0f, 1f, rot0, w / 2, CARD_H / 2), max(0f, 1 - td / REDUCED_FADE_MS))
        }
        // A swiped card springs back to its slot, past it by the spring's overshoot, and is thrown from
        // there (B-67): the engine's throw, [throwShift] ms late. A reader's throw is the design's.
        val swiped = state.dragAtDismiss != 0f
        val d = toonCardDismiss(td - state.throwShift)
        val dx = toonCardThrowDx(td, state.dragAtDismiss, state.dir)
        val hu = (td - state.throwShift - 170) / 300
        var scene = ToonScene.Empty
        if (holeLocal && hu >= 0 && hu < 1) {
            scene +=
                toonHoleScene(
                    c,
                    hu,
                    0f,
                    0f,
                    w,
                    CARD_H,
                    cardRadii(tokens),
                    5,
                ).placed(0f, 0f, 1f, rot0, w / 2, CARD_H / 2)
        }
        if (d.lu >= 0 && d.lu < 1) {
            val lines = toonSpeedScene(c, d.lu, min(abs(dx), w), 0f, CARD_H)
            // A leftward throw mirrors the lines about the card.
            scene += if (state.dir > 0) lines else lines.scaledAbout(w / 2, 0f, -1f, 1f)
        }
        if (d.op > .01f) {
            val card =
                toonCardScene(
                    tokens,
                    i,
                    w,
                    title,
                    meta,
                    time,
                    ToonCardLook(selected = selected, disabled = disabled),
                    timeWidth,
                )
            scene +=
                if (swiped && td < RETURN_MS) {
                    // On its way back it is the card being dragged, its lift and turn let go as it nears the end.
                    val k = 1 - toonSmoothstep(td / RETURN_MS)
                    toonCardDragged(card, w, dx, 0f, 1f, rot0, k)
                } else {
                    val placed = card.placed(dx, 0f, 1f, rot0, w / 2, CARD_H / 2)
                    val smeared = placed.scaledAbout(w / 2 + dx, CARD_H / 2, d.sx, d.sy)
                    // A swiped card's words keep to their places on the smeared sheet, the time on its sticker.
                    ToonScene(
                        smeared.layers,
                        if (swiped) placed.labelsScaled(w / 2 + dx, CARD_H / 2, d.sx, d.sy) else placed.labels,
                    )
                }
        }
        return ToonCardMoment(scene, null)
    }
    if (state.gone) return ToonCardMoment(ToonScene.Empty, null)
    val a =
        when {
            ta == null -> CARD_RESTING.copy(rot = rot0)
            reduceMotion -> CARD_RESTING.copy(rot = rot0)
            else -> toonCardAppear(ta - delayMs, rot0)
        }
    val card =
        toonCardScene(
            tokens,
            i,
            w,
            title,
            meta,
            time,
            ToonCardLook(
                sx = a.sx,
                sy = a.sy,
                press = state.press,
                selected = selected,
                disabled = disabled,
                focus = focus,
                hv = hv,
            ),
            timeWidth,
        )
    val rot = a.rot + state.press * 1.6f * (state.lean ?: -.6f)
    val scene =
        toonCardDragged(card, w, state.drag, a.dy, a.op, rot, 1f) +
            toonDustScene(c, a.du, 34f, 86f, 36f) +
            toonDustScene(c, a.du, w - 34, 86f, 36f)
    val fade = if (reduceMotion && ta != null) min(1f, ta / REDUCED_FADE_MS) else null
    return ToonCardMoment(scene, fade?.takeIf { it < 1f })
}

/** The engine's `cardAppear` at [t] ms: opacity, the 24 dp fall with a 2° turn, the squash, the dust's life. */
internal data class ToonCardAppear(
    val op: Float,
    val dy: Float,
    val rot: Float,
    val sx: Float,
    val sy: Float,
    val du: Float,
)

private val CARD_RESTING = ToonCardAppear(1f, 0f, 0f, 1f, 1f, -1f)

internal fun toonCardAppear(
    t: Float,
    rot: Float,
): ToonCardAppear {
    if (t < 0) return ToonCardAppear(0f, -24f, rot + 2, 1f, 1f, -1f)
    if (t < 120) {
        val u = t / 120
        return ToonCardAppear(min(1f, u * 3), -24 * (1 - u * u), rot + 2 * (1 - u), 1f, 1f, -1f)
    }
    val a = toonOscillation((t - 120) / 1000, .35f, 38f)
    return ToonCardAppear(1f, 0f, rot, 1 + .06f * a, 1 - .1f * a, (t - 120) / 300)
}

/** The engine's `cardDismiss` at [t] ms: the throw, its smear, the lines' life, and the card below's close-up [n]. */
internal class ToonCardDismiss(
    val dx: Float,
    val sx: Float,
    val sy: Float,
    val op: Float,
    val lu: Float,
    val ghost: Boolean,
    val n: Float,
)

internal fun toonCardDismiss(t: Float): ToonCardDismiss {
    var dx = 0f
    var sx = 1f
    var sy = 1f
    var op = 1f
    var lu = -1f
    var n = 0f
    if (t < 70) {
        val u = t / 70
        dx = -10 * (1 - (1 - u).pow(2))
        sx = 1 - .04f * u
    } else if (t < 170) {
        val u = (t - 70) / 100
        dx = -10 + 440 * u * u
        sx = 1 + .5f * sin(PI.toFloat() * min(1f, u * 1.2f))
        sy = 1 - .15f * sin(PI.toFloat() * u)
        lu = u * .5f
    } else {
        dx = 440f
        op = 0f
        lu = .5f + (t - 170) / 200
    }
    if (t > 200) n = -(1 - toonSpring((t - 200) / 1000, .55f, 40f))
    return ToonCardDismiss(dx, sx, sy, op, lu, t >= 170 && t < 212, n)
}

/** What one frame of the card draws: the engine's `card` options. */
internal data class ToonCardLook(
    val sx: Float = 1f,
    val sy: Float = 1f,
    val press: Float = 0f,
    val selected: Boolean = false,
    val disabled: Boolean = false,
    val textOp: Float = 1f,
    /** The pointer page's focus ring, 6 dp out (B-69): the kit drew none on the card. */
    val focus: Boolean = false,
    /** The pointer's hover, 0 to 1 (B-69): the sheet lifts toward the light, its shadow stays. */
    val hv: Float = 0f,
)

private val TITLE = ToonTextStyle(700, 18f)
private val META = ToonTextStyle(500, 14f)
private val TIME = ToonTextStyle(700, 15f)

/** The design's time sticker: 64 dp, which holds its widest time, "20:00" at 43 dp, 10 dp in from each side. */
private const val STICKER_W = 64f
private const val STICKER_PAD = 10f

/** A sticker grows to hold a longer time up to this share of the card; a time longer still is cut (B-83). */
private const val STICKER_MAX = .45f

/** The title and meta end this far short of the sticker. */
private const val TEXT_GAP = 10f

/** The sticker's width on a card [w] wide for a time measured [timeWidth] wide (B-83). */
internal fun toonCardStickerWidth(
    w: Float,
    timeWidth: Float,
): Float = max(STICKER_W, min(w * STICKER_MAX, timeWidth + 2 * STICKER_PAD))

private val STICKER_RADII = ToonRadii(14f, 9f, 15f, 10f)

/**
 * The engine's `card`: one frame [w] dp wide and 84 tall, from its top left. [timeWidth] is the time
 * measured: its sticker grows to hold it past the design's 64 dp, up to [STICKER_MAX] of the card, and
 * the title and meta are cut with an ellipsis short of the sticker (B-83). Zero, the default, is the
 * design's sticker, which holds the design's times.
 */
@OptIn(InternalToonApi::class)
internal fun toonCardScene(
    tokens: ToonTokens,
    index: Int,
    w: Float,
    title: String,
    meta: String,
    time: String,
    o: ToonCardLook,
    timeWidth: Float = 0f,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val h = CARD_H
    val i = index.mod(3)
    val r = CARD_RADII[i].times(sh.radiusScale)
    val press = o.press
    val sx = o.sx * (1 - .02f * press)
    val sy = o.sy * (1 - .02f * press)
    val bw = w * sx
    val bh = h * sy
    val x = (w - bw) / 2
    val y = h - bh
    val dy = (h - bh) / 2
    val dis = o.disabled
    val k = if (o.selected) 1.6f else 1f
    val ins = sh.insets.let { ToonInsets(it.top * k, it.right * k, it.bottom * k, it.left * k) }
    val layers = mutableListOf<ToonLayer>()
    if (o.focus) layers += ringLayer(-6f, -6f, w + 12, h + 12, FOCUS_RADII, c, sh.wobble, sh.seed + 31)
    layers +=
        bodyLayers(
            shapePoints(x, y, bw, bh, r, ins, sh.wobble, sh.seed + i * 13),
            fill = c.cardFills[i],
            colors = c,
            shadow = if (dis) 0f else sh.shadow * (1 - .7f * press),
            inkAlpha = if (dis) .4f else 1f,
        )
    val stw = toonCardStickerWidth(w, timeWidth)
    val sth = 40f
    val stx = w - stw - 18
    val sty = (h - sth) / 2 - 2 + dy
    val sIns = sh.insets.let { ToonInsets(it.top * .8f, it.right * .8f, it.bottom * .8f, it.left * .8f) }
    layers +=
        bodyLayers(
            shapePoints(stx, sty, stw, sth, STICKER_RADII, sIns, sh.wobble * .8f, sh.seed + i * 17 + 5, 4f),
            fill = c.stickers[i],
            colors = c,
            shadow = 0f,
            inkAlpha = if (dis) .4f else 1f,
            rim = null,
        )
    val op = if (dis) .5f else 1f
    val bo = o.textOp
    val labels =
        listOf(
            ToonLabel(title, 22f, 38 + dy, c.text, op * bo, TITLE, ToonAnchor.Start, stx - TEXT_GAP - 22f),
            ToonLabel(meta, 22f, 60 + dy, c.textMuted, op * bo, META, ToonAnchor.Start, stx - TEXT_GAP - 22f),
            ToonLabel(time, stx + stw / 2, sty + 25, c.ink, op * bo, TIME, ToonAnchor.Middle, stw - 2 * STICKER_PAD),
        )
    return ToonScene(layers, labels).hoverLifted(if (dis) 0f else HOVER_LIFT * o.hv * (1 - min(1f, press)), c.ink)
}

/** The ring's radii on the pointer page, 6 dp out from the sheet. */
private val FOCUS_RADII = ToonRadii(22f, 16f, 24f, 18f)

private val ACTIVATE = setOf(Key.Enter, Key.NumPadEnter, Key.Spacebar)

/** The kit's drag spring: ω 30, ζ 0.55. */
private const val SPRING_W = 30f
private const val SPRING_Z = .55f
private val SPRING_WD = SPRING_W * sqrt(1 - SPRING_Z * SPRING_Z)

/** The engine's wind-up, the 70 ms before its fling, which a swiped card's spring back stands in for. */
private const val WIND_UP_MS = 70f

/**
 * A swiped card's way back (B-67): the drag spring let go from rest, to the far end of its first
 * overshoot, where it stands still for an instant — 125 ms, and 12.6 % of the drag past the slot.
 */
internal val RETURN_MS: Float = (PI / SPRING_WD * 1000).toFloat()

/** Where the drag spring, let go [dragAtDismiss] dp out at rest, has the card [t] ms later. */
internal fun toonCardSpringBack(
    t: Float,
    dragAtDismiss: Float,
): Float {
    val s = t / 1000
    val ratio = SPRING_Z / sqrt(1 - SPRING_Z * SPRING_Z)
    return dragAtDismiss * exp(-SPRING_Z * SPRING_W * s) * (cos(SPRING_WD * s) + ratio * sin(SPRING_WD * s))
}

/**
 * How far a thrown card stands from its slot [td] ms into the throw, let go [dragAtDismiss] dp out
 * and thrown toward [dir]. Swiped, it springs back past its slot for [RETURN_MS] and flies from
 * where it stopped, at the engine's fling (B-67); thrown with no drag, it is the kit's throw.
 */
internal fun toonCardThrowDx(
    td: Float,
    dragAtDismiss: Float,
    dir: Float,
): Float {
    if (dragAtDismiss == 0f) return dir * max(0f, toonCardDismiss(td).dx)
    if (td < RETURN_MS) return toonCardSpringBack(td, dragAtDismiss)
    val wound = toonCardSpringBack(RETURN_MS, dragAtDismiss)
    val te = td - RETURN_MS + WIND_UP_MS
    return wound + dir * (toonCardDismiss(te).dx - toonCardDismiss(WIND_UP_MS).dx)
}

internal fun toonSmoothstep(u: Float): Float = min(1f, max(0f, u)).let { it * it * (3 - 2 * it) }

/** How much a drag turns the card: the kit's 0.035° a dp. */
private const val DRAG_TURN = .035f

/**
 * The card [dx] dp off its slot as a drag holds it: lifted off the paper and turned toward the drag,
 * by [k] of the drag's look (1 while dragged). Its words go where the turn takes their places and
 * stay level; the design leaves them where they stood, so the time slid off its sticker (B-67).
 */
private fun toonCardDragged(
    card: ToonScene,
    w: Float,
    dx: Float,
    dy: Float,
    op: Float,
    rot: Float,
    k: Float,
): ToonScene {
    val turn = dx * DRAG_TURN * k
    val px = w / 2 + dx * k
    val placed = card.placed(dx, dy, op * (1 - min(.35f, abs(dx) / 400) * k), rot + turn, px, CARD_H / 2)
    return placed.labelsTurned(turn, px + dx, CARD_H / 2 + dy)
}

/** The labels' places turned [degrees] about ([cx], [cy]), the words themselves level. */
private fun ToonScene.labelsTurned(
    degrees: Float,
    cx: Float,
    cy: Float,
): ToonScene {
    if (degrees == 0f) return this
    val r = degrees * PI.toFloat() / 180
    val cs = cos(r)
    val sn = sin(r)
    return ToonScene(
        layers,
        labels.map {
            val x = it.x - cx
            val y = it.baseline - cy
            ToonLabel(
                it.text,
                cx + cs * x - sn * y,
                cy + sn * x + cs * y,
                it.color,
                it.alpha,
                it.style,
                it.anchor,
                it.maxWidth,
            )
        },
    )
}

/** The labels' places scaled about ([cx], [cy]), the words themselves unscaled. */
private fun ToonScene.labelsScaled(
    cx: Float,
    cy: Float,
    sx: Float,
    sy: Float,
): List<ToonLabel> =
    labels.map {
        ToonLabel(
            it.text,
            cx + (it.x - cx) * sx,
            cy + (it.baseline - cy) * sy,
            it.color,
            it.alpha,
            it.style,
            it.anchor,
            it.maxWidth,
        )
    }
