package io.github.youndie.toon.interaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonLabel
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.wobblePoints
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// The desktop design's pointer rule (B-69; `Desktop Pointer & Keyboard`, rules 1 and 2, and the
// extension's `hoverRamp`, `hoverLift` and `plate`). Hover is a value from 0 to 1 that a component
// reads as its picture: a body with a hard shadow lifts toward the light by up to LIFT dp while its
// shadow stays on the table, and a flat element gets a backdrop in the pressed-surface colour.

/** The extension's `DESKTOP.motion`: hover comes in over 80 ms and goes out over 120. */
internal const val HOVER_IN_MS = 80f
internal const val HOVER_OUT_MS = 120f

/** The extension's `DESKTOP.motion.lift`: how far a hovered body rises, toward the top left. */
internal const val HOVER_LIFT = 2f

/**
 * The pointer page's rule 4 (`DESKTOP.pointer`): the least a pointer target may be, and what the
 * desktop components draw theirs at. The kit's own components keep their 48–52 dp, which the page
 * leaves to them, and `ToonAccessibilityTest` holds.
 */
internal object ToonPointerTarget {
    const val MIN = 28f
    const val ICON = 32f
    const val CONTROL = 40f
    const val ROW = 56f
    const val NAV_ROW = 44f
}

/**
 * The extension's controller `hv`: hover eased from [from] toward [on] by the kit's press curve
 * (`hoverRamp`, ease-out cubic), [ageMs] after the pointer came or went, in 80 ms in and 120 ms out.
 * Reduce motion makes it instant, as the controller's age is then long past.
 */
internal fun toonHoverAt(
    from: Float,
    on: Boolean,
    ageMs: Float,
    reduceMotion: Boolean,
): Float {
    val to = if (on) 1f else 0f
    if (reduceMotion) return to
    val u = max(0f, min(1f, ageMs / if (on) HOVER_IN_MS else HOVER_OUT_MS))
    return from + (to - from) * (1 - (1 - u).pow(3))
}

/**
 * A component's hover: [value] is 0 away from the pointer and 1 under it, eased in between. Only a
 * pointer that hovers sets it — a mouse, not a finger, whose press would leave it on — so touch keeps
 * the kit's look.
 */
@Stable
internal class ToonHoverState {
    var value: Float by mutableFloatStateOf(0f)
        internal set

    /** Where the pointer is now, and the value the current ramp started from. */
    internal var on = false
        private set
    private var from = 0f
    private var startMs: Long? = null

    /** Bumped by every enter and exit: the key of the frame loop, never a Boolean it writes itself. */
    internal var wakes by mutableIntStateOf(0)
        private set

    internal fun set(on: Boolean) {
        if (this.on == on) return
        from = value
        this.on = on
        startMs = PENDING
        wakes++
    }

    /** Steps the ramp to frame time [now]; false once it has arrived. */
    internal fun tick(
        now: Long,
        reduceMotion: Boolean,
    ): Boolean {
        val start = startMs.let { if (it == null || it == PENDING) now.also { startMs = it } else it }
        value = toonHoverAt(from, on, (now - start).toFloat(), reduceMotion)
        return value != if (on) 1f else 0f
    }

    private companion object {
        const val PENDING = -1L
    }
}

/** A [ToonHoverState] that steps on the frame clock only while its ramp runs. */
@Composable
internal fun rememberToonHoverState(): ToonHoverState {
    val state = remember { ToonHoverState() }
    val reduce = Toon.reduceMotion
    LaunchedEffect(state, state.wakes, reduce) {
        do {
            val moving = withFrameMillis { state.tick(it, reduce) }
        } while (moving)
    }
    return state
}

/**
 * Sets [state] while a mouse is over this element and it is [enabled]. A disabled element shows no
 * hover, and one turned disabled under the pointer lets it go.
 */
internal fun Modifier.toonHoverable(
    state: ToonHoverState,
    enabled: Boolean = true,
): Modifier =
    pointerInput(state, enabled) {
        if (!enabled) {
            state.set(false)
            return@pointerInput
        }
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val mouse = event.changes.any { it.type == PointerType.Mouse }
                when (event.type) {
                    PointerEventType.Enter, PointerEventType.Move -> if (mouse) state.set(true)
                    PointerEventType.Exit -> state.set(false)
                }
            }
        }
    }

/**
 * The extension's `hoverLift`: every layer and text moved [d] dp toward the top left, except the
 * body's hard shadows, which stay on the table, so a shadow 4 dp off reads 6.
 *
 * A shadow is an ink fill offset down and right by the same amount, as `body` draws it. The engine
 * tells it by the start of the layer's `transform`, which a `place` in front of it changes: on the
 * pointer page the chip is placed at 0, 0, and its ink outline is then taken for a shadow too. The
 * rule is the page's words, so the port reads the layer's own offset.
 */
internal fun ToonScene.hoverLifted(
    d: Float,
    ink: Color,
): ToonScene {
    if (d == 0f) return this
    return ToonScene(
        layers.map { if (it.isHardShadow(ink)) it else it.moved(-d, -d) },
        labels.map { ToonLabel(it.text, it.x - d, it.baseline - d, it.color, it.alpha, it.style, it.anchor) },
    )
}

private fun ToonLayer.isHardShadow(ink: Color): Boolean =
    color == ink && stroke == 0f && source == null && dx > 0f && dx == dy

/**
 * The extension's `plate`: the flat element's hover, a backdrop of the pressed-surface colour in
 * the shape of a press, at [alpha] (the hover).
 */
@OptIn(InternalToonApi::class)
internal fun toonPlateLayer(
    tokens: ToonTokens,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    alpha: Float,
    radii: ToonRadii = PLATE_RADII,
    seed: Int = 0,
): ToonLayer {
    val s = tokens.shape
    val rs = s.radiusScale
    val r = ToonRadii(radii.topLeft * rs, radii.topRight * rs, radii.bottomRight * rs, radii.bottomLeft * rs)
    return smoothLayer(
        wobblePoints(x, y, w, h, r, s.wobble, s.seed + PLATE_SEED + seed, 6f),
        tokens.colors.surfacePressed,
        alpha,
    )
}

/** The plate's radii and seed offset, the extension's literals. */
private val PLATE_RADII = ToonRadii(14f, 10f, 16f, 11f)
private const val PLATE_SEED = 3100

/** A scene with [layer] drawn first, under everything else. */
internal fun ToonScene.under(layer: ToonLayer?): ToonScene =
    if (layer ==
        null
    ) {
        this
    } else {
        ToonScene(listOf(layer) + layers, labels)
    }
