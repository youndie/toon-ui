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
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonAnchor
import io.github.youndie.toon.draw.ToonInsets
import io.github.youndie.toon.draw.ToonLabel
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonSegments
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.sizeDp
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.icon.ToonIconGlyph
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.motion.toonSpring
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

// A port of the engine's `sheet` and `sheetMove` and the kit's `C.sheet` (B-38): a paper sheet on a
// tongue, over a scrim, with a list item per action; it slides up with an overshoot as the scrim
// darkens, and falls on close. Like the dialog, it fills its parent.

/** One action of a [ToonBottomSheet]: its [label] and the [icon] on its sticker. */
public class ToonSheetItem(
    public val label: String,
    public val icon: ToonIconGlyph = ToonIcons.Star,
)

/**
 * The design's bottom sheet over the whole of its box: [title] and a row per [items], on a sheet
 * that slides up when [open] turns true and falls when it turns false. A tap on a row reports it to
 * [onSelect]; above the sheet's tongue, [onClose]. Closed, it takes no input.
 */
@Composable
public fun ToonBottomSheet(
    items: List<ToonSheetItem>,
    modifier: Modifier = Modifier,
    open: Boolean = true,
    title: String = "Share plan",
    onSelect: (Int) -> Unit = {},
    onClose: () -> Unit = {},
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val texts = rememberToonText()
    val clock = remember { ToonSheetClock(open) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var focusOn by remember { mutableIntStateOf(0) }
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
                                awaitFirstDown()
                                val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                                // Coordinates are taken as dp, as every component draws them (B-47).
                                val h = size.height / density
                                when (val hit = toonSheetHit(h, n, up.position.y / density)) {
                                    ToonSheetHit.Close -> close()
                                    is ToonSheetHit.Item -> select(hit.index)
                                    null -> Unit
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
            val m = toonSheetFrameAt(clock.open, clock.age(), reduce, toonSheetHeight(h, n)) ?: return@inDp
            val scene = toonSheetLayerScene(tokens, w, h, title, items, m, focusOn.takeIf { focused })
            drawScene(if (m.fade == 1f) scene else scene.placed(0f, 0f, m.fade), texts)
        }
    }
}

/** Each action is a 72 dp list item; the first starts 70 dp under the sheet's top. */
private const val ROW_STEP = 72f
private const val ROWS_AT = 70f

/** The kit's `sh`: the sheet is tall enough for its rows, and leaves 80 dp of the screen above it. */
internal fun toonSheetHeight(
    screenH: Float,
    n: Int,
): Float = min(screenH - 80, 110 + n * ROW_STEP)

/** Where a release lands: on a row, above the tongue (which closes), or on the sheet between. */
internal sealed interface ToonSheetHit {
    data object Close : ToonSheetHit

    data class Item(
        val index: Int,
    ) : ToonSheetHit
}

/** The kit's `up`, on a sheet at rest [screenH] dp tall with [n] rows. */
internal fun toonSheetHit(
    screenH: Float,
    n: Int,
    y: Float,
): ToonSheetHit? {
    val top = screenH - toonSheetHeight(screenH, n)
    if (y < top - 16) return ToonSheetHit.Close
    val i = floor((y - top - ROWS_AT) / ROW_STEP).toInt()
    return if (i in 0 until n) ToonSheetHit.Item(i) else null
}

/** The open plays 600 ms in the kit when it mounts open, and a change either way 400; a close is drawn for 260. */
private const val MOUNT_MS = 600f
private const val CHANGE_MS = 400f
private const val CLOSE_DRAWN_MS = 260f
private const val REDUCED_FADE_MS = 120f

/** The engine's `sheetMove`: how far below its rest the sheet is, [h] dp tall, and how dark the scrim. */
internal data class ToonSheetMotion(
    val dy: Float = 0f,
    val scrim: Float = 1f,
)

/** The engine's `sheetMove` at [t] ms: up on a spring as the scrim darkens over 200 ms, or down with ease-in in 220. */
internal fun toonSheetMove(
    t: Float,
    open: Boolean,
    h: Float,
): ToonSheetMotion {
    if (open) return ToonSheetMotion(h * toonSpring(t / 1000, .5f, 22f), max(0f, min(1f, t / 200)))
    val u = max(0f, min(1f, t / 220))
    return ToonSheetMotion(h * u * u * 1.1f, 1 - u)
}

/** A moment of the sheet: its motion, and the fade of the whole under reduce motion. */
internal data class ToonSheetMoment(
    val motion: ToonSheetMotion,
    val fade: Float = 1f,
)

/**
 * The kit's scene at a moment: [open], changed [ageMs] ago (null at rest), for a sheet [h] dp tall.
 * Null when nothing shows. Under reduce motion — the v1 page's `fade 120` — it fades at rest.
 */
internal fun toonSheetFrameAt(
    open: Boolean,
    ageMs: Float?,
    reduceMotion: Boolean,
    h: Float,
): ToonSheetMoment? {
    if (reduceMotion && ageMs != null) {
        val f = min(1f, ageMs / REDUCED_FADE_MS)
        if (!open && f >= 1) return null
        return ToonSheetMoment(ToonSheetMotion(), if (open) f else 1 - f)
    }
    if (!open && (ageMs == null || ageMs > CLOSE_DRAWN_MS)) return null
    return ToonSheetMoment(if (ageMs == null) ToonSheetMotion() else toonSheetMove(ageMs, open, h))
}

/** The kit's clock: the open it mounts with or a change plays from its first frame, then the sheet is at rest. */
@Stable
internal class ToonSheetClock(
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
        at
            ?.let { if (it == PENDING_START) 0f else (nowMs - it).toFloat() }
            ?.takeIf { it < playMs }

    fun running(): Boolean = age() != null
}

private const val PENDING_START = -1L

private val TONGUE_RADII = ToonRadii(14f, 12f, 4f, 4f)
private val SHEET_RADII = ToonRadii(28f, 24f, 0f, 0f)

/** The engine's `sheet` over a screen [w] × [screenH]: the scrim, the tongue, the sheet [h] tall and its [title]. */
@OptIn(InternalToonApi::class)
internal fun toonSheetScene(
    tokens: ToonTokens,
    w: Float,
    screenH: Float,
    h: Float,
    title: String,
    a: ToonSheetMotion,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val y = screenH - h + a.dy
    val layers = mutableListOf<ToonLayer>()
    if (a.scrim > 0) {
        layers +=
            ToonLayer(
                floatArrayOf(0f, 0f, w, 0f, w, screenH, 0f, screenH),
                ToonSegments.Line,
                true,
                c.ink,
                .5f * a.scrim,
            )
    }
    val ins = sh.insets.let { ToonInsets(it.top * .8f, it.right * .8f, it.bottom * .8f, it.left * .8f) }
    val tongue = shapePoints(w / 2 - 30, y - 16, 60f, 30f, TONGUE_RADII, ins, sh.wobble * .7f, sh.seed + 1701, 4f)
    layers += smoothLayer(tongue.ink, c.ink)
    layers += smoothLayer(tongue.fill, c.chipSelected)
    layers +=
        ToonLayer(floatArrayOf(w / 2 - 12, y - 7, w / 2 + 12, y - 7), ToonSegments.Line, false, c.ink, stroke = 2.6f)
    layers +=
        bodyLayers(
            shapePoints(-6f, y, w + 12, h + 40, SHEET_RADII, sh.insets, sh.wobble, sh.seed + 1700),
            c.surface,
            c,
            0f,
        )
    return ToonScene(layers, listOf(ToonLabel(title, 24f, y + 46, c.text, 1f, DIALOG_TITLE, ToonAnchor.Start)))
}

/** The kit's `scene`: the sheet, and a list item per action on it, the focused one ringed. */
internal fun toonSheetLayerScene(
    tokens: ToonTokens,
    w: Float,
    screenH: Float,
    title: String,
    items: List<ToonSheetItem>,
    m: ToonSheetMoment,
    focus: Int? = null,
): ToonScene {
    val h = toonSheetHeight(screenH, items.size)
    val y = screenH - h + m.motion.dy
    var scene = toonSheetScene(tokens, w, screenH, h, title, m.motion)
    items.forEachIndexed { i, it ->
        scene +=
            toonListItemScene(
                tokens,
                w - 48,
                it.icon,
                it.label,
                "",
                ToonListTrailing.Chevron,
                ToonListItemLook(seed = i, last = i == items.size - 1, focus = focus == i),
            ).placed(24f, y + ROWS_AT + i * ROW_STEP)
    }
    return scene
}
