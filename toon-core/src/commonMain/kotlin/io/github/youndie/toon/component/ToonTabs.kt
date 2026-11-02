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
import io.github.youndie.toon.draw.inkLinePoints
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.ringLayer
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.wobblePoints
import io.github.youndie.toon.motion.toonSpring
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

// A port of the engine's `tabs` and `tabsSwitch` and the kit's `C.tabs` (B-28): bookmarks standing on
// an ink shelf. On a switch the bookmark stretches from the old tab to the new one and its tail
// follows 80 ms later, while the content slides 40 dp with an overshoot.

/**
 * A [ToonTabs]'s switch, for the content under it: [contentOffset] is how far it slides sideways
 * while a switch plays (0 at rest, and always under reduce motion).
 */
@Stable
public class ToonTabsState internal constructor(
    initial: Int,
) {
    internal var nowMs by mutableLongStateOf(0L)
    internal var starts by mutableStateOf(0)
    internal var sel by mutableIntStateOf(initial)
    internal var from = initial
    internal var slot = 0f
    internal var reduceMotion = false
    private var at: Long? = null

    internal fun select(i: Int) {
        if (i == sel) return
        from = sel
        sel = i
        at = PENDING_START
        starts++
    }

    internal fun tick(now: Long) {
        nowMs = now
        if (at == PENDING_START) at = now
    }

    internal fun age(): Float? = at?.let { if (it == PENDING_START) 0f else (nowMs - it).toFloat() }

    internal fun running(): Boolean = age()?.let { it < SWITCH_MS } ?: false

    /** The engine's `cx`: the content's sideways offset now. */
    public val contentOffset: Dp
        get() = toonTabsFrameAt(age(), from, sel, slot, reduceMotion).cx.dp
}

/** A [ToonTabsState] that starts on [selected]. */
@Composable
public fun rememberToonTabsState(selected: Int = 0): ToonTabsState = remember { ToonTabsState(selected) }

/**
 * The design's tabs: [labels] in equal slots across [width], the bookmark behind [selected]. A release
 * on another tab switches to it and reports it to [onChange]; [state] tells the content how far to
 * slide while the switch plays.
 */
@Composable
public fun ToonTabs(
    labels: List<String>,
    modifier: Modifier = Modifier,
    selected: Int = 0,
    width: Dp = 342.dp,
    state: ToonTabsState = rememberToonTabsState(selected),
    onChange: (Int) -> Unit = {},
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val texts = rememberToonText()
    var pressed by remember { mutableStateOf<Int?>(null) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var focusOn by remember { mutableIntStateOf(selected) }
    val change by rememberUpdatedState(onChange)
    val w = width.value
    val n = labels.size
    state.slot = w / n
    state.reduceMotion = reduce
    LaunchedEffect(selected) { state.select(selected) }
    LaunchedEffect(state.starts) { while (state.running()) withFrameMillis { state.tick(it) } }

    fun choose(i: Int) {
        if (i == state.sel) return
        state.select(i)
        change(i)
    }

    Canvas(
        modifier
            .size(width, TABS_BOX.dp)
            .semantics {
                stateDescription = labels.getOrNull(state.sel) ?: ""
                customActions = labels.mapIndexed { i, it -> CustomAccessibilityAction(it) { choose(i).let { true } } }
            }.onKeyEvent {
                if (it.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (it.key) {
                    Key.DirectionLeft -> {
                        focusOn = max(0, focusOn - 1)
                        true
                    }

                    Key.DirectionRight -> {
                        focusOn = min(n - 1, focusOn + 1)
                        true
                    }

                    Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                        choose(focusOn)
                        true
                    }

                    else -> {
                        false
                    }
                }
            }.focusable(interactionSource = interaction)
            .pointerInput(n, w) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    pressed = toonNavSlot(w, n, down.position.x)
                    val up = waitForUpOrCancellation()
                    pressed = null
                    if (up != null) choose(toonNavSlot(w, n, up.position.x))
                }
            },
    ) {
        val a = toonTabsFrameAt(state.age(), state.from, state.sel, w / n, reduce)
        val look =
            ToonTabsLook(
                sel = a.sel ?: state.sel,
                ia = a.ia,
                ib = a.ib,
                pressed = pressed,
                focus = focusOn.takeIf { focused },
            )
        drawScene(toonTabsScene(tokens, w, labels, look), texts)
    }
}

/** The kit's box: 52 dp, the shelf at 48. */
private const val TABS_BOX = 52f

/** The switch plays for 700 ms in the kit; under reduce motion — the MVP page's `instant` — none. */
private const val SWITCH_MS = 700f

private const val PENDING_START = -1L

/** One frame of a switch: the bookmark's ends, the content's offset, and which tab reads as selected. */
internal data class ToonTabsMotion(
    val ia: Float? = null,
    val ib: Float? = null,
    val cx: Float = 0f,
    val sel: Int? = null,
)

/** The engine's `tabsSwitch` at [t] ms, from tab [from] to [to], [slot] dp wide. */
internal fun toonTabsSwitch(
    t: Float,
    from: Int,
    to: Int,
    slot: Float,
): ToonTabsMotion {
    val a0 = slot * from + 6
    val a1 = slot * to + 6
    val wd = slot - 12
    val dir = (to - from).toFloat().sign.takeIf { it != 0f } ?: 1f
    val lead = 1 - toonSpring(t / 1000, .5f, 40f)
    val trail = 1 - toonSpring(max(0f, t - 80) / 1000, .5f, 40f)
    return ToonTabsMotion(
        ia = a0 + (a1 - a0) * (if (dir > 0) trail else lead),
        ib = a0 + wd + (a1 - a0) * (if (dir > 0) lead else trail),
        cx = dir * 40 * toonSpring(t / 1000, .45f, 30f),
        sel = if (t > 100) to else from,
    )
}

/** The kit's motion [ageMs] after a switch from [from] to [to]: none once over, or under reduce motion. */
internal fun toonTabsFrameAt(
    ageMs: Float?,
    from: Int,
    to: Int,
    slot: Float,
    reduceMotion: Boolean,
): ToonTabsMotion =
    if (reduceMotion || ageMs == null ||
        ageMs >= SWITCH_MS
    ) {
        ToonTabsMotion()
    } else {
        toonTabsSwitch(ageMs, from, to, slot)
    }

/** What one frame of the tabs draws: the engine's `tabs` options. */
internal data class ToonTabsLook(
    val sel: Int,
    val ia: Float? = null,
    val ib: Float? = null,
    val pressed: Int? = null,
    val focus: Int? = null,
    val disabled: Int? = null,
)

private val MARK_RADII = ToonRadii(12f, 9f, 2f, 2f)
private val MARK_INSETS = ToonInsets(2f, 2.4f, 0f, 2.2f)
private val PRESS_RADII = ToonRadii(12f, 9f, 3f, 3f)
private val RING_RADII = ToonRadii(14f, 11f, 6f, 6f)
private val LABEL = ToonTextStyle(600, 16f)
private val LABEL_ACTIVE = ToonTextStyle(800, 16f)

/** The engine's `tabs`: one frame [w] dp wide from its top left. */
@OptIn(InternalToonApi::class)
internal fun toonTabsScene(
    tokens: ToonTokens,
    w: Float,
    labels: List<String>,
    o: ToonTabsLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val slot = w / labels.size
    val ia = o.ia ?: (slot * o.sel + 6)
    val ib = o.ib ?: (slot * (o.sel + 1) - 6)
    val rs = sh.radiusScale
    val r =
        ToonRadii(
            MARK_RADII.topLeft * rs,
            MARK_RADII.topRight * rs,
            MARK_RADII.bottomRight * rs,
            MARK_RADII.bottomLeft * rs,
        )
    val layers =
        bodyLayers(
            shapePoints(ia, 4f, max(10f, ib - ia), 46f, r, MARK_INSETS, sh.wobble, sh.seed + 850),
            c.chipSelected,
            c,
            0f,
            rim = null,
        ).toMutableList()
    layers += ToonLayer(inkLinePoints(-4f, w + 4, 48f, 3.6f, sh.seed + 851), ToonSegments.Line, true, c.ink)
    val texts = mutableListOf<ToonLabel>()
    labels.forEachIndexed { i, lb ->
        val act = i == o.sel
        val pr = o.pressed == i
        // A pressed tab's shade goes under everything, the bookmark included.
        if (pr &&
            !act
        ) {
            layers.add(
                0,
                smoothLayer(
                    wobblePoints(
                        slot * i + 8,
                        8f,
                        slot - 16,
                        40f,
                        PRESS_RADII,
                        sh.wobble,
                        sh.seed + 852 + i,
                        4f,
                    ),
                    c.surfacePressed,
                ),
            )
        }
        if (o.focus ==
            i
        ) {
            layers += ringLayer(slot * i + 2, -1f, slot - 4, 54f, RING_RADII, c, sh.wobble, sh.seed + 853 + i)
        }
        texts +=
            ToonLabel(
                lb,
                slot * i + slot / 2,
                32f + (if (pr) 2 else 0),
                if (act) c.ink else c.text,
                if (o.disabled == i) .45f else 1f,
                if (act) LABEL_ACTIVE else LABEL,
                ToonAnchor.Middle,
            )
    }
    return ToonScene(layers, texts)
}
