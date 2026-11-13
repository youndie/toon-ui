package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.wobblePoints
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.type.withBacking
import kotlin.math.max
import kotlin.math.min

// A port of the desktop extension's `trafficSlots` and of `ToonTitleBar.dc.html` (B-76, research
// §1.8): the row across the top of a window that draws its own, 52 dp tall (the extension's
// `layout.titleH`). The design draws the places of the window's three controls, not the controls; the
// title and the record button are HTML over that drawing, and here a label and a slot placed by its
// numbers.

/**
 * The design's title bar: [title] from [titleStart], in the muted body face, ending in an ellipsis
 * before the actions, and [actions] in a row 20 dp from the end, 10 dp apart — the design puts a
 * [ToonRecordButton] there. It is as wide as it is allowed (1280 dp where nothing bounds it) and
 * 52 dp tall, and draws no background: the window's shows through, as in the design.
 *
 * With [trafficSlots] it draws the design's dashed places of a window's close, minimise and zoom
 * controls at its start, for a window whose controls are drawn elsewhere, or a mock-up; off by
 * default, since a real window draws its controls there or has none.
 *
 * The actions stand centred on the bar, but never within 8 dp of its top: an undecorated window lays
 * a strip that wide over its edges for resizing, and a press there would move the edge instead. A row
 * taller than 44 dp makes the bar taller rather than reach into that strip.
 *
 * Inside an app frame that draws its own title bar, put the actions themselves in that bar's actions
 * slot instead; this bar is for a window that has none.
 */
@Composable
public fun ToonTitleBar(
    title: String,
    modifier: Modifier = Modifier,
    titleStart: Dp = 96.dp,
    trafficSlots: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val tokens = Toon.tokens
    val backing = Toon.fonts.bodyCyrillic
    val slots = remember(tokens) { toonTitleBarSlotsScene(tokens) }
    Layout(
        content = {
            if (trafficSlots) {
                // The engine draws in dp (research D5); the canvas is pixels (B-47).
                Canvas(Modifier.size(SLOTS_W.dp, TITLE_H.dp)) { inDp { drawScene(slots) } }
            }
            if (title.isNotEmpty()) {
                BasicText(
                    withBacking(title, backing, TITLE_SIZE, TITLE_WEIGHT),
                    Modifier.semantics { heading() },
                    TextStyle(
                        fontFamily = Toon.fonts.body,
                        fontWeight = FontWeight(TITLE_WEIGHT),
                        fontSize = TITLE_SIZE.sp,
                        color = tokens.colors.textMuted,
                    ),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(ACTIONS_GAP.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val w = if (constraints.hasBoundedWidth) constraints.maxWidth else UNBOUNDED_W.dp.roundToPx()
        val loose = Constraints(maxWidth = w)
        val slotsM = if (trafficSlots) measurables.first() else null
        val titleM = if (title.isNotEmpty()) measurables[if (trafficSlots) 1 else 0] else null
        val slotsP = slotsM?.measure(loose)
        val rowP = measurables.last().measure(loose)
        val start = titleStart.roundToPx()
        val box = toonTitleBarBox(w.toFloat(), start.toFloat(), rowP.width.toFloat(), rowP.height.toFloat(), density)
        val titleP = titleM?.measure(Constraints(maxWidth = box.titleWidth.toInt()))
        val h = maxOf(constraints.minHeight, box.height.toInt())
        layout(w, h) {
            slotsP?.place(0, 0)
            titleP?.place(start, (h - titleP.height) / 2)
            rowP.place(box.actionsX.toInt(), box.actionsY.toInt())
        }
    }
}

/** Where the bar's parts go, in pixels: the title's room, the actions' corner, the bar's height. */
internal data class ToonTitleBarBox(
    val titleWidth: Float,
    val actionsX: Float,
    val actionsY: Float,
    val height: Float,
)

/**
 * The design's HTML numbers for a bar [w] wide with actions [rowW] by [rowH], all in pixels at
 * [density]: the title runs from [start] to 220 dp before the end (or 10 dp before the actions, where
 * they are wider than the design's), the actions end 20 dp before the end, centred on the 52 dp bar
 * but no higher than 8 dp from its top.
 */
internal fun toonTitleBarBox(
    w: Float,
    start: Float,
    rowW: Float,
    rowH: Float,
    density: Float,
): ToonTitleBarBox {
    val barH = TITLE_H * density
    val x = w - ACTIONS_END * density - rowW
    val y = max(RESIZE_EDGE * density, (barH - rowH) / 2)
    val titleEnd = min(w - TITLE_END * density, x - ACTIONS_GAP * density)
    return ToonTitleBarBox(
        titleWidth = max(0f, titleEnd - start),
        actionsX = x,
        actionsY = y,
        height = max(barH, y + rowH),
    )
}

/** The extension's `trafficSlots`: three dashed rings 12 dp across, 20 dp apart, on the bar's middle. */
@OptIn(InternalToonApi::class)
internal fun toonTitleBarSlotsScene(tokens: ToonTokens): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val layers =
        SLOT_X.mapIndexed { j, x ->
            smoothLayer(
                wobblePoints(
                    x - SLOT_R,
                    SLOT_TOP,
                    2 * SLOT_R,
                    2 * SLOT_R,
                    SLOT_RADII,
                    SLOT_WOBBLE,
                    sh.seed + 3900 + j,
                    2f,
                ),
                c.ink,
                SLOT_ALPHA,
                stroke = SLOT_STROKE,
                dash = SLOT_DASH,
            )
        }
    return ToonScene(layers, emptyList())
}

/** The bar: the extension's `titleH`, and the design's title box and action row. */
private const val TITLE_H = 52f
private const val TITLE_END = 220f
private const val TITLE_SIZE = 14f
private const val TITLE_WEIGHT = 700
private const val ACTIONS_END = 20f
private const val ACTIONS_GAP = 10f
private const val UNBOUNDED_W = 1280f

/** An undecorated window's resize strip over its edges (appframe's `resizeBorder`). */
private const val RESIZE_EDGE = 8f

/** The slots: the renderer's 80 dp box, three rings centred 20, 40 and 60 dp in, from 20 dp down. */
private const val SLOTS_W = 80f
private val SLOT_X = listOf(20f, 40f, 60f)
private const val SLOT_R = 6f
private const val SLOT_TOP = 20f
private val SLOT_RADII = ToonRadii(6f, 6f, 6f, 6f)
private const val SLOT_WOBBLE = .25f
private const val SLOT_ALPHA = .45f
private const val SLOT_STROKE = 1.3f
private val SLOT_DASH = floatArrayOf(2f, 2.4f)
