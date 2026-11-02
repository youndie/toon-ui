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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
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
import io.github.youndie.toon.icon.ToonIconGlyph
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.icon.iconLayers
import io.github.youndie.toon.motion.toonSpring
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// A port of the engine's `listItem` and `listPress` and the kit's `C.listitem` (B-22): a 72 dp row with
// a sticker icon, a title and a line under it, and a trailing chevron, switch or check. Pressed, the
// row sinks 2 dp and its bottom rule bows 6 dp like a plank; on release it springs back past flat once.

/** The `trailing` prop of `ToonListItem.dc.html`. */
public enum class ToonListTrailing {
    Chevron,
    Switch,
    Check,
    None,
}

/**
 * The design's list item: [icon] on a sticker coloured by [index], [title] and [sub], and the
 * [trailing] control. A click calls [onClick]; with a switch or a check it also toggles [on] and
 * reports it to [onCheckedChange]. [last] leaves out the rule under the row.
 */
@Composable
public fun ToonListItem(
    title: String,
    modifier: Modifier = Modifier,
    icon: ToonIconGlyph = ToonIcons.Calendar,
    sub: String = "",
    trailing: ToonListTrailing = ToonListTrailing.Chevron,
    on: Boolean = false,
    index: Int = 0,
    last: Boolean = false,
    width: Dp = 342.dp,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit = {},
    onClick: () -> Unit = {},
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val text = rememberToonText()
    var checked by remember(on) { mutableStateOf(on) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val clock = remember { ToonListClock() }
    val click by rememberUpdatedState(onClick)
    val change by rememberUpdatedState(onCheckedChange)
    val toggles = trailing == ToonListTrailing.Switch || trailing == ToonListTrailing.Check
    LaunchedEffect(clock.starts) { while (clock.running()) withFrameMillis { clock.tick(it) } }

    fun activate() {
        if (toggles) {
            checked = !checked
            clock.toggled()
            change(checked)
        }
        click()
    }

    val w = width.value
    Canvas(
        modifier
            .size(width, ROW_H.dp)
            .semantics {
                this.text = AnnotatedString(if (sub.isEmpty()) title else "$title, $sub")
                role = if (toggles) Role.Switch else Role.Button
                if (toggles) toggleableState = ToggleableState(checked)
                if (!enabled) disabled()
                onClick {
                    if (enabled) activate()
                    enabled
                }
            }.pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown()
                    clock.pressed()
                    val up = waitForUpOrCancellation()
                    clock.released()
                    if (up != null) activate()
                }
            }.focusable(enabled, interaction),
    ) {
        val look =
            toonListItemFrameAt(clock.pressAge(), clock.releaseAge(), clock.toggleAge(), checked, trailing, reduce)
        drawScene(
            toonListItemScene(
                tokens,
                w,
                icon,
                title,
                sub,
                trailing,
                look.copy(seed = index, last = last, disabled = !enabled, focus = focused),
            ),
            text,
        )
    }
}

private const val ROW_H = 72f

/** The kit runs a press for 200 ms, a release for 500 and the switch's toggle for 520, its curve 480. */
private const val RELEASE_MS = 500f
private const val TOGGLE_MS = 520f
private const val SWITCH_ANIM_MS = 480f

/** The engine's `listPress` at [t] ms: the row's sag, 0 to 1 on the way [down], a bouncy spring back after. */
internal fun toonListPress(
    t: Float,
    down: Boolean,
): Float = if (down) 1 - (1 - c01(t / 100)).pow(3) else toonSpring(t / 1000, .3f, 40f)

/**
 * The kit's `C.listitem.scene` at a moment: pressed [pressAgeMs] ago, released [releaseAgeMs] ago,
 * the switch toggled [toggleAgeMs] ago (null for any that has not happened). Under reduce motion —
 * the MVP page's `instant` — the row is fully down while pressed and flat once let go.
 */
internal fun toonListItemFrameAt(
    pressAgeMs: Float?,
    releaseAgeMs: Float?,
    toggleAgeMs: Float?,
    on: Boolean,
    trailing: ToonListTrailing,
    reduceMotion: Boolean,
): ToonListItemLook {
    val sag =
        when {
            pressAgeMs != null -> toonListPress(if (reduceMotion) SETTLED_MS else pressAgeMs, true)
            releaseAgeMs != null && !reduceMotion && releaseAgeMs < RELEASE_MS -> toonListPress(releaseAgeMs, false)
            else -> 0f
        }
    val sw =
        if (trailing == ToonListTrailing.Switch && !reduceMotion && toggleAgeMs != null &&
            toggleAgeMs < SWITCH_ANIM_MS
        ) {
            toonSwAnim(toggleAgeMs, if (on) 0 else 1, if (on) 1 else 0)
        } else {
            null
        }
    return ToonListItemLook(on = on, sag = sag, switchLook = sw)
}

private const val SETTLED_MS = 1e6f

private fun c01(v: Float): Float = max(0f, min(1f, v))

@Stable
private class ToonListClock {
    var nowMs by mutableLongStateOf(0L)
    var starts by mutableStateOf(0)
    private var pressAt: Long? by mutableStateOf(null)
    private var releaseAt: Long? = null
    private var toggleAt: Long? = null

    fun pressed() {
        pressAt = PENDING_START
        releaseAt = null
        starts++
    }

    fun released() {
        pressAt = null
        releaseAt = PENDING_START
        starts++
    }

    fun toggled() {
        toggleAt = PENDING_START
        starts++
    }

    fun tick(now: Long) {
        nowMs = now
        if (pressAt == PENDING_START) pressAt = now
        if (releaseAt == PENDING_START) releaseAt = now
        if (toggleAt == PENDING_START) toggleAt = now
    }

    private fun age(at: Long?): Float? = at?.let { if (it == PENDING_START) 0f else (nowMs - it).toFloat() }

    fun pressAge(): Float? = age(pressAt)

    fun releaseAge(): Float? = age(releaseAt)

    fun toggleAge(): Float? = age(toggleAt)

    fun running(): Boolean =
        pressAt != null || (releaseAge()?.let { it < RELEASE_MS } ?: false) ||
            (toggleAge()?.let { it < TOGGLE_MS } ?: false)
}

private const val PENDING_START = -1L

/** What one frame of the row draws: the engine's `listItem` options. */
internal data class ToonListItemLook(
    val on: Boolean = false,
    val sag: Float = 0f,
    val selected: Boolean = false,
    val switchLook: ToonSwitchLook? = null,
    val seed: Int = 0,
    val last: Boolean = false,
    val disabled: Boolean = false,
    val focus: Boolean = false,
)

private val FOCUS_RADII = ToonRadii(16f, 12f, 18f, 13f)
private val PRESS_RADII = ToonRadii(14f, 10f, 16f, 11f)
private val STICKER_RADII = ToonRadii(20f, 18f, 20f, 17f)
private val TITLE = ToonTextStyle(700, 16f)
private val SUB = ToonTextStyle(500, 14f)

/** The engine's `listItem`: one 72 dp row [w] wide, from its top left. */
@OptIn(InternalToonApi::class)
internal fun toonListItemScene(
    tokens: ToonTokens,
    w: Float,
    icon: ToonIconGlyph,
    title: String,
    sub: String,
    trailing: ToonListTrailing,
    o: ToonListItemLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val h = ROW_H
    val sag = o.sag
    val dy = 2 * max(0f, sag)
    val dis = o.disabled
    val op = if (dis) .5f else 1f
    val sd = o.seed
    val layers = mutableListOf<ToonLayer>()
    if (o.focus) layers += ringLayer(-10f, 1f, w + 20, h - 2, FOCUS_RADII, c, sh.wobble, sh.seed + 905 + sd)
    if (o.selected || sag > .01f) {
        layers +=
            smoothLayer(
                wobblePoints(-8f, 3f, w + 16, h - 6, PRESS_RADII, sh.wobble, sh.seed + 900 + sd),
                c.surfacePressed,
                if (o.selected) {
                    1f
                } else {
                    c01(sag * 1.4f)
                },
            )
    }
    val ins = sh.insets.let { ToonInsets(it.top * .75f, it.right * .75f, it.bottom * .75f, it.left * .75f) }
    layers +=
        bodyLayers(
            shapePoints(0f, 16 + dy, 40f, 40f, STICKER_RADII, ins, sh.wobble, sh.seed + 910 + sd, 3f),
            fill = c.stickers[sd.mod(3)],
            colors = c,
            shadow = 0f,
            inkAlpha = if (dis) .4f else 1f,
            rim = null,
        )
    layers += iconLayers(icon, 8f, 24 + dy, c.ink, alpha = op)
    val labels =
        mutableListOf(
            ToonLabel(title, 56f, (if (sub.isNotEmpty()) 33f else 42f) + dy, c.text, op, TITLE, ToonAnchor.Start),
        )
    if (sub.isNotEmpty()) labels += ToonLabel(sub, 56f, 54 + dy, c.textMuted, op, SUB, ToonAnchor.Start)
    when (trailing) {
        ToonListTrailing.Chevron -> {
            layers += iconLayers(ToonIcons.Chevron, w - 28, 24 + dy, c.text, alpha = op)
        }

        ToonListTrailing.Switch -> {
            val sw = (o.switchLook ?: ToonSwitchLook(on = o.on)).copy(on = o.on, disabled = dis)
            layers += toonSwitchScene(tokens, sw).placed(w - 60, 12 + dy).layers
        }

        ToonListTrailing.Check -> {
            layers +=
                toonCheckboxScene(
                    tokens,
                    "",
                    ToonCheckboxLook(on = o.on, disabled = dis),
                ).placed(w - 48, 12 + dy).layers
        }

        // Nothing trails the text.
        ToonListTrailing.None -> {}
    }
    if (!o.last) {
        layers +=
            ToonLayer(
                inkLinePoints(0f, w, h, 2.6f, sh.seed + 920 + sd, 6 * sag),
                ToonSegments.Line,
                true,
                c.ink,
                if (dis) .4f else 1f,
            )
    }
    return ToonScene(layers, labels)
}
