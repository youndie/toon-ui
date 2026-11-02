package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selectableGroup
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
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.ringLayer
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.wobblePoints
import io.github.youndie.toon.interaction.rememberToonPressState
import io.github.youndie.toon.interaction.toonPressable
import io.github.youndie.toon.motion.toonOscillation
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// A port of the engine's `radio` and `radioSelect` and the kit's `C.radio` (B-17): a column of rows,
// each a 52 dp target; selecting one drops its dot from above the ring as a stretched drop that hangs,
// falls and splats with two droplets, while the previous dot deflates in 120 ms.

/**
 * The design's radio group: one row per [options] entry, [selected] the chosen index. A click on
 * another row selects it and reports it to [onSelectedChange]; [selected] from outside sets it, as
 * the kit's `value` prop does.
 */
@Composable
public fun ToonRadioGroup(
    options: List<String>,
    selected: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val text = rememberToonText()
    val width =
        remember(options, text) {
            ROW_LABEL_X +
                ceil(options.maxOfOrNull { text.measure(it, CHECK_LABEL).multiParagraph.width } ?: 0f) +
                8
        }
    var sel by remember(selected) { mutableIntStateOf(selected) }
    var prev by remember { mutableIntStateOf(-1) }
    val clock = remember { ToonToggleClock() }
    val change by rememberUpdatedState(onSelectedChange)
    LaunchedEffect(clock.starts) { while (clock.running(SELECT_MS)) withFrameMillis { clock.tick(it) } }

    Column(modifier.semantics { selectableGroup() }) {
        options.forEachIndexed { i, option ->
            val press = rememberToonPressState()
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            val look = toonRadioFrameAt(i, sel, prev, clock.age(), press.progress, reduce)
            Canvas(
                Modifier
                    .size(width.dp, ROW_H.dp)
                    .semantics {
                        this.selected = i == sel
                        this.text = AnnotatedString(option)
                    }.toonPressable(press, enabled = enabled, role = Role.RadioButton) {
                        if (i != sel) {
                            prev = sel
                            sel = i
                            clock.start()
                            change(i)
                        }
                    }.focusable(enabled, interaction),
            ) {
                drawScene(toonRadioScene(tokens, option, look.copy(disabled = !enabled, focus = focused)), text)
            }
        }
    }
}

/** A row: 52 dp, the label from x 54. */
private const val ROW_H = 52f
private const val ROW_LABEL_X = 54f

/** The selection runs 450 ms in the kit, its drop 400; the old dot deflates in 120. */
private const val SELECT_MS = 450f
private const val DROP_MS = 400f
private const val DEFLATE_MS = 120f

/** The press squashes the ring as the MVP page's Pressed cell draws it, to 0.86. */
private const val PRESS_SQUASH = .14f

/** The engine's `radioSelect` at [t] ms: the dot's size, its fall [dotY], its stretch, the droplets' life. */
internal fun toonRadioSelect(t: Float): ToonRadioLook {
    if (t < 60) return ToonRadioLook(selected = true, ds = 1f, dotY = -24f, dsx = .9f, dsy = 1.12f, sp = -1f)
    if (t < 150) {
        val u = (t - 60) / 90
        return ToonRadioLook(
            selected = true,
            ds = 1f,
            dotY = -24 * (1 - u * u),
            dsx = .9f - .12f * u,
            dsy =
                1.12f + .2f * u,
            sp = -1f,
        )
    }
    val a = toonOscillation((t - 150) / 1000, .32f, 38f)
    return ToonRadioLook(
        selected = true,
        ds = 1f,
        dotY = 0f,
        dsx = 1 + .45f * a,
        dsy = 1 - .4f * a,
        sp =
            (t - 150) / 220,
    )
}

/**
 * The kit's `C.radio.scene` for row [index] at a moment: [selectedIndex] chosen after [previousIndex],
 * [ageMs] ago (null before any), the row under press [p]. Under reduce motion — the MVP page's
 * `instant` — the new dot is in place and the old one gone at once.
 */
internal fun toonRadioFrameAt(
    index: Int,
    selectedIndex: Int,
    previousIndex: Int,
    ageMs: Float?,
    p: Float,
    reduceMotion: Boolean,
): ToonRadioLook {
    val t = if (reduceMotion) ageMs?.let { SETTLED_MS } else ageMs
    val base =
        when {
            index == selectedIndex -> {
                if (t != null &&
                    t < DROP_MS
                ) {
                    toonRadioSelect(t)
                } else {
                    ToonRadioLook(selected = true)
                }
            }

            index == previousIndex && t != null && t < DEFLATE_MS -> {
                ToonRadioLook(ds = 1 - t / DEFLATE_MS)
            }

            else -> {
                ToonRadioLook()
            }
        }
    return base.copy(sq = 1 - PRESS_SQUASH * c01(p))
}

private const val SETTLED_MS = 1e6f

private fun c01(v: Float): Float = max(0f, min(1f, v))

/** What one frame of a radio row draws: the engine's `radio` options. */
internal data class ToonRadioLook(
    val selected: Boolean = false,
    val ds: Float = if (selected) 1f else 0f,
    val dotY: Float = 0f,
    val dsx: Float = 1f,
    val dsy: Float = 1f,
    val sp: Float = -1f,
    val sq: Float = 1f,
    val disabled: Boolean = false,
    val focus: Boolean = false,
)

private val RING_RADII = ToonRadii(14f, 13f, 14f, 12.5f)
private val FOCUS_RADII = ToonRadii(20f, 19f, 20f, 18f)

/** The engine's `radio`: one row, the ring's 28 dp circle at (10, 10), the label at x 54. */
@OptIn(InternalToonApi::class)
internal fun toonRadioScene(
    tokens: ToonTokens,
    label: String,
    o: ToonRadioLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val dis = o.disabled
    val cx = 24f
    val cy = 24f
    val cw = 28 * (2 - o.sq)
    val ch = 28 * o.sq
    val layers = mutableListOf<ToonLayer>()
    if (o.focus) layers += ringLayer(4f, 4f, 40f, 40f, FOCUS_RADII, c, sh.wobble, sh.seed + 501)
    val ins = sh.insets.let { ToonInsets(it.top * .85f, it.right * .85f, it.bottom * .85f, it.left * .85f) }
    layers +=
        bodyLayers(
            shapePoints(10 + (28 - cw) / 2, 10 + (28 - ch), cw, ch, RING_RADII, ins, sh.wobble, sh.seed + 500, 3f),
            fill = if (dis) c.disabled else c.surface,
            colors = c,
            shadow = if (dis) 0f else 2f,
            inkAlpha = if (dis) .35f else 1f,
            rim = null,
        )
    if (o.ds > .03f) {
        val r = 6.5f * o.ds
        val rx = r * o.dsx
        val ry = r * o.dsy
        val by = cy + 6.5f + o.dotY + (28 - ch) / 2
        val g =
            shapePoints(
                cx - rx,
                by - 2 * ry,
                2 * rx,
                2 * ry,
                ToonRadii(r, r * .95f, r, r * .9f),
                DOT_INSETS,
                .4f,
                sh.seed + 502,
                2.5f,
            )
        layers += smoothLayer(g.ink, c.ink, if (dis) .4f else 1f)
        layers += smoothLayer(g.fill, if (dis) c.disabled else c.primary)
        val sp = o.sp
        if (sp > 0 && sp < 1) {
            listOf(-1, 1).forEachIndexed { j, dir ->
                val px = cx + dir * (7 + 8 * sp)
                val py = by - 2 - 9 * sin(PI.toFloat() * min(1f, sp * 1.15f)) + 3 * sp
                val rr = 1.7f * (1 - sp * .5f)
                val op = 1 - sp * sp
                val e = rr + 1
                layers +=
                    smoothLayer(
                        wobblePoints(px - e, py - e, 2 * e, 2 * e, ToonRadii(e, e, e, e), .1f, 522 + j, 1.5f),
                        c.ink,
                        op,
                    )
                layers +=
                    smoothLayer(
                        wobblePoints(px - rr, py - rr, 2 * rr, 2 * rr, ToonRadii(rr, rr, rr, rr), .1f, 520 + j, 1.5f),
                        c.primary,
                        op,
                    )
            }
        }
    }
    val labels =
        if (label.isEmpty()) {
            emptyList()
        } else {
            listOf(ToonLabel(label, ROW_LABEL_X, 30f, c.text, if (dis) .6f else 1f, CHECK_LABEL, ToonAnchor.Start))
        }
    return ToonScene(layers, labels)
}

private val DOT_INSETS = ToonInsets(1.4f, 1.6f, 2.2f, 1.4f)
