package io.github.youndie.toon.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.inkGouacheLight
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-77: the sidebar against the desktop extension's panel `sheet`, `navPlate` and `liftBody`
 * (`engine-goldens/desktop.json`, group `Sidebar`), and what `ToonSidebar.dc.html` lays out and does:
 * where its parts and words stand, what a click and the keys select, and what the pointer draws.
 */
@OptIn(ExperimentalTestApi::class)
class ToonSidebarTest {
    @Test
    fun `the panel, the rows, the recording and the navigation rows are the extension's frames layer by layer`() {
        val cells = EngineFrames.cells("Sidebar")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                val w = o.f("w")!!
                val bool = { k: String -> o[k]?.jsonPrimitive?.booleanOrNull ?: false }
                val look = { ToonSidebarRowLook(bool("sel"), o.f("hv")!!, bool("focus"), o.f("seed")!!.toInt()) }
                val scene =
                    when (val part = o.getValue("part").jsonPrimitive.content) {
                        "panel" -> {
                            toonSidebarPanelScene(cell.tokens, w, o.f("h")!!, 2)
                        }

                        "row" -> {
                            toonSidebarRowScene(cell.tokens, w, o.f("h")!!, look())
                        }

                        "rec" -> {
                            toonSidebarRecordingScene(cell.tokens, w, o.f("pulse")!!, bool("thick"), o.f("hv")!!)
                        }

                        "nav" -> {
                            val icon =
                                if (o.getValue("icon").jsonPrimitive.content ==
                                    "person"
                                ) {
                                    ToonIcons.Person
                                } else {
                                    ToonIcons.Settings
                                }
                            toonSidebarNavScene(cell.tokens, w, icon, look())
                        }

                        else -> {
                            error("no part $part")
                        }
                    }
                EngineFrames.compare(cell, scene)
            }
        println("sidebar frames: ${cells.size} cells, worst coordinate $worst dp")
        // 24 looks in three themes.
        assertEquals(72, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the parts stand where the design's renderVals puts them`() {
        assertEquals(
            ToonSidebarBox(listTop = 62f, footH = 116f, rowW = 274f, navW = 288f),
            toonSidebarBox(300f, false, 2),
        )
        assertEquals(128f, toonSidebarBox(300f, true, 2).listTop, "a recording takes 66 dp more")
        assertEquals(ToonSidebarBox(62f, 10f, 214f, 228f), toonSidebarBox(240f, false, 0))
        assertEquals(72f, toonSidebarBox(300f, false, 1).footH)
        // The rule over the navigation rows: the design's `fy`, h − 10 − 2 · 44 − 8.
        assertEquals(594f, toonSidebarRuleY(700f, 2))
    }

    private val groups =
        listOf(
            ToonSidebarGroup(
                "Today",
                listOf(
                    ToonSidebarItem("a", "Weekly sync", "10:00 · 42 min"),
                    ToonSidebarItem("b", "Design review", "12:30 · 18 min"),
                ),
            ),
            ToonSidebarGroup("Yesterday", listOf(ToonSidebarItem("c", "Retro", "16:00 · 55 min"))),
        )
    private val nav =
        listOf(
            ToonSidebarNav("people", "People", ToonIcons.Person),
            ToonSidebarNav("settings", "Settings", ToonIcons.Settings),
        )

    private fun ComposeUiTest.bounds(text: String): DpRect = onNodeWithText(text).getBoundsInRoot()

    private fun ComposeUiTest.wordBounds(text: String): DpRect =
        onNodeWithText(text, useUnmergedTree = true).getBoundsInRoot()

    private fun near(
        want: Float,
        got: androidx.compose.ui.unit.Dp,
        what: String,
        slack: Float = .5f,
    ) = assertTrue(abs(got.value - want) <= slack, "$what: $got, want $want")

    @Test
    fun `rows, the recording and the navigation rows stand where the design puts them, the words in them`() =
        runComposeUiTest {
            // The recording's mark breathes on the frame clock: never idle, so the clock is stepped.
            mainClock.autoAdvance = false
            setContent {
                ToonTheme(inkGouacheLight) {
                    Box(Modifier.height(700.dp)) {
                        ToonSidebar(
                            groups,
                            selected = "b",
                            onSelect = {},
                            query = "",
                            onQuery = {},
                            recording = ToonSidebarRecording("rec", "Standup", "00:12:04", "Room A"),
                            nav = nav,
                        )
                    }
                }
            }
            mainClock.advanceTimeBy(100)
            val rec = bounds("Standup")
            near(6f, rec.left, "recording left")
            near(62f, rec.top, "recording top")
            near(288f, rec.right - rec.left, "recording width")
            near(60f, rec.bottom - rec.top, "recording height")
            // The list starts at 128 with the recording; a group's header is 16 + 11 + 6 tall.
            val first = bounds("Weekly sync")
            near(6f, first.left, "row left")
            near(128f + 33f, first.top, "first row top")
            near(274f, first.right - first.left, "row width")
            near(56f, first.bottom - first.top, "row height")
            near(128f + 33f + 56f, bounds("Design review").top, "second row top")
            near(128f + 33f + 112f + 33f, bounds("Retro").top, "second group's row")
            // The words: 12 dp in, the 18 + 5 + 15.6 dp column centred on the 56 dp row.
            val title = wordBounds("Weekly sync")
            near(18f, title.left, "title left")
            near(128f + 33f + (56f - 38.6f) / 2, title.top, "title top", 1f)
            near(128f + 33f + (56f - 38.6f) / 2 + 18f + 5f, wordBounds("10:00 · 42 min").top, "meta top", 1f)
            near(6f + 40f, wordBounds("Standup").left, "recording title left")
            near(6f + 40f, wordBounds("00:12:04 · Room A").left, "recording meta left")
            // The navigation rows from 10 dp above the foot, 44 dp each; their label 46 dp in.
            val people = bounds("People")
            near(700f - 10f - 88f, people.top, "first navigation row")
            near(44f, people.bottom - people.top, "navigation row height")
            near(288f, people.right - people.left, "navigation row width")
            near(700f - 10f - 44f, bounds("Settings").top, "second navigation row")
            near(6f + 46f, wordBounds("People").left, "navigation label")
        }

    @Test
    fun `results replace the groups, a result row is 80 dp, and with nothing to list the empty words stand`() =
        runComposeUiTest {
            var results by mutableStateOf<List<ToonSidebarGroup>?>(null)
            var shown by mutableStateOf(groups)
            setContent {
                ToonTheme(inkGouacheLight) {
                    Box(Modifier.height(700.dp)) {
                        ToonSidebar(
                            shown,
                            null,
                            {},
                            "sync",
                            {},
                            results = results,
                            count = "1",
                            emptyTitle = "No calls yet",
                            emptyText = "They show up here.",
                        )
                    }
                }
            }
            onNodeWithText("Weekly sync").assertExists()
            results =
                listOf(
                    ToonSidebarGroup(
                        "Results",
                        listOf(
                            ToonSidebarItem(
                                "a",
                                "Weekly sync",
                                "10:00",
                                ToonSidebarSnippet("the ", "sync", " ran long"),
                            ),
                        ),
                    ),
                )
            waitForIdle()
            onNodeWithText("Design review").assertDoesNotExist()
            val row = bounds("Weekly sync")
            near(80f, row.bottom - row.top, "result row height")
            near(62f + 33f, row.top, "result row top")
            results = null
            shown = emptyList()
            waitForIdle()
            onNodeWithText("Weekly sync").assertDoesNotExist()
            // 22 dp under the list's top, 18 dp in.
            val empty = wordBounds("No calls yet")
            near(18f, empty.left, "empty title left")
            near(62f + 22f, empty.top, "empty title top")
            onNodeWithText("They show up here.").assertExists()
        }

    @Test
    fun `a click selects a row, the recording or a navigation row`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val picked = mutableListOf<String>()
            setContent {
                ToonTheme(inkGouacheLight) {
                    Box(Modifier.height(700.dp)) {
                        ToonSidebar(
                            groups,
                            null,
                            { picked += it },
                            "",
                            {},
                            recording = ToonSidebarRecording("rec", "Standup", "00:00:03"),
                            nav = nav,
                            onNav = { picked += "nav:$it" },
                        )
                    }
                }
            }
            mainClock.advanceTimeBy(100)
            for (word in listOf("Design review", "Standup", "Settings")) {
                onNodeWithText(word).performClick()
                mainClock.advanceTimeBy(100)
            }
            assertEquals(listOf("b", "rec", "nav:settings"), picked)
        }

    @Test
    fun `Up and Down move the focus from row to row, and Enter or Space select`() =
        runComposeUiTest {
            val picked = mutableListOf<String>()
            setContent {
                ToonTheme(inkGouacheLight) {
                    Box(Modifier.height(700.dp)) {
                        ToonSidebar(groups, null, { picked += it }, "", {}, nav = nav, onNav = { picked += "nav:$it" })
                    }
                }
            }
            onNodeWithText("Weekly sync").requestFocus()
            onNodeWithText("Weekly sync").assertIsFocused()
            onNodeWithText("Weekly sync").performKeyInput { pressKey(Key.DirectionDown) }
            onNodeWithText("Design review").assertIsFocused()
            onNodeWithText("Design review").performKeyInput { pressKey(Key.Enter) }
            onNodeWithText("Design review").performKeyInput { pressKey(Key.DirectionDown) }
            onNodeWithText("Retro").assertIsFocused()
            onNodeWithText("Retro").performKeyInput { pressKey(Key.Spacebar) }
            onNodeWithText("Retro").performKeyInput { pressKey(Key.DirectionUp) }
            onNodeWithText("Design review").assertIsFocused()
            onNodeWithText("Design review").performKeyInput { pressKey(Key.DirectionUp) }
            onNodeWithText("Weekly sync").assertIsFocused()
            onNodeWithText("Weekly sync").performKeyInput { pressKey(Key.NumPadEnter) }
            onNodeWithText("Retro").requestFocus()
            onNodeWithText("Retro").performKeyInput { pressKey(Key.DirectionDown) }
            onNodeWithText("People").assertIsFocused()
            onNodeWithText("People").performKeyInput { pressKey(Key.Enter) }
            assertEquals(listOf("b", "c", "a", "nav:people"), picked)
        }

    @Test
    fun `a selected navigation row takes the list's selection away, and the recording's ink thickens`() =
        runComposeUiTest {
            var selectedNav by mutableStateOf<String?>(null)
            setContent {
                ToonTheme(inkGouacheLight) {
                    Box(Modifier.height(700.dp)) {
                        ToonSidebar(groups, "b", {}, "", {}, nav = nav, selectedNav = selectedNav)
                    }
                }
            }

            fun SemanticsNodeInteraction.isSelected() =
                fetchSemanticsNode().config.getOrNull(SemanticsProperties.Selected)
            assertEquals(true, onNodeWithText("Design review").isSelected())
            assertEquals(false, onNodeWithText("Weekly sync").isSelected())
            assertEquals(false, onNodeWithText("Settings").isSelected())
            selectedNav = "settings"
            waitForIdle()
            assertEquals(false, onNodeWithText("Design review").isSelected())
            assertEquals(true, onNodeWithText("Settings").isSelected())
            // The design thickens the recording's ink while it, or a navigation row, is selected.
            val plain = toonSidebarRecordingScene(inkGouacheLight, 288f, 0f, thick = false)
            val thick = toonSidebarRecordingScene(inkGouacheLight, 288f, 0f, thick = true)
            assertTrue(!plain.layers[2].numbers.contentEquals(thick.layers[2].numbers), "the fill is inset further")
        }

    /** Pixels where [a] and [b] differ, as (x, y). */
    private fun differ(
        a: BufferedImage,
        b: BufferedImage,
    ): List<Pair<Int, Int>> =
        buildList {
            for (y in 0 until minOf(a.height, b.height)) {
                for (x in 0 until minOf(a.width, b.width)) {
                    if (a.getRGB(x, y) !=
                        b.getRGB(x, y)
                    ) {
                        add(x to y)
                    }
                }
            }
        }

    @Test
    fun `a mouse over a row lays the plate under it, and a selected row is a plate`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var selected by mutableStateOf<String?>(null)
            setContent {
                ToonTheme(inkGouacheLight) {
                    Box(Modifier.height(700.dp).testTag("sb")) { ToonSidebar(groups, selected, {}, "", {}) }
                }
            }
            mainClock.advanceTimeBy(100)

            fun shot() = onNodeWithTag("sb").captureToImage().toAwtImage()
            val rest = shot()
            // The second row: 62 + 33 + 56 down, its plate from 3 dp in; a point clear of its words.
            val y = 62 + 33 + 56 + 50
            onNodeWithTag("sb").performMouseInput { moveTo(Offset(240f, y.toFloat())) }
            mainClock.advanceTimeBy(200)
            val hovered = shot()
            assertEquals(inkGouacheLight.colors.surfacePressed.toArgb(), hovered.getRGB(240, y) or 0xFF000000.toInt())
            assertTrue(differ(hovered, rest).all { it.second in (62 + 33 + 56)..(62 + 33 + 112) }, "only that row")
            onNodeWithTag("sb").performMouseInput { moveTo(Offset(240f, 680f)) }
            selected = "b"
            mainClock.advanceTimeBy(300)
            assertEquals(inkGouacheLight.colors.chipSelected.toArgb(), shot().getRGB(240, y) or 0xFF000000.toInt())
        }

    @Test
    fun `the scrollbar's thumb stands 6 dp from the edge, over the list`() =
        runComposeUiTest {
            var long by mutableStateOf(false)
            setContent {
                val many = ToonSidebarGroup("All", (1..40).map { ToonSidebarItem("$it", "Call $it", "$it min") })
                ToonTheme(inkGouacheLight) {
                    Box(Modifier.height(500.dp).testTag("sb")) {
                        ToonSidebar(if (long) listOf(many) else groups, null, {}, "", {}, nav = nav)
                    }
                }
            }

            fun shot() = onNodeWithTag("sb").captureToImage().toAwtImage()
            val short = shot()
            long = true
            waitForIdle()
            // Only the bar's columns: what the rows draw differs left of 274 + 6.
            val bar = differ(shot(), short).filter { it.first >= 280 }
            assertTrue(bar.isNotEmpty(), "the thumb drew")
            // The design's `scrollThumb` at rest: 7 dp, from 5 to 12 in a bar 4 dp from the edge, its
            // shadow none; the ink wobbles under a dp.
            assertTrue(bar.minOf { it.first } >= 300 - 18 + 4, "thumb from ${bar.minOf { it.first }}")
            assertTrue(bar.maxOf { it.first } <= 300 - 6 + 1, "thumb to ${bar.maxOf { it.first }}")
            assertTrue(bar.maxOf { it.first } >= 300 - 6 - 1, "the thumb reaches 6 dp from the edge: ${bar.maxOf { it.first }}")
            // From 8 dp under the list's top.
            assertTrue(bar.minOf { it.second } >= 62 + 8 - 1, "thumb top ${bar.minOf { it.second }}")
        }

    @Test
    fun `the recording's mark breathes, and holds its full size with reduced motion`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var reduce by mutableStateOf(false)
            setContent {
                ToonTheme(inkGouacheLight, reduceMotion = reduce) {
                    Box(Modifier.height(500.dp).testTag("sb")) {
                        ToonSidebar(
                            emptyList(),
                            null,
                            {},
                            "",
                            {},
                            recording = ToonSidebarRecording("r", "Live", "00:00:01"),
                        )
                    }
                }
            }

            // The paper dot's width on its middle row: 6 + 20 dp across, 62 + 30 down.
            fun markWidth(): Int {
                val shot = onNodeWithTag("sb").captureToImage().toAwtImage()
                val paper = inkGouacheLight.colors.surface.toArgb()
                return (6 until 40).count { x -> shot.getRGB(x, 92) or 0xFF000000.toInt() == paper }
            }
            mainClock.advanceTimeByFrame()
            val widths =
                (0 until 8).map {
                    mainClock.advanceTimeBy(160)
                    markWidth()
                }
            assertTrue(widths.distinct().size > 2, "the mark breathes: $widths")
            reduce = true
            mainClock.advanceTimeBy(200)
            val held = markWidth()
            mainClock.advanceTimeBy(600)
            assertEquals(held, markWidth(), "reduced motion holds the mark")
            assertTrue(held >= widths.max() - 1, "at its full size: $held against $widths")
        }

    @Test
    fun `the width is held to 240-360 dp`() =
        runComposeUiTest {
            setContent {
                ToonTheme {
                    Box(Modifier.height(500.dp)) {
                        ToonSidebar(groups, null, {}, "", {}, Modifier.testTag("narrow"), width = 100.dp)
                    }
                    Box(Modifier.height(500.dp)) {
                        ToonSidebar(groups, null, {}, "", {}, Modifier.testTag("wide"), width = 900.dp)
                    }
                }
            }
            near(240f, onNodeWithTag("narrow").getBoundsInRoot().let { it.right - it.left }, "narrow")
            near(360f, onNodeWithTag("wide").getBoundsInRoot().let { it.right - it.left }, "wide")
        }
}
