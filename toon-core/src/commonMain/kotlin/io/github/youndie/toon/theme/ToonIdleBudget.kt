package io.github.youndie.toon.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * How many always-on idle animations a theme scope allows at once: the brief's "1–2 living elements
 * per screen" (principle 9), counted rather than trusted (research R1). A component asks for a slot
 * with [rememberToonIdleSlot]; the first [capacity] askers in composition order get one, and the
 * rest stay still. A slot is given back when its owner leaves the composition; one already refused
 * does not then get it, because a still element starting to breathe later reads as a glitch.
 */
public class ToonIdleBudget(
    public val capacity: Int,
) {
    private var used = 0

    internal fun tryAcquire(): Boolean = if (used < capacity) true.also { used++ } else false

    internal fun release() {
        used--
    }

    /** Slots handed out and not given back. */
    public val inUse: Int get() = used
}

private class IdleSlot(
    private val budget: ToonIdleBudget,
) : RememberObserver {
    val granted = mutableStateOf(false)

    override fun onRemembered() {
        granted.value = budget.tryAcquire()
    }

    override fun onForgotten() {
        if (granted.value) budget.release()
        granted.value = false
    }

    override fun onAbandoned() = Unit
}

/**
 * Asks the theme's [ToonIdleBudget] for an idle slot while [wanted] holds and motion is not reduced.
 * The answer arrives from the frame after the first composition: a slot is taken when the request is
 * remembered, in composition order, so the same screen always lets the same elements breathe.
 */
@Composable
public fun rememberToonIdleSlot(wanted: Boolean = true): State<Boolean> {
    val budget = Toon.idleBudget
    val active = wanted && !Toon.reduceMotion
    val slot = remember(budget, active) { IdleSlot(budget).takeIf { active } }
    return slot?.granted ?: remember { mutableStateOf(false) }
}
