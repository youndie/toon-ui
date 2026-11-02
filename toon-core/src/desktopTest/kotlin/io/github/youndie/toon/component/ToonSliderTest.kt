package io.github.youndie.toon.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.theme.ToonTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-36: every v1 page slider cell and its grab strip against the engine's frames, `sliderGrab` at
 * every millisecond both ways, reduce motion, the stops, and setting the value by a tap and by a
 * screen reader.
 */
@OptIn(ExperimentalTestApi::class)
class ToonSliderTest {
    @Test
    fun `every slider cell and grab frame is the engine's`() {
        val cells = EngineFrames.cells("Slider")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts

                fun flag(k: String) = o[k]?.jsonPrimitive?.booleanOrNull ?: false
                val scene =
                    if (o.getValue("kind").jsonPrimitive.content == "grab") {
                        toonSliderScene(
                            cell.tokens,
                            200f,
                            .5f,
                            ToonSliderLook(grab = toonSliderGrab(o.f("t")!!, flag("down"))),
                        )
                    } else {
                        toonSliderScene(
                            cell.tokens,
                            260f,
                            o.f("value")!!,
                            ToonSliderLook(o.f("grab") ?: 0f, flag("focus"), flag("disabled")),
                        )
                    }
                EngineFrames.compare(cell, scene)
            }
        println("slider frames: ${cells.size} cells, worst coordinate $worst dp")
        assertEquals(33, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the grab follows the engine's curve at every millisecond, held and let go`() {
        val curves =
            Json
                .parseToJsonElement(
                    checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText(),
                ).jsonObject
        var worst = 0f
        for (down in listOf(true, false)) {
            val c = curves.getValue("sliderGrab(t, $down)").jsonObject
            val at = c.getValue("at").jsonArray.map { it.jsonPrimitive.content.toFloat() }
            val v = c.getValue("value").jsonArray.map { it.jsonPrimitive.content.toFloat() }
            for (i in at.indices) worst = maxOf(worst, abs(v[i] - toonSliderGrab(at[i], down)))
        }
        println("sliderGrab: worst $worst")
        assertTrue(worst <= 1e-5f, "sliderGrab off the engine by $worst")
    }

    @Test
    fun `the release plays 500 ms, and under reduce motion the ball is squashed exactly while held`() {
        assertEquals(toonSliderGrab(60f, false), toonSliderFrameAt(false, 60f, false))
        assertEquals(toonSliderGrab(499f, false), toonSliderFrameAt(false, 499f, false))
        assertEquals(0f, toonSliderFrameAt(false, 500f, false))
        assertEquals(toonSliderGrab(40f, true), toonSliderFrameAt(true, 40f, false))
        assertEquals(1f, toonSliderFrameAt(true, 10f, true))
        assertEquals(0f, toonSliderFrameAt(false, 60f, true))
        assertEquals(0f, toonSliderFrameAt(false, null, false))
    }

    @Test
    fun `steps snap the value, and a position maps along the rope 14 dp in`() {
        assertEquals(.5f, toonSliderSnap(.55f, 3))
        assertEquals(1f, toonSliderSnap(.8f, 3))
        assertEquals(.55f, toonSliderSnap(.55f, 0))
        assertEquals(0f, toonSliderAt(300f, 14f))
        assertEquals(1f, toonSliderAt(300f, 286f))
        assertEquals(.5f, toonSliderAt(300f, 150f))
        assertEquals(0f, toonSliderAt(300f, 2f))
    }

    @Test
    fun `a tap sets the value under it, and a screen reader sets it too`() {
        runComposeUiTest {
            var v by mutableFloatStateOf(.4f)
            setContent { ToonTheme { ToonSlider(v, { v = it }, Modifier.testTag("slider"), steps = 5) } }
            onNodeWithTag("slider").performTouchInput { click(Offset(220f, 24f)) }
            waitForIdle()
            // (220 − 14) / 272 = .76, snapped to the stop at .75.
            assertEquals(.75f, v)
            onNodeWithTag("slider").performSemanticsAction(SemanticsActions.SetProgress) { it(.3f) }
            assertEquals(.25f, v)
        }
    }
}
