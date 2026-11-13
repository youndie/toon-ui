package io.github.youndie.toon.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.theme.ToonTheme
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-69: the pointer rule through a component's own input, as an application gets it: a mouse over a
 * button, a card or a menu item changes what it draws, and leaving gives back the rest picture to the
 * pixel; a finger's press and release leave no hover behind. B-79: the same for the pressables the
 * pointer page does not draw. The box is padded 16 dp, so a slot's middle is 16 dp further in.
 */
@OptIn(ExperimentalTestApi::class)
class ToonHoverPathTest {
    private fun ComposeUiTest.shot(): BufferedImage = onNodeWithTag("box").captureToImage().toAwtImage()

    private fun BufferedImage.differs(o: BufferedImage): Int {
        var n = 0
        for (y in 0 until height) for (x in 0 until width) if (getRGB(x, y) != o.getRGB(x, y)) n++
        return n
    }

    private fun hoverChanges(
        at: (Offset) -> Offset = { it },
        content: @Composable () -> Unit,
    ) = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { ToonTheme { Box(Modifier.testTag("box").padding(16.dp)) { content() } } }
        mainClock.advanceTimeBy(1_000)
        val rest = shot()
        onNodeWithTag("box").performMouseInput { moveTo(at(center)) }
        mainClock.advanceTimeBy(200)
        val hovered = shot()
        assertTrue(hovered.differs(rest) > 50, "the hover changed ${hovered.differs(rest)} pixels")
        onNodeWithTag("box").performMouseInput { moveTo(Offset(-40f, -40f)) }
        mainClock.advanceTimeBy(300)
        assertEquals(0, shot().differs(rest), "pixels off the rest picture after the pointer left")
    }

    @Test
    fun `a button lifts under the mouse and settles back`() = hoverChanges { ToonButton("Add plan", onClick = {}) }

    @Test
    fun `a card lifts under the mouse and settles back`() =
        hoverChanges { ToonCard("Pancake breakfast", meta = "Kitchen", time = "9:00", width = 300.dp) }

    @Test
    fun `a menu item takes the shade under the mouse and lets it go`() =
        hoverChanges(at = { Offset(it.x, 16f + 14f + 44f * 1.5f) }) {
            ToonMenu(
                open = true,
                items = listOf(ToonMenuItem("Edit", ToonIcons.Edit), ToonMenuItem("Share", ToonIcons.Share)),
            )
        }

    @Test
    fun `a checkbox lifts its box under the mouse and settles back`() =
        hoverChanges { ToonCheckbox(checked = true, onCheckedChange = {}, label = "Bring snacks") }

    @Test
    fun `a radio lifts its ring under the mouse and settles back`() =
        hoverChanges { ToonRadioGroup(listOf("Noon"), selected = 0, onSelectedChange = {}) }

    @Test
    fun `a switch lifts under the mouse and settles back`() =
        hoverChanges { ToonSwitch(checked = false, onCheckedChange = {}, label = "Reminders") }

    @Test
    fun `a segment takes its press plate under the mouse and lets it go`() =
        hoverChanges(at = { Offset(16f + 250f, it.y) }) { ToonSegmented(listOf("Day", "Week", "Month")) }

    @Test
    fun `a tab takes its press plate under the mouse and lets it go`() =
        hoverChanges(at = { Offset(16f + 171f, it.y) }) { ToonTabs(listOf("Today", "Weekend", "Someday")) }

    @Test
    fun `a navigation bar slot takes its press plate under the mouse and lets it go`() =
        hoverChanges(at = { Offset(16f + 97.5f * 2.5f, it.y) }) {
            ToonNavigationBar(
                listOf(
                    ToonNavItem(ToonIcons.Home, "Home"),
                    ToonNavItem(ToonIcons.Search, "Explore"),
                    ToonNavItem(ToonIcons.Heart, "Saved"),
                    ToonNavItem(ToonIcons.Person, "Profile"),
                ),
            )
        }

    @Test
    fun `a drawer row takes its press plate under the mouse and lets it go`() =
        hoverChanges(at = { Offset(16f + 120f, 16f + 120f + 56f + 12f) }) {
            Box(Modifier.size(360.dp, 520.dp)) {
                ToonNavigationDrawer(
                    listOf(
                        ToonDrawerItem("Home", ToonIcons.Home),
                        ToonDrawerItem("Weekend", ToonIcons.Calendar),
                    ),
                )
            }
        }

    @Test
    fun `a field fills with the pressed surface under the mouse and gives it back`() =
        hoverChanges { ToonTextField(value = "", onValueChange = {}, label = "Plan name") }

    @Test
    fun `a finger's press on a button leaves no hover`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                ToonTheme {
                    Box(
                        Modifier.testTag("box").padding(16.dp),
                    ) { ToonButton("Add plan", onClick = {}) }
                }
            }
            mainClock.advanceTimeBy(1_000)
            val rest = shot()
            onNodeWithTag("box").performTouchInput {
                down(center)
                up()
            }
            mainClock.advanceTimeBy(1_000)
            assertEquals(0, shot().differs(rest), "pixels off the rest picture after a tap")
        }
}
