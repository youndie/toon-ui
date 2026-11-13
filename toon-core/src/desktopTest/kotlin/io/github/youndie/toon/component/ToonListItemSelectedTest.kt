package io.github.youndie.toon.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.theme.ToonThemes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B-87: the chosen row of a master–detail list. The engine's `listItem` draws `sel` as the pressed
 * surface's plate under the row; the composable now takes it, and a reader hears it.
 */
@OptIn(ExperimentalTestApi::class)
class ToonListItemSelectedTest {
    @Test
    fun `a selected row draws the engine's plate in every theme and an unselected one none`() {
        for (tokens in listOf(ToonThemes.InkGouacheLight, ToonThemes.InkGouacheNight, ToonThemes.BrightCel)) {
            val plate = tokens.colors.surfacePressed

            fun near(a: Color) =
                abs(a.red - plate.red) <= .03f && abs(a.green - plate.green) <= .03f && abs(a.blue - plate.blue) <= .03f
            runComposeUiTest {
                mainClock.autoAdvance = false
                setContent {
                    ToonTheme(tokens) {
                        Column(Modifier.padding(12.dp)) {
                            ToonListItem("Board", Modifier.testTag("on"), sub = "Five repositories", selected = true)
                            ToonListItem("Board", Modifier.testTag("off"), sub = "Five repositories")
                        }
                    }
                }
                mainClock.advanceTimeBy(500)

                // The middle of the row's right half, between the words and the chevron: the plate or nothing.
                fun plated(tag: String): Int {
                    val image = onNodeWithTag(tag).captureToImage().toPixelMap()
                    var n = 0
                    for (y in 10..62) for (x in 240..300) if (near(image[x, y])) n++
                    return n
                }
                val on = plated("on")
                val off = plated("off")
                assertTrue(on > 61 * 53 * .9, "${tokens.name}: the selected row's plate covers $on of ${61 * 53}")
                assertEquals(0, off, "${tokens.name}: an unselected row drew a plate")
            }
        }
    }

    @Test
    fun `a reader hears the selected row as selected`() {
        runComposeUiTest {
            setContent {
                ToonTheme {
                    Column {
                        ToonListItem("Board", Modifier.testTag("on"), selected = true)
                        ToonListItem("Board", Modifier.testTag("off"))
                    }
                }
            }
            assertEquals(true, onNodeWithTag("on").fetchSemanticsNode().config.getOrNull(SemanticsProperties.Selected))
            assertNull(onNodeWithTag("off").fetchSemanticsNode().config.getOrNull(SemanticsProperties.Selected))
        }
    }
}
