package io.github.youndie.toon.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.fx.ToonFxHost
import io.github.youndie.toon.parity.DialogFrames
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.inkGouacheLight
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B-24: every MVP page dialog cell and its open and close strips against the engine's frames,
 * `dialogOpen` and `dialogClose` at every millisecond, reduce motion, the kit's hit areas, and the
 * pop's stars handed to an FX host.
 */
@OptIn(ExperimentalTestApi::class)
class ToonDialogTest {
    /** `tw` without a DOM: length × 18 × 0.55, the width the goldens were drawn with. */
    private val noDom: (String) -> Float = { it.length * 18 * .55f }

    @Test
    fun `every dialog cell and strip frame is the engine's`() {
        val cells = EngineFrames.cells("Dialog")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                val scene =
                    when (o.getValue("kind").jsonPrimitive.content) {
                        "state" -> DialogFrames.cell(cell.tokens, o.getValue("i").jsonPrimitive.int, noDom)
                        "open" -> DialogFrames.open(cell.tokens, o.f("t")!!, noDom)
                        else -> DialogFrames.close(cell.tokens, o.f("t")!!, noDom)
                    }
                EngineFrames.compare(cell, scene)
            }
        println("dialog frames: ${cells.size} cells, worst coordinate $worst dp")
        assertEquals(42, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the open and the close follow the engine's curves at every millisecond`() {
        val curves =
            Json
                .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText())
                .jsonObject

        fun col(
            curve: String,
            name: String,
        ) = curves
            .getValue(curve)
            .jsonObject
            .getValue(name)
            .jsonArray
            .map { it.jsonPrimitive.content.toFloat() }
        var worst = 0f
        for ((curve, f) in listOf<Pair<String, (Float) -> ToonDialogMotion>>(
            "dialogOpen(t)" to ::toonDialogOpen,
            "dialogClose(t)" to ::toonDialogClose,
        )) {
            val at = col(curve, "at")
            for ((name, get) in listOf<Pair<String, (ToonDialogMotion) -> Float>>(
                "R" to { it.r },
                "s" to { it.s },
                "op" to { it.op },
                "textOp" to { it.textOp },
                "stars" to { it.stars },
            )) {
                val c = col(curve, name)
                for (i in at.indices) worst = maxOf(worst, abs(c[i] - get(f(at[i]))))
            }
        }
        println("dialog curves: worst $worst")
        // The iris radius reaches 330, where a Float's step is 3·10⁻⁵: six decimals and Float, as elsewhere.
        assertTrue(worst <= 6e-5f, "dialog curves off the engine by $worst")
    }

    @Test
    fun `the kit draws the close for 320 ms and an open dialog from the start at rest`() {
        assertTrue(toonDialogFrameAt(false, 319f, false) != null)
        assertNull(toonDialogFrameAt(false, 320f, false))
        assertNull(toonDialogFrameAt(false, null, false), "closed from the start draws nothing")
        assertEquals(DIALOG_AT_REST, toonDialogFrameAt(true, null, false))
    }

    @Test
    fun `under reduce motion the open and the close fade in 120 ms at rest`() {
        for (t in listOf(0f, 30f, 60f, 119f)) {
            assertEquals(DIALOG_AT_REST.copy(fade = t / 120), toonDialogFrameAt(true, t, true))
            assertEquals(DIALOG_AT_REST.copy(fade = 1 - t / 120), toonDialogFrameAt(false, t, true))
        }
        assertEquals(DIALOG_AT_REST, toonDialogFrameAt(true, 500f, true))
        assertNull(toonDialogFrameAt(false, 120f, true))
        val card =
            toonDialogCardScene(inkGouacheLight, "Delete plan?", listOf("Gone."), "Delete", "Cancel", true, null, 40f)
        val h = toonDialogHeight(1)
        val full = toonDialogLayerScene(inkGouacheLight, 390f, 700f, card, h, DIALOG_AT_REST)
        val half = toonDialogLayerScene(inkGouacheLight, 390f, 700f, card, h, DIALOG_AT_REST.copy(fade = .5f))
        assertEquals(full.layers.map { it.alpha * .5f }, half.layers.map { it.alpha })
        assertEquals(full.labels.map { it.alpha * .5f }, half.labels.map { it.alpha })
    }

    @Test
    fun `the iris reaches the corners at its widest and hugs the dialog at rest`() {
        val card =
            toonDialogCardScene(inkGouacheLight, "Delete plan?", listOf("Gone."), "Delete", "Cancel", true, null, 40f)
        val h = toonDialogHeight(1)

        fun radius(r: Float) =
            toonDialogLayerScene(inkGouacheLight, 390f, 700f, card, h, DIALOG_AT_REST.copy(r = r)).layers[0].numbers[10]
        assertEquals(kotlin.math.hypot(195f, 350f) + 10, radius(330f), 1e-3f)
        assertEquals(kotlin.math.hypot(150f, h / 2) + 16, radius(190f), 1e-3f)
    }

    @Test
    fun `the kit's hit areas confirm, cancel, or do nothing`() {
        val h = toonDialogHeight(2)
        // A 390 × 700 layer: the dialog spans x 45..345, y 350 ± h / 2; its button row is 72 from the bottom.
        val y0 = 350 - h / 2
        val row = y0 + h - 72 + 26
        assertEquals(ToonDialogHit.Confirm, toonDialogHit(390f, 700f, h, 260f, row))
        assertEquals(ToonDialogHit.Cancel, toonDialogHit(390f, 700f, h, 150f, row))
        assertEquals(ToonDialogHit.Cancel, toonDialogHit(390f, 700f, h, 20f, 350f), "outside")
        assertNull(toonDialogHit(390f, 700f, h, 100f, y0 + 40), "on the title")
        assertNull(toonDialogHit(390f, 700f, h, 60f, row), "left of the cancel button")
    }

    @Test
    fun `taps on the open dialog confirm and cancel, and a closed one takes none`() {
        runComposeUiTest {
            var confirms = 0
            var cancels = 0
            var open by mutableStateOf(true)
            setContent {
                ToonTheme {
                    ToonDialog(
                        "Delete plan?",
                        Modifier.size(390.dp, 700.dp).testTag("dialog"),
                        open = open,
                        body = "“Paint the fence” will be gone for good.",
                        confirm = "Delete",
                        onConfirm = { confirms++ },
                        onCancel = { cancels++ },
                    )
                }
            }
            val lines = 2
            val h = toonDialogHeight(lines)
            val row = 350 - h / 2 + h - 72 + 26
            val dialog = onNodeWithTag("dialog")
            dialog.performTouchInput { click(Offset(260f, row)) }
            assertEquals(1, confirms)
            dialog.performTouchInput { click(Offset(20f, 20f)) }
            assertEquals(1, cancels, "a tap outside")
            open = false
            waitForIdle()
            dialog.performTouchInput { click(Offset(260f, row)) }
            dialog.performTouchInput { click(Offset(20f, 20f)) }
            assertEquals(1 to 1, confirms to cancels, "taps once closed")
        }
    }

    @Test
    fun `the body wraps at 252 dp as the kit's lines do`() {
        assertEquals(
            listOf("aaaa aaaa", "aaaa"),
            toonDialogLines("aaaa  aaaa\naaaa") { it.length * 28f },
        )
        assertEquals(emptyList(), toonDialogLines(" ") { 0f })
    }

    @Test
    fun `inside an FX host the close hands its stars to the host 170 ms in`() {
        runComposeUiTest {
            val heard = mutableListOf<String>()
            var open by mutableStateOf(true)
            setContent {
                ToonTheme {
                    ToonFxHost(onEvent = { heard += it.name }) {
                        ToonDialog("Delete plan?", Modifier.size(390.dp, 700.dp), open = open)
                    }
                }
            }
            mainClock.autoAdvance = false
            mainClock.advanceTimeBy(1_000)
            open = false
            mainClock.advanceTimeBy(100)
            assertEquals(emptyList(), heard.filter { it == "dialog.pop" })
            mainClock.advanceTimeBy(200)
            assertEquals(listOf("dialog.pop"), heard.filter { it == "dialog.pop" })
        }
    }
}
