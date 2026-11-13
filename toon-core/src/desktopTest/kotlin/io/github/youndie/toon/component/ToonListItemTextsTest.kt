package io.github.youndie.toon.component

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.inkGouacheLight
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-84: a list item's long title wraps and grows the row; its sub is cut short of the trailing control. */
@OptIn(ExperimentalTestApi::class)
class ToonListItemTextsTest {
    private val longTitle = "Move the board to the new kit and record the goldens on both machines again"
    private val longSub = "Зелёный · PR 2, 1 падает · 177 из 184 — 96 % · ждут 1"

    @Test
    fun `the words keep to the room left of the trailing control`() {
        assertEquals(342f - 28 - 8 - 56, toonListTextRoom(342f, ToonListTrailing.Chevron))
        assertEquals(342f - 60 - 8 - 56, toonListTextRoom(342f, ToonListTrailing.Switch))
        assertEquals(342f - 48 - 8 - 56, toonListTextRoom(342f, ToonListTrailing.Check))
        assertEquals(342f - 56, toonListTextRoom(342f, ToonListTrailing.None))
        val scene =
            toonListItemScene(
                inkGouacheLight,
                358f,
                ToonIcons.Star,
                "T",
                "S",
                ToonListTrailing.Chevron,
                ToonListItemLook(titleMaxLines = 2, titleExtra = 20f),
            )
        assertEquals(358f - 28 - 8 - 56, scene.labels[0].maxWidth)
        assertEquals(2, scene.labels[0].maxLines)
        assertEquals(33f, scene.labels[0].baseline, "the title starts where a one-line row's does")
        assertEquals(54f + 20f, scene.labels[1].baseline, "the sub moves down by the title's added line")
        assertEquals(1, scene.labels[1].maxLines)
    }

    @Test
    fun `a long title wraps onto two lines and the row grows, a design row stays 72 dp`() {
        runComposeUiTest {
            setContent {
                ToonTheme {
                    Column {
                        ToonListItem("Favourites", Modifier.testTag("design"), width = 358.dp)
                        ToonListItem(
                            "Notifications",
                            Modifier.testTag("designSub"),
                            sub = "Push and email",
                            width = 358.dp,
                        )
                        ToonListItem(longTitle, Modifier.testTag("long"), sub = longSub, width = 358.dp)
                        ToonListItem(longTitle, Modifier.testTag("one"), width = 358.dp, titleMaxLines = 1)
                    }
                }
            }
            for (tag in listOf("design", "designSub", "one")) {
                onNodeWithTag(tag).assertIsDisplayed()
                assertEquals(72f, onNodeWithTag(tag).getBoundsInRoot().let { (it.bottom - it.top).value }, tag)
            }
            val long = onNodeWithTag("long").assertIsDisplayed().getBoundsInRoot().let { (it.bottom - it.top).value }
            assertTrue(long in 85f..100f, "a two-line title grows the row by one line, not $long")
        }
    }

    @Test
    fun `no title or sub is drawn between the words' room and the trailing control`() {
        val c = inkGouacheLight.colors

        fun near(
            a: Color,
            b: Color,
            by: Float,
        ) = abs(a.red - b.red) <= by && abs(a.green - b.green) <= by && abs(a.blue - b.blue) <= by
        for (trailing in listOf(ToonListTrailing.Chevron, ToonListTrailing.Check)) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                setContent {
                    ToonTheme {
                        ToonListItem(
                            longTitle + " " + longTitle,
                            Modifier.testTag("row"),
                            sub = longSub,
                            width = 358.dp,
                            trailing = trailing,
                        )
                    }
                }
                mainClock.advanceTimeBy(1_000)
                val image = onNodeWithTag("row").captureToImage().toPixelMap()
                val end = (56 + toonListTextRoom(358f, trailing)).toInt()
                var ink = 0
                var muted = 0
                for (y in 8 until image.height - 8) {
                    for (x in end + 2..end + 6) {
                        if (near(image[x, y], c.text, .12f)) ink++
                        if (near(image[x, y], c.textMuted, .06f)) muted++
                    }
                }
                assertEquals(0, ink, "$trailing: the title drawn past its room")
                assertEquals(0, muted, "$trailing: the sub drawn past its room")
            }
        }
    }
}
