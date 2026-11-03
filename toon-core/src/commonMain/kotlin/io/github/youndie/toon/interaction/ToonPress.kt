package io.github.youndie.toon.interaction

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonMotion
import io.github.youndie.toon.tokens.ToonSize
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

/**
 * The kit's press machine (`toon-kit.js`, `Base.press`, `release`, `stepSq`), the one place motion is
 * not a closed-form curve (research D7): where the spring starts depends on when the finger lifted.
 *
 * Pressed, the squash goes from wherever it is to 1 in 80 ms, ease-out cubic. Released, it springs
 * from that position and velocity with the bouncy spring (ζ 0.35, ω 45), integrated in four
 * sub-steps per frame as the kit does, and rests once both are small. [progress] is that squash:
 * 0 at rest, 1 fully pressed, negative on the overshoot (`squashOf` turns it into scale).
 */
internal class ToonPressMachine {
    enum class Mode { Rest, Press, Spring }

    var mode: Mode = Mode.Rest
        private set
    var progress: Double = 0.0
        private set
    private var velocity = 0.0
    private var pressedFor = 0.0
    private var from = 0.0

    fun press() {
        mode = Mode.Press
        pressedFor = 0.0
        from = progress
    }

    /** Ends a press; false when there was none, which is when a click must not fire either. */
    fun release(): Boolean {
        if (mode != Mode.Press) return false
        mode = Mode.Spring
        return true
    }

    fun step(
        ms: Double,
        reduceMotion: Boolean,
    ) {
        if (mode == Mode.Rest) return
        if (reduceMotion) {
            progress = if (mode == Mode.Press) 1.0 else 0.0
            if (mode != Mode.Press) mode = Mode.Rest
            return
        }
        if (mode == Mode.Press) {
            pressedFor += ms
            val u = min(1.0, pressedFor / ToonMotion.SNAP)
            progress = from + (1 - from) * (1 - (1 - u).pow(3))
            velocity = 0.0
            return
        }
        val w = ToonMotion.Bouncy.omega.toDouble()
        val z = ToonMotion.Bouncy.zeta.toDouble()
        val h = ms / 1000 / SUB_STEPS
        repeat(SUB_STEPS) {
            val a = -w * w * progress - 2 * z * w * velocity
            velocity += a * h
            progress += velocity * h
        }
        if (abs(progress) < REST_POSITION && abs(velocity) < REST_VELOCITY) {
            progress = 0.0
            velocity = 0.0
            mode = Mode.Rest
        }
    }

    private companion object {
        const val SUB_STEPS = 4
        const val REST_POSITION = 0.002
        const val REST_VELOCITY = 0.02
    }
}

/** A component's press: its squash [progress], driven by the frame clock only while it moves. */
@Stable
public class ToonPressState internal constructor() {
    internal val machine = ToonPressMachine()

    /** 0 at rest, 1 pressed, negative on the release overshoot. */
    public var progress: Float by mutableFloatStateOf(0f)
        internal set

    /**
     * Bumped by every press and release, and the key of the frame loop (B-53). A Boolean "running"
     * was the key before: a release landing between the loop's end and the next recomposition wrote
     * it back to true unseen, the loop never relaunched, and the press stuck for good.
     */
    internal var wakes by mutableIntStateOf(0)

    /** The last frame the loop stepped on while the machine moved, so a relaunch continues its time. */
    internal var lastFrameMs: Long? = null

    internal fun press() {
        machine.press()
        wakes++
    }

    internal fun release(): Boolean = machine.release().also { wakes++ }
}

/** A [ToonPressState] that steps on the frame clock while it moves, and sleeps at rest or fully pressed. */
@Composable
public fun rememberToonPressState(): ToonPressState {
    val state = remember { ToonPressState() }
    val reduce = Toon.reduceMotion
    LaunchedEffect(state, state.wakes, reduce) {
        if (state.machine.mode == ToonPressMachine.Mode.Rest) return@LaunchedEffect
        var last = withFrameMillis { it }
        // A relaunch mid-motion (a release while the press was still going down) steps the time since
        // the cancelled loop's last frame, as one loop running through would have.
        state.machine.step(state.lastFrameMs?.let { min(MAX_FRAME_MS, (last - it).toDouble()) } ?: 0.0, reduce)
        state.lastFrameMs = last
        state.progress = state.machine.progress.toFloat()
        // A held press stops asking for frames once it is fully down; the release wakes it again.
        while (state.machine.mode == ToonPressMachine.Mode.Spring ||
            (state.machine.mode == ToonPressMachine.Mode.Press && state.machine.progress < 1.0)
        ) {
            withFrameMillis { now ->
                // The kit caps a frame at 50 ms so that a stall does not fling the spring.
                state.machine.step(min(MAX_FRAME_MS, (now - last).toDouble()), reduce)
                last = now
                state.lastFrameMs = now
                state.progress = state.machine.progress.toFloat()
            }
        }
        state.lastFrameMs = null
    }
    return state
}

private const val MAX_FRAME_MS = 50.0

/**
 * Makes this element pressable the toon way: [state] squashes on the press and springs on the
 * release, and [onClick] fires on the release, never at the end of the animation (the brief's hard
 * limit). A cancelled gesture springs back without a click. The element is at least
 * [ToonSize.TARGET_MIN] square, whatever it draws: the touch target does not animate, the drawing does.
 */
public fun Modifier.toonPressable(
    state: ToonPressState,
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier =
    sizeIn(minWidth = ToonSize.TARGET_MIN.dp, minHeight = ToonSize.TARGET_MIN.dp)
        .semantics {
            this.role = role
            onClick {
                if (enabled) onClick()
                enabled
            }
            if (!enabled) disabled()
        }.pointerInput(enabled, state) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                awaitFirstDown()
                state.press()
                val up = waitForUpOrCancellation()
                if (state.release() && up != null) onClick()
            }
        }
