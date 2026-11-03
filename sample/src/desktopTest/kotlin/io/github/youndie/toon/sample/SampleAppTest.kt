package io.github.youndie.toon.sample

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

/** B-29: the sample runs its screens: sign in, dismiss a plan and undo it, open the settings, sign out. B-32: the kit. */
@OptIn(ExperimentalTestApi::class)
class SampleAppTest {
    @Test
    fun `sign in, swipe a plan away and undo it, open the settings, switch the look, sign out`() {
        runComposeUiTest {
            // The idle button, the cards' entrances and the FAB animate: the clock is driven by hand.
            mainClock.autoAdvance = false
            setContent { SampleApp() }
            mainClock.advanceTimeBy(1_000)
            onNodeWithTag("sign-in").performClick()
            mainClock.advanceTimeBy(1_000)
            onNodeWithTag("card-0").assertExists()
            onNodeWithTag("card-0").performTouchInput { swipeLeft() }
            mainClock.advanceTimeBy(1_000)
            onNodeWithTag("card-0").assertDoesNotExist()
            // The snackbar's action is its last 110 dp.
            onNodeWithTag("snackbar").performTouchInput { click(Offset(width - 40f, height / 2f)) }
            mainClock.advanceTimeBy(1_000)
            onNodeWithTag("card-0").assertExists()
            onNodeWithTag("nav").performTouchInput { click(Offset(width * 7 / 8f, height / 2f)) }
            mainClock.advanceTimeBy(1_000)
            onNodeWithTag("look").assertExists()
            onNodeWithText("Sign out").assertExists()
            onNodeWithText("Sign out").performClick()
            mainClock.advanceTimeBy(1_000)
            onNodeWithTag("sign-in").assertExists()
        }
    }

    @Test
    fun `the kit shows every section in each theme, and the header opens the screens`() {
        runComposeUiTest {
            // The kit breathes and spins without end: the clock is driven by hand.
            mainClock.autoAdvance = false
            setContent { ToonUiSample() }
            mainClock.advanceTimeBy(1_000)
            for (look in listOf("Night", "Cel", "Day")) {
                onNodeWithTag("theme").performCustomAccessibilityActionWithLabel(look)
                mainClock.advanceTimeBy(1_000)
                for (section in listOf("Actions", "Inputs", "Content", "Navigation", "Feedback", "Transitions")) {
                    onNodeWithText(section).assertExists()
                }
            }
            onNodeWithTag("page").performCustomAccessibilityActionWithLabel("Screens")
            mainClock.advanceTimeBy(1_000)
            onNodeWithTag("sign-in").assertExists()
            onNodeWithText("Actions").assertDoesNotExist()
        }
    }
}
