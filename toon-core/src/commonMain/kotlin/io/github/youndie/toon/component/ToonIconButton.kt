package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonInsets
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.ringLayer
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.fx.drawToonFlash
import io.github.youndie.toon.icon.ToonIconGlyph
import io.github.youndie.toon.icon.iconLayers
import io.github.youndie.toon.interaction.rememberToonPressState
import io.github.youndie.toon.interaction.toonPressable
import io.github.youndie.toon.motion.toonSquashOf
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonSize
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// A port of the engine's `iconBtn` and the kit's `C.iconbutton` (B-15): a hand-drawn circle round a
// 24 dp icon that squashes like a button; on the release the icon hops on its own spring
// (`iconBounce`), and turning a toggle on flashes behind it.

/** The `variant` prop of `ToonIconButton.dc.html`. */
public enum class ToonIconButtonVariant {
    Standard,
    Filled,
}

/**
 * The design's icon button: [icon] in a hand-drawn circle. [onClick] fires on the release. A [toggle]
 * flips [selected] on each click and reports it to [onSelectedChange], drawing the icon's cel fill
 * while selected; [selected] from outside overrides it, as the kit's `selected` prop does.
 * [contentDescription] names the action for accessibility.
 */
@Composable
public fun ToonIconButton(
    icon: ToonIconGlyph,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    variant: ToonIconButtonVariant = ToonIconButtonVariant.Standard,
    selected: Boolean = false,
    toggle: Boolean = true,
    enabled: Boolean = true,
    onSelectedChange: (Boolean) -> Unit = {},
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    var sel by remember(selected) { mutableStateOf(selected) }
    val press = rememberToonPressState()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val clock = remember { ToonIconButtonClock() }
    val click by rememberUpdatedState(onClick)
    val change by rememberUpdatedState(onSelectedChange)

    LaunchedEffect(clock.starts) {
        while (clock.running()) withFrameMillis { clock.tick(it) }
    }
    val moment = toonIconButtonFrameAt(clock.bounceAge(), clock.popAge(), reduce)

    Canvas(
        modifier
            .size(ICON_BUTTON.dp)
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
                if (toggle) toggleableState = ToggleableState(sel)
            }.toonPressable(press, enabled = enabled, role = if (toggle) Role.Checkbox else Role.Button) {
                clock.bounce()
                if (toggle) {
                    sel = !sel
                    if (sel) clock.pop()
                    change(sel)
                }
                click()
            }.focusable(enabled, interaction),
    ) {
        // The engine draws in dp (research D5); the canvas is pixels (B-47).

        inDp {
            if (sel && moment.flash != null) drawToonFlash(tokens.colors, moment.flash, CENTER, CENTER, FLASH_SIZE)
            drawScene(
                toonIconButtonScene(
                    tokens,
                    icon,
                    variant,
                    ToonIconButtonLook(
                        p = press.progress,
                        iy = moment.iy,
                        selected = sel,
                        disabled = !enabled,
                        focus = focused,
                    ),
                ),
            )
        }
    }
}

/** The kit's box: 48 dp, the target size; the circle is 44 inside it. */
private const val ICON_BUTTON = ToonSize.TARGET_MIN
private const val CENTER = 24f
private const val FLASH_SIZE = 30f

/** The icon's hop after the release, 400 ms; the flash behind a toggle turning on, 220 of its 260. */
private const val BOUNCE_MS = 400f
private const val POP_MS = 260f
private const val FLASH_MS = 220f

/** What the icon button draws at a moment: the icon's hop [iy] and the flash's life, or null. */
internal class ToonIconButtonMoment(
    val iy: Float,
    val flash: Float?,
)

/**
 * The kit's `C.iconbutton.scene` over time: the hop [bounceAgeMs] and the toggle's pop [popAgeMs]
 * after they started, or null. Under reduce motion — the MVP page's `instant` — both are already
 * over: the icon at rest, no flash.
 */
internal fun toonIconButtonFrameAt(
    bounceAgeMs: Float?,
    popAgeMs: Float?,
    reduceMotion: Boolean,
): ToonIconButtonMoment =
    if (reduceMotion) {
        ToonIconButtonMoment(0f, null)
    } else {
        ToonIconButtonMoment(
            iy = bounceAgeMs?.let { toonIconBounce(it) } ?: 0f,
            flash = popAgeMs?.takeIf { it < FLASH_MS }?.let { it / FLASH_MS },
        )
    }

/** The engine's `iconBounce`: the icon's y at [t] ms after the release, a 7 dp hop and a wobble. */
internal fun toonIconBounce(t: Float): Float {
    if (t < 0) return 0f
    if (t < 140) return -7 * sin(PI.toFloat() * t / 140)
    return 1.6f * exp(-(t - 140) / 60) * sin((t - 140) / 22)
}

@Stable
private class ToonIconButtonClock {
    var nowMs by mutableLongStateOf(0L)
    var starts by mutableStateOf(0)
    private var bounceAt: Long? = null
    private var popAt: Long? = null

    fun bounce() {
        bounceAt = PENDING_START
        starts++
    }

    fun pop() {
        popAt = PENDING_START
        starts++
    }

    fun tick(now: Long) {
        nowMs = now
        if (bounceAt == PENDING_START) bounceAt = now
        if (popAt == PENDING_START) popAt = now
    }

    fun running(): Boolean = (bounceAge() ?: BOUNCE_MS) < BOUNCE_MS || (popAge() ?: POP_MS) < POP_MS

    fun bounceAge(): Float? = bounceAt?.let { if (it == PENDING_START) 0f else (nowMs - it).toFloat() }

    fun popAge(): Float? = popAt?.let { if (it == PENDING_START) 0f else (nowMs - it).toFloat() }
}

private const val PENDING_START = -1L

/** What one frame of the icon button draws: the engine's `iconBtn` options. */
internal data class ToonIconButtonLook(
    val p: Float = 0f,
    val iy: Float = 0f,
    val selected: Boolean = false,
    val disabled: Boolean = false,
    val focus: Boolean = false,
    val seed: Int = 0,
)

private fun c01(v: Float): Float = max(0f, min(1f, v))

private val CIRCLE_RADII = ToonRadii(22f, 20f, 23f, 19f)
private val RING_RADII = ToonRadii(28f, 26f, 28f, 25f)

/** The engine's `iconBtn`: one frame, the 44 dp circle at (2, 2) of the 48 dp box. */
@OptIn(InternalToonApi::class)
internal fun toonIconButtonScene(
    tokens: ToonTokens,
    icon: ToonIconGlyph,
    variant: ToonIconButtonVariant,
    o: ToonIconButtonLook,
): ToonScene {
    val c = tokens.colors
    val s = tokens.shape
    val p = o.p
    val sq = toonSquashOf(p)
    val d = 44f
    val bw = d * sq.sx
    val bh = d * sq.sy
    val x = 2 + (d - bw) / 2
    val y = 2 + d - bh
    val dis = o.disabled
    val filled = variant == ToonIconButtonVariant.Filled
    val layers =
        buildList {
            if (o.focus) add(ringLayer(-4f, -4f, 56f, 56f, RING_RADII, c, s.wobble, s.seed + 401))
            val ins = s.insets.let { ToonInsets(it.top * .85f, it.right * .85f, it.bottom * .85f, it.left * .85f) }
            addAll(
                bodyLayers(
                    shapePoints(x, y, bw, bh, CIRCLE_RADII, ins, s.wobble, s.seed + 400 + o.seed, 4f),
                    fill =
                        if (dis) {
                            c.disabled
                        } else if (filled) {
                            c.primary
                        } else {
                            c.surface
                        },
                    colors = c,
                    shadow = if (dis) 0f else 3 * (1 - c01(p)),
                    inkAlpha = if (dis) .35f else 1f,
                    rim =
                        if (c.rim == null || dis) {
                            null
                        } else if (filled) {
                            c.primaryHighlight
                        } else {
                            c.rim
                        },
                ),
            )
            val iy = 12 + o.iy + (d - bh) / 2 + 1.5f * c01(p)
            addAll(
                iconLayers(
                    icon,
                    12f,
                    iy,
                    color = if (filled) c.onPrimary else c.text,
                    fill = if (o.selected) (if (filled) c.surface else c.primary) else null,
                    alpha = if (dis) .4f else 1f,
                ),
            )
        }
    return ToonScene(layers, emptyList())
}
