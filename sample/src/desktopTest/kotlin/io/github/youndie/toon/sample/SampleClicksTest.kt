package io.github.youndie.toon.sample

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * B-94: the sample on Pages answered no click. Its closed overlays stay composed over the whole
 * window, and the closed drawer took every press. The whole sample, clicked as a person clicks it.
 */
@OptIn(ExperimentalTestApi::class)
class SampleClicksTest {
    @Test
    fun `a click on the header's theme switches it, by mouse and by finger`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent { ToonUiSample() }
            mainClock.advanceTimeBy(2_000)
            val theme = onNodeWithTag("theme")

            fun chosen() = theme.fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)
            assertEquals("Day", chosen())
            // The header's segmented button is 260 dp wide, three segments: Night is the middle one.
            theme.performMouseInput { click(Offset(width / 2f, height / 2f)) }
            mainClock.advanceTimeBy(1_000)
            assertEquals("Night", chosen(), "a mouse click on Night")
            theme.performTouchInput { click(Offset(width * 5 / 6f, height / 2f)) }
            mainClock.advanceTimeBy(1_000)
            assertEquals("Cel", chosen(), "a tap on Cel")
        }
}
