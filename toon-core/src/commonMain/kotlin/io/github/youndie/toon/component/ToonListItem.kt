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
import io.github.youndie.toon.draw.ToonTextRenderer
import io.github.youndie.toon.draw.ToonTextStyle
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.inkLinePoints
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.ringLayer
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.wobblePoints
import io.github.youndie.toon.icon.ToonIconGlyph
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.icon.iconLayers
import io.github.youndie.toon.interaction.rememberToonHoverState
import io.github.youndie.toon.interaction.toonHoverable
import io.github.youndie.toon.motion.toonSpring
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// A port of the engine's `listItem` and `listPress` and the kit's `C.listitem` (B-22): a 72 dp row with
// a sticker icon, a title and a line under it, and a trailing chevron, switch or check. Pressed, the
// row sinks 2 dp and its bottom rule bows 6 dp like a plank; on release it springs back past flat once.
// A trailing check plays the checkbox's own toggle, which the design's row leaves out (B-65).

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
 *
 * The title wraps onto up to [titleMaxLines] lines and the sub onto [subMaxLines], each cut with an
 * ellipsis short of the trailing control; the row grows from the design's 72 dp by the lines added
 * (B-84).
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
    titleMaxLines: Int = 2,
    subMaxLines: Int = 1,
    onCheckedChange: (Boolean) -> Unit = {},
    onClick: () -> Unit = {},
) {
    val tokens = Toon.tokens
    val reduce = Toon.reduceMotion
    val text = rememberToonText()
    var checked by remember(on) { mutableStateOf(on) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hover = rememberToonHoverState()
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
    // B-84: the words laid out in the room left of the trailing control; each line past the first grows the row.
    val room = toonListTextRoom(w, trailing)
    val titleExtra = remember(title, room, titleMaxLines, text) { text.extraLines(title, TITLE, room, titleMaxLines) }
    val subExtra = remember(sub, room, subMaxLines, text) { text.extraLines(sub, SUB, room, subMaxLines) }
    Canvas(
        modifier
            .size(width, (ROW_H + titleExtra + subExtra).dp)
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
            }.toonHoverable(hover, enabled)
            .focusable(enabled, interaction),
    ) {
        // The engine draws in dp (research D5); the canvas is pixels (B-47).
        inDp {
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
                    look.copy(
                        seed = index,
                        last = last,
                        disabled = !enabled,
                        focus = focused,
                        hv = hover.value,
                        titleMaxLines = titleMaxLines,
                        subMaxLines = subMaxLines,
                        titleExtra = titleExtra,
                        subExtra = subExtra,
                    ),
                ),
                text,
            )
        }
    }
}

private const val ROW_H = 72f

/** The words end this far short of the trailing control (B-84). */
private const val TEXT_GAP = 8f

/** Where the words start: right of the 40 dp sticker. */
private const val TEXT_X = 56f

/** The width the title and sub have on a row [w] wide with [trailing]: from the sticker to the control (B-84). */
internal fun toonListTextRoom(
    w: Float,
    trailing: ToonListTrailing,
): Float =
    when (trailing) {
        ToonListTrailing.Chevron -> w - 28
        ToonListTrailing.Switch -> w - 60
        ToonListTrailing.Check -> w - 48
        ToonListTrailing.None -> w + TEXT_GAP
    } - TEXT_GAP - TEXT_X

/** How much taller than one line [text] stands laid out in [room] on up to [maxLines] lines: the lines added. */
private fun ToonTextRenderer.extraLines(
    text: String,
    style: ToonTextStyle,
    room: Float,
    maxLines: Int,
): Float {
    if (text.isEmpty() || maxLines <= 1) return 0f
    val layout = measure(text, style, room, maxLines)
    return layout.getLineBaseline(layout.lineCount - 1) - layout.getLineBaseline(0)
}

/**
 * The kit runs a press for 200 ms, a release for 500 and the switch's toggle for 520, its curve 480; the
 * check's toggle is the checkbox's, 300.
 */
private const val RELEASE_MS = 500f
private const val TOGGLE_MS = 520f
private const val SWITCH_ANIM_MS = 480f
private const val CHECK_ANIM_MS = 300f

/** The engine's `listPress` at [t] ms: the row's sag, 0 to 1 on the way [down], a bouncy spring back after. */
internal fun toonListPress(
    t: Float,
    down: Boolean,
): Float = if (down) 1 - (1 - c01(t / 100)).pow(3) else toonSpring(t / 1000, .3f, 40f)

/**
 * The kit's `C.listitem.scene` at a moment: pressed [pressAgeMs] ago, released [releaseAgeMs] ago,
 * the switch or the check toggled [toggleAgeMs] ago (null for any that has not happened). Under
 * reduce motion — the MVP page's `instant` — the row is fully down while pressed and flat once let go.
 * The check plays the checkbox's `chkAnim`, reduce motion as the checkbox's (B-65): the design's row
 * draws it settled at once, the one control in the kit that does not animate.
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
    val check =
        if (trailing == ToonListTrailing.Check && toggleAgeMs != null && toggleAgeMs < CHECK_ANIM_MS) {
            toonCheckboxFrameAt(on, toggleAgeMs, 0f, reduceMotion)
        } else {
            null
        }
    return ToonListItemLook(on = on, sag = sag, switchLook = sw, checkLook = check)
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

    private fun age(at: Long?): Float? =
        nowMs.let { now ->
            at?.let {
                if (it ==
                    PENDING_START
                ) {
                    0f
                } else {
                    (now - it).toFloat()
                }
            }
        }

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
    val checkLook: ToonCheckboxLook? = null,
    val seed: Int = 0,
    val last: Boolean = false,
    val disabled: Boolean = false,
    val focus: Boolean = false,
    /**
     * The pointer's hover, 0 to 1 (B-69): the row is flat, and the pointer page draws its hover as the
     * selected row's plate, which a press also draws.
     */
    val hv: Float = 0f,
    /** The lines the title and sub may take, and how much their lines past the first add to the row (B-84). */
    val titleMaxLines: Int = 1,
    val subMaxLines: Int = 1,
    val titleExtra: Float = 0f,
    val subExtra: Float = 0f,
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
    val h = ROW_H + o.titleExtra + o.subExtra
    val sag = o.sag
    // The sticker and the trailing control stay in the middle of a grown row (B-84).
    val dy = 2 * max(0f, sag)
    val my = dy + (h - ROW_H) / 2
    val dis = o.disabled
    val op = if (dis) .5f else 1f
    val sd = o.seed
    val layers = mutableListOf<ToonLayer>()
    if (o.focus) layers += ringLayer(-10f, 1f, w + 20, h - 2, FOCUS_RADII, c, sh.wobble, sh.seed + 905 + sd)
    val hv = if (dis) 0f else o.hv
    if (o.selected || sag > .01f || hv > 0f) {
        layers +=
            smoothLayer(
                wobblePoints(-8f, 3f, w + 16, h - 6, PRESS_RADII, sh.wobble, sh.seed + 900 + sd),
                c.surfacePressed,
                if (o.selected) {
                    1f
                } else {
                    max(hv, c01(sag * 1.4f))
                },
            )
    }
    val ins = sh.insets.let { ToonInsets(it.top * .75f, it.right * .75f, it.bottom * .75f, it.left * .75f) }
    layers +=
        bodyLayers(
            shapePoints(0f, 16 + my, 40f, 40f, STICKER_RADII, ins, sh.wobble, sh.seed + 910 + sd, 3f),
            fill = c.stickers[sd.mod(3)],
            colors = c,
            shadow = 0f,
            inkAlpha = if (dis) .4f else 1f,
            rim = null,
        )
    layers += iconLayers(icon, 8f, 24 + my, c.ink, alpha = op)
    val labels =
        mutableListOf(
            ToonLabel(
                title,
                TEXT_X,
                (if (sub.isNotEmpty()) 33f else 42f) + dy,
                c.text,
                op,
                TITLE,
                ToonAnchor.Start,
                toonListTextRoom(w, trailing),
                o.titleMaxLines,
            ),
        )
    if (sub.isNotEmpty()) {
        labels +=
            ToonLabel(
                sub,
                TEXT_X,
                54 + dy + o.titleExtra,
                c.textMuted,
                op,
                SUB,
                ToonAnchor.Start,
                toonListTextRoom(w, trailing),
                o.subMaxLines,
            )
    }
    when (trailing) {
        ToonListTrailing.Chevron -> {
            layers += iconLayers(ToonIcons.Chevron, w - 28, 24 + my, c.text, alpha = op)
        }

        ToonListTrailing.Switch -> {
            val sw = (o.switchLook ?: ToonSwitchLook(on = o.on)).copy(on = o.on, disabled = dis)
            layers += toonSwitchScene(tokens, sw).placed(w - 60, 12 + my).layers
        }

        ToonListTrailing.Check -> {
            val look = (o.checkLook ?: ToonCheckboxLook(on = o.on)).copy(disabled = dis)
            val fade = look.fade
            if (fade != null && fade < 1f) {
                // Reduce motion's `fade 120`, as the checkbox: the unchecked box fades into the checked one.
                layers +=
                    toonCheckboxScene(tokens, "", ToonCheckboxLook(on = false, disabled = dis))
                        .placed(w - 48, 12 + my, 1 - fade)
                        .layers
            }
            layers += toonCheckboxScene(tokens, "", look).placed(w - 48, 12 + my, fade ?: 1f).layers
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
