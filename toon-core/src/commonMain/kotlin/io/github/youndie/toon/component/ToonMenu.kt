package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
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
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.ringLayer
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.wobblePoints
import io.github.youndie.toon.icon.ToonIconGlyph
import io.github.youndie.toon.icon.iconLayers
import io.github.youndie.toon.motion.toonSpring
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// A port of the engine's `menu` and `menuUnroll` and the kit's `C.menu` (B-37): a paper scroll whose
// bottom roll drops and unrolls the paper with an overshoot, the items fading in as it passes them;
// closed, it rolls back up in 160 ms. An item fires on release, before the roll finishes.

/** One item of a [ToonMenu]: its [label] and an [icon] before it. A label naming a deletion is drawn in red. */
public class ToonMenuItem(
    public val label: String,
    public val icon: ToonIconGlyph? = null,
)

/**
 * The design's menu: [items] on a scroll [width] wide that unrolls when [open] turns true and rolls
 * up when it turns false; a release on an item reports it to [onSelect]. [selected] is shaded.
 */
@Composable
public fun ToonMenu(
    items: List<ToonMenuItem>,
    modifier: Modifier = Modifier,
    open: Boolean = true,
    selected: Int = -1,
    width: Dp = 200.dp,
    onSelect: (Int) -> Unit = {},
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val texts = rememberToonText()
    val clock = remember { ToonMenuClock(open) }
    var pressed by remember { mutableStateOf<Int?>(null) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var focusOn by remember { mutableIntStateOf(0) }
    val select by rememberUpdatedState(onSelect)
    val w = width.value
    val n = items.size
    LaunchedEffect(open) { clock.changed(open) }
    LaunchedEffect(clock.starts) { while (clock.running()) withFrameMillis { clock.tick(it) } }

    Canvas(
        modifier
            .size(width, toonMenuHeight(n).dp)
            .semantics {
                if (selected in items.indices) stateDescription = items[selected].label
                if (clock.open) {
                    customActions =
                        items.mapIndexed { i, it -> CustomAccessibilityAction(it.label) { select(i).let { true } } }
                }
            }.onKeyEvent {
                if (!clock.open || it.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (it.key) {
                    Key.DirectionDown -> {
                        focusOn = min(n - 1, focusOn + 1)
                        true
                    }

                    Key.DirectionUp -> {
                        focusOn = max(0, focusOn - 1)
                        true
                    }

                    Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                        select(focusOn)
                        true
                    }

                    else -> {
                        false
                    }
                }
            }.focusable(open, interaction)
            .pointerInput(n) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    pressed = toonMenuItemAt(n, down.position.y / density)
                    val up = waitForUpOrCancellation()
                    val i = pressed
                    pressed = null
                    if (up != null && clock.open && i != null) select(i)
                }
            },
    ) {
        // The engine draws in dp (research D5); the canvas is pixels (B-47).
        inDp {
            val m = toonMenuFrameAt(clock.open, clock.age(), reduce) ?: return@inDp
            val look = ToonMenuLook(u = m.u, pressed = pressed, selected = selected, focus = focusOn.takeIf { focused })
            val scene = toonMenuScene(tokens, w, items, look)
            drawScene(if (m.fade == 1f) scene else scene.placed(0f, 0f, m.fade), texts)
        }
    }
}

/** Each item is 44 dp; the paper starts 14 dp down. */
private const val ITEM_H = 44f

/** The kit's box: the items, the rolls and their shadow. */
internal fun toonMenuHeight(n: Int): Float = 14 + n * ITEM_H + 25

/**
 * The kit's `down`: the item under [y]. The first item's target reaches up to the scroll's top and
 * the last one's down to its bottom, which makes both 48 dp and more (the brief's target) without
 * moving what the design draws; an item between them stays 44 dp, the owner's named exception (B-50).
 */
internal fun toonMenuItemAt(
    n: Int,
    y: Float,
): Int? = if (n == 0 || y < 0 || y > toonMenuHeight(n)) null else floor((y - 14) / ITEM_H).toInt().coerceIn(0, n - 1)

/** The kit plays the open it mounts with for 600 ms, and every change after it, either way, for 400. */
private const val MOUNT_MS = 600f
private const val CHANGE_MS = 400f
private const val REDUCED_FADE_MS = 120f

/** The engine's `menuUnroll` at [t] ms: unrolled with an overshoot when [open], rolled up in 160 ms after. */
internal fun toonMenuUnroll(
    t: Float,
    open: Boolean,
): Float = if (open) 1 - toonSpring(t / 1000, .42f, 24f) else max(0f, 1 - max(0f, min(1f, t / 160)).pow(2))

/** How much of the paper shows, and the fade of the whole under reduce motion. */
internal data class ToonMenuMoment(
    val u: Float,
    val fade: Float = 1f,
)

/**
 * The kit's scene at a moment: [open], changed [ageMs] ago (null for one open or shut from the
 * start). Null when nothing shows. Under reduce motion the scroll fades in and out unrolled.
 */
internal fun toonMenuFrameAt(
    open: Boolean,
    ageMs: Float?,
    reduceMotion: Boolean,
): ToonMenuMoment? {
    if (reduceMotion && ageMs != null) {
        val f = min(1f, ageMs / REDUCED_FADE_MS)
        if (!open && f >= 1) return null
        return ToonMenuMoment(1f, if (open) f else 1 - f)
    }
    val u = if (ageMs == null) (if (open) 1f else 0f) else toonMenuUnroll(ageMs, open)
    return if (u < .01f) null else ToonMenuMoment(u)
}

/** The kit's clock: the open it mounts with or a change plays from its first frame, and once played the menu is at rest. */
@Stable
internal class ToonMenuClock(
    initial: Boolean,
) {
    var nowMs by mutableLongStateOf(0L)
    var starts by mutableStateOf(0)
    var open by mutableStateOf(initial)
    private var at: Long? = if (initial) PENDING_START else null
    private var playMs = MOUNT_MS

    fun changed(now: Boolean) {
        if (now == open) return
        open = now
        at = PENDING_START
        playMs = CHANGE_MS
        starts++
    }

    fun tick(now: Long) {
        nowMs = now
        if (at == PENDING_START) at = now
    }

    /** Milliseconds since the change, or null at rest — as the kit's `t` is, so an open ends unrolled exactly. */
    fun age(): Float? =
        nowMs
            .let { now -> at?.let { if (it == PENDING_START) 0f else (now - it).toFloat() } }
            ?.takeIf { it < playMs }

    fun running(): Boolean = age() != null
}

private const val PENDING_START = -1L

/** What one frame of the menu draws: the engine's `menu` options. */
internal data class ToonMenuLook(
    val u: Float = 1f,
    val pressed: Int? = null,
    val selected: Int = -1,
    val focus: Int? = null,
    val disabled: Int? = null,
)

private val ROLL_RADII = ToonRadii(8f, 7f, 8f, 7f)
private val ROLL_INSETS = ToonInsets(2f, 2.2f, 3f, 2.2f)
private val PAPER_RADII = ToonRadii(4f, 4f, 10f, 8f)
private val SHADE_RADII = ToonRadii(10f, 7f, 11f, 8f)
private val RING_RADII = ToonRadii(12f, 9f, 13f, 10f)
private val ITEM_LABEL = ToonTextStyle(600, 16f)

/** A deletion's label, drawn in the danger text colour. */
private val DANGER = Regex("delete|remove", RegexOption.IGNORE_CASE)

/** The engine's `menu`: one frame [w] dp wide from its top left. */
@OptIn(InternalToonApi::class)
internal fun toonMenuScene(
    tokens: ToonTokens,
    w: Float,
    items: List<ToonMenuItem>,
    o: ToonMenuLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val ih = ITEM_H
    val full = 14 + items.size * ih + 10
    val h = max(0f, full * o.u)
    val layers = mutableListOf<ToonLayer>()
    val labels = mutableListOf<ToonLabel>()

    fun roll(
        y: Float,
        seed: Int,
    ) {
        val g = shapePoints(0f, y - 8, w, 16f, ROLL_RADII, ROLL_INSETS, sh.wobble * .6f, seed, 4f)
        layers += smoothLayer(g.ink, c.ink, dx = sh.shadow * .6f, dy = sh.shadow * .6f)
        layers += smoothLayer(g.ink, c.ink)
        layers += smoothLayer(g.fill, c.surfacePressed)
        layers += ToonLayer(floatArrayOf(8f, y + 1, w - 8, y + 1), ToonSegments.Line, false, c.ink, .35f, stroke = 1f)
    }
    if (h > 2) {
        layers +=
            bodyLayers(
                shapePoints(6f, 7f, w - 12, h, PAPER_RADII, sh.insets, sh.wobble, sh.seed + 1600),
                c.surface,
                c,
                sh.shadow,
                rim = null,
            )
    }
    roll(7f, sh.seed + 1601)
    items.forEachIndexed { i, it ->
        val iy = 14 + i * ih
        if (iy + ih * .7f > h) return@forEachIndexed
        val colour: Color = if (DANGER.containsMatchIn(it.label)) c.dangerText else c.text
        if (o.selected == i || o.pressed == i) {
            layers +=
                smoothLayer(
                    wobblePoints(14f, iy + 4, w - 28, ih - 8, SHADE_RADII, .8f, sh.seed + 1610 + i, 4f),
                    c.surfacePressed,
                )
        }
        if (o.focus == i) layers += ringLayer(10f, iy + 1, w - 20, ih - 2, RING_RADII, c, sh.wobble, sh.seed + 1620 + i)
        val off = o.disabled == i
        it.icon?.let { g -> layers += iconLayers(g, 22f, iy + 11, colour, size = 22f, alpha = if (off) .4f else 1f) }
        val op = max(0f, min(1f, (h - iy - ih * .7f) / 20)) * (if (off) .45f else 1f)
        labels +=
            ToonLabel(it.label, if (it.icon != null) 54f else 24f, iy + 28, colour, op, ITEM_LABEL, ToonAnchor.Start)
    }
    roll(7 + h, sh.seed + 1602)
    return ToonScene(layers, labels)
}
