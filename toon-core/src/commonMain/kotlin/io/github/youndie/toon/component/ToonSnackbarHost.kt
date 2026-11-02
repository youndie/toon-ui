package io.github.youndie.toon.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.theme.Toon
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

// B-23: "the host queues one at a time". The design draws one sign and says nothing about how long it
// hangs, so the durations are Material's (B-23 findings): a caller that wants another
// passes Indefinite and dismisses it itself.

/** How long a sign hangs before it drops on its own. */
public enum class ToonSnackbarDuration(
    internal val ms: Long,
) {
    Short(4_000),
    Long(10_000),
    Indefinite(kotlin.Long.MAX_VALUE),
}

/** How a sign left: on its own (or [ToonSnackbarHostState.dismiss]), or by a tap on its action. */
public enum class ToonSnackbarResult {
    Dismissed,
    ActionPerformed,
}

/**
 * The queue behind [ToonSnackbarHost]: [show] suspends until its sign has left, and a second caller
 * waits for the first sign to drop out before its own flies in.
 */
@Stable
public class ToonSnackbarHostState {
    private val queue = Mutex()

    internal var current by mutableStateOf<ToonSnackbarData?>(null)
        private set

    /** Hangs [text] with [action] until it times out per [duration] or the action is tapped. */
    public suspend fun show(
        text: String,
        action: String = "",
        type: ToonSnackbarType = ToonSnackbarType.Info,
        duration: ToonSnackbarDuration = ToonSnackbarDuration.Short,
    ): ToonSnackbarResult =
        queue.withLock {
            try {
                suspendCancellableCoroutine { current = ToonSnackbarData(text, action, type, duration, it) }
            } finally {
                current = null
            }
        }

    /** Drops the sign now hanging, if any; its [show] returns [ToonSnackbarResult.Dismissed]. */
    public fun dismiss() {
        current?.leave(ToonSnackbarResult.Dismissed)
    }
}

/** One queued sign: shown until [leave], then resumed once its hide has been drawn. */
internal class ToonSnackbarData(
    val text: String,
    val action: String,
    val type: ToonSnackbarType,
    val duration: ToonSnackbarDuration,
    private val continuation: CancellableContinuation<ToonSnackbarResult>,
) {
    var result by mutableStateOf<ToonSnackbarResult?>(null)
        private set

    fun leave(how: ToonSnackbarResult) {
        if (result == null) result = how
    }

    fun finish() {
        if (continuation.isActive) continuation.resume(checkNotNull(result))
    }
}

/** Shows [state]'s signs one at a time, each [width] wide; the caller places it, as Material's host. */
@Composable
public fun ToonSnackbarHost(
    state: ToonSnackbarHostState,
    modifier: Modifier = Modifier,
    width: Dp = 342.dp,
) {
    val data = state.current ?: return
    val reduce = Toon.reduceMotion
    key(data) {
        val leaving = data.result != null
        LaunchedEffect(data) {
            delay(data.duration.ms)
            data.leave(ToonSnackbarResult.Dismissed)
        }
        LaunchedEffect(data, leaving) {
            if (!leaving) return@LaunchedEffect
            // The next sign waits for this one to have dropped out, as the kit draws it.
            delay(if (reduce) REDUCED_HIDE_MS else HIDE_MS)
            data.finish()
        }
        ToonSnackbar(
            data.text,
            modifier,
            action = data.action,
            type = data.type,
            shown = !leaving,
            width = width,
            onAction = { data.leave(ToonSnackbarResult.ActionPerformed) },
        )
    }
}

/** The kit draws the hide for 260 ms; reduce motion fades it in 120. */
private const val HIDE_MS = 260L
private const val REDUCED_HIDE_MS = 120L
