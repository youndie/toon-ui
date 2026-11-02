package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
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
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonTextStyle
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.fx.LocalToonFxEmitter
import io.github.youndie.toon.fx.NoToonFxHost
import io.github.youndie.toon.fx.ToonFxEvent
import io.github.youndie.toon.fx.toonDustScene
import io.github.youndie.toon.fx.toonHoleScene
import io.github.youndie.toon.fx.toonSpeedScene
import io.github.youndie.toon.motion.toonOscillation
import io.github.youndie.toon.motion.toonSpring
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

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
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val text = rememberToonText()
    val fx = LocalToonFxEmitter.current
    val w = width.value
    val i = index.mod(3)
    val rot0 = if (tilt) tokens.shape.tilts[i] else 0f
    val state = remember { ToonCardState(appear) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val click by rememberUpdatedState(onClick)
    val dismissed by rememberUpdatedState(onDismiss)

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
        if (fx !== NoToonFxHost && !reduce) {
            // A host draws the hole above everything; the card leaves it to the host.
            fx.emit(ToonFxEvent.CardGone(bounds, cardRadii(tokens)))
        }
        var waited = 0L
        val start = withFrameMillis { it }
        while (!reduce && waited < GONE_AFTER_MS) waited = withFrameMillis { it } - start
        state.gone = true
        dismissed()
    }

    val holeLocal = fx === NoToonFxHost
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
    private var held = false
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
            val w = 30f
            val z = .55f
            val h = ms / 1000
            dragV += (-w * w * drag - 2 * z * w * dragV) * h
            drag += dragV * h
            if (abs(drag) < .3f && abs(dragV) < 3) {
                drag = 0f
                dragV = 0f
            }
        }
    }

    fun age(at: Long?): Float? = at?.let { if (it == PENDING_START) 0f else (nowMs - it).toFloat() }

    fun running(delayMs: Int): Boolean =
        abs(target - press) > .01f || drag != 0f || held ||
            (age(appearAt)?.let { it < APPEAR_MS + delayMs } ?: false) ||
            (age(dismissAt)?.let { it < DISMISS_MS } ?: false)
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
): ToonCardMoment {
    val c = tokens.colors
    val ta = state.age(state.appearAt)
    val td = state.age(state.dismissAt)
    if (td != null) {
        if (reduceMotion) {
            val card =
                toonCardScene(tokens, i, w, title, meta, time, ToonCardLook(selected = selected, disabled = disabled))
            return ToonCardMoment(card.placed(0f, 0f, 1f, rot0, w / 2, CARD_H / 2), max(0f, 1 - td / REDUCED_FADE_MS))
        }
        val d = toonCardDismiss(td)
        val dx = state.dragAtDismiss * max(0f, 1 - td / 70) + state.dir * max(0f, d.dx)
        val hu = (td - 170) / 300
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
                toonCardScene(tokens, i, w, title, meta, time, ToonCardLook(selected = selected, disabled = disabled))
            val placed = card.placed(dx, 0f, 1f, rot0, w / 2, CARD_H / 2)
            scene += ToonScene(placed.scaledAbout(w / 2 + dx, CARD_H / 2, d.sx, d.sy).layers, placed.labels)
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
            ToonCardLook(sx = a.sx, sy = a.sy, press = state.press, selected = selected, disabled = disabled),
        )
    val drag = state.drag
    val rot = a.rot + state.press * 1.6f * (state.lean ?: -.6f) + drag * .035f
    val scene =
        card.placed(drag, a.dy, a.op * (1 - min(.35f, abs(drag) / 400)), rot, w / 2 + drag, CARD_H / 2) +
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
)

private val TITLE = ToonTextStyle(700, 18f)
private val META = ToonTextStyle(500, 14f)
private val TIME = ToonTextStyle(700, 15f)
private val STICKER_RADII = ToonRadii(14f, 9f, 15f, 10f)

/** The engine's `card`: one frame [w] dp wide and 84 tall, from its top left. */
@OptIn(InternalToonApi::class)
internal fun toonCardScene(
    tokens: ToonTokens,
    index: Int,
    w: Float,
    title: String,
    meta: String,
    time: String,
    o: ToonCardLook,
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
    val layers =
        bodyLayers(
            shapePoints(x, y, bw, bh, r, ins, sh.wobble, sh.seed + i * 13),
            fill = c.cardFills[i],
            colors = c,
            shadow = if (dis) 0f else sh.shadow * (1 - .7f * press),
            inkAlpha = if (dis) .4f else 1f,
        ).toMutableList()
    val stw = 64f
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
            ToonLabel(title, 22f, 38 + dy, c.text, op * bo, TITLE, ToonAnchor.Start),
            ToonLabel(meta, 22f, 60 + dy, c.textMuted, op * bo, META, ToonAnchor.Start),
            ToonLabel(time, stx + stw / 2, sty + 25, c.ink, op * bo, TIME),
        )
    return ToonScene(layers, labels)
}
