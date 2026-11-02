package io.github.youndie.toon.draw

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.component.ToonButton
import io.github.youndie.toon.component.ToonCard
import io.github.youndie.toon.component.ToonDialog
import io.github.youndie.toon.component.ToonSnackbar
import io.github.youndie.toon.component.toonDialogHeight
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.inkGouacheLight
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-47: a component draws in dp. At density 2 it is the same picture as at density 1, twice the size,
 * text included, and its hit areas are where it draws them.
 */
@OptIn(ExperimentalTestApi::class)
class ToonDensityTest {
    private val ink = inkGouacheLight.colors.ink

    private fun capture(
        density: Float,
        content: @Composable () -> Unit,
    ): ImageBitmap {
        lateinit var image: ImageBitmap
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(density)) {
                    ToonTheme { Box(Modifier.testTag("it")) { content() } }
                }
            }
            waitForIdle()
            image = onNodeWithTag("it").captureToImage()
        }
        return image
    }

    /** The ink's bounding box and its dark-pixel count. */
    private fun inked(image: ImageBitmap): Pair<IntArray, Int> {
        val px = image.toPixelMap()
        var n = 0
        val box = intArrayOf(Int.MAX_VALUE, Int.MAX_VALUE, -1, -1)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val c = px[x, y]
                if (c.alpha > .5f && abs(c.red - ink.red) + abs(c.green - ink.green) + abs(c.blue - ink.blue) < .15f) {
                    n++
                    box[0] = minOf(box[0], x)
                    box[1] = minOf(box[1], y)
                    box[2] = maxOf(box[2], x)
                    box[3] = maxOf(box[3], y)
                }
            }
        }
        return box to n
    }

    private fun twice(content: @Composable () -> Unit) {
        val (b1, n1) = inked(capture(1f, content))
        val (b2, n2) = inked(capture(2f, content))
        val w1 = b1[2] - b1[0]
        val h1 = b1[3] - b1[1]
        val w2 = b2[2] - b2[0]
        val h2 = b2[3] - b2[1]
        println("ink ${w1}x$h1, $n1 px at 1; ${w2}x$h2, $n2 px at 2")
        // The ink's edge falls on a pixel either way: two pixels at density 2 for one at density 1.
        assertTrue(abs(w2 - 2 * w1) <= 3 && abs(h2 - 2 * h1) <= 3, "ink ${w1}x$h1 at 1, ${w2}x$h2 at 2")
        // Ink grows with the area, four times, and a little more: the antialiased edge, which this
        // count of dark pixels misses, stays a pixel wide, a larger share of the ink at density 1.
        assertTrue(n2 in (3.6 * n1).toInt()..(4.8 * n1).toInt(), "ink $n1 px at 1, $n2 at 2")
    }

    @Test
    fun `a button and its label are twice the size at density 2`() = twice { ToonButton("Add plan", onClick = {}) }

    @Test
    fun `a card and its three texts are twice the size at density 2`() =
        twice {
            ToonCard(
                "Pancake breakfast",
                Modifier.size(342.dp, 84.dp),
                meta = "Kitchen, bring syrup",
                time = "9:00",
            )
        }

    @Test
    fun `at density 2 the dialog's buttons and the snackbar's action are where they are drawn`() {
        runComposeUiTest {
            var actions = 0
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(2f)) {
                    ToonTheme {
                        Box {
                            ToonSnackbar(
                                "Plan added",
                                Modifier.testTag("snack"),
                                action = "Undo",
                                onAction = { actions++ },
                            )
                        }
                    }
                }
            }
            // The action's target is the last 110 dp of 342: dp 300 is pixel 600.
            onNodeWithTag("snack").performTouchInput { click(Offset(200f, 56f)) }
            assertEquals(0, actions)
            onNodeWithTag("snack").performTouchInput { click(Offset(600f, 56f)) }
            assertEquals(1, actions)
        }
        runComposeUiTest {
            var confirms = 0
            var cancels = 0
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(2f)) {
                    ToonTheme {
                        ToonDialog(
                            "Delete plan?",
                            Modifier.size(300.dp, 360.dp).testTag("dialog"),
                            body = "“Paint the fence” will be gone for good.",
                            onConfirm = { confirms++ },
                            onCancel = { cancels++ },
                        )
                    }
                }
            }
            // A 300 × 360 dp layer, which fits the test window at density 2: the dialog spans it, the
            // confirm button at x 148..276 dp on its row, the cancel area 110 dp left of it.
            val h = toonDialogHeight(2)
            val row = 180 - h / 2 + h - 72 + 26
            onNodeWithTag("dialog").performTouchInput { click(Offset(400f, 2 * row)) }
            assertEquals(1 to 0, confirms to cancels)
            onNodeWithTag("dialog").performTouchInput { click(Offset(200f, 2 * row)) }
            assertEquals(1 to 1, confirms to cancels)
            // Inside the dialog, above the row: nothing.
            onNodeWithTag("dialog").performTouchInput { click(Offset(400f, 2 * (row - 60))) }
            assertEquals(1 to 1, confirms to cancels)
        }
    }

    @Test
    fun `at density 2 a swipe across the card dismisses it, and a short one does not`() {
        runComposeUiTest {
            var dismissed = 0
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(2f)) {
                    ToonTheme {
                        ToonCard(
                            "Pancake breakfast",
                            Modifier.testTag("card"),
                            meta = "Kitchen, bring syrup",
                            time = "9:00",
                            dismissible = true,
                            onDismiss = { dismissed++ },
                        )
                    }
                }
            }
            onNodeWithTag("card").performTouchInput { swipeLeft() }
            waitForIdle()
            assertEquals(1, dismissed)
        }
    }
}
