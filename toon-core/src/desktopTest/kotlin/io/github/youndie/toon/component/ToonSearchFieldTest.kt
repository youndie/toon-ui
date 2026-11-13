package io.github.youndie.toon.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.draw.ToonTextStyle
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.inkGouacheLight
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B-71: the search field against the desktop extension's `searchBox` (`engine-goldens/desktop.json`,
 * group `SearchField`), and the behaviour `ToonSearchField.dc.html` gives it: what shows when, the
 * keys, and its words where the design's HTML puts them.
 */
@OptIn(ExperimentalTestApi::class)
class ToonSearchFieldTest {
    @Test
    fun `rest, hover, focus and a value at three widths are the extension's frames layer by layer`() {
        val cells = EngineFrames.cells("SearchField")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                EngineFrames.compare(
                    cell,
                    toonSearchFieldScene(
                        cell.tokens,
                        o.f("w")!!,
                        ToonSearchFieldLook(
                            focus = o.f("focus") == 1f,
                            hv = o.f("hv")!!,
                            keyHint = o.f("kbdW") == 34f,
                        ),
                    ),
                )
            }
        println("search field frames: ${cells.size} cells, worst coordinate $worst dp")
        // Five looks at the page's width and the rest at both ends of its range, in three themes.
        assertEquals(21, cells.size)
        assertTrue(cells.all { (it.opts["ring"]?.jsonPrimitive?.booleanOrNull ?: false) == (it.opts.f("focus") == 1f) })
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the key hint shows only empty and unfocused, the count only beside a value`() {
        fun parts(
            value: String,
            focused: Boolean,
            hint: String = "⌘K",
            count: String = "3 / 12",
        ) = toonSearchFieldParts(value, focused, hint, count)
        assertEquals(
            ToonSearchFieldParts(placeholder = true, keyHint = true, count = false, countSet = true),
            parts("", false),
        )
        assertEquals(
            ToonSearchFieldParts(placeholder = true, keyHint = false, count = false, countSet = true),
            parts("", true),
        )
        assertEquals(
            ToonSearchFieldParts(placeholder = false, keyHint = false, count = true, countSet = true),
            parts("ink", false),
        )
        assertEquals(
            ToonSearchFieldParts(placeholder = false, keyHint = false, count = true, countSet = true),
            parts("ink", true),
        )
        assertFalse(parts("", false, hint = "").keyHint, "an empty hint shows nothing")
        assertFalse(parts("ink", false, count = "").count)
        // The design's `iw`: room for the hint's plate, else for a count, else 12 dp.
        assertEquals(276f - 38 - 50, parts("", false).inputWidth(276f))
        assertEquals(276f - 38 - 84, parts("ink", true).inputWidth(276f))
        assertEquals(276f - 38 - 12, parts("ink", true, count = "").inputWidth(276f))
    }

    @Test
    fun `Enter steps forward, Shift+Enter back, and Escape clears and leaves`() =
        runComposeUiTest {
            var value by mutableStateOf("ink")
            val steps = mutableListOf<Int>()
            setContent {
                ToonTheme {
                    ToonSearchField(
                        value,
                        { value = it },
                        "Search",
                        Modifier.testTag("search"),
                        count = "1 / 3",
                        onNext = { steps += it },
                    )
                }
            }
            val input = onNode(hasSetTextAction())
            input.requestFocus()
            input.assertIsFocused()
            input.performKeyInput { pressKey(Key.Enter) }
            input.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Enter) } }
            input.performKeyInput { pressKey(Key.NumPadEnter) }
            assertEquals(listOf(1, -1, 1), steps)
            assertEquals("ink", value, "Enter types nothing")
            input.performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            assertEquals("", value)
            input.assertIsNotFocused()
        }

    /** Pixels where [a] and [b] differ, as (x, y). */
    private fun differ(
        a: BufferedImage,
        b: BufferedImage,
    ): List<Pair<Int, Int>> =
        buildList {
            for (y in 0 until a.height) for (x in 0 until a.width) if (a.getRGB(x, y) != b.getRGB(x, y)) add(x to y)
        }

    /**
     * One field, unfocused at first, whose props the test sets: its looks are compared at one place,
     * since the same outline drawn lower in the window antialiases a few edge pixels differently.
     */
    private class Props(
        value: String = "",
        placeholder: String = "",
        keyHint: String = "",
        count: String = "",
    ) {
        var value by mutableStateOf(value)
        var placeholder by mutableStateOf(placeholder)
        var keyHint by mutableStateOf(keyHint)
        var count by mutableStateOf(count)
    }

    @Test
    fun `the placeholder stands where the value is typed`() =
        runComposeUiTest {
            val p = Props()
            setContent {
                ToonTheme(inkGouacheLight) {
                    ToonSearchField(p.value, {}, p.placeholder, Modifier.testTag("f"), keyHint = p.keyHint)
                }
            }

            fun shot() = onNodeWithTag("f").captureToImage().toAwtImage()
            val bare = shot()
            p.placeholder = "Поиск ink"
            val placeholder = differ(shot(), bare)
            p.placeholder = ""
            p.value = "Поиск ink"
            val typed = differ(shot(), bare)
            assertTrue(placeholder.size > 100 && typed.size > 100, "both words drew")
            // The same rows and the same columns, to a pixel: one line, two colours.
            for ((what, pick) in listOf<Pair<String, (Pair<Int, Int>) -> Int>>(
                "rows" to { it.second },
                "columns" to { it.first },
            )) {
                val pr = placeholder.minOf(pick)..placeholder.maxOf(pick)
                val tr = typed.minOf(pick)..typed.maxOf(pick)
                assertTrue(
                    kotlin.math.abs(pr.first - tr.first) <= 1 && kotlin.math.abs(pr.last - tr.last) <= 1,
                    "$what: placeholder $pr, value $tr",
                )
            }
        }

    @Test
    fun `focus takes the key hint away, and a count shows only beside a value`() =
        runComposeUiTest {
            val p = Props(placeholder = "Search", keyHint = "⌘K")
            setContent {
                ToonTheme(inkGouacheLight) {
                    ToonSearchField(
                        p.value,
                        {},
                        p.placeholder,
                        Modifier.testTag("f"),
                        count = p.count,
                        keyHint = p.keyHint,
                    )
                }
            }

            fun shot() = onNodeWithTag("f").captureToImage().toAwtImage()
            val hinted = shot()
            p.keyHint = ""
            val hintless = shot()
            assertTrue(differ(hinted, hintless).isNotEmpty(), "an empty unfocused field shows its hint")
            p.count = "3 / 12"
            assertEquals(0, differ(shot(), hintless).size, "no value, no count")
            p.value = "ink"
            val counted = shot()
            p.count = ""
            assertTrue(differ(counted, shot()).isNotEmpty(), "a value shows its count")
            p.value = ""
            p.keyHint = "⌘K"
            onNode(hasSetTextAction()).requestFocus()

            // Focused, the hint's field is the hintless one where the plate stood (the caret aside).
            fun plateSide(i: BufferedImage) = i.getSubimage(200, 0, i.width - 200, i.height)
            val focusedHinted = plateSide(shot())
            p.keyHint = ""
            assertEquals(0, differ(focusedHinted, plateSide(shot())).size, "a focused field shows no hint")
        }

    @Test
    fun `a long placeholder stops where the value would`() =
        runComposeUiTest {
            // A count set keeps 84 dp at the end: the input runs from 38 to 194 dp, its text from 40.
            val p = Props(count = "3 / 12")
            setContent {
                ToonTheme(inkGouacheLight) {
                    ToonSearchField(
                        p.value,
                        {},
                        p.placeholder,
                        Modifier.testTag("f"),
                        count = p.count,
                        keyHint = p.keyHint,
                    )
                }
            }

            fun shot() = onNodeWithTag("f").captureToImage().toAwtImage()
            val bare = shot()
            p.placeholder = "Search every recording of the whole season"
            val long = differ(shot(), bare)
            assertTrue(long.isNotEmpty())
            assertTrue(long.maxOf { it.first } < 194, "the placeholder reached ${long.maxOf { it.first }} px")
            assertTrue(long.maxOf { it.first } > 180, "it runs to the input's end")
        }

    @Test
    fun `a mouse over the field fills it, focused it keeps its fill`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                ToonTheme(
                    inkGouacheLight,
                ) { ToonSearchField("", {}, "Search", Modifier.testTag("f"), keyHint = "") }
            }
            mainClock.advanceTimeBy(100)

            fun shot() = onNodeWithTag("f").captureToImage().toAwtImage()
            val rest = shot()
            onNodeWithTag("f").performMouseInput { moveTo(Offset(240f, 20f)) }
            mainClock.advanceTimeBy(200)
            val hovered = shot()
            val fill = inkGouacheLight.colors.surfacePressed.toArgb()
            assertEquals(fill, hovered.getRGB(240, 32) or 0xFF000000.toInt(), "the hovered box is surface2")
            assertTrue(differ(hovered, rest).size > 1000, "the hover filled the box")
            onNode(hasSetTextAction()).requestFocus()
            mainClock.advanceTimeBy(100)
            assertEquals(
                inkGouacheLight.colors.field.toArgb(),
                shot().getRGB(240, 32) or 0xFF000000.toInt(),
                "focus wins over hover",
            )
        }

    @Test
    fun `the field is 40 dp tall, held to its width range, and named by its placeholder`() =
        runComposeUiTest {
            setContent {
                ToonTheme {
                    Column {
                        ToonSearchField("", {}, "Search", Modifier.testTag("narrow"), width = 100.dp)
                        ToonSearchField("", {}, "Search", Modifier.testTag("wide"), width = 800.dp)
                        ToonSearchField("ink", {}, "Search calls", Modifier.testTag("counted"), count = "2 / 5")
                    }
                }
            }
            onNodeWithTag("narrow").assertWidthIsEqualTo(160.dp).assertHeightIsEqualTo(40.dp)
            onNodeWithTag("wide").assertWidthIsEqualTo(600.dp)
            val counted =
                onNodeWithTag(
                    "counted",
                ).onChildren().filter(hasSetTextAction()).onFirst().fetchSemanticsNode()
            assertEquals(listOf("Search calls"), counted.config.getOrNull(SemanticsProperties.ContentDescription))
            assertEquals("2 / 5", counted.config.getOrNull(SemanticsProperties.StateDescription))
            val plain =
                onNodeWithTag("narrow")
                    .onChildren()
                    .filter(hasSetTextAction())
                    .onFirst()
                    .fetchSemanticsNode()
            assertNull(plain.config.getOrNull(SemanticsProperties.StateDescription), "no value, no count to read")
        }

    @Test
    fun `the count's figures are all one width`() =
        runComposeUiTest {
            var widths: List<Float> = emptyList()
            setContent {
                ToonTheme {
                    val text = rememberToonText()
                    val tabular = ToonTextStyle(600, 13f, tabular = true)
                    widths =
                        "0123456789".map {
                            text
                                .measure("$it", tabular)
                                .size.width
                                .toFloat()
                        }
                }
            }
            waitForIdle()
            assertEquals(1, widths.distinct().size, "figure widths $widths")
        }
}
