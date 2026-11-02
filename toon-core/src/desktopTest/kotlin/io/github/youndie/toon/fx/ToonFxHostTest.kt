package io.github.youndie.toon.fx

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.theme.ToonTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** B-12: events reach their listener, play their effect for its length, and follow reduce motion. */
@OptIn(ExperimentalTestApi::class)
class ToonFxHostTest {
    private val card = Rect(90f, 62f, 170f, 110f)

    private fun run(
        reduceMotion: Boolean,
        event: ToonFxEvent,
        check: (heard: List<String>, playingAt: (Long) -> Int) -> Unit,
    ) = runComposeUiTest {
        val state = ToonFxHostState()
        val heard = mutableListOf<String>()
        var emitter: ToonFxEmitter? = null
        mainClock.autoAdvance = false
        setContent {
            ToonTheme(reduceMotion = reduceMotion) {
                ToonFxHost(state, onEvent = { heard += it.name }) { emitter = Toon.fx }
            }
        }
        waitForIdle()
        runOnIdle { checkNotNull(emitter).emit(event) }
        var elapsed = 0L
        check(heard) { ms ->
            mainClock.advanceTimeBy(ms - elapsed)
            elapsed = ms
            state.playing.size
        }
    }

    @Test
    fun `a listener receives card land`() =
        run(reduceMotion = false, ToonFxEvent.Land("card", card)) { heard, _ ->
            assertEquals(listOf("card.land"), heard)
        }

    @Test
    fun `dust plays for its 300 ms and is gone after`() =
        run(reduceMotion = false, ToonFxEvent.Land("card", card)) { _, playingAt ->
            assertEquals(1, playingAt(150))
            assertEquals(0, playingAt(450))
        }

    @Test
    fun `under reduce motion dust is off but the event is still heard`() =
        run(reduceMotion = true, ToonFxEvent.Land("card", card)) { heard, playingAt ->
            assertEquals(listOf("card.land"), heard)
            assertEquals(0, playingAt(16))
        }

    @Test
    fun `under reduce motion the flash fades in 120 ms instead of 220`() =
        run(reduceMotion = true, ToonFxEvent.Pop("badge", card)) { heard, playingAt ->
            assertEquals(listOf("badge.pop"), heard)
            assertEquals(1, playingAt(64))
            assertEquals(0, playingAt(200))
        }

    @Test
    fun `under reduce motion the iris wipe is off`() =
        run(reduceMotion = true, ToonFxEvent.SceneChange(card)) { _, playingAt ->
            assertEquals(0, playingAt(16))
        }

    @Test
    fun `every effect plays without reduce motion`() {
        for (fx in ToonFx.entries) assertTrue(fx.durationMs in 200..600, "$fx lasts ${fx.durationMs} ms")
    }

    @Test
    fun `dialog pop is stars, not a flash`() {
        assertEquals(ToonFx.Stars, ToonFxEvent.DialogPop(card).fx)
        assertFailsWith<IllegalArgumentException> { ToonFxEvent.Pop("dialog", card) }
    }
}
