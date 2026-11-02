package io.github.youndie.toon.type

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-02: the bundled faces load and the variable `wght` setting reaches the renderer. Measured, not
 * photographed: a face that failed to load falls back to the platform font, and its width gives it
 * away without a golden.
 */
@OptIn(ExperimentalTestApi::class)
class ToonFontFamiliesTest {
    private val sample = "Pancake breakfast"

    private fun widths(families: @Composable () -> Map<String, Pair<FontFamily, FontWeight>>): Map<String, Float> {
        val out = mutableMapOf<String, Float>()
        runComposeUiTest {
            setContent {
                val measurer = rememberTextMeasurer()
                families().forEach { (name, face) ->
                    val style = TextStyle(fontFamily = face.first, fontWeight = face.second, fontSize = 16.sp)
                    out[name] =
                        measurer
                            .measure(sample, style)
                            .size.width
                            .toFloat()
                }
            }
            waitForIdle()
        }
        return out
    }

    @Test
    fun `every bundled face measures differently from the platform fallback`() {
        val w =
            widths {
                val ink = inkAndGouacheFonts()
                val cel = brightCelFonts()
                mapOf(
                    "default" to (FontFamily.Default to FontWeight.Normal),
                    "Shrikhand" to (ink.display to FontWeight.Normal),
                    "Figtree" to (ink.body to FontWeight.Normal),
                    "Titan One" to (cel.display to FontWeight.Normal),
                    "Fredoka" to (cel.body to FontWeight.Normal),
                )
            }
        val fallback = w.getValue("default")
        w.filterKeys { it != "default" }.forEach { (name, width) ->
            assertTrue(width != fallback, "$name measured $width, the same as the fallback: it did not load")
        }
        assertTrue(w.values.toSet().size == w.size, "two faces measured alike: $w")
    }

    @Test
    fun `the wght setting reaches the variable body faces`() {
        val w =
            widths {
                val ink = inkAndGouacheFonts()
                val cel = brightCelFonts()
                mapOf(
                    "Figtree 400" to (ink.body to FontWeight(400)),
                    "Figtree 800" to (ink.body to FontWeight(800)),
                    "Fredoka 400" to (cel.body to FontWeight(400)),
                    "Fredoka 700" to (cel.body to FontWeight(700)),
                )
            }
        assertTrue(w.getValue("Figtree 800") > w.getValue("Figtree 400"), "Figtree did not get bolder: $w")
        assertTrue(w.getValue("Fredoka 700") > w.getValue("Fredoka 400"), "Fredoka did not get bolder: $w")
    }
}
