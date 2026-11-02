package io.github.youndie.toon.fx

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
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
    fun `an effect lands where its root coordinates say, wherever the host sits`() =
        runComposeUiTest {
            var emitter: ToonFxEmitter? = null
            mainClock.autoAdvance = false
            setContent {
                ToonTheme {
                    Box(Modifier.padding(start = 40.dp, top = 30.dp)) {
                        ToonFxHost(Modifier.size(300.dp, 200.dp).testTag("host")) {
                            emitter = Toon.fx
                            Box(Modifier.fillMaxSize().background(Toon.colors.stage))
                        }
                    }
                }
            }
            waitForIdle()
            // Puffs at root (148, 180) and (212, 180): host (108, 150) and (172, 150).
            runOnIdle { checkNotNull(emitter).emit(ToonFxEvent.Land("card", Rect(140f, 130f, 220f, 178f))) }
            mainClock.advanceTimeBy(100)
            val image = onNodeWithTag("host").captureToImage().toPixelMap()
            val stage = image[0, 0]

            fun inked(
                x0: Int,
                y0: Int,
            ) = (x0 - 12..x0 + 12).any { x -> (y0 - 24..y0 + 4).any { y -> image[x, y] != stage } }
            assertTrue(inked(108, 150), "no dust at the event's point")
            assertTrue(!inked(148, 190), "dust drawn without the host's offset")
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
