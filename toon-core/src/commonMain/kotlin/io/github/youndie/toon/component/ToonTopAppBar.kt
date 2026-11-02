package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonAnchor
import io.github.youndie.toon.draw.ToonInsets
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
import io.github.youndie.toon.icon.ToonIconGlyph
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.icon.iconLayers
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

// A port of the engine's `topBar` and `sway` and the kit's `C.topbar` (B-26): a sign hung on two
// nails that a tap, a scroll fling or a change of `scrolled` kicks, so it swings about a point 40 dp
// above it and settles; its shadow grows once content is underneath.

/** The `variant` prop of `ToonTopAppBar.dc.html`: the title after the back arrow, or centred. */
public enum class ToonTopAppBarVariant {
    Small,
    Center,
}

/**
 * Kicks a [ToonTopAppBar]'s sway from outside: [kick] directly, or [nestedScrollConnection] on the
 * scrolling content, which kicks on every fling.
 */
@Stable
public class ToonTopAppBarState {
    internal var kicks by mutableStateOf(0)
        private set

    /** Swings the sign, the other way from the last kick, as the kit's `kick` does. */
    public fun kick() {
        kicks++
    }

    /** Kicks the sign when the content under it is flung. */
    public val nestedScrollConnection: NestedScrollConnection =
        object : NestedScrollConnection {
            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity {
                if (consumed != Velocity.Zero) kick()
                return Velocity.Zero
            }
        }
}

/**
 * The design's top app bar: [title] on a sign [width] wide, a back arrow when [nav] that calls
 * [onNav], and [actions] from the icon set at its end, each calling [onAction] with its glyph. Its
 * shadow grows while [scrolled]. A tap anywhere on it, a change of [scrolled] and [state]'s kicks
 * swing it.
 */
@Composable
public fun ToonTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    variant: ToonTopAppBarVariant = ToonTopAppBarVariant.Small,
    actions: List<ToonIconGlyph> = listOf(ToonIcons.Search, ToonIcons.More),
    nav: Boolean = true,
    scrolled: Boolean = false,
    width: Dp = 390.dp,
    state: ToonTopAppBarState = remember { ToonTopAppBarState() },
    onNav: () -> Unit = {},
    onAction: (ToonIconGlyph) -> Unit = {},
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val texts = rememberToonText()
    val sway = remember { ToonSwayClock() }
    var pressed by remember { mutableStateOf<ToonTopBarKey?>(null) }
    val goBack by rememberUpdatedState(onNav)
    val act by rememberUpdatedState(onAction)
    val w = width.value
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(scrolled) {
        if (first) first = false else sway.kick()
    }
    LaunchedEffect(state.kicks) { if (state.kicks > 0) sway.kick() }
    LaunchedEffect(sway.starts) { while (sway.running()) withFrameMillis { sway.tick(it) } }

    Canvas(
        modifier
            .size(width, BAR_BOX.dp)
            .semantics {
                text = AnnotatedString(title)
                heading()
                customActions =
                    buildList {
                        if (nav) add(CustomAccessibilityAction("Back") { goBack().let { true } })
                        actions.forEach { add(CustomAccessibilityAction(it.name) { act(it).let { true } }) }
                    }
            }.pointerInput(actions, nav, w) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    pressed = toonTopBarTarget(w, actions.size, nav, down.position.x / density)
                    val up = waitForUpOrCancellation()
                    pressed = null
                    if (up == null) return@awaitEachGesture
                    sway.kick()
                    when (val key = toonTopBarTarget(w, actions.size, nav, up.position.x / density)) {
                        ToonTopBarKey.Nav -> goBack()
                        is ToonTopBarKey.Action -> act(actions[key.index])
                        null -> Unit
                    }
                }
            },
    ) {
        // The engine draws in dp (research D5); the canvas is pixels (B-47).

        inDp {
            val look = ToonTopBarLook(scrolled = scrolled, pressed = pressed)
            val rot = sway.angle(reduce)
            drawScene(
                toonTopBarScene(tokens, w, title, variant, actions, nav, look).placed(0f, 0f, 1f, rot, w / 2, PIVOT_Y),
                texts,
            )
        }
    }
}

/** The kit's box: 76 dp, the sign 56 tall and 10 down. */
private const val BAR_BOX = 76f

/** The sway turns the sign about a point 40 dp above it. */
private const val PIVOT_Y = -40f

/** A kick starts the pendulum at 24°/s, the other way from the last; the kit plays it for 900 ms. */
private const val KICK_V0 = 24f
private const val SWAY_MS = 900f

/** The engine's `sway`: the angle in degrees [t] ms after a kick of [v0], a pendulum of ζ 0.25 and ω 14. */
internal fun toonSway(
    t: Float,
    v0: Float,
): Float {
    val w = 14f
    val z = .25f
    val wd = w * sqrt(1 - z * z)
    return v0 / wd * exp(-z * w * t / 1000) * sin(wd * t / 1000)
}

@Stable
internal class ToonSwayClock {
    var nowMs by mutableLongStateOf(0L)
    var starts by mutableStateOf(0)
    private var v0 = 0f
    private var at: Long? = null

    fun kick() {
        v0 = (if (v0 > 0) -1 else 1) * KICK_V0
        at = PENDING_START
        starts++
    }

    fun tick(now: Long) {
        nowMs = now
        if (at == PENDING_START) at = now
    }

    private fun age(): Float? = at?.let { if (it == PENDING_START) 0f else (nowMs - it).toFloat() }

    fun running(): Boolean = age()?.let { it < SWAY_MS } ?: false

    /**
     * The sign's angle now; at rest once the kit's 900 ms are over, and always under reduce motion — the
     * MVP page's `off`.
     */
    fun angle(reduceMotion: Boolean): Float =
        if (reduceMotion) 0f else age()?.takeIf { it < SWAY_MS }?.let { toonSway(it, v0) } ?: 0f
}

private const val PENDING_START = -1L

/** What the bar's pointer is on: the back arrow, or an action by its index from the end. */
internal sealed class ToonTopBarKey {
    data object Nav : ToonTopBarKey()

    data class Action(
        val index: Int,
    ) : ToonTopBarKey()
}

/**
 * The kit's `target`: an action's 40 dp around its icon, else the first 76 dp for the back arrow. The
 * actions stand 40 dp apart, under the brief's 48 dp target (B-30): the outermost reaches the sign's
 * edge and the innermost 8 dp further in, so with one or two actions each target is 48 dp wide; a
 * middle one of three or more stays 40 (B-49).
 */
internal fun toonTopBarTarget(
    w: Float,
    actions: Int,
    nav: Boolean,
    x: Float,
): ToonTopBarKey? {
    for (i in 0 until actions) {
        val ax = w - 12 - 16 - 24 - i * 40
        val right = if (i == 0) w - 12 else ax + 32
        val left = if (i == actions - 1) ax - 16 else ax - 8
        if (x >= left && x <= right) return ToonTopBarKey.Action(i)
    }
    return if (x < 76 && nav) ToonTopBarKey.Nav else null
}

/** What one frame of the bar draws: the engine's `topBar` options. */
internal data class ToonTopBarLook(
    val scrolled: Boolean = false,
    val pressed: ToonTopBarKey? = null,
    val focus: ToonTopBarKey? = null,
)

private val SIGN_RADII = ToonRadii(10f, 14f, 9f, 13f)
private val NAIL_RADII = ToonRadii(3.5f, 3.5f, 3.5f, 3.5f)
private val NAIL_INSETS = ToonInsets(1.2f, 1.2f, 1.6f, 1.2f)
private val PRESS_RADII = ToonRadii(20f, 18f, 20f, 17f)
private val RING_RADII = ToonRadii(22f, 20f, 22f, 19f)
internal val TOP_BAR_TITLE = ToonTextStyle(400, 22f, display = true)

/** The engine's `topBar`: one frame [w] dp wide from its top left, the sign 56 dp tall and 10 down. */
@OptIn(InternalToonApi::class)
internal fun toonTopBarScene(
    tokens: ToonTokens,
    w: Float,
    title: String,
    variant: ToonTopAppBarVariant,
    actions: List<ToonIconGlyph>,
    nav: Boolean,
    o: ToonTopBarLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val sx = 12f
    val sy = 10f
    val sw = w - 24
    val h = 56f
    val cy = sy + h / 2
    val fs = TOP_BAR_TITLE.size
    val rs = sh.radiusScale
    val r =
        ToonRadii(
            SIGN_RADII.topLeft * rs,
            SIGN_RADII.topRight * rs,
            SIGN_RADII.bottomRight * rs,
            SIGN_RADII.bottomLeft * rs,
        )
    // The shadow grows by 3 dp once content is underneath.
    val shadow = if (o.scrolled) sh.shadow + 3 else sh.shadow
    val layers =
        bodyLayers(
            shapePoints(sx, sy, sw, h, r, sh.insets, sh.wobble, sh.seed + 750),
            c.surface,
            c,
            shadow,
        ).toMutableList()
    listOf(sx + 8, sx + sw - 8).forEachIndexed { j, nx ->
        val g = shapePoints(nx - 3.5f, sy + 8 - 3.5f, 7f, 7f, NAIL_RADII, NAIL_INSETS, .3f, sh.seed + 760 + j, 2f)
        layers += smoothLayer(g.ink, c.ink)
        layers += smoothLayer(g.fill, c.nail)
    }

    fun hit(
        key: ToonTopBarKey,
        x: Float,
    ): Float {
        val down = o.pressed == key
        if (down) {
            layers +=
                smoothLayer(
                    wobblePoints(x - 8, cy - 20, 40f, 40f, PRESS_RADII, .8f, sh.seed + 770, 3f),
                    c.surfacePressed,
                )
        }
        if (o.focus == key) layers += ringLayer(x - 10, cy - 22, 44f, 44f, RING_RADII, c, sh.wobble, sh.seed + 771)
        return if (down) 1.5f else 0f
    }
    if (nav) {
        val dy = hit(ToonTopBarKey.Nav, sx + 16)
        layers += iconLayers(ToonIcons.Back, sx + 16, cy - 12 + dy, c.text)
    }
    actions.forEachIndexed { i, glyph ->
        val x = sx + sw - 16 - 24 - i * 40
        val dy = hit(ToonTopBarKey.Action(i), x)
        layers += iconLayers(glyph, x, cy - 12 + dy, c.text)
    }
    val label =
        if (variant == ToonTopAppBarVariant.Center) {
            ToonLabel(title, w / 2, cy + fs * .36f, c.text, 1f, TOP_BAR_TITLE, ToonAnchor.Middle)
        } else {
            ToonLabel(title, sx + 56, cy + fs * .36f, c.text, 1f, TOP_BAR_TITLE, ToonAnchor.Start)
        }
    return ToonScene(layers.toList<ToonLayer>(), listOf(label))
}
