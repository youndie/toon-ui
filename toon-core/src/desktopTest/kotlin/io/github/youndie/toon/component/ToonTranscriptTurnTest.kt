package io.github.youndie.toon.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.inkGouacheLight
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * B-78: the transcript turn against `ToonTranscriptTurn.dc.html` — its drawing against the
 * extension's frames (`engine-goldens/desktop.json`, group `TranscriptTurn`), its words where the
 * page's CSS puts them, its `vis()`, its highlighting, and every action by the mouse and the keys.
 */
@OptIn(ExperimentalTestApi::class)
class ToonTranscriptTurnTest {
    private val design = "Тогда давайте так. Релиз не в четверг. Переносим на вторник, семнадцатое."

    @Test
    fun `sixteen looks in three themes are the extension's frames layer by layer`() {
        val cells = EngineFrames.cells("TranscriptTurn")
        val icons = listOf(ToonIcons.Play, ToonIcons.Copy, ToonIcons.More)
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                val acts = o.getValue("acts").jsonPrimitive.int == 1
                val hv = o.getValue("hv").jsonArray.map { it.jsonPrimitive.float }
                val p = o.getValue("p").jsonArray.map { it.jsonPrimitive.float }
                val audio = o.getValue("audio").jsonPrimitive.int == 1
                EngineFrames.compare(
                    cell,
                    toonTranscriptTurnScene(
                        cell.tokens,
                        ToonTurnLook(
                            w = o.f("w")!!,
                            h = o.f("h")!!,
                            kind =
                                ToonSpeakerKind.valueOf(
                                    o
                                        .getValue("kind")
                                        .jsonPrimitive.content
                                        .replaceFirstChar { it.uppercase() },
                                ),
                            shape = o.getValue("shape").jsonPrimitive.int,
                            color = o.getValue("color").jsonPrimitive.int,
                            letter = o.getValue("letter").jsonPrimitive.content,
                            seed = o.getValue("seed").jsonPrimitive.int,
                            plate = o.getValue("plate").jsonPrimitive.int == 1,
                            ring = o.getValue("ring").jsonPrimitive.int == 1,
                            micX = o["micX"]?.jsonPrimitive?.floatOrNull,
                            waveX = o["waveX"]?.jsonPrimitive?.floatOrNull,
                            actions =
                                if (!acts) {
                                    null
                                } else {
                                    icons.mapIndexed { i, g ->
                                        ToonIconButton32Look(g, hv[i], p[i], disabled = i == 0 && !audio, seed = i)
                                    }
                                },
                        ),
                    ),
                )
            }
        println("transcript turn frames: ${cells.size} cells, worst coordinate $worst dp")
        assertEquals(48, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the text measure is the extension's textMax`() {
        val manifest =
            Json
                .parseToJsonElement(javaClass.getResource("/engine-goldens/manifest.json")!!.readText())
                .jsonObject
        val layout =
            manifest
                .getValue("desktopTokens")
                .jsonObject
                .getValue("layout")
                .jsonObject
        assertEquals(layout.f("textMax"), TEXT_MAX)
        assertEquals(layout.f("gutter"), GUTTER)
    }

    @Test
    fun `the plate, the ring and the actions show as the design's vis() says`() {
        fun v(
            hovered: Boolean = false,
            focused: Boolean = false,
            menu: Boolean = false,
            playing: Boolean = false,
            found: Boolean = false,
        ) = toonTurnVisibility(hovered, focused, menu, playing, found)
        assertEquals(ToonTurnVisibility(plate = false, ring = false, actions = false), v())
        assertEquals(ToonTurnVisibility(plate = true, ring = false, actions = true), v(hovered = true))
        assertEquals(ToonTurnVisibility(plate = true, ring = true, actions = true), v(focused = true))
        assertEquals(ToonTurnVisibility(plate = true, ring = false, actions = true), v(focused = true, menu = true))
        assertEquals(ToonTurnVisibility(plate = true, ring = false, actions = false), v(playing = true))
        assertEquals(ToonTurnVisibility(plate = true, ring = false, actions = false), v(found = true))
        assertEquals(ToonTurnVisibility(plate = true, ring = true, actions = false), v(focused = true, playing = true))
        assertEquals(ToonTurnVisibility(plate = true, ring = false, actions = true), v(hovered = true, playing = true))
    }

    @Test
    fun `the header's mic, wave, time and link follow the name 10 dp apart`() {
        val plain = toonTurnHeaderRow(timeW = 30f, linkW = null, me = false, playing = false)
        assertEquals(ToonTurnHeaderRow(null, null, 10f, null, 40f), plain)
        // The mic is pulled 4 dp back toward the name.
        val me = toonTurnHeaderRow(timeW = 30f, linkW = null, me = true, playing = true)
        assertEquals(ToonTurnHeaderRow(6f, 32f, 60f, null, 90f), me)
        val unnamed = toonTurnHeaderRow(timeW = 30f, linkW = 100f, me = false, playing = false)
        assertEquals(ToonTurnHeaderRow(null, null, 10f, 50f, 150f), unnamed)
    }

    @Test
    fun `a match runs to the end of its word, ignoring case, numbered through the paragraphs`() {
        val marks = toonTurnMarks(listOf("Релиз в четверг; релиз-кандидат", "Ещё РЕЛИЗЫ"), "рел", 1)
        assertEquals(
            listOf(
                ToonTurnMark(0, 0, 5, false),
                ToonTurnMark(0, 17, 31, true),
                ToonTurnMark(1, 4, 10, false),
            ),
            marks,
        )
        assertEquals(emptyList(), toonTurnMarks(listOf("abc"), "", 0), "no query, no marks")
        // A query ending mid-word still covers itself; one starting with a space ends.
        assertEquals(listOf(ToonTurnMark(0, 2, 5, true)), toonTurnMarks(listOf("a b c d"), "b c", 0))
        assertEquals(
            listOf(ToonTurnMark(0, 1, 2, false), ToonTurnMark(0, 3, 4, false)),
            toonTurnMarks(listOf("a b c"), " ", -1),
        )
        assertEquals(
            listOf(ToonTurnMark(0, 0, 4, false)),
            toonTurnMarks(listOf("x2½y z"), "x", 5),
            "figures continue a word",
        )
    }

    @Test
    fun `the menu plays only with audio, and names an unnamed voice where it renames a named one`() {
        assertEquals(
            listOf(ToonTurnAction.Play, ToonTurnAction.Copy, ToonTurnAction.Reassign, ToonTurnAction.Rename),
            toonTurnMenuActions(ToonSpeakerKind.Named, audio = true),
        )
        assertEquals(
            listOf(ToonTurnAction.Copy, ToonTurnAction.Reassign, ToonTurnAction.Name),
            toonTurnMenuActions(ToonSpeakerKind.Unnamed, audio = false),
        )
    }

    private fun SemanticsNodeInteraction.left() = getUnclippedBoundsInRoot().left

    private fun SemanticsNodeInteraction.top() = getUnclippedBoundsInRoot().top

    private fun near(
        expected: Dp,
        actual: Dp,
        what: String,
        tolerance: Float = .5f,
    ) = assertTrue(abs(expected.value - actual.value) <= tolerance, "$what: expected $expected, was $actual")

    @Test
    fun `the words stand where the page's CSS puts them`() =
        runComposeUiTest {
            setContent {
                ToonTheme(inkGouacheLight) {
                    ToonTranscriptTurn("Марина", listOf(design), Modifier.testTag("turn").width(760.dp), time = "12:04")
                }
            }
            val turn = onNodeWithTag("turn").getUnclippedBoundsInRoot()
            val text = onNodeWithText(design).getUnclippedBoundsInRoot()
            // The grid's 44 dp column after 12 of padding; 10 down, the 32 dp header and 4 more.
            near(56.dp, text.left, "text left")
            near(46.dp, text.top, "text top")
            // One line of 16/1.6 and 12 of padding under it.
            near(46.dp + 25.6.dp + 12.dp, turn.bottom, "turn height", 1f)
            val name = onNodeWithText("Марина").getUnclippedBoundsInRoot()
            near(56.dp, name.left, "name left")
            near(10.dp + 16.dp, (name.top + name.bottom) / 2, "name centred on the header")
            val time = onNodeWithText("12:04").getUnclippedBoundsInRoot()
            near(name.right + 10.dp, time.left, "time 10 dp after the name")
            near(10.dp + 16.dp, (time.top + time.bottom) / 2, "time centred on the header")
        }

    @Test
    fun `the text is at most 640 dp wide, wraps 1,6 apart, and paragraphs stand 4 dp apart`() =
        runComposeUiTest {
            val long = List(6) { design }.joinToString(" ")
            setContent {
                ToonTheme(inkGouacheLight) {
                    ToonTranscriptTurn("Марина", listOf(long, "Второй."), Modifier.testTag("turn").width(1000.dp))
                }
            }
            val first = onNodeWithText(long).getUnclippedBoundsInRoot()
            assertTrue(first.right - first.left <= 640.dp, "measure ${first.right - first.left}")
            assertTrue(first.right - first.left > 600.dp, "it fills the measure: ${first.right - first.left}")
            // Skia sets each line on whole pixels: 25.6 is 26 px at this density, half a pixel at most.
            val h = (first.bottom - first.top).value
            val lines = Math.round(h / 25.6f)
            assertTrue(lines >= 3 && abs(h - lines * 25.6f) <= lines * .5f, "$lines lines of 25.6 dp: $h dp")
            val second = onNodeWithText("Второй.").getUnclippedBoundsInRoot()
            near(first.bottom + 4.dp, second.top, "4 dp between paragraphs")
            near(second.bottom + 12.dp, onNodeWithTag("turn").getUnclippedBoundsInRoot().bottom, "12 dp under the text")
        }

    @Test
    fun `a mouse shows the actions at once at the top end, a finger does not`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                ToonTheme(inkGouacheLight) {
                    ToonTranscriptTurn("Марина", listOf(design), Modifier.testTag("turn").width(760.dp), time = "12:04")
                }
            }
            mainClock.advanceTimeBy(100)
            val turn = onNodeWithTag("turn")
            // A finger that lands and drags across the turn hovers nothing.
            turn.performTouchInput {
                down(Offset(300f, 60f))
                moveBy(Offset(40f, 0f))
            }
            mainClock.advanceTimeBy(100)
            onNodeWithContentDescription("Copy").assertDoesNotExist()
            turn.performTouchInput { up() }
            turn.performMouseInput { moveTo(Offset(300f, 60f)) }
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            val more = onNodeWithContentDescription("More").getUnclippedBoundsInRoot()
            near(760.dp - 10.dp, more.right, "10 dp from the end")
            near(10.dp, more.top, "10 dp from the top")
            near(760.dp - 118.dp, onNodeWithContentDescription("Play from here").left(), "play first, 6 apart")
            // The reading zone's plate is instant: full on the first frame, no ramp.
            val shot = turn.captureToImage().toAwtImage()
            assertEquals(
                inkGouacheLight.colors.surfacePressed.toArgb(),
                shot.getRGB(380, 6) or 0xFF000000.toInt(),
                "the plate",
            )
            turn.performMouseInput { moveTo(Offset(-50f, -50f)) }
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            onNodeWithContentDescription("Copy").assertDoesNotExist()
        }

    @Test
    fun `the header keeps clear of the actions, the name ending first`() =
        runComposeUiTest {
            val long = "Марина Александровна Константинопольская-Преображенская из отдела продаж"
            setContent {
                ToonTheme(inkGouacheLight) {
                    ToonTranscriptTurn(long, listOf(design), Modifier.testTag("turn").width(420.dp), time = "12:04")
                }
            }
            onNodeWithTag("turn").performMouseInput { moveTo(Offset(200f, 60f)) }
            waitForIdle()
            val time = onNodeWithText("12:04").getUnclippedBoundsInRoot()
            assertTrue(time.right <= 420.dp - 12.dp - 120.dp + .5.dp, "time ends at ${time.right}")
            assertTrue(time.right - time.left > 20.dp, "the time keeps its room")
        }

    private fun turnNode(rule: androidx.compose.ui.test.ComposeUiTest) =
        rule.onNodeWithContentDescription("Марина, 12:04")

    @Test
    fun `Space plays, Cmd or Ctrl+C copies, and without audio Space does nothing`() =
        runComposeUiTest {
            var audio by mutableStateOf(true)
            val calls = mutableListOf<ToonTurnAction>()
            setContent {
                ToonTheme(inkGouacheLight) {
                    ToonTranscriptTurn(
                        "Марина",
                        listOf(
                            design,
                        ),
                        Modifier.width(760.dp),
                        time = "12:04",
                        audio = audio,
                        onAction = {
                            calls +=
                                it
                        },
                    )
                }
            }
            val turn = turnNode(this)
            turn.requestFocus()
            turn.performKeyInput { pressKey(Key.Spacebar) }
            turn.performKeyInput { withKeyDown(Key.MetaLeft) { pressKey(Key.C) } }
            turn.performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.C) } }
            turn.performKeyInput { pressKey(Key.C) }
            audio = false
            waitForIdle()
            turn.performKeyInput { pressKey(Key.Spacebar) }
            assertEquals(listOf(ToonTurnAction.Play, ToonTurnAction.Copy, ToonTurnAction.Copy), calls)
        }

    /** The open menu: the node with the menu's actions that is not the turn. */
    private val menuNode =
        SemanticsMatcher("the open menu") {
            it.config.getOrNull(SemanticsActions.CustomActions)?.any { a -> a.label == "Reassign turn" } == true &&
                it.config.getOrNull(SemanticsProperties.ContentDescription) == null
        }

    @Test
    fun `Shift+F10 opens the menu, the arrows and Enter pick from it, Escape closes it and focus comes back`() =
        runComposeUiTest {
            val calls = mutableListOf<ToonTurnAction>()
            setContent {
                ToonTheme(inkGouacheLight) {
                    ToonTranscriptTurn("Марина", listOf(design), Modifier.width(760.dp), time = "12:04", onAction = {
                        calls +=
                            it
                    })
                }
            }
            val turn = turnNode(this)
            turn.requestFocus()
            turn.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.F10) } }
            waitForIdle()
            onNode(menuNode).assertIsFocused()
            onNode(menuNode).performKeyInput {
                pressKey(Key.DirectionDown)
                pressKey(Key.DirectionDown)
                pressKey(Key.Enter)
            }
            waitForIdle()
            assertEquals(listOf(ToonTurnAction.Reassign), calls)
            onNode(menuNode).assertDoesNotExist()
            turn.assertIsFocused()
            turn.performKeyInput { pressKey(Key.Menu) }
            waitForIdle()
            onNode(menuNode).performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            onNode(menuNode).assertDoesNotExist()
            turn.assertIsFocused()
            assertEquals(listOf(ToonTurnAction.Reassign), calls, "Escape picks nothing")
        }

    @Test
    fun `the arrows move to the turn below and above`() =
        runComposeUiTest {
            setContent {
                ToonTheme(inkGouacheLight) {
                    Column {
                        ToonTranscriptTurn("Марина", listOf(design), Modifier.width(760.dp), time = "12:04")
                        ToonTranscriptTurn("Олег", listOf(design), Modifier.width(760.dp), time = "12:05")
                    }
                }
            }
            val first = onNodeWithContentDescription("Марина, 12:04")
            val second = onNodeWithContentDescription("Олег, 12:05")
            first.requestFocus()
            first.performKeyInput { pressKey(Key.DirectionDown) }
            second.assertIsFocused()
            second.performKeyInput { pressKey(Key.DirectionUp) }
            first.assertIsFocused()
        }

    @Test
    fun `the mouse reaches every action - the buttons, the more menu, a right click and the naming link`() =
        runComposeUiTest {
            val calls = mutableListOf<ToonTurnAction>()
            setContent {
                ToonTheme(inkGouacheLight) {
                    Column {
                        ToonTranscriptTurn(
                            "Марина",
                            listOf(
                                design,
                            ),
                            Modifier.testTag("turn").width(760.dp),
                            time = "12:04",
                            onAction = {
                                calls +=
                                    it
                            },
                        )
                        ToonTranscriptTurn(
                            "Спикер 2",
                            listOf(design),
                            Modifier.width(760.dp),
                            letter = "2",
                            kind = ToonSpeakerKind.Unnamed,
                            onAction = { calls += it },
                        )
                    }
                }
            }
            val turn = onNodeWithTag("turn")
            turn.performMouseInput { moveTo(Offset(300f, 60f)) }
            waitForIdle()
            onNodeWithContentDescription("Play from here").performMouseInput { click() }
            onNodeWithContentDescription("Copy").performMouseInput { click() }
            onNodeWithContentDescription("More").performMouseInput { click() }
            waitForIdle()
            onNode(menuNode).performMouseInput { click(Offset(100f, 14f + 44f * 3 + 22f)) }
            waitForIdle()
            onNode(menuNode).assertDoesNotExist()
            turn.performMouseInput { rightClick(Offset(300f, 60f)) }
            waitForIdle()
            onNode(menuNode).performMouseInput { click(Offset(100f, 14f + 44f * 2 + 22f)) }
            waitForIdle()
            onNodeWithText("Name this voice").performClick()
            assertEquals(
                listOf(
                    ToonTurnAction.Play,
                    ToonTurnAction.Copy,
                    ToonTurnAction.Rename,
                    ToonTurnAction.Reassign,
                    ToonTurnAction.Name,
                ),
                calls,
            )
        }

    @Test
    fun `without audio the play button is disabled and does nothing`() =
        runComposeUiTest {
            val calls = mutableListOf<ToonTurnAction>()
            setContent {
                ToonTheme(inkGouacheLight) {
                    ToonTranscriptTurn(
                        "Марина",
                        listOf(
                            design,
                        ),
                        Modifier.testTag("turn").width(760.dp),
                        audio = false,
                        onAction = {
                            calls +=
                                it
                        },
                    )
                }
            }
            onNodeWithTag("turn").performMouseInput { moveTo(Offset(300f, 60f)) }
            waitForIdle()
            val play = onNodeWithContentDescription("Play from here")
            assertTrue(play.fetchSemanticsNode().config.contains(SemanticsProperties.Disabled))
            play.performMouseInput { click() }
            assertEquals(emptyList(), calls)
        }

    private fun count(
        img: BufferedImage,
        c: Color,
    ): Int {
        val argb = c.toArgb() and 0xFFFFFF
        var n = 0
        for (y in 0 until img.height) for (x in 0 until img.width) if ((img.getRGB(x, y) and 0xFFFFFF) == argb) n++
        return n
    }

    private fun rows(
        img: BufferedImage,
        c: Color,
        x: Int,
    ): IntRange {
        val argb = c.toArgb() and 0xFFFFFF
        val ys = (0 until img.height).filter { (img.getRGB(x, it) and 0xFFFFFF) == argb }
        return ys.min()..ys.max()
    }

    @Test
    fun `a match lies on the accent, the current one on primary ringed in ink, as tall as the face and 2 dp more`() =
        runComposeUiTest {
            var current by mutableStateOf(-1)
            setContent {
                ToonTheme(inkGouacheLight) {
                    ToonTranscriptTurn(
                        "Марина",
                        listOf(design),
                        Modifier.testTag("turn").width(760.dp),
                        mark = "релиз",
                        currentMark = current,
                    )
                }
            }
            val tokens: ToonTokens = inkGouacheLight
            val accent = accentsOf(tokens)[1]
            val shot = onNodeWithTag("turn").captureToImage().toAwtImage()
            assertTrue(count(shot, accent) > 300, "the match lies on the accent")
            assertEquals(0, count(shot, tokens.colors.primary), "no current match")
            // The face's ascent and descent at 16 and the 2 dp spread: about 24 of the line's 25.6.
            val text = onNodeWithText(design).getUnclippedBoundsInRoot()
            val x = (text.left.value + 140).toInt()
            val col =
                (0 until 760).first {
                    (shot.getRGB(it, (text.top.value + 13).toInt()) and 0xFFFFFF) ==
                        (accent.toArgb() and 0xFFFFFF)
                }
            val tall = rows(shot, accent, col + 3)
            assertTrue(tall.last - tall.first + 1 in 20..27, "the highlight is ${tall.last - tall.first + 1} px tall")
            assertTrue(x > 0)
            current = 0
            waitForIdle()
            val now = onNodeWithTag("turn").captureToImage().toAwtImage()
            assertTrue(count(now, tokens.colors.primary) > 300, "the current match lies on primary")
            assertEquals(0, count(now, accent), "the only match is current")
            val ink = rows(now, tokens.colors.ink, col + 3)
            val fill = rows(now, tokens.colors.primary, col + 3)
            assertTrue(ink.first <= fill.first - 2 && ink.last >= fill.last + 2, "ink rings it: ink $ink, fill $fill")
            // Holding the current match lays the turn on its plate.
            assertEquals(tokens.colors.surfacePressed.toArgb(), now.getRGB(380, 6) or 0xFF000000.toInt(), "the plate")
        }

    @Test
    fun `a selection is drawn in chipSelected, never in a match's colour`() =
        runComposeUiTest {
            setContent {
                ToonTheme(inkGouacheLight) {
                    SelectionContainer {
                        ToonTranscriptTurn("Марина", listOf(design), Modifier.testTag("turn").width(760.dp))
                    }
                }
            }
            val tokens: ToonTokens = inkGouacheLight
            // The sticker's first accent is chipSelected's colour in this theme: count what the selection adds.
            val before = count(onNodeWithTag("turn").captureToImage().toAwtImage(), tokens.colors.chipSelected)
            onNodeWithText(design).performMouseInput { doubleClick(Offset(30f, 12f)) }
            waitForIdle()
            val shot = onNodeWithTag("turn").captureToImage().toAwtImage()
            assertTrue(count(shot, tokens.colors.chipSelected) > before + 100, "the selected word lies on chipSelected")
            assertFalse(tokens.colors.chipSelected == accentsOf(tokens)[1], "never a match's colour")
        }

    @Test
    fun `the user's turn wears the mic, the playing turn a bold time and the wave`() =
        runComposeUiTest {
            var playing by mutableStateOf(false)
            setContent {
                ToonTheme(inkGouacheLight) {
                    ToonTranscriptTurn(
                        "Я",
                        listOf(design),
                        Modifier.testTag("turn").width(760.dp),
                        kind = ToonSpeakerKind.Me,
                        time = "12:04",
                        playing = playing,
                    )
                }
            }
            val name = onNodeWithText("Я").getUnclippedBoundsInRoot()
            val time = onNodeWithText("12:04")
            // The mic: 10 dp, less the 4 it is pulled back, then 16 and 10 more.
            near(name.right + 6.dp + 16.dp + 10.dp, time.left(), "the time after the mic")
            val restW = time.getUnclippedBoundsInRoot().let { it.right - it.left }
            playing = true
            waitForIdle()
            near(name.right + 6.dp + 16.dp + 10.dp + 18.dp + 10.dp, time.left(), "the time after the wave")
            val boldW = time.getUnclippedBoundsInRoot().let { it.right - it.left }
            assertTrue(boldW > restW, "extra-bold is wider: $restW, $boldW")
        }
}
