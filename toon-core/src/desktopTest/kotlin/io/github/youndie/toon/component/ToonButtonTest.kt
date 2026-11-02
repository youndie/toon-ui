package io.github.youndie.toon.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.down
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.theme.ToonTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * B-14: the button clicks on the release, never while loading or disabled, and its frame over time
 * follows the Core page's strips and reduce-motion words.
 */
@OptIn(ExperimentalTestApi::class)
class ToonButtonTest {
    private fun clicks(
        enabled: Boolean = true,
        loading: Boolean = false,
        reduceMotion: Boolean = false,
    ): Int {
        var clicks = 0
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                ToonTheme(reduceMotion = reduceMotion) {
                    ToonButton("Add plan", { clicks++ }, Modifier.testTag("b"), enabled = enabled, loading = loading)
                }
            }
            val b = onNodeWithTag("b")
            b.performTouchInput { down(center) }
            mainClock.advanceTimeBy(200)
            assertEquals(0, clicks, "a click before the release")
            b.performTouchInput { up() }
            mainClock.advanceTimeBy(100)
        }
        return clicks
    }

    @Test
    fun `a press clicks on its release`() = assertEquals(1, clicks())

    @Test
    fun `a loading button does not click`() = assertEquals(0, clicks(loading = true))

    @Test
    fun `a disabled button does not click and says it is disabled`() {
        assertEquals(0, clicks(enabled = false))
        runComposeUiTest {
            setContent { ToonTheme { ToonButton("Add plan", {}, Modifier.testTag("b"), enabled = false) } }
            onNodeWithTag("b").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
        }
    }

    @Test
    fun `width 0 fits the label, a width is kept, the height is 52`() =
        runComposeUiTest {
            var width by mutableStateOf(0.dp)
            setContent { ToonTheme { ToonButton("Add plan", {}, Modifier.testTag("b"), width = width) } }
            // The label is 74 dp wide in Figtree 700 at 18 sp; 56 dp of padding, 120 at least.
            onNodeWithTag("b").assertWidthIsEqualTo(130.dp).assertHeightIsEqualTo(52.dp)
            width = 200.dp
            onNodeWithTag("b").assertWidthIsEqualTo(200.dp)
        }

    private fun at(
        nowMs: Long,
        loadAgeMs: Float? = null,
        buzzAgeMs: Float? = null,
        breathing: Boolean = false,
        reduceMotion: Boolean = false,
    ) = toonButtonFrameAt(nowMs, loadAgeMs, buzzAgeMs, 0f, breathing, disabled = false, focus = false, reduceMotion)

    @Test
    fun `idle breathes 1_5 percent over 2500 ms and boils through three outlines at 10 fps`() {
        // The Core page's idle strip shows the breath at 0, 1250 and 2500 ms and the three outlines;
        // the kit changes outline every 100 ms (`fr = floor(clock / 100) % 3`, seed fr × 31).
        for ((t, br) in listOf(0L to 1f, 1250L to 1.015f, 2500L to 1f)) {
            assertEquals(br, at(t, breathing = true).look.br, 1e-6f, "breath at $t ms")
        }
        assertEquals(listOf(0, 31, 62, 0), listOf(0L, 100L, 250L, 300L).map { at(it, breathing = true).look.seed })
        assertEquals(1f, at(1250, breathing = false).look.br)
    }

    @Test
    fun `loading hands off by the clock, or fades in 120 ms under reduce motion`() {
        assertEquals(60f, at(1000, loadAgeMs = 60f).look.lt)
        assertNull(at(1000, loadAgeMs = 60f).fade)
        val reduced = at(1000, loadAgeMs = 60f, reduceMotion = true)
        assertEquals(.5f, reduced.fade!!, 1e-6f)
        assertEquals(1e6f, reduced.look.lt)
    }

    @Test
    fun `a disabled press buzzes for 320 ms, never under reduce motion`() {
        assertEquals(100f, at(0, buzzAgeMs = 100f).buzz)
        assertNull(at(0, buzzAgeMs = 320f).buzz)
        assertNull(at(0, buzzAgeMs = 100f, reduceMotion = true).buzz)
    }
}
