package io.github.youndie.toon.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.theme.ToonTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * B-94: an overlay filling the window stays composed while closed, over the content it covers when
 * open. Closed, it must let every press through, by mouse and by finger: the closed drawer's hover
 * listener took them all, and nothing in the sample answered a click.
 */
@OptIn(ExperimentalTestApi::class)
class ToonClosedOverlayTest {
    private fun assertPassesClicks(
        name: String,
        overlay: @Composable () -> Unit,
    ) {
        runComposeUiTest {
            mainClock.autoAdvance = false
            var clicks = 0
            setContent {
                ToonTheme {
                    Box(Modifier.size(390.dp, 700.dp)) {
                        Box(Modifier.fillMaxSize().testTag("content").clickable { clicks++ })
                        overlay()
                    }
                }
            }
            mainClock.advanceTimeBy(1_000)
            onNodeWithTag("content").performMouseInput { click(center) }
            mainClock.advanceTimeBy(500)
            onNodeWithTag("content").performTouchInput { click(center) }
            mainClock.advanceTimeBy(500)
            assertEquals(2, clicks, "a closed $name took the content's clicks")
        }
    }

    @Test
    fun `a closed drawer lets clicks through to the content under it`() =
        assertPassesClicks("drawer") { ToonNavigationDrawer(listOf(ToonDrawerItem("Home")), open = false) }

    @Test
    fun `a closed dialog lets clicks through to the content under it`() =
        assertPassesClicks("dialog") { ToonDialog("Delete plan?", "Delete", "Cancel", open = false) }

    @Test
    fun `a closed sheet lets clicks through to the content under it`() =
        assertPassesClicks("sheet") { ToonBottomSheet(listOf(ToonSheetItem("Family")), "Share plan", open = false) }

    @Test
    fun `a closed sheet with a slot lets clicks through to the content under it`() =
        assertPassesClicks("sheet with a slot") {
            ToonBottomSheetBox("Share plan", open = false) { ToonRadioGroup(listOf("A"), 0, {}) }
        }
}
