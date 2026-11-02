package io.github.youndie.toon.fx

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonColors

/**
 * The eight effects of the FX page, each played once over its [durationMs] (`toon-ui FX.dc.html`,
 * FX 1–8).
 */
public enum class ToonFx(
    public val durationMs: Int,
) {
    Dust(300),
    Stars(350),
    Speed(200),
    Flash(220),
    Shake(320),
    Iris(600),
    Hole(300),
    Sweat(360),
}

/**
 * A named event that fires an effect: the FX page's names (`card.land`, `field.error`, `dialog.pop` …),
 * which will carry sounds later as well. [bounds] is the element that fired it, in root coordinates
 * (`boundsInRoot()`); the effect is placed on it as the FX page places it on its demo block.
 */
public sealed class ToonFxEvent {
    /** The spec's name of the event, as a listener (sound, analytics) reads it. */
    public abstract val name: String

    /** The element that fired the event, in root coordinates. */
    public abstract val bounds: Rect

    /** The effect the event plays. */
    public abstract val fx: ToonFx

    override fun toString(): String = "ToonFxEvent($name)"

    /** `*.land`: an element landed. Dust. */
    public class Land(
        public val source: String,
        override val bounds: Rect,
    ) : ToonFxEvent() {
        override val name: String get() = "$source.land"
        override val fx: ToonFx get() = ToonFx.Dust
    }

    /** `*.hit`: an element was struck. Stars. */
    public class Hit(
        public val source: String,
        override val bounds: Rect,
    ) : ToonFxEvent() {
        override val name: String get() = "$source.hit"
        override val fx: ToonFx get() = ToonFx.Stars
    }

    /** `field.error`: a text field refused its input. Stars. */
    public class FieldError(
        override val bounds: Rect,
    ) : ToonFxEvent() {
        override val name: String get() = "field.error"
        override val fx: ToonFx get() = ToonFx.Stars
    }

    /** `dialog.pop`: a dialog closed with a pop. Stars, unlike every other `*.pop`. */
    public class DialogPop(
        override val bounds: Rect,
    ) : ToonFxEvent() {
        override val name: String get() = "dialog.pop"
        override val fx: ToonFx get() = ToonFx.Stars
    }

    /** `*.whoosh`: an element left fast. Speed lines. */
    public class Whoosh(
        public val source: String,
        override val bounds: Rect,
    ) : ToonFxEvent() {
        override val name: String get() = "$source.whoosh"
        override val fx: ToonFx get() = ToonFx.Speed
    }

    /** `*.pop`, `badge.pop` among them: an element appeared with a pop. Flash. */
    public class Pop(
        public val source: String,
        override val bounds: Rect,
    ) : ToonFxEvent() {
        init {
            require(source != "dialog") { "dialog.pop plays stars: use DialogPop" }
        }

        override val name: String get() = "$source.pop"
        override val fx: ToonFx get() = ToonFx.Flash
    }

    /** `success`: an action was confirmed. Flash. */
    public class Success(
        override val bounds: Rect,
    ) : ToonFxEvent() {
        override val name: String get() = "success"
        override val fx: ToonFx get() = ToonFx.Flash
    }

    /** `*.buzz`: an element buzzed. Shake. */
    public class Buzz(
        public val source: String,
        override val bounds: Rect,
    ) : ToonFxEvent() {
        override val name: String get() = "$source.buzz"
        override val fx: ToonFx get() = ToonFx.Shake
    }

    /** `locked`: the element is locked. Shake. */
    public class Locked(
        override val bounds: Rect,
    ) : ToonFxEvent() {
        override val name: String get() = "locked"
        override val fx: ToonFx get() = ToonFx.Shake
    }

    /** The FX page's "action denied". Shake. */
    public class ActionDenied(
        override val bounds: Rect,
    ) : ToonFxEvent() {
        override val name: String get() = "action.denied"
        override val fx: ToonFx get() = ToonFx.Shake
    }

    /** `screen.scene`: a scene change, the iris closing on [bounds]' centre (B-44 swaps the screens). */
    public class SceneChange(
        override val bounds: Rect,
    ) : ToonFxEvent() {
        override val name: String get() = "screen.scene"
        override val fx: ToonFx get() = ToonFx.Iris
    }

    /** `*.crash`: an element left so fast it punched its outline, corner [radii], into the paper. Hole. */
    public class Crash(
        public val source: String,
        override val bounds: Rect,
        public val radii: ToonRadii,
    ) : ToonFxEvent() {
        override val name: String get() = "$source.crash"
        override val fx: ToonFx get() = ToonFx.Hole
    }

    /** `card.gone`: a card was dismissed through the paper. Hole. */
    public class CardGone(
        override val bounds: Rect,
        public val radii: ToonRadii,
    ) : ToonFxEvent() {
        override val name: String get() = "card.gone"
        override val fx: ToonFx get() = ToonFx.Hole
    }

    /** `*.worry`: a long wait or a destructive confirm. Sweat drops. */
    public class Worry(
        public val source: String,
        override val bounds: Rect,
    ) : ToonFxEvent() {
        override val name: String get() = "$source.worry"
        override val fx: ToonFx get() = ToonFx.Sweat
    }
}

/** Where components send their [ToonFxEvent]s: the nearest [ToonFxHost], or nowhere without one. */
@Stable
public fun interface ToonFxEmitter {
    public fun emit(event: ToonFxEvent)
}

internal val LocalToonFxEmitter = staticCompositionLocalOf { ToonFxEmitter { } }

/** The nearest [ToonFxHost]'s emitter; outside any host, events go nowhere. */
public val Toon.fx: ToonFxEmitter
    @Composable get() = LocalToonFxEmitter.current

/**
 * How long an effect plays under reduce motion, per the FX page's word for it, or null where the
 * word is `off`. The flash's `fade 120` holds its settled frame and fades it out over 120 ms. The
 * iris's `cross-fade 120` is the scene change's, which B-44 owns; the wipe itself is off.
 */
internal fun ToonFx.reducedDurationMs(): Int? =
    when (this) {
        ToonFx.Flash -> 120
        else -> null
    }

/** The flash's frame held under reduce motion: the page's 90 ms frame, settled at full size. */
private const val FLASH_SETTLED_U = .4f

/** One playing effect: its event and the frame time it started at, set on its first frame. */
internal class ToonFxPlaying(
    val event: ToonFxEvent,
    val reduced: Boolean,
) {
    var startMs: Long = -1L
    val durationMs: Int get() = if (reduced) event.fx.reducedDurationMs() ?: 0 else event.fx.durationMs
}

/** What a host is playing, read by the tests. */
internal class ToonFxHostState {
    val playing = mutableStateListOf<ToonFxPlaying>()
    var frameMs by mutableLongStateOf(0L)
}

/**
 * The FX layer: draws every effect fired inside [content] above it, unclipped by the element that
 * fired it. Each event reaches [onEvent] whether or not its effect plays, so sound can follow the
 * names under reduce motion too. Must sit inside a `ToonTheme`.
 */
@Composable
public fun ToonFxHost(
    modifier: Modifier = Modifier,
    onEvent: (ToonFxEvent) -> Unit = {},
    content: @Composable () -> Unit,
) {
    ToonFxHost(remember { ToonFxHostState() }, modifier, onEvent, content)
}

@Composable
internal fun ToonFxHost(
    state: ToonFxHostState,
    modifier: Modifier = Modifier,
    onEvent: (ToonFxEvent) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val colors = Toon.colors
    val reduce = Toon.reduceMotion
    val listener by rememberUpdatedState(onEvent)
    val reduceNow by rememberUpdatedState(reduce)
    val emitter =
        remember(state) {
            ToonFxEmitter { event ->
                listener(event)
                val playing = ToonFxPlaying(event, reduceNow)
                if (playing.durationMs > 0) state.playing += playing
            }
        }
    val hasPlaying = state.playing.isNotEmpty()
    LaunchedEffect(state, hasPlaying) {
        while (state.playing.isNotEmpty()) {
            withFrameMillis { now ->
                state.frameMs = now
                state.playing.forEach { if (it.startMs < 0) it.startMs = now }
                state.playing.removeAll { now - it.startMs >= it.durationMs }
            }
        }
    }
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier
            .onGloballyPositioned { origin = it.positionInRoot() }
            .drawWithContent {
                drawContent()
                val now = state.frameMs
                for (p in state.playing) {
                    val t = if (p.startMs < 0) 0f else (now - p.startMs).toFloat()
                    drawToonFx(colors, p, t, p.event.bounds.translate(-origin))
                }
            },
    ) {
        CompositionLocalProvider(LocalToonFxEmitter provides emitter, content = content)
    }
}

/**
 * One effect at [t] ms, placed on its element's bounds [b] in the host's coordinates as the FX page places it on its 80 × 48 block at
 * (90, 62): dust from both bottom corners, stars on an arc over the top, speed lines from the left
 * edge, the flash behind the centre, buzz arcs either side, the hole on the outline, sweat off the top.
 */
private fun DrawScope.drawToonFx(
    colors: ToonColors,
    p: ToonFxPlaying,
    t: Float,
    b: Rect,
) {
    val u = t / p.event.fx.durationMs
    when (val e = p.event) {
        is ToonFxEvent.Land -> {
            drawToonDust(colors, u, b.left + 8, b.bottom + 2, 46f)
            drawToonDust(colors, u, b.right - 8, b.bottom + 2, 46f)
        }

        is ToonFxEvent.Hit, is ToonFxEvent.FieldError, is ToonFxEvent.DialogPop -> {
            drawToonStars(colors, u, b.center.x, b.top - 6, 40f)
        }

        is ToonFxEvent.Whoosh -> {
            drawToonSpeed(colors, u, b.left, b.top, b.height)
        }

        is ToonFxEvent.Pop, is ToonFxEvent.Success -> {
            if (p.reduced) {
                drawToonFlash(colors, FLASH_SETTLED_U, b.center.x, b.center.y - 2, 46f, opacity = 1 - t / p.durationMs)
            } else {
                drawToonFlash(colors, u, b.center.x, b.center.y - 2, 46f)
            }
        }

        is ToonFxEvent.Buzz, is ToonFxEvent.Locked, is ToonFxEvent.ActionDenied -> {
            drawToonVibe(colors, u, b.left, b.top, b.width, b.height)
        }

        is ToonFxEvent.SceneChange -> {
            drawToonIris(colors, toonIrisAt(t).k, size.width, size.height, b.center.x, b.center.y)
        }

        is ToonFxEvent.Crash -> {
            drawToonHole(colors, u, b.left, b.top, b.width, b.height, e.radii)
        }

        is ToonFxEvent.CardGone -> {
            drawToonHole(colors, u, b.left, b.top, b.width, b.height, e.radii)
        }

        is ToonFxEvent.Worry -> {
            drawToonSweat(colors, u, b.left, b.top, b.width)
        }
    }
}
