package io.github.youndie.toon.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.theme.ToonTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-51: a component whose state changes after it is shown plays the change through and comes to rest
 * on what it draws when shown in that state from the start. Each clock reads its time even while a
 * play waits for its first frame; one that did not left whatever read it at that moment unsubscribed,
 * and the canvas stuck on the play's first frame — an opened menu or tooltip never appeared.
 */
@OptIn(ExperimentalTestApi::class)
class ToonClockSubscriptionTest {
    /** The box's pixels 1.5 s after [content] is shown with false, then switched to true — or shown with true. */
    private fun pixels(
        switched: Boolean,
        content: @Composable (Boolean) -> Unit,
    ): IntArray {
        lateinit var out: IntArray
        runComposeUiTest {
            var on by mutableStateOf(!switched)
            setContent {
                ToonTheme {
                    Box(Modifier.size(400.dp, 560.dp).background(Toon.colors.stage).testTag("box")) { content(on) }
                }
            }
            waitForIdle()
            on = true
            mainClock.advanceTimeBy(1500)
            waitForIdle()
            val image = onNodeWithTag("box").captureToImage().toPixelMap()
            out = IntArray(image.width * image.height) { image[it % image.width, it / image.width].toArgb() }
        }
        return out
    }

    private fun assertComesToRest(
        name: String,
        content: @Composable (Boolean) -> Unit,
    ) {
        val shown = pixels(false, content)
        val switched = pixels(true, content)
        val stage = shown[0]
        assertTrue(shown.any { it != stage }, "$name draws nothing at rest")
        val differ = shown.indices.count { shown[it] != switched[it] }
        assertEquals(0, differ, "$name switched after showing differs from shown so by $differ px")
    }

    @Test
    fun `an opened menu, tooltip, sheet, drawer and dialog come to rest as if shown open`() {
        assertComesToRest("menu") { ToonMenu(listOf(ToonMenuItem("Edit"), ToonMenuItem("Share")), open = it) }
        assertComesToRest("tooltip") { ToonTooltip("Tap to add a plan", show = it) }
        assertComesToRest("sheet") { ToonBottomSheet(listOf(ToonSheetItem("Family")), open = it) }
        assertComesToRest("drawer") { ToonNavigationDrawer(listOf(ToonDrawerItem("Home")), open = it) }
        assertComesToRest("dialog") { ToonDialog("Delete plan?", open = it) }
        assertComesToRest("snackbar") { ToonSnackbar("Plan added", shown = it) }
    }

    @Test
    fun `a selection changed after showing comes to rest on the new one`() {
        assertComesToRest("tabs") { ToonTabs(listOf("Today", "Weekend"), selected = if (it) 1 else 0) }
        assertComesToRest("segmented") { ToonSegmented(listOf("Day", "Week"), selected = if (it) 1 else 0) }
        assertComesToRest("navigation bar") {
            ToonNavigationBar(
                listOf(ToonNavItem(ToonIcons.Home, "Home"), ToonNavItem(ToonIcons.Heart, "Saved")),
                selected = if (it) 1 else 0,
            )
        }
    }
}
