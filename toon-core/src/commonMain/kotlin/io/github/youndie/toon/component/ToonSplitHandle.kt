package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonInsets
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.ringLayer
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.sizeDp
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.interaction.HOVER_LIFT
import io.github.youndie.toon.interaction.hoverLifted
import io.github.youndie.toon.interaction.rememberToonHoverState
import io.github.youndie.toon.interaction.toonHoverable
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens

// A port of the desktop extension's `splitGrip` and of `ToonSplitHandle.dc.html` (B-73, research
// §1.8): two ink nails between a sidebar and the content, on a lifted grip while the mouse is over
// them and a pressed one while they are held. The handle reports how far it has been dragged; the
// panes it stands between are the caller's to size.

/**
 * The design's handle between a sidebar and the content: a column 16 dp wide and as tall as it is let
 * be, at least 80 dp. Lay it between the two panes and size the sidebar from what it reports.
 *
 * A press calls [onDragStart], each move after it [onDrag] with the pointer's distance from where it
 * was pressed, in dp, right positive, and the release [onDragEnd]. With keyboard focus, ← and → move
 * it by 8 dp: a whole drag of −8 or 8 each. A mouse over it lifts its grip, the column-resize cursor
 * shows on a desktop and in a browser; keyboard focus rings it.
 *
 * To assistive technology it is [label], with two actions, [moveLeftLabel] and [moveRightLabel], that
 * do what the keys do.
 */
@Composable
public fun ToonSplitHandle(
    onDrag: (dxFromStart: Float) -> Unit,
    modifier: Modifier = Modifier,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    label: String = "Resize",
    moveLeftLabel: String = "Move left",
    moveRightLabel: String = "Move right",
) {
    val tokens = Toon.tokens
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hover = rememberToonHoverState()
    var dragging by remember { mutableStateOf(false) }
    val start by rememberUpdatedState(onDragStart)
    val move by rememberUpdatedState(onDrag)
    val end by rememberUpdatedState(onDragEnd)
    // The design's keys: a whole drag of one step, start, move and end at once.
    val step: (Float) -> Unit = { d ->
        start()
        move(d)
        end()
    }

    Box(
        modifier
            .width(W.dp)
            .fillMaxHeight()
            .defaultMinSize(minHeight = MIN_H.dp)
            .semantics {
                contentDescription = label
                customActions =
                    listOf(
                        CustomAccessibilityAction(moveLeftLabel) {
                            step(-KEY_STEP)
                            true
                        },
                        CustomAccessibilityAction(moveRightLabel) {
                            step(KEY_STEP)
                            true
                        },
                    )
            }.pointerHoverIcon(toonColumnResizeIcon)
            .toonHoverable(hover)
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (e.key) {
                    Key.DirectionLeft -> step(-KEY_STEP)
                    Key.DirectionRight -> step(KEY_STEP)
                    else -> return@onKeyEvent false
                }
                true
            }.focusable(interactionSource = interaction)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    dragging = true
                    start()
                    // Summed from each move, not read off the pointer's place in the handle: the
                    // handle moves with the sidebar it resizes, the pointer's distance does not.
                    var dx = 0f
                    try {
                        drag(down.id) { change ->
                            dx += change.positionChange().x / density
                            change.consume()
                            move(dx)
                        }
                    } finally {
                        dragging = false
                        end()
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // The engine draws in dp (research D5); the canvas is pixels (B-47).
            inDp {
                val st =
                    when {
                        dragging -> ToonSplitHandleState.Drag
                        hover.value > .5f -> ToonSplitHandleState.Hover
                        else -> ToonSplitHandleState.Rest
                    }
                drawScene(toonSplitHandleScene(tokens, sizeDp.height, st, focused))
            }
        }
    }
}

/** The design's `st`: at rest, under the mouse, or held. */
internal enum class ToonSplitHandleState { Rest, Hover, Drag }

/**
 * The extension's `splitGrip` for a handle [h] dp tall, from its top left, and the ring its `paint()`
 * pushes after it with [focus]: two nails 12 dp above and below the middle, on a grip 10 by 60 dp
 * while the handle is under the mouse or held.
 */
@OptIn(InternalToonApi::class)
internal fun toonSplitHandleScene(
    tokens: ToonTokens,
    h: Float,
    st: ToonSplitHandleState,
    focus: Boolean,
): ToonScene {
    val c = tokens.colors
    val seed = tokens.shape.seed
    val cx = W / 2
    val cy = h / 2
    val layers = mutableListOf<ToonLayer>()
    if (st != ToonSplitHandleState.Rest) {
        // `liftBody` with no rim: under the mouse the grip rises 2 dp and leaves its 2 dp shadow;
        // held, the press takes both back and fills it with the selected shade.
        val held = st == ToonSplitHandleState.Drag
        val shape =
            shapePoints(cx - 5, cy - 30, 10f, 60f, GRIP_RADII, GRIP_INSETS, .4f, seed + 3600, 3f)
        val body =
            ToonScene(
                bodyLayers(
                    shape,
                    if (held) c.chipSelected else c.surface,
                    c,
                    shadow = if (held) 0f else 2f,
                    rim = null,
                ),
                emptyList(),
            )
        layers += if (held) body.layers else body.hoverLifted(HOVER_LIFT, c.ink).layers
    }
    // The nails ride the grip's lift.
    val l = if (st == ToonSplitHandleState.Hover) HOVER_LIFT else 0f
    for ((j, y) in listOf(cy - NAIL_GAP, cy + NAIL_GAP).withIndex()) {
        val nail = shapePoints(cx - l - 3, y - l - 3, 6f, 6f, NAIL_RADII, NAIL_INSETS, .25f, seed + 3610 + j, 2f)
        layers += smoothLayer(nail.ink, c.ink)
        layers += smoothLayer(nail.fill, c.nail)
    }
    if (focus) {
        layers += ringLayer(-2f, cy - 40, 20f, 80f, RING_RADII, c, tokens.shape.wobble, seed + 3620)
    }
    return ToonScene(layers, emptyList())
}

/** The design's width, `DESKTOP.layout.split`, and the least height of its `height` editor. */
private const val W = 16f
private const val MIN_H = 80f

/** What ← and → move it by. */
private const val KEY_STEP = 8f
private const val NAIL_GAP = 12f
private val GRIP_RADII = ToonRadii(5f, 4f, 5f, 4f)
private val GRIP_INSETS = ToonInsets(1.4f, 1.6f, 2.4f, 1.4f)
private val NAIL_RADII = ToonRadii(3f, 3f, 3f, 3f)
private val NAIL_INSETS = ToonInsets(1.1f, 1.1f, 1.5f, 1.1f)
private val RING_RADII = ToonRadii(10f, 8f, 10f, 8f)
