package io.github.youndie.toon.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.fx.ToonFxHost
import io.github.youndie.toon.fx.toonStarsScene
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.theme.ToonTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B-19: every Core page text field cell and strip frame against the engine's `field` (and `stars`),
 * `fldFocus` against the engine's at every millisecond, typing on desktop, and the field over time.
 */
@OptIn(ExperimentalTestApi::class)
class ToonTextFieldTest {
    private fun JsonObject.flag(key: String) = this[key]?.jsonPrimitive?.booleanOrNull ?: false

    private fun JsonObject.text(key: String) = this[key]?.jsonPrimitive?.content ?: ""

    @Test
    fun `every text field cell and strip frame is the engine's`() {
        val cells = EngineFrames.cells("TextField")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                // The strips' frames (`<Strip>_<t>ms_<theme>`) are 240 wide, the state cells 280.
                val w = if (Regex("_\\d+ms_").containsMatchIn(cell.name)) 240f else 280f
                val focus = o.flag("focus")
                val value = o.text("value")
                val look =
                    ToonFieldLook(
                        label = o.text("label"),
                        value = value,
                        helper = o.text("helper"),
                        focus = focus,
                        lift = o.f("lift") ?: if (focus || value.isNotEmpty()) 1f else 0f,
                        thick = o.f("thick") ?: if (focus) 1f else 0f,
                        bounce = o.f("bounce") ?: 1f,
                        error = o.flag("error"),
                        disabled = o.flag("disabled"),
                        caretX = if (o.flag("caret")) engineCaretX(value) else null,
                    )
                var scene = toonFieldScene(cell.tokens, w, look)
                o.f("stars")?.let {
                    scene =
                        (scene + toonStarsScene(cell.tokens.colors, it, 210f, -4f, 26f)).placed(o.f("shake")!!, 0f)
                }
                EngineFrames.compare(cell, scene)
            }
        println("text field frames: ${cells.size} cells, worst coordinate $worst dp")
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `focus and blur follow the engine's fldFocus at every millisecond`() {
        val curves =
            Json
                .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText())
                .jsonObject
        var worst = 0f
        for (on in listOf(true, false)) {
            val c = curves.getValue("fldFocus(t, $on)").jsonObject
            val at = c.getValue("at").jsonArray.map { it.jsonPrimitive.float }
            for (name in listOf("lift", "thick", "bounce")) {
                val col = c.getValue(name).jsonArray.map { it.jsonPrimitive.float }
                for (i in at.indices) {
                    val mine = toonFldFocus(at[i], on)
                    val v = mapOf("lift" to mine.lift, "thick" to mine.thick, "bounce" to mine.bounce).getValue(name)
                    worst = maxOf(worst, abs(col[i] - v))
                }
            }
        }
        println("fldFocus: worst $worst")
        assertTrue(worst <= 1e-5f, "fldFocus off the engine by $worst")
    }

    @Test
    fun `the caret blinks 500 on and 300 off, steady under reduce motion`() {
        fun on(
            now: Long,
            reduce: Boolean = false,
        ) = toonFieldFrameAt(
            now,
            focused = true,
            filled = true,
            focusAgeMs = now.toFloat(),
            errorAgeMs = null,
            reduceMotion = reduce,
        ).caretOn
        assertTrue(on(0))
        assertTrue(on(499))
        assertFalse(on(500))
        assertFalse(on(799))
        assertTrue(on(800))
        assertTrue(on(600, reduce = true))
        assertFalse(
            toonFieldFrameAt(
                600,
                focused = false,
                filled = true,
                focusAgeMs = 600f,
                errorAgeMs = null,
                reduceMotion = false,
            ).caretOn,
        )
    }

    @Test
    fun `an error shakes with stars, and under reduce motion is red only`() {
        val live =
            toonFieldFrameAt(
                0,
                focused = true,
                filled = true,
                focusAgeMs = null,
                errorAgeMs = 30f,
                reduceMotion = false,
            )
        assertEquals(
            io.github.youndie.toon.fx
                .toonShake(30f),
            live.shake,
        )
        assertEquals(30f / 350, live.stars!!, 1e-6f)
        val reduced =
            toonFieldFrameAt(0, focused = true, filled = true, focusAgeMs = null, errorAgeMs = 30f, reduceMotion = true)
        assertEquals(0f, reduced.shake)
        assertNull(reduced.stars)
    }

    @Test
    fun `a filled field keeps its label lifted, focused or not`() {
        assertEquals(
            1f,
            toonFieldFrameAt(
                0,
                focused = false,
                filled = true,
                focusAgeMs = 30f,
                errorAgeMs = null,
                reduceMotion = false,
            ).look.lift,
        )
        assertEquals(
            0f,
            toonFieldFrameAt(
                0,
                focused = false,
                filled = false,
                focusAgeMs = null,
                errorAgeMs = null,
                reduceMotion = false,
            ).look.lift,
        )
    }

    @Test
    fun `typing reaches the value on desktop, inside and outside an FX host`() {
        for (hosted in listOf(false, true)) {
            runComposeUiTest {
                var value by mutableStateOf("")
                mainClock.autoAdvance = false
                setContent {
                    ToonTheme {
                        val field = @androidx.compose.runtime.Composable {
                            ToonTextField(
                                value,
                                { value = it },
                                "Plan name",
                                error =
                                    if (value.length in
                                        1..3
                                    ) {
                                        "At least 4 letters"
                                    } else {
                                        ""
                                    },
                            )
                        }
                        if (hosted) ToonFxHost { field() } else field()
                    }
                }
                val node = onNodeWithContentDescription("Plan name")
                node.performClick()
                mainClock.advanceTimeBy(100)
                node.performTextInput("Pan")
                mainClock.advanceTimeBy(600)
                assertEquals("Pan", value, "hosted: $hosted")
            }
        }
    }

    /** The box's pixels at every frame of a focus, then a blur, of an empty field labelled [label]. */
    private fun focusAndBlurFrames(label: String): List<IntArray> {
        val frames = mutableListOf<IntArray>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            lateinit var focus: FocusManager
            setContent {
                ToonTheme {
                    focus = LocalFocusManager.current
                    Box(Modifier.background(Toon.colors.stage).testTag("box")) {
                        ToonTextField("", {}, label, width = 300.dp)
                    }
                }
            }
            mainClock.advanceTimeBy(500)

            fun grab(n: Int) =
                repeat(n) {
                    val image = onNodeWithTag("box").captureToImage().toPixelMap()
                    frames +=
                        IntArray(image.width * image.height) { image[it % image.width, it / image.width].toArgb() }
                    mainClock.advanceTimeByFrame()
                }
            grab(1)
            onNode(hasSetTextAction()).performClick()
            grab(30)
            focus.clearFocus()
            grab(30)
        }
        return frames
    }

    @Test
    fun `the label flies out and back without standing a frame at the far end`() {
        // B-63: the same frames with and without the label differ only in the label; its rows' mean is
        // where it stands. The composition that first saw a new focus drew it at the old play's settled
        // age, so the label stood a frame at its far end — up on focus, down on blur — then started back.
        val labelled = focusAndBlurFrames("Plan name")
        val bare = focusAndBlurFrames("")
        val width = 300
        val ys =
            labelled.indices.map { i ->
                val rows = labelled[i].indices.filter { labelled[i][it] != bare[i][it] }.map { it / width }
                assertTrue(rows.isNotEmpty(), "frame $i: no label drawn")
                rows.average()
            }
        val rest = ys.first()
        val lifted = ys[30]
        println("label rows by frame: ${ys.joinToString { "%.1f".format(it) }}")
        assertTrue(rest - lifted > 30, "the label did not lift: $rest to $lifted")
        val steps = ys.zipWithNext { a, b -> abs(b - a) }
        val worst = steps.indices.maxBy { steps[it] }
        assertTrue(
            steps[worst] <= MAX_LABEL_STEP,
            "the label jumped ${"%.1f".format(steps[worst])} px between frames $worst and ${worst + 1}: " +
                ys.joinToString { "%.1f".format(it) },
        )
    }
}

/** The widest move of the label between two 16 ms frames: the spring's steepest step measures 12 dp; the jump was 44. */
private const val MAX_LABEL_STEP = 20.0
