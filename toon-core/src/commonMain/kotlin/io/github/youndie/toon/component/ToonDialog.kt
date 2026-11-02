package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonAnchor
import io.github.youndie.toon.draw.ToonLabel
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonTextStyle
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.irisLayer
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.fx.LocalToonFxEmitter
import io.github.youndie.toon.fx.NoToonFxHost
import io.github.youndie.toon.fx.ToonFxEvent
import io.github.youndie.toon.fx.toonStarsScene
import io.github.youndie.toon.motion.toonOscillation
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// A port of the engine's `dialog`, `dialogOpen`, `dialogClose` and `iris`, and the kit's `C.dialog`
// (B-24): a full-screen layer. Darkness closes in from the edges as an iris and stops as a spotlight
// around the dialog, which pops 0.6 → 1.1 → 1.0; on close it swells, collapses to a point with a burst
// of stars, and the iris opens back up.

/** Which of the dialog's two buttons carries the keyboard focus ring. */
internal enum class ToonDialogFocus { Confirm, Cancel }

/**
 * The design's dialog over the whole of its box: [title], [body] wrapped to the dialog's width, and
 * two buttons, [cancel] and [confirm] — the latter drawn `onDanger` on `danger` when [destructive]. A
 * tap on [confirm] calls [onConfirm]; on [cancel] or outside the dialog, [onCancel]. It irises in when
 * [open] turns true and pops out when it turns false; closed, it takes no input.
 */
@Composable
public fun ToonDialog(
    title: String,
    modifier: Modifier = Modifier,
    open: Boolean = true,
    body: String = "",
    confirm: String = "OK",
    cancel: String = "Cancel",
    destructive: Boolean = true,
    onConfirm: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val fx = LocalToonFxEmitter.current
    val texts = rememberToonText()
    val clock = remember { ToonDialogClock(open) }
    val ok by rememberUpdatedState(onConfirm)
    val no by rememberUpdatedState(onCancel)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var focusOn by remember { mutableStateOf(ToonDialogFocus.Confirm) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val lines = remember(body, texts) { toonDialogLines(body) { texts.measure(it, DIALOG_BODY).multiParagraph.width } }
    val cancelWidth = remember(cancel, texts) { texts.measure(cancel, BUTTON_LABEL).multiParagraph.width }
    val card =
        toonDialogCardScene(tokens, title, lines, confirm, cancel, destructive, focusOn.takeIf { focused }, cancelWidth)
    val cardH = toonDialogHeight(lines.size)

    LaunchedEffect(open) { clock.changed(open) }
    LaunchedEffect(clock.starts) { while (clock.running()) withFrameMillis { clock.tick(it) } }
    LaunchedEffect(clock.starts) {
        // A host draws the pop's stars above everything (`dialog.pop`), from the moment the dialog
        // has collapsed; without one the dialog draws them itself, as the kit does.
        if (clock.open || clock.starts == 0 || fx === NoToonFxHost || reduce) return@LaunchedEffect
        delay(STARS_AT_MS.toLong())
        fx.emit(ToonFxEvent.DialogPop(bounds, at = bounds.center, spread = STARS_SPREAD))
    }

    val active = clock.open
    Canvas(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .then(
                if (active) {
                    Modifier
                        .semantics {
                            paneTitle = title
                            dialog()
                            customActions =
                                listOf(
                                    CustomAccessibilityAction(confirm) { ok().let { true } },
                                    CustomAccessibilityAction(cancel) { no().let { true } },
                                )
                        }.onKeyEvent {
                            if (it.type != KeyEventType.KeyDown) return@onKeyEvent false
                            when (it.key) {
                                Key.Tab, Key.DirectionLeft, Key.DirectionRight -> {
                                    focusOn =
                                        if (focusOn ==
                                            ToonDialogFocus.Confirm
                                        ) {
                                            ToonDialogFocus.Cancel
                                        } else {
                                            ToonDialogFocus.Confirm
                                        }
                                    true
                                }

                                Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                                    if (focusOn == ToonDialogFocus.Confirm) ok() else no()
                                    true
                                }

                                Key.Escape -> {
                                    no()
                                    true
                                }

                                else -> {
                                    false
                                }
                            }
                        }.focusable(interactionSource = interaction)
                        .pointerInput(lines, cardH) {
                            awaitEachGesture {
                                awaitFirstDown()
                                val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                                // Coordinates are taken as dp, as every component draws them (B-47).
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                when (toonDialogHit(w, h, cardH, up.position.x, up.position.y)) {
                                    ToonDialogHit.Confirm -> ok()
                                    ToonDialogHit.Cancel -> no()
                                    null -> Unit
                                }
                            }
                        }
                } else {
                    Modifier
                },
            ),
    ) {
        val w = size.width
        val h = size.height
        val moment = toonDialogFrameAt(clock.open, clock.age(), reduce) ?: return@Canvas
        val scene = toonDialogLayerScene(tokens, w, h, card, cardH, moment, stars = fx === NoToonFxHost)
        drawScene(scene, texts)
    }
}

private const val DIALOG_W = 300f
private const val OK_W = 128f
private const val CANCEL_W = 104f

/** The kit wraps the body at 252 dp, in the body face at 400 and 15. */
private const val BODY_WRAP = 252f
internal val DIALOG_TITLE = ToonTextStyle(400, 24f, display = true)
internal val DIALOG_BODY = ToonTextStyle(400, 15f)

/** The kit plays the open for 500 ms and the close for 700, drawn for 320; reduce motion fades in 120. */
private const val OPEN_MS = 500f
private const val CLOSE_MS = 700f
private const val CLOSE_DRAWN_MS = 320f
private const val REDUCED_FADE_MS = 120f

/** The stars start 170 ms into the close, on a 70 dp arc. */
private const val STARS_AT_MS = 170f
private const val STARS_SPREAD = 70f

/** The kit's `lines`: [body]'s words, wrapped where a line would measure more than 252 dp. */
internal fun toonDialogLines(
    body: String,
    width: (String) -> Float,
): List<String> {
    val out = mutableListOf<String>()
    var cur = ""
    for (word in body.split(Regex("\\s+")).filter { it.isNotEmpty() }) {
        val next = if (cur.isEmpty()) word else "$cur $word"
        if (width(next) > BODY_WRAP && cur.isNotEmpty()) {
            out += cur
            cur = word
        } else {
            cur = next
        }
    }
    if (cur.isNotEmpty()) out += cur
    return out
}

/** The engine's dialog height: title, body lines at 22 dp, buttons. */
internal fun toonDialogHeight(lines: Int): Float = 24f + 30 + 10 + lines * 22 + 22 + 52 + 20

/** One frame of the dialog's motion: the iris radius on the engine's scale, the pop, its fades, the stars. */
internal data class ToonDialogMotion(
    val r: Float,
    val s: Float,
    val op: Float,
    val textOp: Float,
    val stars: Float,
    /** Reduce motion's fade of the whole layer; 1 otherwise. */
    val fade: Float = 1f,
)

/** The engine's `dialogOpen` at [t] ms. */
internal fun toonDialogOpen(t: Float): ToonDialogMotion {
    val u = c01(t / 260)
    val e = if (u < .5f) 2 * u * u else 1 - (-2 * u + 2).pow(2) / 2
    val s = if (t < 120) .6f + .5f * (t / 120) else 1 + .1f * toonOscillation((t - 120) / 1000, .4f, 40f)
    return ToonDialogMotion(330 + (190 - 330) * e, s, c01(t / 60), c01((t - 140) / 80), 0f)
}

/** The engine's `dialogClose` at [t] ms. */
internal fun toonDialogClose(t: Float): ToonDialogMotion {
    val s =
        if (t < 80) {
            1 + .05f * t / 80
        } else {
            val u = c01((t - 80) / 100)
            1.05f * (1 - u * u)
        }
    val u2 = c01((t - 120) / 180)
    return ToonDialogMotion(
        190 + 140 * u2 * u2,
        s,
        if (t <
            180
        ) {
            1f
        } else {
            0f
        },
        1 - c01(t / 60),
        if (t >= 170) (t - 170) / 350 else 0f,
    )
}

/** The dialog at rest: the spotlight closed down on it. */
internal val DIALOG_AT_REST = ToonDialogMotion(190f, 1f, 1f, 1f, 0f)

/**
 * The kit's `C.dialog.scene` at a moment: [open], the last change [ageMs] ago (null for one open
 * from the start). Null when nothing is drawn. Under reduce motion — the MVP page's `fade 120` for
 * both — the layer fades in and out at rest, without the pop or the stars.
 */
internal fun toonDialogFrameAt(
    open: Boolean,
    ageMs: Float?,
    reduceMotion: Boolean,
): ToonDialogMotion? {
    if (reduceMotion && ageMs != null) {
        val u = min(1f, ageMs / REDUCED_FADE_MS)
        if (!open && u >= 1) return null
        return DIALOG_AT_REST.copy(fade = if (open) u else 1 - u)
    }
    if (open) return if (ageMs == null) DIALOG_AT_REST else toonDialogOpen(ageMs)
    if (ageMs == null || ageMs >= CLOSE_DRAWN_MS) return null
    return toonDialogClose(ageMs)
}

private fun c01(v: Float): Float = max(0f, min(1f, v))

/** Where a release landed: on the confirm button, on cancel or outside the dialog, or inside it. */
internal enum class ToonDialogHit { Confirm, Cancel }

/** The kit's `up`: the hit for a release at ([x], [y]) in a [w] × [h] layer with a dialog [cardH] tall. */
internal fun toonDialogHit(
    w: Float,
    h: Float,
    cardH: Float,
    x: Float,
    y: Float,
): ToonDialogHit? {
    val x0 = (w - DIALOG_W) / 2
    val y0 = h / 2 - cardH / 2
    val by = y0 + cardH - 72
    val okX = x0 + DIALOG_W - 24 - OK_W
    val onRow = y >= by && y <= by + 52
    if (onRow && x >= okX && x <= okX + OK_W) return ToonDialogHit.Confirm
    if ((onRow && x >= okX - 110 && x < okX) || x < x0 || x > x0 + DIALOG_W || y < y0 ||
        y > y0 + cardH
    ) {
        return ToonDialogHit.Cancel
    }
    return null
}

private val DIALOG_RADII = ToonRadii(20f, 14f, 22f, 16f)

/** The engine's `dialog`: the 300 dp card from its top left, [toonDialogHeight] tall. */
@OptIn(InternalToonApi::class)
internal fun toonDialogCardScene(
    tokens: ToonTokens,
    title: String,
    lines: List<String>,
    confirm: String,
    cancel: String,
    destructive: Boolean,
    focus: ToonDialogFocus?,
    cancelWidth: Float,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val w = DIALOG_W
    val h = toonDialogHeight(lines.size)
    val rs = sh.radiusScale
    val r =
        ToonRadii(
            DIALOG_RADII.topLeft * rs,
            DIALOG_RADII.topRight * rs,
            DIALOG_RADII.bottomRight * rs,
            DIALOG_RADII.bottomLeft * rs,
        )
    val layers =
        bodyLayers(
            shapePoints(0f, 0f, w, h, r, sh.insets, sh.wobble, sh.seed + 650),
            c.surface,
            c,
            sh.shadow + 2,
        )
    val labels = mutableListOf(ToonLabel(title, 24f, 48f, c.text, 1f, DIALOG_TITLE, ToonAnchor.Start))
    lines.forEachIndexed { i, ln ->
        labels +=
            ToonLabel(ln, 24f, 80f + i * 22, c.textMuted, 1f, DIALOG_BODY, ToonAnchor.Start)
    }
    val by = h - 72
    val no =
        toonButtonScene(
            tokens,
            ToonButtonVariant.Text,
            CANCEL_W,
            cancel,
            cancelWidth,
            ToonButtonLook(
                focus =
                    focus == ToonDialogFocus.Cancel,
            ),
        ).placed(w - 24 - OK_W - 6 - CANCEL_W, by)
    val yes =
        toonButtonScene(
            tokens,
            ToonButtonVariant.Filled,
            OK_W,
            confirm,
            0f,
            ToonButtonLook(
                focus = focus == ToonDialogFocus.Confirm,
                fill = if (destructive) c.danger else null,
                labelColor = if (destructive) c.onDanger else null,
            ),
        ).placed(w - 24 - OK_W, by)
    val buttons = no + yes
    return ToonScene(layers + buttons.layers, labels + buttons.labels)
}

/**
 * The layer the kit and the page both draw over what is behind: the iris of radius [r] about
 * ([w] / 2, [cy]) in a [w] × [h] window, the [card] centred on it and popped by [a], and the stars
 * when [stars] is set (else a host draws them).
 */
internal fun toonDialogOverlay(
    tokens: ToonTokens,
    w: Float,
    h: Float,
    cy: Float,
    r: Float,
    card: ToonScene,
    cardH: Float,
    a: ToonDialogMotion,
    stars: Boolean = true,
): ToonScene {
    val cx = w / 2
    var scene = ToonScene(listOf(irisLayer(w, h, cx, cy, r, tokens.colors.ink, IRIS_ALPHA)), emptyList())
    if (a.op > .01f && a.s > .01f) {
        val placed = card.placed((w - DIALOG_W) / 2, cy - cardH / 2)
        val popped = placed.scaledAbout(cx, cy, max(.001f, a.s), max(.001f, a.s))
        scene +=
            ToonScene(
                popped.layers.map { it.copy(alpha = it.alpha * a.op) },
                popped.labels.map {
                    ToonLabel(
                        it.text,
                        it.x,
                        it.baseline,
                        it.color,
                        it.alpha * a.textOp,
                        it.style,
                        it.anchor,
                    )
                },
            )
    }
    if (stars && a.stars > 0) scene += toonStarsScene(tokens.colors, a.stars, cx, cy, STARS_SPREAD)
    return scene
}

/** The iris darkens what is outside it to 62 %. */
private const val IRIS_ALPHA = .62f

/** The kit's scene in a [w] × [h] layer: the iris reaches the corners at its widest and hugs the card at rest. */
internal fun toonDialogLayerScene(
    tokens: ToonTokens,
    w: Float,
    h: Float,
    card: ToonScene,
    cardH: Float,
    a: ToonDialogMotion,
    stars: Boolean = true,
): ToonScene {
    val cx = w / 2
    val cy = h / 2
    val far = hypot(cx, cy) + 10
    val near = hypot(DIALOG_W / 2, cardH / 2) + 16
    val r = near + (far - near) * max(0f, min(1.2f, (a.r - 190) / 140))
    val scene = toonDialogOverlay(tokens, w, h, cy, r, card, cardH, a, stars)
    return if (a.fade == 1f) scene else scene.placed(0f, 0f, a.fade)
}

@Stable
private class ToonDialogClock(
    initial: Boolean,
) {
    var nowMs by mutableLongStateOf(0L)
    var starts by mutableStateOf(0)
    var open by mutableStateOf(initial)
    private var at: Long? = if (initial) PENDING_START else null

    fun changed(now: Boolean) {
        if (now == open) return
        open = now
        at = PENDING_START
        starts++
    }

    fun tick(now: Long) {
        nowMs = now
        if (at == PENDING_START) at = now
    }

    fun age(): Float? = at?.let { if (it == PENDING_START) 0f else (nowMs - it).toFloat() }

    fun running(): Boolean = age()?.let { it < (if (open) OPEN_MS else CLOSE_MS) } ?: false
}

private const val PENDING_START = -1L
