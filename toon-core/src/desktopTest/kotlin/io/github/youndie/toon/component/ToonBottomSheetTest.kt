package io.github.youndie.toon.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.parity.SheetFrames
import io.github.youndie.toon.theme.ToonTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B-38: every v1 page bottom sheet cell and its open strip against the engine's frames, `sheetMove`
 * at every millisecond both ways, reduce motion, and choosing a row or closing by a release, by the
 * keyboard and by a screen reader.
 */
@OptIn(ExperimentalTestApi::class)
class ToonBottomSheetTest {
    private val items = listOf(ToonSheetItem("Family", ToonIcons.Person), ToonSheetItem("Copy link", ToonIcons.Share))

    @Test
    fun `every bottom sheet cell and open frame is the engine's`() {
        val cells = EngineFrames.cells("BottomSheet")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                val scene =
                    if (o.getValue("kind").jsonPrimitive.content == "open") {
                        SheetFrames.open(cell.tokens, o.f("t")!!)
                    } else {
                        SheetFrames.scene(cell.tokens, ToonSheetMotion(o.f("dy")!!, o.f("scrim")!!))
                    }
                EngineFrames.compare(cell, scene)
            }
        println("sheet frames: ${cells.size} cells, worst coordinate $worst dp")
        assertEquals(21, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the kit's own scene, its rows on the sheet, is the engine's at rest and opening`() {
        val all =
            listOf(
                ToonSheetItem("Family", ToonIcons.Person),
                ToonSheetItem("Copy link", ToonIcons.Share),
                ToonSheetItem("Saved", ToonIcons.Star),
                ToonSheetItem("Plans", ToonIcons.Calendar),
            )
        val cells = EngineFrames.cells("BottomSheetKit")
        val worst =
            cells.maxOf { cell ->
                val n =
                    cell.opts
                        .getValue("n")
                        .jsonPrimitive.content
                        .toInt()
                val t = cell.opts.f("t")
                val m =
                    ToonSheetMoment(
                        if (t ==
                            null
                        ) {
                            ToonSheetMotion()
                        } else {
                            toonSheetMove(t, true, toonSheetHeight(520f, n))
                        },
                    )
                EngineFrames.compare(cell, toonSheetLayerScene(cell.tokens, 360f, 520f, "Share plan", all.take(n), m))
            }
        println("kit sheet frames: ${cells.size} cells, worst coordinate $worst dp")
        assertEquals(12, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the sheet follows the engine's curve at every millisecond, up and down`() {
        val curves =
            Json
                .parseToJsonElement(
                    checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText(),
                ).jsonObject
        var worst = 0f
        for (open in listOf(true, false)) {
            val c = curves.getValue("sheetMove(t, $open, 260)").jsonObject

            fun col(k: String) = c.getValue(k).jsonArray.map { it.jsonPrimitive.content.toFloat() }
            val at = col("at")
            val dy = col("dy")
            val scrim = col("scrim")
            for (i in at.indices) {
                val m = toonSheetMove(at[i], open, 260f)
                worst = maxOf(worst, abs(dy[i] - m.dy), abs(scrim[i] - m.scrim))
            }
        }
        println("sheetMove: worst $worst")
        // dy reaches 286, where a Float's step is 3·10⁻⁵: six decimals and Float, as elsewhere.
        assertTrue(worst <= 6e-5f, "sheetMove off the engine by $worst")
    }

    @Test
    fun `the kit draws a close for 260 ms, and under reduce motion the sheet fades at rest in 120`() {
        assertEquals(ToonSheetMoment(ToonSheetMotion()), toonSheetFrameAt(true, null, false, 254f))
        assertNull(toonSheetFrameAt(false, null, false, 254f))
        assertEquals(ToonSheetMoment(toonSheetMove(90f, true, 254f)), toonSheetFrameAt(true, 90f, false, 254f))
        assertEquals(ToonSheetMoment(toonSheetMove(260f, false, 254f)), toonSheetFrameAt(false, 260f, false, 254f))
        assertNull(toonSheetFrameAt(false, 261f, false, 254f))
        assertEquals(ToonSheetMoment(ToonSheetMotion(), .5f), toonSheetFrameAt(true, 60f, true, 254f))
        assertEquals(ToonSheetMoment(ToonSheetMotion(), .5f), toonSheetFrameAt(false, 60f, true, 254f))
        assertNull(toonSheetFrameAt(false, 120f, true, 254f))
        assertEquals(ToonSheetMoment(ToonSheetMotion(), 1f), toonSheetFrameAt(true, 300f, true, 254f))
    }

    @Test
    fun `the open it mounts with plays 600 ms and a change either way 400, then the sheet is at rest`() {
        val clock = ToonSheetClock(true)
        assertEquals(0f, clock.age())
        clock.tick(1000)
        clock.tick(1599)
        assertEquals(599f, clock.age())
        clock.tick(1600)
        assertNull(clock.age())
        for (open in listOf(false, true)) {
            clock.changed(open)
            clock.tick(2000)
            clock.tick(2399)
            assertEquals(399f, clock.age())
            clock.tick(2400)
            assertNull(clock.age())
        }
        assertNull(ToonSheetClock(false).age())
    }

    @Test
    fun `the sheet is as tall as its rows and leaves 80 dp above, and a release finds its row`() {
        assertEquals(254f, toonSheetHeight(520f, 2))
        assertEquals(320f, toonSheetHeight(400f, 5))
        // 520 tall, two rows: the sheet's top at 266, its rows from 336 and 408.
        assertEquals(ToonSheetHit.Close, toonSheetHit(520f, 2, 249.9f))
        assertNull(toonSheetHit(520f, 2, 250f))
        assertNull(toonSheetHit(520f, 2, 335.9f))
        assertEquals(ToonSheetHit.Item(0), toonSheetHit(520f, 2, 336f))
        assertEquals(ToonSheetHit.Item(1), toonSheetHit(520f, 2, 408f))
        assertNull(toonSheetHit(520f, 2, 480f))
    }

    @Test
    fun `a release on a row chooses it, above the tongue closes, and a closed sheet takes nothing`() {
        runComposeUiTest {
            val heard = mutableListOf<String>()
            var open by mutableStateOf(true)
            setContent {
                ToonTheme {
                    Box(Modifier.size(360.dp, 520.dp)) {
                        ToonBottomSheet(
                            items,
                            Modifier.testTag("sheet"),
                            open = open,
                            onSelect = { heard += "select $it" },
                            onClose = { heard += "close" },
                        )
                    }
                }
            }
            val sheet = onNodeWithTag("sheet")
            sheet.performTouchInput { click(Offset(180f, 420f)) }
            sheet.performTouchInput { click(Offset(180f, 100f)) }
            sheet.performTouchInput { click(Offset(180f, 300f)) }
            assertEquals(listOf("select 1", "close"), heard)
            open = false
            waitForIdle()
            sheet.performTouchInput { click(Offset(180f, 100f)) }
            assertEquals(listOf("select 1", "close"), heard, "closed, it takes no input")
        }
    }

    @Test
    fun `the keyboard walks the rows, chooses and closes, and a screen reader can do both`() {
        runComposeUiTest {
            val heard = mutableListOf<String>()
            setContent {
                ToonTheme {
                    ToonBottomSheet(items, Modifier.testTag("sheet"), onSelect = { heard += "select $it" }, onClose = {
                        heard +=
                            "close"
                    })
                }
            }
            val sheet = onNodeWithTag("sheet")
            sheet.performSemanticsAction(SemanticsActions.RequestFocus)
            sheet.performKeyInput {
                pressKey(Key.Enter)
                pressKey(Key.DirectionDown)
                pressKey(Key.DirectionDown)
                pressKey(Key.Spacebar)
                pressKey(Key.DirectionUp)
                pressKey(Key.Enter)
                pressKey(Key.Escape)
            }
            assertEquals(listOf("select 0", "select 1", "select 0", "close"), heard)
            val config = sheet.fetchSemanticsNode().config
            assertEquals("Share plan", config.getOrNull(SemanticsProperties.PaneTitle))
            assertEquals(
                listOf("Family", "Copy link"),
                config.getOrNull(SemanticsActions.CustomActions).orEmpty().map { it.label },
            )
            sheet.performSemanticsAction(SemanticsActions.Dismiss)
            assertEquals("close", heard.last())
        }
    }
}
