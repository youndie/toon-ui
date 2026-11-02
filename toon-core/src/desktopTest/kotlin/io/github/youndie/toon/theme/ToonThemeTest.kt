package io.github.youndie.toon.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** B-08: the theme provides what it was given, follows the platform's reduce motion, and counts idle slots. */
@OptIn(ExperimentalTestApi::class)
class ToonThemeTest {
    @Test
    fun `the third idle request in a theme is refused`() =
        runComposeUiTest {
            val granted = mutableListOf(false, false, false)
            var budget: ToonIdleBudget? = null
            setContent {
                ToonTheme {
                    budget = Toon.idleBudget
                    repeat(3) { i ->
                        val slot by rememberToonIdleSlot()
                        granted[i] = slot
                    }
                }
            }
            waitForIdle()
            assertEquals(listOf(true, true, false), granted)
            assertEquals(2, budget?.inUse)
        }

    @Test
    fun `a slot is given back when its owner leaves, and a refused element stays still`() =
        runComposeUiTest {
            var showFirst by mutableStateOf(true)
            val granted = mutableMapOf<String, Boolean>()
            var budget: ToonIdleBudget? = null
            setContent {
                ToonTheme {
                    budget = Toon.idleBudget
                    if (showFirst) {
                        val a by rememberToonIdleSlot()
                        granted["a"] = a
                    }
                    val b by rememberToonIdleSlot()
                    granted["b"] = b
                    val c by rememberToonIdleSlot()
                    granted["c"] = c
                }
            }
            waitForIdle()
            assertEquals(mapOf("a" to true, "b" to true, "c" to false), granted)
            showFirst = false
            waitForIdle()
            assertEquals(1, budget?.inUse)
            assertEquals(false, granted["c"])
        }

    @Test
    fun `reduced motion grants no idle slot`() =
        runComposeUiTest {
            var slot = true
            var reduced = false
            setContent {
                ToonTheme(reduceMotion = true) {
                    reduced = Toon.reduceMotion
                    slot = rememberToonIdleSlot().value
                }
            }
            waitForIdle()
            assertEquals(true, reduced)
            assertEquals(false, slot)
        }

    @Test
    fun `without an explicit setting the desktop JVM does not reduce motion`() =
        runComposeUiTest {
            var reduced = true
            setContent { ToonTheme { reduced = Toon.reduceMotion } }
            waitForIdle()
            assertEquals(false, reduced)
        }

    @Test
    fun `the theme provides the tokens and the fonts it was given`() =
        runComposeUiTest {
            var name = ""
            var stage = 0L
            var display = ""
            setContent {
                ToonTheme(ToonThemes.BrightCel) {
                    name = Toon.tokens.name
                    stage =
                        Toon.colors.stage.value
                            .toLong()
                    display = Toon.fonts.display.toString()
                }
            }
            waitForIdle()
            assertEquals("brightCel", name)
            assertEquals(
                ToonThemes.BrightCel.colors.stage.value
                    .toLong(),
                stage,
            )
            assertEquals(true, display.isNotEmpty())
        }
}
