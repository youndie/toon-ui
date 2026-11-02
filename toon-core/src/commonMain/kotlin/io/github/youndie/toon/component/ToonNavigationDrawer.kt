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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import io.github.youndie.toon.draw.sizeDp
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.wobblePoints
import io.github.youndie.toon.icon.ToonIconGlyph
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.icon.iconLayers
import io.github.youndie.toon.motion.toonSpring
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// A port of the engine's `drawer`, `drawerOpen` and `drawerClose` and the kit's `C.drawer` (B-39): a
// door hung on three hinges at the left edge, which swings open — the engine squeezes it across and
// shears it down (`matrix(sx k 0 1 0 0)`, an affine skew, not a perspective) — overshoots, and
// shivers on its hinges; the labels fade in once it is flat. Like the dialog, it fills its parent.

/** One destination of a [ToonNavigationDrawer]: its [label] and its [icon]. */
public class ToonDrawerItem(
    public val label: String,
    public val icon: ToonIconGlyph = ToonIcons.Star,
)

/**
 * The design's navigation drawer over the whole of its box: [title] and a row per [items], the
 * [selected] one marked, on a door that swings open when [open] turns true and shut when it turns
 * false. A release on a row reports it to [onSelect]; right of the door, [onClose]. Closed, it takes
 * no input.
 */
@Composable
public fun ToonNavigationDrawer(
    items: List<ToonDrawerItem>,
    modifier: Modifier = Modifier,
    open: Boolean = true,
    title: String = "Saturday",
    selected: Int = 0,
    onSelect: (Int) -> Unit = {},
    onClose: () -> Unit = {},
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val texts = rememberToonText()
    val clock = remember { ToonDrawerClock(open) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var focusOn by remember { mutableIntStateOf(selected) }
    var pressed by remember { mutableStateOf<Int?>(null) }
    val select by rememberUpdatedState(onSelect)
    val close by rememberUpdatedState(onClose)
    val n = items.size
    LaunchedEffect(open) { clock.changed(open) }
    LaunchedEffect(clock.starts) { while (clock.running()) withFrameMillis { clock.tick(it) } }

    Canvas(
        modifier
            .fillMaxSize()
            .then(
                if (clock.open) {
                    Modifier
                        .semantics {
                            paneTitle = title
                            if (selected in items.indices) stateDescription = items[selected].label
                            dismiss { close().let { true } }
                            customActions =
                                items.mapIndexed {
                                    i,
                                    it,
                                    ->
                                    CustomAccessibilityAction(it.label) { select(i).let { true } }
                                }
                        }.onKeyEvent {
                            if (it.type != KeyEventType.KeyDown) return@onKeyEvent false
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
                                    if (focusOn in items.indices) select(focusOn)
                                    true
                                }

                                Key.Escape -> {
                                    close()
                                    true
                                }

                                else -> {
                                    false
                                }
                            }
                        }.focusable(interactionSource = interaction)
                        .pointerInput(n) {
                            awaitEachGesture {
                                // Coordinates are taken as dp, as every component draws them (B-47).
                                val dw = toonDrawerWidth(size.width / density)
                                val down = awaitFirstDown()
                                pressed = toonDrawerItemAt(dw, n, down.position.x / density, down.position.y / density)
                                val up = waitForUpOrCancellation()
                                val i = pressed
                                pressed = null
                                if (up == null) return@awaitEachGesture
                                if (up.position.x / density > dw) {
                                    close()
                                } else if (i != null) {
                                    select(i)
                                }
                            }
                        }
                } else {
                    Modifier
                },
            ),
    ) {
        // The engine draws in dp (research D5); the canvas is pixels (B-47).
        inDp {
            val w = sizeDp.width
            val h = sizeDp.height
            val m = toonDrawerFrameAt(clock.open, clock.age(), reduce) ?: return@inDp
            val look = ToonDrawerLook(m.motion, selected, pressed, focusOn.takeIf { focused })
            val scene = toonDrawerScene(tokens, toonDrawerWidth(w), h, w, title, items, look)
            drawScene(if (m.fade == 1f) scene else scene.placed(0f, 0f, m.fade), texts)
        }
    }
}

/** Each destination is a 56 dp row; the first starts 120 dp down, its target 8 dp above that. */
private const val ROW_STEP = 56f
private const val ROWS_AT = 120f
private const val TARGETS_AT = 112f

/** The kit's `dw`: the door is 300 dp wide, or the screen less 60 when that is narrower. */
internal fun toonDrawerWidth(screenW: Float): Float = min(300f, screenW - 60)

/** The kit's `down`: the row under ([x], [y]) on a door [dw] wide with [n] rows, or null. */
internal fun toonDrawerItemAt(
    dw: Float,
    n: Int,
    x: Float,
    y: Float,
): Int? {
    val i = floor((y - TARGETS_AT) / ROW_STEP).toInt()
    return if (x < dw && i in 0 until n) i else null
}

/** The open plays 600 ms in the kit when it mounts open, and a change either way 400; a close is drawn for 240. */
private const val MOUNT_MS = 600f
private const val CHANGE_MS = 400f
private const val CLOSE_DRAWN_MS = 240f
private const val REDUCED_FADE_MS = 120f

/** The door's squeeze across ([sx]), its shiver in degrees ([rot]), the scrim, and the labels' opacity. */
internal data class ToonDrawerMotion(
    val sx: Float = 1f,
    val rot: Float = 0f,
    val scrim: Float = 1f,
    val textOp: Float = 1f,
)

/** The engine's `drawerOpen` at [t] ms: swings open on a spring, shivers from 160 ms, labels from 180. */
internal fun toonDrawerOpen(t: Float): ToonDrawerMotion {
    val sx = 1 - toonSpring(t / 1000, .35f, 26f)
    val shiver = if (t > 160) .7f * sin((t - 160) / 16) * exp(-(t - 160) / 90) else 0f
    return ToonDrawerMotion(max(.02f, sx), shiver, c01(t / 220), c01((t - 180) / 100))
}

/** The engine's `drawerClose` at [t] ms: swings shut with ease-in in 220 ms, the labels gone in 60. */
internal fun toonDrawerClose(t: Float): ToonDrawerMotion {
    val u = c01(t / 220)
    return ToonDrawerMotion(max(.02f, 1 - u * u), 0f, 1 - u, 1 - c01(t / 60))
}

private fun c01(v: Float) = max(0f, min(1f, v))

/** A moment of the drawer: its motion, and the fade of the whole under reduce motion. */
internal data class ToonDrawerMoment(
    val motion: ToonDrawerMotion,
    val fade: Float = 1f,
)

/**
 * The kit's scene at a moment: [open], changed [ageMs] ago (null at rest). Null when nothing shows.
 * Under reduce motion — the v1 page's `fade 120` — the door fades in and out flat.
 */
internal fun toonDrawerFrameAt(
    open: Boolean,
    ageMs: Float?,
    reduceMotion: Boolean,
): ToonDrawerMoment? {
    if (reduceMotion && ageMs != null) {
        val f = min(1f, ageMs / REDUCED_FADE_MS)
        if (!open && f >= 1) return null
        return ToonDrawerMoment(ToonDrawerMotion(), if (open) f else 1 - f)
    }
    if (!open && (ageMs == null || ageMs > CLOSE_DRAWN_MS)) return null
    val m =
        when {
            ageMs == null -> ToonDrawerMotion()
            open -> toonDrawerOpen(ageMs)
            else -> toonDrawerClose(ageMs)
        }
    return ToonDrawerMoment(m)
}

/** The kit's clock: the open it mounts with or a change plays from its first frame, then the drawer is at rest. */
@Stable
internal class ToonDrawerClock(
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

    /** Milliseconds since the change, or null at rest — as the kit's `t` is. */
    fun age(): Float? =
        nowMs
            .let { now -> at?.let { if (it == PENDING_START) 0f else (now - it).toFloat() } }
            ?.takeIf { it < playMs }

    fun running(): Boolean = age() != null
}

private const val PENDING_START = -1L

/** What one frame of the drawer draws: the engine's `drawer` options. */
internal data class ToonDrawerLook(
    val motion: ToonDrawerMotion = ToonDrawerMotion(),
    val sel: Int = 0,
    val pressed: Int? = null,
    val focus: Int? = null,
)

private val DOOR_RADII = ToonRadii(0f, 18f, 22f, 0f)
private val HINGE_RADII = ToonRadii(3f, 4f, 3f, 4f)
private val HINGE_INSETS = ToonInsets(1.4f, 1.6f, 2f, 1.4f)
private val KNOB_RADII = ToonRadii(9f, 9f, 9f, 9f)
private val KNOB_INSETS = ToonInsets(1.8f, 2f, 2.6f, 1.8f)
private val ROW_RADII = ToonRadii(14f, 10f, 16f, 11f)
private val FOCUS_RADII = ToonRadii(16f, 12f, 18f, 13f)
private val TITLE = ToonTextStyle(400, 30f, display = true)
private val ROW_LABEL = ToonTextStyle(700, 16f)

/** The engine's `drawer`: a door [w] × [h] at the left of a screen [screenW] wide, its scrim over all of it. */
@OptIn(InternalToonApi::class)
internal fun toonDrawerScene(
    tokens: ToonTokens,
    w: Float,
    h: Float,
    screenW: Float,
    title: String,
    items: List<ToonDrawerItem>,
    o: ToonDrawerLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val a = o.motion
    val sx = max(.02f, a.sx)
    val k = (1 - min(1f, sx)) * .3f
    val layers = mutableListOf<ToonLayer>()
    if (a.scrim > 0) {
        layers +=
            ToonLayer(
                floatArrayOf(0f, 0f, screenW, 0f, screenW, h, 0f, h),
                ToonSegments.Line,
                true,
                c.ink,
                .45f * a.scrim,
            )
    }
    val door = mutableListOf<ToonLayer>()
    door +=
        bodyLayers(
            shapePoints(0f, 0f, w, h, DOOR_RADII, sh.insets, sh.wobble, sh.seed + 1800),
            c.surface,
            c,
            sh.shadow + 2,
        )
    listOf(90f, h / 2, h - 90).forEachIndexed { j, hy ->
        val g = shapePoints(-4f, hy - 16, 12f, 32f, HINGE_RADII, HINGE_INSETS, .3f, sh.seed + 1810 + j, 2f)
        door += smoothLayer(g.ink, c.ink)
        door += smoothLayer(g.fill, c.nail)
    }
    val knob = shapePoints(w - 30, h / 2 - 9, 18f, 18f, KNOB_RADII, KNOB_INSETS, .4f, sh.seed + 1820, 3f)
    door += smoothLayer(knob.ink, c.ink)
    door += smoothLayer(knob.fill, c.warning)
    items.forEachIndexed { i, it ->
        val iy = ROWS_AT + i * ROW_STEP
        if (i ==
            o.sel
        ) {
            door +=
                smoothLayer(wobblePoints(16f, iy - 8, w - 64, 42f, ROW_RADII, 1.4f, sh.seed + 1830, 4f), c.chipSelected)
        }
        if (o.pressed == i && i != o.sel) {
            door +=
                smoothLayer(wobblePoints(16f, iy - 8, w - 64, 42f, ROW_RADII, 1f, sh.seed + 1831, 4f), c.surfacePressed)
        }
        door += iconLayers(it.icon, 28f, iy + 1, if (i == o.sel) c.ink else c.text)
        // The engine draws no keyboard focus; the library rings the focused row as the list item does.
        if (o.focus == i) door += ringLayer(12f, iy - 12, w - 56, 50f, FOCUS_RADII, c, sh.wobble, sh.seed + 1832)
    }
    val scene = ToonScene(layers, emptyList()) + ToonScene(door, emptyList()).skewedAbout(sx, k, a.rot, h / 2)
    val labels = mutableListOf(ToonLabel(title, 24f, 72f, c.text, a.textOp, TITLE, ToonAnchor.Start))
    items.forEachIndexed { i, it ->
        labels +=
            ToonLabel(
                it.label,
                64f,
                ROWS_AT + i * ROW_STEP + 19,
                if (i ==
                    o.sel
                ) {
                    c.ink
                } else {
                    c.text
                },
                a.textOp,
                ROW_LABEL,
                ToonAnchor.Start,
            )
    }
    return scene + ToonScene(emptyList(), labels)
}
