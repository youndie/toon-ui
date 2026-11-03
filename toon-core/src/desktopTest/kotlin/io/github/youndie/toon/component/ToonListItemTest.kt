package io.github.youndie.toon.component

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.theme.ToonTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * B-22: every MVP page list item cell and press frame, and every divider cell, against the engine's
 * `listItem` and `divider`; `listPress` at every millisecond; the row under a click.
 */
@OptIn(ExperimentalTestApi::class)
class ToonListItemTest {
    @Test
    fun `every list item cell and press frame is the engine's`() {
        val trailing =
            mapOf(
                "chevron" to ToonListTrailing.Chevron,
                "switch" to ToonListTrailing.Switch,
                "check" to ToonListTrailing.Check,
            )
        // The page's cells, and the probes at sags no page frame shows (scripts/component-cells.mjs).
        val cells = EngineFrames.cells("ListItem") + EngineFrames.cells("ListItemProbe")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts

                fun flag(k: String) = o[k]?.jsonPrimitive?.booleanOrNull ?: false
                val look =
                    ToonListItemLook(
                        on = flag("on"),
                        sag = o.f("sag") ?: 0f,
                        selected = flag("sel"),
                        seed = o["seed"]?.jsonPrimitive?.int ?: 0,
                        disabled = flag("disabled"),
                        focus = flag("focus"),
                    )
                val icon = ToonIcons.all.single { it.name == o.getValue("icon").jsonPrimitive.content }
                val scene =
                    toonListItemScene(
                        cell.tokens,
                        342f,
                        icon,
                        o.getValue("title").jsonPrimitive.content,
                        o["sub"]?.jsonPrimitive?.content ?: "",
                        trailing.getValue(o.getValue("trailing").jsonPrimitive.content),
                        look,
                    )
                EngineFrames.compare(cell, scene)
            }
        println("list item frames: ${cells.size} cells, worst coordinate $worst dp")
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `every divider cell is the engine's`() {
        val cells = EngineFrames.cells("Divider")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                EngineFrames.compare(
                    cell,
                    toonDividerScene(
                        cell.tokens,
                        o.f("x0") ?: 0f,
                        o.f("x1")!!,
                        o.f("th")!!,
                        o.getValue("seed").jsonPrimitive.int,
                    ),
                )
            }
        println("divider frames: ${cells.size} cells, worst coordinate $worst dp")
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the press and the release follow the engine's listPress at every millisecond`() {
        val curves =
            Json
                .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText())
                .jsonObject
        var worst = 0f
        for (down in listOf(true, false)) {
            val c = curves.getValue("listPress(t, $down)").jsonObject
            val at = c.getValue("at").jsonArray.map { it.jsonPrimitive.float }
            val v = c.getValue("value").jsonArray.map { it.jsonPrimitive.float }
            for (i in at.indices) worst = maxOf(worst, abs(v[i] - toonListPress(at[i], down)))
        }
        println("listPress: worst $worst")
        assertTrue(worst <= 1e-5f, "listPress off the engine by $worst")
    }

    @Test
    fun `under reduce motion the row is down while pressed and flat once let go`() {
        assertEquals(1f, toonListItemFrameAt(10f, null, null, false, ToonListTrailing.Chevron, true).sag)
        assertEquals(0f, toonListItemFrameAt(null, 60f, null, false, ToonListTrailing.Chevron, true).sag)
        assertEquals(
            toonListPress(60f, false),
            toonListItemFrameAt(null, 60f, null, false, ToonListTrailing.Chevron, false).sag,
        )
        assertEquals(null, toonListItemFrameAt(null, null, 40f, true, ToonListTrailing.Switch, true).switchLook)
    }

    @Test
    fun `a click on a switch row toggles it and clicks`() =
        runComposeUiTest {
            val changes = mutableListOf<Boolean>()
            var clicks = 0
            setContent {
                ToonTheme {
                    ToonListItem(
                        "Reminders",
                        sub = "At noon",
                        trailing = ToonListTrailing.Switch,
                        on = true,
                        onCheckedChange = { changes += it },
                        onClick = { clicks++ },
                    )
                }
            }
            val row = onNodeWithText("Reminders, At noon").assertHeightIsEqualTo(72.dp)
            row.performClick()
            row.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
            assertEquals(listOf(false), changes)
            assertEquals(1, clicks)
        }

    /** How many different pictures a check row shows over the 20 frames after a click that sets it [to]. */
    private fun checkRowPictures(to: Boolean): Int {
        val pictures = mutableSetOf<List<Int>>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                ToonTheme {
                    ToonListItem(
                        "Share plans",
                        sub = "With family",
                        trailing = ToonListTrailing.Check,
                        on = !to,
                        last = true,
                    )
                }
            }
            mainClock.advanceTimeBy(500)
            val row = onNodeWithText("Share plans, With family")
            // Through semantics, not the pointer: no press, so the row does not sink and only the check moves.
            row.performSemanticsAction(SemanticsActions.OnClick)
            repeat(20) {
                val image = row.captureToImage().toPixelMap()
                pictures += List(image.width * image.height) { image[it % image.width, it / image.width].toArgb() }
                mainClock.advanceTimeByFrame()
            }
            row.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState(to)))
        }
        return pictures.size
    }

    @Test
    fun `a check row plays the checkbox's check and uncheck`() {
        // B-65: the design's row drew its check settled at once; it plays the checkbox's chkAnim now.
        val check = checkRowPictures(true)
        val uncheck = checkRowPictures(false)
        println("check row pictures over 20 frames: check $check, uncheck $uncheck")
        assertTrue(check >= 8, "the check drew $check different frames")
        assertTrue(uncheck >= 4, "the uncheck drew $uncheck different frames")
    }

    @Test
    fun `a check row draws the checkbox's frames, and its fade under reduce motion`() {
        // The standalone checkbox's frame, unpressed: chkAnim with its squash's overshoot held at 1.
        for (at in listOf(0f, 40f, 120f, 200f, 299f)) {
            for (on in listOf(true, false)) {
                assertEquals(
                    toonCheckboxFrameAt(on, at, 0f, false),
                    toonListItemFrameAt(null, null, at, on, ToonListTrailing.Check, false).checkLook,
                    "$on at $at",
                )
            }
        }
        assertEquals(null, toonListItemFrameAt(null, null, 300f, true, ToonListTrailing.Check, false).checkLook)
        assertEquals(null, toonListItemFrameAt(null, null, 40f, true, ToonListTrailing.Switch, false).checkLook)
        val reduced = assertNotNull(toonListItemFrameAt(null, null, 60f, true, ToonListTrailing.Check, true).checkLook)
        assertEquals(.5f, reduced.fade!!, 1e-6f)
    }
}
