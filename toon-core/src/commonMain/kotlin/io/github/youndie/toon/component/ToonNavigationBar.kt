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
import io.github.youndie.toon.draw.ToonLabel
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonTextStyle
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.ringLayer
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.wobblePoints
import io.github.youndie.toon.fx.toonDustScene
import io.github.youndie.toon.icon.ToonIconGlyph
import io.github.youndie.toon.icon.ToonIconWeight
import io.github.youndie.toon.icon.iconLayers
import io.github.youndie.toon.motion.toonOscillation
import io.github.youndie.toon.motion.toonSpring
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin

// A port of the engine's `navBar` and `navSelect` and the kit's `C.navbar` (B-27): a paper strip along
// the bottom; the selected icon jumps 8 dp and lands with a squash and dust, while the paint-stroke
// indicator stretches toward it and its tail catches up.

/** One destination of a [ToonNavigationBar]: its [icon] and the [label] under it. */
public class ToonNavItem(
    public val icon: ToonIconGlyph,
    public val label: String,
)

/**
 * The design's navigation bar: [items] in equal slots across [width], [selected] painted over. A
 * release on another item selects it and reports it to [onChange]; [badgeIndex] puts a dot on an item.
 */
@Composable
public fun ToonNavigationBar(
    items: List<ToonNavItem>,
    modifier: Modifier = Modifier,
    selected: Int = 0,
    badgeIndex: Int = -1,
    width: Dp = 390.dp,
    onChange: (Int) -> Unit = {},
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val texts = rememberToonText()
    val clock = remember { ToonNavClock(selected) }
    var pressed by remember { mutableStateOf<Int?>(null) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var focusOn by remember { mutableIntStateOf(selected) }
    val change by rememberUpdatedState(onChange)
    val w = width.value
    val n = items.size
    LaunchedEffect(selected) { clock.select(selected) }
    LaunchedEffect(clock.starts) { while (clock.running()) withFrameMillis { clock.tick(it) } }

    fun choose(i: Int) {
        if (i == clock.sel) return
        clock.select(i)
        change(i)
    }

    Canvas(
        modifier
            .size(width, NAV_BOX.dp)
            .semantics {
                stateDescription = items.getOrNull(clock.sel)?.label ?: ""
                customActions =
                    items.mapIndexed { i, it -> CustomAccessibilityAction(it.label) { choose(i).let { true } } }
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
                    pressed = toonNavSlot(w, n, down.position.x / density)
                    val up = waitForUpOrCancellation()
                    pressed = null
                    if (up != null) choose(toonNavSlot(w, n, up.position.x / density))
                }
            },
    ) {
        // The engine draws in dp (research D5); the canvas is pixels (B-47).

        inDp {
            val motion = toonNavFrameAt(clock.age(), clock.from, clock.sel, w / n, reduce)
            val look =
                ToonNavLook(
                    sel = clock.sel,
                    motion = motion,
                    pressed = pressed,
                    focus = focusOn.takeIf { focused },
                    badge = if (badgeIndex in items.indices) badgeIndex to ToonBadgeLook("", dot = true) else null,
                )
            drawScene(toonNavBarScene(tokens, w, items, look), texts)
        }
    }
}

/** The kit's box: 80 dp; the paper runs 8 dp past each side and on below it. */
private const val NAV_BOX = 80f

/** The selection plays for 700 ms in the kit; under reduce motion — the MVP page's `instant` — none. */
private const val SELECT_MS = 700f

/** The kit's slot under [x]: equal slots across [w], clamped to the [n] items. */
internal fun toonNavSlot(
    w: Float,
    n: Int,
    x: Float,
): Int = floor(x / (w / n)).toInt().coerceIn(0, n - 1)

/** One frame of the selection: the indicator's ends, the icon's jump and squash, the dust's age. */
internal data class ToonNavMotion(
    val ia: Float? = null,
    val ib: Float? = null,
    val iy: Float = 0f,
    val isx: Float = 1f,
    val isy: Float = 1f,
    val du: Float = -1f,
)

/** The engine's `navSelect` at [t] ms, from slot [from] to [to], [slot] dp wide. */
internal fun toonNavSelect(
    t: Float,
    from: Int,
    to: Int,
    slot: Float,
): ToonNavMotion {
    val x0 = slot * from + slot / 2 - 32
    val x1 = slot * to + slot / 2 - 32
    val dir = (to - from).toFloat().sign.takeIf { it != 0f } ?: 1f
    val lead = 1 - toonSpring(t / 1000, .55f, 34f)
    val trail = 1 - toonSpring(max(0f, t - 70) / 1000, .55f, 34f)
    val ia = x0 + (x1 - x0) * (if (dir > 0) trail else lead)
    val ib = x0 + 64 + (x1 - x0) * (if (dir > 0) lead else trail)
    if (t < 180) return ToonNavMotion(ia, ib, iy = -8 * sin(PI.toFloat() * t / 180))
    val a = toonOscillation((t - 180) / 1000, .35f, 45f)
    return ToonNavMotion(ia, ib, isx = 1 + .2f * a, isy = 1 - .24f * a, du = (t - 180) / 300)
}

/** The kit's motion [ageMs] after a selection from [from] to [to]: none once over, or under reduce motion. */
internal fun toonNavFrameAt(
    ageMs: Float?,
    from: Int,
    to: Int,
    slot: Float,
    reduceMotion: Boolean,
): ToonNavMotion =
    if (reduceMotion || ageMs == null ||
        ageMs >= SELECT_MS
    ) {
        ToonNavMotion()
    } else {
        toonNavSelect(ageMs, from, to, slot)
    }

@Stable
private class ToonNavClock(
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

    fun age(): Float? = nowMs.let { now -> at?.let { if (it == PENDING_START) 0f else (now - it).toFloat() } }

    fun running(): Boolean = age()?.let { it < SELECT_MS } ?: false
}

private const val PENDING_START = -1L

/** What one frame of the bar draws: the engine's `navBar` options. */
internal data class ToonNavLook(
    val sel: Int,
    val motion: ToonNavMotion = ToonNavMotion(),
    val pressed: Int? = null,
    val focus: Int? = null,
    val disabled: Int? = null,
    val badge: Pair<Int, ToonBadgeLook>? = null,
)

private val PAPER_RADII = ToonRadii(22f, 26f, 0f, 0f)
private val PAINT_RADII = ToonRadii(17f, 15f, 17f, 16f)
private val RING_RADII = ToonRadii(16f, 12f, 18f, 13f)
private val DUST_DIRS =
    listOf(
        floatArrayOf(-1f, -.12f, 1f),
        floatArrayOf(-.7f, -.45f, .75f),
        floatArrayOf(.7f, -.45f, .75f),
        floatArrayOf(1f, -.12f, 1f),
    )
private val LABEL = ToonTextStyle(600, 12f)
private val LABEL_ACTIVE = ToonTextStyle(800, 12f)

/** The engine's `navBar`: one frame [w] dp wide from its top left. */
@OptIn(InternalToonApi::class)
internal fun toonNavBarScene(
    tokens: ToonTokens,
    w: Float,
    items: List<ToonNavItem>,
    o: ToonNavLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val n = items.size
    val slot = w / n
    var scene =
        ToonScene(
            bodyLayers(
                shapePoints(-8f, 0f, w + 16, 96f, PAPER_RADII, sh.insets, sh.wobble, sh.seed + 800),
                c.surface,
                c,
                0f,
            ),
            emptyList(),
        )
    val m = o.motion
    val ia = m.ia ?: (slot * o.sel + slot / 2 - 32)
    val ib = m.ib ?: (ia + 64)
    scene +=
        ToonScene(
            listOf(
                smoothLayer(
                    wobblePoints(ia, 10f, max(8f, ib - ia), 34f, PAINT_RADII, 1.6f, sh.seed + 801, 4f),
                    c.chipSelected,
                ),
            ),
            emptyList(),
        )
    var over = ToonScene.Empty
    items.forEachIndexed { i, it ->
        val cx = slot * i + slot / 2
        val act = i == o.sel
        val pr = o.pressed == i
        val iop = if (o.disabled == i) .4f else 1f
        val iy = 15 + (if (act) m.iy else 0f) + (if (pr) 2 else 0)
        val layers = mutableListOf<ToonLayer>()
        if (o.focus == i) layers += ringLayer(cx - 34, 4f, 68f, 72f, RING_RADII, c, sh.wobble, sh.seed + 805 + i)
        if (pr &&
            !act
        ) {
            layers +=
                smoothLayer(
                    wobblePoints(cx - 28, 10f, 56f, 34f, PAINT_RADII, 1.2f, sh.seed + 806, 4f),
                    c.surfacePressed,
                )
        }
        if (act && m.du > 0 &&
            m.du < 1
        ) {
            layers += toonDustScene(c, m.du, cx, 39f, 20f, k = .55f, gap = 11f, dirs = DUST_DIRS).layers
        }
        scene += ToonScene(layers, emptyList())
        val icon =
            iconLayers(
                it.icon,
                cx - 12,
                iy,
                if (act) c.ink else c.text,
                weight = if (act) ToonIconWeight.Bold else ToonIconWeight.Regular,
                fill = if (act) c.primary else null,
                alpha = iop,
            )
        val sx = (if (act) m.isx else 1f) * (if (pr) 1.12f else 1f)
        val sy = (if (act) m.isy else 1f) * (if (pr) .84f else 1f)
        scene += ToonScene(icon, emptyList()).scaledAbout(cx, iy + 24, sx, sy)
        scene +=
            ToonScene(
                emptyList(),
                listOf(ToonLabel(it.label, cx, 66f, c.text, iop, if (act) LABEL_ACTIVE else LABEL, ToonAnchor.Middle)),
            )
        val badge = o.badge
        if (badge != null && badge.first == i) over += toonBadgeScene(tokens, badge.second).placed(cx + 12, 16f)
    }
    return scene + over
}
