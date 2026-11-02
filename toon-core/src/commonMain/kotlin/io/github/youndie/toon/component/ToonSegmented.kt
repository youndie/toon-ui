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
import androidx.compose.ui.semantics.disabled
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
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonSize
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

// A port of the engine's `segmented` and `segSwitch` and the kit's `C.segmented` (B-41): segments in
// one shared outline, the selection a drop of paint. On a switch the drop stretches toward the new
// segment, thinning as it stretches, and its tail follows 80 ms later — the tabs' `tabsSwitch`, a
// dp wider each end.

/**
 * The design's segmented button: [labels] in equal segments across [width], the paint drop on
 * [selected]. A release on another segment moves the drop there and reports it to [onChange];
 * disabled, it takes no input and greys.
 */
@Composable
public fun ToonSegmented(
    labels: List<String>,
    modifier: Modifier = Modifier,
    selected: Int = 0,
    width: Dp = 300.dp,
    enabled: Boolean = true,
    onChange: (Int) -> Unit = {},
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val texts = rememberToonText()
    val clock = remember { ToonSegmentedClock(selected) }
    var pressed by remember { mutableStateOf<Int?>(null) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var focusOn by remember { mutableIntStateOf(selected) }
    val change by rememberUpdatedState(onChange)
    val w = width.value
    val n = labels.size
    LaunchedEffect(selected) { clock.select(selected) }
    LaunchedEffect(clock.starts) { while (clock.running()) withFrameMillis { clock.tick(it) } }

    fun choose(i: Int) {
        if (i == clock.sel) return
        clock.select(i)
        change(i)
    }

    Canvas(
        modifier
            .size(width, ToonSize.TARGET_MIN.dp)
            .semantics {
                stateDescription = labels.getOrNull(clock.sel) ?: ""
                if (!enabled) {
                    disabled()
                } else {
                    customActions =
                        labels.mapIndexed { i, it -> CustomAccessibilityAction(it) { choose(i).let { true } } }
                }
            }.then(
                if (enabled) {
                    Modifier
                        .onKeyEvent {
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
                                // Coordinates are taken as dp, as every component draws them (B-47).
                                val down = awaitFirstDown()
                                pressed = toonNavSlot(w, n, down.position.x / density)
                                val up = waitForUpOrCancellation()
                                val i = pressed
                                pressed = null
                                if (up != null && i != null) choose(i)
                            }
                        }
                } else {
                    Modifier
                },
            ),
    ) {
        // The engine draws in dp (research D5); the canvas is pixels (B-47).
        inDp {
            val a = toonSegmentedFrameAt(clock.age(), clock.from, clock.sel, w / n, reduce)
            val look =
                ToonSegmentedLook(
                    sel = a.sel ?: clock.sel,
                    ia = a.ia,
                    ib = a.ib,
                    sq = a.sq,
                    pressed = pressed,
                    focus = focusOn.takeIf { focused && enabled },
                    disabled = !enabled,
                )
            drawScene(toonSegmentedScene(tokens, w, labels, look).placed(0f, SEG_DROP), texts)
        }
    }
}

/** The design draws it 44 dp tall; it is laid out 48, a finger's target (research §1.6), the drawing 2 dp down. */
private const val SEG_H = 44f
private const val SEG_DROP = (ToonSize.TARGET_MIN - SEG_H) / 2

/** The switch plays for 700 ms in the kit; under reduce motion — the v1 page's `instant` — none. */
private const val SWITCH_MS = 700f

/** One frame of a switch: the drop's ends, how thin it is, and which segment reads as selected. */
internal data class ToonSegmentedMotion(
    val ia: Float? = null,
    val ib: Float? = null,
    val sq: Float = 0f,
    val sel: Int? = null,
)

/** The engine's `segSwitch` at [t] ms, from segment [from] to [to], [slot] dp wide. */
internal fun toonSegSwitch(
    t: Float,
    from: Int,
    to: Int,
    slot: Float,
): ToonSegmentedMotion {
    val a = toonTabsSwitch(t, from, to, slot)
    val ia = a.ia!!
    val ib = a.ib!!
    val base = slot - 12
    return ToonSegmentedMotion(ia - 1, ib + 1, max(0f, min(1f, (ib - ia - base) / base)) * .8f, a.sel)
}

/** The kit's motion [ageMs] after a switch: none once over, or under reduce motion. */
internal fun toonSegmentedFrameAt(
    ageMs: Float?,
    from: Int,
    to: Int,
    slot: Float,
    reduceMotion: Boolean,
): ToonSegmentedMotion =
    if (reduceMotion || ageMs == null ||
        ageMs >= SWITCH_MS
    ) {
        ToonSegmentedMotion()
    } else {
        toonSegSwitch(ageMs, from, to, slot)
    }

/** The kit's clock: the selection, where it came from, and when it moved. */
@Stable
internal class ToonSegmentedClock(
    initial: Int,
) {
    var nowMs by mutableLongStateOf(0L)
    var starts by mutableStateOf(0)
    var sel by mutableIntStateOf(initial)
    var from = initial
    private var at: Long? = null

    fun select(i: Int) {
        if (i == sel) return
        from = sel
        sel = i
        at = PENDING_START
        starts++
    }

    fun tick(now: Long) {
        nowMs = now
        if (at == PENDING_START) at = now
    }

    fun age(): Float? = at?.let { if (it == PENDING_START) 0f else (nowMs - it).toFloat() }

    fun running(): Boolean = age()?.let { it < SWITCH_MS } ?: false
}

private const val PENDING_START = -1L

/** What one frame of the segmented button draws: the engine's `segmented` options. */
internal data class ToonSegmentedLook(
    val sel: Int,
    val ia: Float? = null,
    val ib: Float? = null,
    val sq: Float = 0f,
    val pressed: Int? = null,
    val focus: Int? = null,
    val disabled: Boolean = false,
)

private val OUTLINE_RADII = ToonRadii(14f, 10f, 16f, 11f)
private val PRESS_RADII = ToonRadii(14f, 12f, 14f, 13f)
private val DROP_RADII = ToonRadii(16f, 14f, 16f, 15f)
private val DROP_INSETS = ToonInsets(1.6f, 1.8f, 2.6f, 1.6f)
private val RING_RADII = ToonRadii(14f, 11f, 15f, 12f)
private val LABEL = ToonTextStyle(600, 15f)
private val LABEL_SELECTED = ToonTextStyle(800, 15f)

/** The engine's `f`: a number as it writes it into a path, to a tenth. */
private fun tenth(v: Float) = floor(v * 10 + .5f) / 10

/** The engine's `segmented`: one frame [w] dp wide and 44 tall, from its top left. */
@OptIn(InternalToonApi::class)
internal fun toonSegmentedScene(
    tokens: ToonTokens,
    w: Float,
    labels: List<String>,
    o: ToonSegmentedLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val h = SEG_H
    val n = labels.size
    val slot = w / n
    val dis = o.disabled
    val g =
        shapePoints(
            0f,
            0f,
            w,
            h,
            OUTLINE_RADII.let {
                ToonRadii(
                    it.topLeft * sh.radiusScale,
                    it.topRight * sh.radiusScale,
                    it.bottomRight * sh.radiusScale,
                    it.bottomLeft * sh.radiusScale,
                )
            },
            sh.insets,
            sh.wobble,
            sh.seed + 2000,
        )
    val layers = mutableListOf<ToonLayer>()
    layers += smoothLayer(g.ink, c.ink, if (dis) 0f else 1f, sh.shadow, sh.shadow)
    layers += smoothLayer(g.ink, c.ink, if (dis) .35f else 1f)
    layers += smoothLayer(g.fill, if (dis) c.disabled else c.surface)
    for (i in 1 until n) {
        layers +=
            ToonLayer(
                floatArrayOf(tenth(slot * i), 9f, tenth(slot * i + .6f), h - 9),
                ToonSegments.Line,
                false,
                c.ink,
                .45f,
                stroke = 2f,
            )
    }
    labels.indices.forEach { i ->
        if (o.pressed == i && i != o.sel) {
            layers +=
                smoothLayer(
                    wobblePoints(slot * i + 6, 7f, slot - 12, h - 14, PRESS_RADII, .9f, sh.seed + 2005 + i, 4f),
                    c.surfacePressed,
                )
        }
    }
    val ia = o.ia ?: (slot * o.sel + 5)
    val ib = o.ib ?: (slot * (o.sel + 1) - 5)
    val sq = o.sq
    val drop =
        shapePoints(
            ia,
            6 + 3 * sq,
            max(10f, ib - ia),
            h - 12 - 6 * sq,
            DROP_RADII,
            DROP_INSETS,
            1.1f,
            sh.seed + 2001,
            4f,
        )
    layers +=
        bodyLayers(drop, if (dis) c.disabled else c.chipSelected, c, 0f, inkAlpha = if (dis) .35f else 1f, rim = null)
    val labelsOut = mutableListOf<ToonLabel>()
    labels.forEachIndexed { i, lb ->
        val act = i == o.sel
        if (o.focus ==
            i
        ) {
            layers += ringLayer(slot * i + 1, -5f, slot - 2, h + 10, RING_RADII, c, sh.wobble, sh.seed + 2010 + i)
        }
        labelsOut +=
            ToonLabel(
                lb,
                slot * i + slot / 2,
                h / 2 + 5.5f + (if (o.pressed == i) 1.5f else 0f),
                if (act) c.ink else c.text,
                if (dis) .5f else 1f,
                if (act) LABEL_SELECTED else LABEL,
                ToonAnchor.Middle,
            )
    }
    return ToonScene(layers, labelsOut)
}
