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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonLabel
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonSegments
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.ringLayer
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.spinnerPoints
import io.github.youndie.toon.draw.wobblePoints
import io.github.youndie.toon.fx.drawToonVibe
import io.github.youndie.toon.fx.toonVibeOffset
import io.github.youndie.toon.interaction.rememberToonPressState
import io.github.youndie.toon.interaction.toonPressable
import io.github.youndie.toon.motion.toonOscillation
import io.github.youndie.toon.motion.toonSquashOf
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.theme.rememberToonIdleSlot
import io.github.youndie.toon.tokens.ToonSize
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.ToonTypeScale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// A port of the engine's `btn` and the kit's `C.button` (B-14): three variants, the squash on press,
// the loading hand-off (the label flies up, an ink blob drops into its place and spins), idle
// breathing and boiling under the idle budget, the disabled buzz, and the focus ring. Numbers are the
// engine's; the label is the Core page's Title step, 18 sp bold in the theme's body face.

/** The `variant` prop of `ToonButton.dc.html`. */
public enum class ToonButtonVariant {
    Filled,
    Outlined,
    Text,
}

/**
 * The design's button: [label] in a thick, uneven ink outline that squashes into the paper when
 * pressed. [onClick] fires on the release, never while [loading]. [width] 0 fits the label (56 dp of
 * padding, 120 dp at least); [idle] lets the screen's main action breathe, if the theme's idle budget
 * grants it a slot. A press on a disabled button buzzes instead.
 */
@Composable
public fun ToonButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ToonButtonVariant = ToonButtonVariant.Filled,
    width: Dp = 0.dp,
    enabled: Boolean = true,
    loading: Boolean = false,
    idle: Boolean = false,
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val text = rememberToonButtonLabel(label)
    val w =
        if (width >
            0.dp
        ) {
            width.value
        } else {
            max(MIN_WIDTH, (text.multiParagraph.width + LABEL_PADDING).roundToInt().toFloat())
        }
    val press = rememberToonPressState()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val breathing by rememberToonIdleSlot(idle && variant != ToonButtonVariant.Text && enabled)
    val clock = remember { ToonButtonClock() }
    val click by rememberUpdatedState(onClick)
    val loadingNow by rememberUpdatedState(loading)

    LaunchedEffect(loading) { clock.loadStartMs = if (loading) PENDING else null }
    LaunchedEffect(loading, breathing, clock.buzzes) {
        while (loading || breathing || clock.buzzing() || clock.loadStartMs == PENDING) {
            withFrameMillis { now -> clock.tick(now) }
        }
    }

    val frame =
        toonButtonFrameAt(
            nowMs = clock.nowMs,
            loadAgeMs = clock.loadAge(),
            buzzAgeMs = clock.buzzAge(),
            p = press.progress,
            breathing = breathing,
            disabled = !enabled,
            focus = focused,
            reduceMotion = reduce,
        )
    val look = frame.look
    val fade = frame.fade
    val buzz = frame.buzz

    Canvas(
        modifier
            .size(w.dp, ToonSize.BUTTON.dp)
            .semantics {
                this.text = AnnotatedString(label)
                if (loading) progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }.toonPressable(press, enabled = enabled) { if (!loadingNow) click() }
            .pointerInput(enabled) {
                if (enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown()
                    clock.buzz()
                }
            }.focusable(enabled, interaction)
            .onPreviewKeyEvent { e ->
                if (!enabled || e.key !in ACTIVATE) return@onPreviewKeyEvent false
                when (e.type) {
                    KeyEventType.KeyDown -> press.press()
                    KeyEventType.KeyUp -> if (press.release() && !loadingNow) click()
                }
                true
            },
    ) {
        val dx = buzz?.let { toonVibeOffset(it / BUZZ_MS) } ?: 0f
        if (buzz != null) drawToonVibe(tokens.colors, buzz / BUZZ_MS, 0f, 0f, w, ToonSize.BUTTON)
        translate(dx, 0f) {
            if (fade != null && fade < 1f) {
                // Reduce motion's `fade 120`: the resting button fades into the loading one.
                withAlpha(1 - fade) { drawToonButton(tokens, variant, w, text, look.copy(lt = null)) }
                withAlpha(fade) { drawToonButton(tokens, variant, w, text, look) }
            } else {
                drawToonButton(tokens, variant, w, text, look)
            }
        }
    }
}

private val ACTIVATE = setOf(Key.Enter, Key.NumPadEnter, Key.Spacebar)
private const val MIN_WIDTH = 120f
private const val LABEL_PADDING = 56f

/** Idle: a 1.5 % breath every 2500 ms, the outline boiling through three seeds at 10 fps. */
private const val BREATH = .0075f
private const val BREATH_MS = 2500f
private const val BOIL_MS = 100
private const val BOIL_SEED = 31

/** The disabled buzz lasts 320 ms; under reduce motion the loading hand-off cross-fades in 120. */
private const val BUZZ_MS = 320f
private const val REDUCED_FADE_MS = 120f

/** The kit's age of an animation under reduce motion: long finished. */
private const val SETTLED = 1e6f

private const val PENDING = -1L

/**
 * What the button draws at frame time [nowMs]: its [look], the reduce-motion cross-fade into loading
 * ([fade], 0 to 1, or null when not fading), and the disabled buzz's age ([buzz], or null).
 */
internal class ToonButtonMoment(
    val look: ToonButtonLook,
    val fade: Float?,
    val buzz: Float?,
)

/**
 * The kit's `C.button.scene` at frame time [nowMs]: loading [loadAgeMs] and the buzz [buzzAgeMs] ago
 * (null when neither runs), under press [p]. Idle breathes 1.5 % over 2500 ms and boils through three
 * seeds at 10 fps. Under reduce motion — the Core page's words — the loading hand-off is `fade 120`
 * into its settled frame, idle is `off` (no slot is granted), and there is no buzz, as in the kit.
 */
internal fun toonButtonFrameAt(
    nowMs: Long,
    loadAgeMs: Float?,
    buzzAgeMs: Float?,
    p: Float,
    breathing: Boolean,
    disabled: Boolean,
    focus: Boolean,
    reduceMotion: Boolean,
): ToonButtonMoment =
    ToonButtonMoment(
        look =
            ToonButtonLook(
                p = p,
                br = if (breathing) 1 + BREATH * (1 - cos(2 * PI.toFloat() * nowMs / BREATH_MS)) else 1f,
                seed = if (breathing) floor(nowMs / BOIL_MS.toFloat()).toInt() % 3 * BOIL_SEED else 0,
                disabled = disabled,
                focus = focus,
                lt = loadAgeMs?.let { if (reduceMotion) SETTLED else it },
                spin = nowMs.toFloat(),
            ),
        fade = if (reduceMotion) loadAgeMs?.let { min(1f, it / REDUCED_FADE_MS) } else null,
        buzz = if (reduceMotion) null else buzzAgeMs?.takeIf { it < BUZZ_MS },
    )

/** The button's own clock: frame time, and when loading and the last buzz started. */
@Stable
private class ToonButtonClock {
    var nowMs by mutableLongStateOf(0L)
    var loadStartMs: Long? by mutableStateOf(null)
    var buzzStartMs: Long? by mutableStateOf(null)
    var buzzes by mutableStateOf(0)

    fun tick(now: Long) {
        nowMs = now
        if (loadStartMs == PENDING) loadStartMs = now
        if (buzzStartMs == PENDING) buzzStartMs = now
    }

    fun buzz() {
        buzzStartMs = PENDING
        buzzes++
    }

    fun buzzing(): Boolean = buzzStartMs.let { it == PENDING || (it != null && nowMs - it < BUZZ_MS) }

    fun loadAge(): Float? = loadStartMs?.let { if (it == PENDING) 0f else (nowMs - it).toFloat() }

    fun buzzAge(): Float? = buzzStartMs?.let { if (it == PENDING) 0f else (nowMs - it).toFloat() }
}

private inline fun DrawScope.withAlpha(
    alpha: Float,
    block: DrawScope.() -> Unit,
) {
    drawIntoCanvas {
        it.saveLayer(
            Rect(-LAYER_BLEED, -LAYER_BLEED, size.width + LAYER_BLEED, size.height + LAYER_BLEED),
            Paint().apply {
                this.alpha =
                    alpha
            },
        )
    }
    block()
    drawIntoCanvas { it.restore() }
}

/** How far a layer reaches past the button for what it draws outside: shadow, ring, blob. */
private const val LAYER_BLEED = 48f

/** What one frame of the button draws: the engine's `btn` options. */
internal data class ToonButtonLook(
    /** The squash, from the press machine. */
    val p: Float = 0f,
    /** The label's own squash; the Core page's release strip has it 40 ms behind [p]. */
    val pl: Float = p,
    /** The idle breath, a scale on top of the squash. */
    val br: Float = 1f,
    /** Added to the theme's seed: the boil's path variant. */
    val seed: Int = 0,
    val disabled: Boolean = false,
    val focus: Boolean = false,
    /** Milliseconds since loading started, or null while not loading. */
    val lt: Float? = null,
    /** The spinner's clock in ms. */
    val spin: Float = 0f,
)

/** The label's style: the Core page's Title step in the theme's body face. */
@Composable
internal fun rememberToonButtonLabel(label: String): TextLayoutResult {
    val measurer: TextMeasurer = rememberTextMeasurer()
    val family = Toon.fonts.body
    return remember(label, family) {
        measurer.measure(
            label,
            TextStyle(
                fontFamily = family,
                fontWeight = FontWeight(ToonTypeScale.Title.weight),
                fontSize = ToonTypeScale.Title.size.sp,
            ),
        )
    }
}

private fun c01(v: Float): Float = max(0f, min(1f, v))

/** The text variant's pressed blob radii and the loading blob's radius: the engine's literals. */
private val TEXT_BLOB_RADII = ToonRadii(14f, 10f, 16f, 11f)
private const val BLOB_R = 10f

/** The engine's `btn` drawn: [toonButtonScene] with [label] laid out at its place. */
internal fun DrawScope.drawToonButton(
    tokens: ToonTokens,
    variant: ToonButtonVariant,
    w: Float,
    label: TextLayoutResult,
    o: ToonButtonLook,
) = drawScene(toonButtonScene(tokens, variant, w, label.multiParagraph.width, o), label)

/**
 * The engine's `btn`: one frame of the button, [w] dp wide from its top left, whose label is
 * [labelWidth] dp wide, as layers and the label's place.
 */
@OptIn(InternalToonApi::class)
internal fun toonButtonScene(
    tokens: ToonTokens,
    variant: ToonButtonVariant,
    w: Float,
    labelWidth: Float,
    o: ToonButtonLook,
): ToonScene {
    val c = tokens.colors
    val s = tokens.shape
    val h = ToonSize.BUTTON
    val p = o.p
    val pl = o.pl
    val sq = toonSquashOf(p)
    val bw = w * sq.sx * o.br
    val bh = h * sq.sy * o.br
    val x = (w - bw) / 2
    val y = h - bh
    val rs = s.radiusScale
    val r = s.buttonRadii.let { ToonRadii(it.topLeft * rs, it.topRight * rs, it.bottomRight * rs, it.bottomLeft * rs) }
    val seed = s.seed + 900 + o.seed
    val ins = s.insets.copy(bottom = s.insets.bottom * (1 + .3f * c01(p)))
    val dis = o.disabled
    val filled = variant == ToonButtonVariant.Filled
    val layers = mutableListOf<ToonLayer>()
    val labels = mutableListOf<ToonLabel>()
    if (o.focus) {
        val ring = ToonRadii(r.topLeft + 6, r.topRight + 6, r.bottomRight + 6, r.bottomLeft + 6)
        layers += ringLayer(-6f, -6f, w + 12, h + 12, ring, c, s.wobble, seed + 3)
    }
    if (variant != ToonButtonVariant.Text) {
        layers +=
            bodyLayers(
                shapePoints(x, y, bw, bh, r, ins, s.wobble, seed),
                fill =
                    if (dis) {
                        c.disabled
                    } else if (filled) {
                        c.primary
                    } else {
                        c.surface
                    },
                colors = c,
                shadow = if (dis) 0f else (if (filled) s.shadow else s.shadow * .6f) * (1 - c01(p)),
                inkAlpha = if (dis) .35f else 1f,
                rim =
                    if (c.rim == null || dis) {
                        null
                    } else if (filled) {
                        c.primaryHighlight
                    } else {
                        c.rim
                    },
            )
    } else if (p > .02f) {
        layers +=
            smoothLayer(
                wobblePoints(x + 8, y + 5, bw - 16, bh - 10, TEXT_BLOB_RADII, s.wobble, seed),
                c.surfacePressed,
                c01(p * 1.5f),
            )
    }
    val fs = ToonTypeScale.Title.size
    val lc =
        if (dis) {
            c.textMuted
        } else if (filled) {
            c.onPrimary
        } else {
            c.text
        }
    val lh = h * (if (pl > 0) 1 - .15f * pl else 1 - .26f * pl)
    val ly = h / 2 + fs * .35f + (h - lh) / 2 + 2 * c01(pl)
    var lOp = if (dis) .6f else 1f
    var lDy = 0f
    if (o.lt != null) {
        val u = c01(o.lt / 130)
        lDy = -26 * u * u
        lOp *= 1 - u
    }
    if (lOp > .01f) labels += ToonLabel(w / 2, ly + lDy, lc, lOp)
    if (variant == ToonButtonVariant.Text) {
        val x0 = w / 2 - labelWidth / 2
        val yy = ly + 7 + lDy
        val n = 8
        val wave = FloatArray(2 + 4 * n)
        wave[0] = x0
        wave[1] = yy
        for (i in 1..n) {
            val xx = x0 + labelWidth * i / n
            wave[4 * i - 2] = xx - labelWidth / n / 2
            wave[4 * i - 1] = yy + (if (i % 2 == 1) 2.4f else -2.4f)
            wave[4 * i] = xx
            wave[4 * i + 1] = yy
        }
        layers +=
            ToonLayer(
                wave,
                ToonSegments.Quad,
                closed = false,
                color = if (dis) c.textMuted else c.primary,
                alpha = (if (dis) .5f else 1f) * (if (o.lt != null) lOp else 1f),
                stroke = 3f,
            )
    }
    if (o.lt != null && o.lt > 100) {
        val u = c01((o.lt - 100) / 100)
        val cy0 = h / 2 - 34
        val cy1 = y + bh / 2
        val cy = cy0 + (cy1 - cy0) * u * u
        var ssx = 1f
        var ssy = 1f
        if (o.lt > 200) {
            val q = toonOscillation((o.lt - 200) / 1000, .35f, 40f)
            ssx = 1 + .22f * q
            ssy = 1 - .2f * q
        }
        val ph = o.spin / 1000 * 2 * PI.toFloat()
        layers += smoothLayer(spinnerPoints(w / 2, cy, BLOB_R, BLOB_R + 3, ph, ssx, ssy), c.ink)
        layers +=
            smoothLayer(spinnerPoints(w / 2, cy, BLOB_R, BLOB_R, ph, ssx, ssy), if (filled) c.surface else c.primary)
    }
    return ToonScene(layers, labels)
}

/** One frame of the button without its interaction: what the fixtures and previews draw. */
@Composable
internal fun ToonButtonFrame(
    label: String,
    variant: ToonButtonVariant,
    width: Float,
    look: ToonButtonLook,
    modifier: Modifier = Modifier,
) {
    val tokens = Toon.tokens
    val text = rememberToonButtonLabel(label)
    Canvas(modifier.size(width.dp, ToonSize.BUTTON.dp)) { drawToonButton(tokens, variant, width, text, look) }
}
