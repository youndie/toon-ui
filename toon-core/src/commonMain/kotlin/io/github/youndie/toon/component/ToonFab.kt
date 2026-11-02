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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonAnchor
import io.github.youndie.toon.draw.ToonLabel
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonTextStyle
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.ringLayer
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.wobblePoints
import io.github.youndie.toon.fx.toonDustScene
import io.github.youndie.toon.icon.ToonIconGlyph
import io.github.youndie.toon.icon.ToonIconWeight
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.icon.iconLayers
import io.github.youndie.toon.interaction.rememberToonPressState
import io.github.youndie.toon.interaction.toonPressable
import io.github.youndie.toon.motion.toonOscillation
import io.github.youndie.toon.motion.toonSquashOf
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.theme.rememberToonIdleSlot
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// A port of the engine's `fab` and `fabAppear` and the kit's `C.fab` (B-16): the floating action
// button, regular or extended with a label, that appears in the air, hangs 90 ms, falls with a
// stretch and lands with a squash and a dust puff; then breathes while idle, as a screen's main action.

/**
 * The design's floating action button: [icon], and [label] when not empty (the extended FAB). It
 * lands when it first shows, [delay] ms later, unless [land] is false; [idle] lets it breathe if the
 * theme's idle budget grants it a slot. [onClick] fires on the release.
 */
@Composable
public fun ToonFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ToonIconGlyph = ToonIcons.Plus,
    label: String = "",
    contentDescription: String? = null,
    land: Boolean = true,
    delay: Int = 0,
    enabled: Boolean = true,
    idle: Boolean = true,
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val text = rememberToonText()
    val labelWidth =
        remember(label, text) { if (label.isEmpty()) 0f else text.measure(label, FAB_LABEL).multiParagraph.width }
    val w = fabWidth(labelWidth, label.isNotEmpty())
    val press = rememberToonPressState()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val breathing by rememberToonIdleSlot(idle && enabled)
    val clock = remember { ToonFabClock(land) }
    val click by rememberUpdatedState(onClick)

    LaunchedEffect(clock, breathing) {
        while (breathing || clock.landing(delay)) withFrameMillis { clock.tick(it) }
    }
    val moment = toonFabFrameAt(clock.nowMs, clock.landAge(), delay, breathing, reduce)

    Canvas(
        modifier
            .size(ceil(w).dp, FAB_H.dp)
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
                if (label.isNotEmpty()) this.text = AnnotatedString(label)
            }.toonPressable(press, enabled = enabled) { click() }
            .focusable(enabled, interaction),
    ) {
        val look = moment.look.copy(p = press.progress, disabled = !enabled, focus = focused)
        drawScene(toonFabLandScene(tokens, icon, label, labelWidth, look, moment.appear, w), text)
    }
}

/** The extended FAB's label: 17 sp bold in the theme's body face. */
internal val FAB_LABEL = ToonTextStyle(700, 17f)
private const val FAB_H = 64f

/** The engine's FAB width: 64 round, or icon and label with their padding when extended. */
internal fun fabWidth(
    labelWidth: Float,
    extended: Boolean,
): Float = if (extended) 20 + 24 + 10 + labelWidth + 24 else FAB_H

/** The kit's idle: a 1.8 % breath every 2200 ms, the outline boiling through three seeds at 10 fps. */
private const val BREATH = .009f
private const val BREATH_MS = 2200f
private const val BOIL_MS = 100
private const val BOIL_SEED = 31

/** The landing lasts 700 ms; under reduce motion it is the kit's cross-fade, 120 ms, into the landed FAB. */
private const val LAND_MS = 700f
private const val REDUCED_FADE_MS = 120f

/** The engine's `fabAppear` at [t] ms: opacity, fall [dy], squash, and the dust's life [du]. */
internal class ToonFabAppear(
    val op: Float,
    val dy: Float,
    val sx: Float,
    val sy: Float,
    val du: Float,
)

internal val FAB_LANDED = ToonFabAppear(1f, 0f, 1f, 1f, -1f)

/** The engine's `fabAppear`: in the air 70 dp up, a 90 ms hang, a 140 ms fall, a squash and dust. */
internal fun toonFabAppear(t: Float): ToonFabAppear {
    if (t < 0) return ToonFabAppear(0f, -70f, 1f, 1f, -1f)
    if (t < 90) return ToonFabAppear(c01(t / 40), -70 + 3 * sin(t / 90 * PI.toFloat()), 1f, 1f, -1f)
    if (t < 230) {
        val u = (t - 90) / 140
        return ToonFabAppear(1f, -70 * (1 - u * u), 1 - .06f * u, 1 + .1f * u, -1f)
    }
    val a = toonOscillation((t - 230) / 1000, .35f, 40f)
    return ToonFabAppear(1f, 0f, 1 + .16f * a, 1 - .2f * a, (t - 230) / 300)
}

/** What the FAB draws at a moment: its [look], its landing [appear], and the reduce-motion fade-in. */
internal class ToonFabMoment(
    val look: ToonFabLook,
    val appear: ToonFabAppear,
)

/**
 * The kit's `C.fab.scene` at frame time [nowMs], the landing [landAgeMs] ago (null without one),
 * [delayMs] late. Under reduce motion — the MVP page's `fade 120` — it is the landed FAB fading in over
 * 120 ms, and idle is off.
 */
internal fun toonFabFrameAt(
    nowMs: Long,
    landAgeMs: Float?,
    delayMs: Int,
    breathing: Boolean,
    reduceMotion: Boolean,
): ToonFabMoment {
    val appear =
        when {
            landAgeMs == null -> FAB_LANDED
            reduceMotion -> ToonFabAppear(min(1f, landAgeMs / REDUCED_FADE_MS), 0f, 1f, 1f, -1f)
            else -> toonFabAppear(landAgeMs - delayMs)
        }
    val idle = breathing && !reduceMotion
    val look =
        ToonFabLook(
            br = if (idle) 1 + BREATH * (1 - cos(2 * PI.toFloat() * nowMs / BREATH_MS)) else 1f,
            seed = if (idle) floor(nowMs / BOIL_MS.toFloat()).toInt() % 3 * BOIL_SEED else 0,
            sx = appear.sx,
            sy = appear.sy,
        )
    return ToonFabMoment(look, appear)
}

@Stable
private class ToonFabClock(
    land: Boolean,
) {
    var nowMs by mutableLongStateOf(0L)
    private var landAt: Long? by mutableStateOf(if (land) PENDING_START else null)

    fun tick(now: Long) {
        nowMs = now
        if (landAt == PENDING_START) landAt = now
    }

    fun landAge(): Float? = landAt?.let { if (it == PENDING_START) 0f else (nowMs - it).toFloat() }

    fun landing(delayMs: Int): Boolean = landAge()?.let { it < LAND_MS + delayMs } ?: false
}

private const val PENDING_START = -1L

private fun c01(v: Float): Float = max(0f, min(1f, v))

/** What one frame of the FAB draws: the engine's `fab` options. */
internal data class ToonFabLook(
    val p: Float = 0f,
    val sx: Float = 1f,
    val sy: Float = 1f,
    val br: Float = 1f,
    val seed: Int = 0,
    val disabled: Boolean = false,
    val focus: Boolean = false,
)

/** The kit's landing frame: the FAB moved and faded by [appear], dust under its centre. */
internal fun toonFabLandScene(
    tokens: ToonTokens,
    icon: ToonIconGlyph,
    label: String,
    labelWidth: Float,
    look: ToonFabLook,
    appear: ToonFabAppear,
    w: Float = fabWidth(labelWidth, label.isNotEmpty()),
): ToonScene =
    toonFabScene(tokens, icon, label, labelWidth, look).placed(0f, appear.dy, appear.op) +
        toonDustScene(tokens.colors, appear.du, w / 2, 66f, 40f)

private val FAB_RADII = ToonRadii(22f, 16f, 24f, 18f)

/** The engine's `fab`: one frame from its top left, 64 dp tall; extended when [label] is not empty. */
@OptIn(InternalToonApi::class)
internal fun toonFabScene(
    tokens: ToonTokens,
    icon: ToonIconGlyph,
    label: String,
    labelWidth: Float,
    o: ToonFabLook,
): ToonScene {
    val c = tokens.colors
    val s = tokens.shape
    val h = FAB_H
    val fs = FAB_LABEL.size
    val w = fabWidth(labelWidth, label.isNotEmpty())
    val p = o.p
    val sq = toonSquashOf(p)
    val sy = sq.sy * o.sy * o.br
    val sx = sq.sx * o.sx * o.br
    val bw = w * sx
    val bh = h * sy
    val x = (w - bw) / 2
    val y = h - bh
    val rs = s.radiusScale
    val r =
        ToonRadii(
            FAB_RADII.topLeft * rs,
            FAB_RADII.topRight * rs,
            FAB_RADII.bottomRight * rs,
            FAB_RADII.bottomLeft * rs,
        )
    val dis = o.disabled
    val layers =
        buildList {
            if (o.focus) {
                add(
                    ringLayer(
                        -6f,
                        -6f,
                        w + 12,
                        h + 12,
                        ToonRadii(
                            r.topLeft + 6,
                            r.topRight + 6,
                            r.bottomRight + 6,
                            r.bottomLeft + 6,
                        ),
                        c,
                        s.wobble,
                        s.seed + 451,
                    ),
                )
            }
            addAll(
                bodyLayers(
                    shapePoints(x, y, bw, bh, r, s.insets, s.wobble, s.seed + 450 + o.seed),
                    fill = if (dis) c.disabled else c.primary,
                    colors = c,
                    shadow = if (dis) 0f else (s.shadow + 2) * (1 - c01(p)),
                    inkAlpha = if (dis) .35f else 1f,
                    rim = if (c.rim == null || dis) null else c.primaryHighlight,
                ),
            )
            if (!dis) {
                val inner =
                    ToonRadii(
                        max(4f, r.topLeft - 7),
                        max(4f, r.topRight - 7),
                        max(4f, r.bottomRight - 7),
                        max(
                            4f,
                            r.bottomLeft - 7,
                        ),
                    )
                add(
                    smoothLayer(
                        wobblePoints(
                            x + 7,
                            y + 7 * sy,
                            bw - 14,
                            bh - 14 * sy,
                            inner,
                            s.wobble * .8f,
                            s.seed + 452,
                        ),
                        c.ink,
                        .5f,
                        stroke = 1.6f,
                        dash = STITCH,
                    ),
                )
            }
            val cy = y + bh / 2
            addAll(
                iconLayers(icon, 20f, cy - 12, c.onPrimary, weight = ToonIconWeight.Bold, alpha = if (dis) .4f else 1f),
            )
        }
    val labels =
        if (label.isEmpty()) {
            emptyList()
        } else {
            listOf(
                ToonLabel(
                    label,
                    54f,
                    y + bh / 2 + fs * .35f,
                    if (dis) c.textMuted else c.onPrimary,
                    1f,
                    FAB_LABEL,
                    ToonAnchor.Start,
                ),
            )
        }
    return ToonScene(layers, labels)
}

/** The inner stitch: a dashed line 7 dp inside the outline. */
private val STITCH = floatArrayOf(3f, 4f)
