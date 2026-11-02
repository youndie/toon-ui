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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
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
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.ringLayer
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.wobblePoints
import io.github.youndie.toon.icon.ToonIconGlyph
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.icon.iconLayers
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

// A port of the engine's `chip` and `chipSelect` and the kit's `C.chip` (B-20): a 36 dp lozenge in a
// 48 dp target, 6 dp down, as the kit lays it out since the second snapshot (research §1.6). A filter
// chip inflates to 1.15 and wobbles back while its fill arrives as a paint stroke from the left.

/** The `variant` prop of `ToonChip.dc.html`. */
public enum class ToonChipVariant {
    Filter,
    Assist,
    Input,
}

/**
 * The design's chip: [label] in a hand-drawn lozenge, tilted by [tilt] degrees (the label stays
 * level). A filter chip toggles [selected] on each click and reports it to [onSelectedChange]; an
 * assist chip leads with [icon]; an input chip trails a close mark. [onClick] fires on the release.
 */
@Composable
public fun ToonChip(
    label: String,
    modifier: Modifier = Modifier,
    variant: ToonChipVariant = ToonChipVariant.Filter,
    icon: ToonIconGlyph = ToonIcons.Calendar,
    selected: Boolean = false,
    tilt: Float = 0f,
    enabled: Boolean = true,
    onSelectedChange: (Boolean) -> Unit = {},
    onClick: () -> Unit = {},
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val text = rememberToonText()
    val labelWidth = remember(label, text) { text.measure(label, CHIP_LABEL).multiParagraph.width }
    var sel by remember(selected) { mutableStateOf(selected) }
    val press = rememberToonPressState()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val clock = remember { ToonToggleClock() }
    val change by rememberUpdatedState(onSelectedChange)
    val click by rememberUpdatedState(onClick)
    LaunchedEffect(clock.starts) { while (clock.running(SELECT_MS)) withFrameMillis { clock.tick(it) } }
    val look = toonChipFrameAt(sel, clock.age(), press.progress, reduce)
    val w = chipWidth(variant, sel, labelWidth)

    Canvas(
        modifier
            .size(ceil(w).dp, ToonSize.TARGET_MIN.dp)
            .semantics {
                this.text = AnnotatedString(label)
                if (variant == ToonChipVariant.Filter) this.selected = sel
            }.toonPressable(
                press,
                enabled = enabled,
                role =
                    if (variant ==
                        ToonChipVariant.Filter
                    ) {
                        Role.Checkbox
                    } else {
                        Role.Button
                    },
            ) {
                if (variant == ToonChipVariant.Filter) {
                    sel = !sel
                    if (sel) clock.start()
                    change(sel)
                }
                click()
            }.focusable(enabled, interaction),
    ) {
        // The engine draws in dp (research D5); the canvas is pixels (B-47).

        inDp {
            val scene =
                toonChipScene(tokens, variant, label, labelWidth, icon, look.copy(disabled = !enabled, focus = focused))
            translate(0f, CHIP_TOP) {
                // The kit's place: the shape turns about its centre; the label keeps level.
                rotate(tilt, Offset(w / 2, CHIP_H / 2)) { drawScene(ToonScene(scene.layers, emptyList())) }
                drawScene(ToonScene(emptyList(), scene.labels), text)
            }
        }
    }
}

/** The kit's box: 48 dp tall, the 36 dp chip 6 dp down. */
private const val CHIP_TOP = 6f
private const val CHIP_H = 36f

/** The label: 15 sp semibold in the theme's body face. */
internal val CHIP_LABEL = ToonTextStyle(600, 15f)

/** The selection runs 700 ms in the kit. */
private const val SELECT_MS = 700f

/** The engine's chip width: padding, a 24 dp lead (an icon, or a filter's tick), the label, a 22 dp close. */
internal fun chipWidth(
    variant: ToonChipVariant,
    selected: Boolean,
    labelWidth: Float,
): Float {
    val lead = if (variant == ToonChipVariant.Assist || (variant == ToonChipVariant.Filter && selected)) 24f else 0f
    val trail = if (variant == ToonChipVariant.Input) 22f else 0f
    return 16 + lead + labelWidth + trail + 16
}

/** The engine's `chipSelect` at [t] ms: the inflation, and how far the paint has swept. */
internal fun toonChipSelect(t: Float): ToonChipLook {
    val sc =
        if (t <
            90
        ) {
            1 + .15f * sin(PI.toFloat() / 2 * t / 90)
        } else {
            1 + .15f * toonOscillation((t - 90) / 1000, .25f, 55f)
        }
    val u = c01(t / 180)
    return ToonChipLook(selected = true, scale = sc, swipe = 1 - (1 - u).pow(2))
}

/**
 * The kit's `C.chip.scene` at a moment: [selected], the selection [selectAgeMs] ago (null before
 * any), under press [p]. Under reduce motion — the MVP page's `instant` — the chip is painted at once.
 */
internal fun toonChipFrameAt(
    selected: Boolean,
    selectAgeMs: Float?,
    p: Float,
    reduceMotion: Boolean,
): ToonChipLook {
    val base =
        when {
            !selected -> ToonChipLook()
            !reduceMotion && selectAgeMs != null && selectAgeMs < SELECT_MS -> toonChipSelect(selectAgeMs)
            else -> ToonChipLook(selected = true)
        }
    return base.copy(p = p)
}

private fun c01(v: Float): Float = max(0f, min(1f, v))

/** What one frame of the chip draws: the engine's `chip` options. */
internal data class ToonChipLook(
    val selected: Boolean = false,
    val scale: Float = 1f,
    val swipe: Float = if (selected) 1f else 0f,
    val p: Float = 0f,
    val focus: Boolean = false,
    val disabled: Boolean = false,
)

private val CHIP_RADII = ToonRadii(9f, 15f, 8f, 14f)

/** The engine's `chip`: one frame from its top left, 36 dp tall. */
@OptIn(InternalToonApi::class)
internal fun toonChipScene(
    tokens: ToonTokens,
    variant: ToonChipVariant,
    label: String,
    labelWidth: Float,
    icon: ToonIconGlyph,
    o: ToonChipLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val fs = CHIP_LABEL.size
    val h = CHIP_H
    val lead = variant == ToonChipVariant.Assist || (variant == ToonChipVariant.Filter && o.selected)
    val w = chipWidth(variant, o.selected, labelWidth)
    val p = o.p
    val bw = w * o.scale * (1 + .06f * p)
    val bh = h * o.scale * (1 - .12f * p)
    val x = (w - bw) / 2
    val y = (h - bh) / 2 + h * .06f * max(0f, p)
    val rs = sh.radiusScale
    val r =
        ToonRadii(
            CHIP_RADII.topLeft * rs,
            CHIP_RADII.topRight * rs,
            CHIP_RADII.bottomRight * rs,
            CHIP_RADII.bottomLeft * rs,
        )
    val dis = o.disabled
    val layers = mutableListOf<ToonLayer>()
    if (o.focus) {
        layers +=
            ringLayer(
                -5f,
                -5f,
                w + 10,
                h + 10,
                ToonRadii(r.topLeft + 5, r.topRight + 5, r.bottomRight + 5, r.bottomLeft + 5),
                c,
                sh.wobble,
                sh.seed + 551,
            )
    }
    val ins = sh.insets.let { ToonInsets(it.top * .8f, it.right * .8f, it.bottom * .8f, it.left * .8f) }
    val g = shapePoints(x, y, bw, bh, r, ins, sh.wobble * .8f, sh.seed + 550, 4f)
    if (!dis) {
        val d = 3 * (1 - c01(p))
        layers += smoothLayer(g.ink, c.ink, dx = d, dy = d)
    }
    layers += smoothLayer(g.ink, c.ink, if (dis) .35f else 1f)
    layers += smoothLayer(g.fill, if (dis) c.disabled else c.surface)
    val swp = o.swipe
    if (swp > .01f && !dis) {
        val fx = x + ins.left + 1
        val fy = y + ins.top + 1
        val fw = bw - ins.right - ins.left - 2
        val fh = bh - ins.top - ins.bottom - 2
        val paint = ToonRadii(5f, min(fh / 2, 13f), min(fh / 2, 11f), 5f)
        layers +=
            smoothLayer(wobblePoints(fx, fy, max(3f, fw * swp), fh, paint, .7f, sh.seed + 553, 3f), c.chipSelected)
    }
    val cy = y + bh / 2
    val op = if (dis) .5f else 1f
    val tc = if (swp > .5f) c.ink else c.text
    var tx = 16f
    if (lead) {
        if (variant == ToonChipVariant.Filter) {
            val tick = floatArrayOf(tx + 1, cy, tx + 6, cy + 5, tx + 16, cy - 6)
            layers += ToonLayer(tick, ToonSegments.Line, false, tc, op, stroke = 3f)
        } else {
            layers += iconLayers(icon, tx - 2, cy - 10, tc, size = 20f, alpha = op)
        }
        tx += 24
    }
    val labels =
        listOf(ToonLabel(label, tx, cy + fs * .35f, if (dis) c.textMuted else tc, op, CHIP_LABEL, ToonAnchor.Start))
    if (variant ==
        ToonChipVariant.Input
    ) {
        layers +=
            iconLayers(ToonIcons.Close, tx + labelWidth + 6, cy - 8, tc, size = 16f, alpha = op)
    }
    return ToonScene(layers, labels)
}
