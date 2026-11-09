package io.github.youndie.toon.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.fx.ToonFxHost
import io.github.youndie.toon.parity.CardFrames
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
import kotlin.test.assertTrue

/**
 * B-21: every Core page card cell and its appear, press and dismiss strips against the engine's
 * frames, `cardAppear` and `cardDismiss` at every millisecond, and the card under a tap and a swipe.
 */
@OptIn(ExperimentalTestApi::class)
class ToonCardTest {
    @Test
    fun `every card cell and strip frame is the engine's`() {
        val cells = EngineFrames.cells("Card")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts

                fun flag(k: String) = o[k]?.jsonPrimitive?.booleanOrNull ?: false
                val scene =
                    when (o.getValue("kind").jsonPrimitive.content) {
                        "state" -> {
                            CardFrames.state(
                                cell.tokens,
                                o.getValue("i").jsonPrimitive.int,
                                ToonCardLook(
                                    press = o.f("press") ?: 0f,
                                    selected = flag("sel"),
                                    disabled = flag("dis"),
                                ),
                            )
                        }

                        "appear" -> {
                            CardFrames.appear(cell.tokens, o.f("t")!!)
                        }

                        "press" -> {
                            CardFrames.press(cell.tokens, o.f("p")!!)
                        }

                        else -> {
                            CardFrames.dismiss(cell.tokens, o.f("t")!!)
                        }
                    }
                EngineFrames.compare(cell, scene)
            }
        println("card frames: ${cells.size} cells, worst coordinate $worst dp")
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the appear and the dismiss follow the engine's curves at every millisecond`() {
        val curves =
            Json
                .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText())
                .jsonObject

        fun col(
            curve: String,
            name: String,
        ) = curves.getValue(curve).jsonObject.getValue(name).jsonArray.map {
            it.jsonPrimitive.content.toFloatOrNull() ?: if (it.jsonPrimitive.content == "true") 1f else 0f
        }
        val at = col("cardAppear(t, 1.5)", "at")
        var worst = 0f
        val appear =
            listOf<Pair<String, (ToonCardAppear) -> Float>>(
                "op" to { it.op },
                "dy" to { it.dy },
                "rot" to { it.rot },
                "sx" to { it.sx },
                "sy" to { it.sy },
                "du" to { it.du },
            )
        for ((name, get) in appear) {
            val c = col("cardAppear(t, 1.5)", name)
            for (i in at.indices) worst = maxOf(worst, abs(c[i] - get(toonCardAppear(at[i], 1.5f))))
        }
        val dt = col("cardDismiss(t)", "at")
        val dismiss =
            listOf<Pair<String, (ToonCardDismiss) -> Float>>(
                "dx" to { it.dx },
                "sx" to { it.sx },
                "sy" to { it.sy },
                "op" to { it.op },
                "lu" to { it.lu },
                "ghost" to { if (it.ghost) 1f else 0f },
                "n" to { it.n },
            )
        for ((name, get) in dismiss) {
            val c = col("cardDismiss(t)", name)
            for (i in dt.indices) worst = maxOf(worst, abs(c[i] - get(toonCardDismiss(dt[i]))))
        }
        println("card curves: worst $worst")
        // The throw reaches 440 dp, where a Float's step is 3·10⁻⁵: six decimals and Float, as elsewhere.
        assertTrue(worst <= 1e-4f, "card curves off the engine by $worst")
    }

    @Test
    fun `a tap clicks, a swipe past 80 dp dismisses and fires card gone`() {
        for (left in listOf(false, true)) {
            runComposeUiTest {
                var clicks = 0
                var dismissed = 0
                val heard = mutableListOf<String>()
                setContent {
                    ToonTheme {
                        ToonFxHost(onEvent = { heard += it.name }) {
                            ToonCard(
                                "Pancake breakfast",
                                Modifier.testTag("card"),
                                meta = "Kitchen, bring syrup",
                                time = "9:00",
                                dismissible = true,
                                onDismiss = { dismissed++ },
                                onClick = { clicks++ },
                            )
                        }
                    }
                }
                val card = onNodeWithTag("card")
                card.performClick()
                assertEquals(1, clicks)
                card.performTouchInput { if (left) swipeLeft() else swipeRight() }
                waitForIdle()
                assertEquals(1, dismissed, "dismissed, left: $left")
                assertEquals(listOf("card.gone"), heard, "left: $left")
                assertEquals(1, clicks, "a swipe does not click")
            }
        }
    }

    @Test
    fun `a short drag springs back without dismissing`() =
        runComposeUiTest {
            var dismissed = 0
            setContent {
                ToonTheme {
                    ToonCard(
                        "Pancake breakfast",
                        Modifier.testTag("card"),
                        dismissible = true,
                        onDismiss = { dismissed++ },
                    )
                }
            }
            onNodeWithTag("card").performTouchInput {
                down(center)
                moveBy(
                    androidx.compose.ui.geometry
                        .Offset(40f, 0f),
                )
                up()
            }
            waitForIdle()
            assertEquals(0, dismissed)
        }

    @Test
    fun `under reduce motion the card fades in over 120 ms and does not fall`() {
        val state = ToonCardState(appear = true)
        state.step(1000, 0f)
        state.step(1060, 60f)
        val tokens = io.github.youndie.toon.tokens.inkGouacheLight
        val reduced = toonCardFrame(tokens, 0, 342f, "T", "", "", 0f, state, 0, true, false, false, true)
        assertEquals(.5f, reduced.fade!!, 1e-6f)
        val live = toonCardFrame(tokens, 0, 342f, "T", "", "", 0f, state, 0, false, false, false, true)
        assertEquals(null, live.fade)
    }

    @Test
    fun `a swiped card springs back past its slot and flies off from where it stopped`() {
        // B-67, the owner: "pulled far enough — the card came back to its place — ran away, even with a
        // little wind-up the other way". B-66 had thrown it on from the finger.
        val r = RETURN_MS
        for ((drag, dir) in listOf(-150f to -1f, 120f to 1f, -85f to -1f)) {
            assertEquals(drag, toonCardThrowDx(0f, drag, dir), 1e-3f, "starts at the finger, $drag")
            var last = drag
            var t = 1f
            while (t <= r + 200) {
                val dx = toonCardThrowDx(t, drag, dir)
                val step = dx - last
                if (t <= r) {
                    assertTrue(step * dir <= 1e-3f, "drag $drag: out toward the throw on the way back at $t ms")
                } else {
                    assertTrue(step * dir >= -1e-3f, "drag $drag: back toward the slot in the throw at $t ms")
                }
                // Drawn until the fling ends, 100 ms after the way back; then it is gone.
                if (t < r + 100) assertTrue(abs(step) < 15f, "drag $drag: a jump of $step dp at $t ms")
                last = dx
                t += 1f
            }
            val wound = toonCardThrowDx(r, drag, dir)
            assertTrue(wound * dir < 0, "drag $drag: past the slot the other way at the turn, at $wound")
            assertTrue(abs(wound) in abs(drag) * .1f..abs(drag) * .15f, "drag $drag: the wind-up is $wound")
            // It stands still at the turn: no step in its speed either side of it.
            val before = wound - toonCardThrowDx(r - .5f, drag, dir)
            val after = toonCardThrowDx(r + .5f, drag, dir) - wound
            assertTrue(abs(before) < .05f && abs(after) < .05f, "drag $drag: moving at the turn, $before then $after")
            assertTrue(abs(last) > 342f, "drag $drag: off the row by ${r + 200} ms, at $last")
        }
        // Thrown with no drag, by a reader's action, it is the kit's throw.
        for (t in listOf(0f, 70f, 140f, 190f)) {
            assertEquals(kotlin.math.max(0f, toonCardDismiss(t).dx), toonCardThrowDx(t, 0f, 1f), 1e-3f)
        }
    }

    @Test
    fun `the swiped card's first frame after the release is its last frame under the finger`() {
        // B-67: on release the card turned level and opaque in one frame.
        val tokens = io.github.youndie.toon.tokens.inkGouacheLight
        for (drag in listOf(-150f, 120f)) {
            val state = ToonCardState(appear = false)
            state.press(171f, 342f)
            state.drag(drag)
            state.step(1000, 16f)
            fun frame() = toonCardFrame(tokens, 1, 342f, "Bike to the lake", "Check the tyre pump", "11:30", 1.5f, state, 0, false, false, false, false).scene
            val held = frame()
            state.release(true)
            state.step(1000, 0f)
            val let = frame()
            val a = held.layers.filter { it.alpha > 0f }
            val b = let.layers.filter { it.alpha > 0f }
            assertEquals(a.size, b.size, "drag $drag: layers")
            for ((x, y) in a.zip(b)) {
                assertEquals(x.alpha, y.alpha, 1e-4f, "drag $drag: opacity")
                for ((p, q) in listOf(x.outer.a to y.outer.a, x.outer.b to y.outer.b, x.outer.e to y.outer.e, x.outer.f to y.outer.f)) {
                    assertEquals(p, q, 1e-3f, "drag $drag: placement")
                }
            }
            for ((x, y) in held.labels.zip(let.labels)) {
                assertEquals(x.x, y.x, 1e-3f, "drag $drag: ${x.text}")
                assertEquals(x.baseline, y.baseline, 1e-3f, "drag $drag: ${x.text}")
            }
        }
    }

    @Test
    fun `a dragged card's time stays on its sticker`() {
        // B-67: the design turns the sheet and leaves its words where they stood, so a long drag slid the
        // time a dozen dp off its sticker. The words now go where the turn takes their places.
        val tokens = io.github.youndie.toon.tokens.inkGouacheLight
        val still = ToonCardState(appear = false)
        val dragged = ToonCardState(appear = false).apply { drag(-150f) }

        fun time(s: ToonCardState) =
            toonCardFrame(tokens, 1, 342f, "T", "m", "11:30", 0f, s, 0, false, false, false, false)
                .scene.labels
                .single { it.text == "11:30" }
        val turn = -150f * .035f * kotlin.math.PI.toFloat() / 180
        // The sticker's middle, 18 dp in from the right and 64 wide, turned about the dragged card's pivot.
        val cx = 342f / 2 - 150f
        val sx = 342f - 18 - 32 - cx
        val sy = time(still).baseline - 84f / 2
        val ex = cx + kotlin.math.cos(turn) * sx - kotlin.math.sin(turn) * sy - 150f
        val ey = 84f / 2 + kotlin.math.sin(turn) * sx + kotlin.math.cos(turn) * sy
        assertEquals(ex, time(dragged).x, .01f, "the time's x")
        assertEquals(ey, time(dragged).baseline, .01f, "the time's baseline")
    }

    @Test
    fun `the cards below close up only once a swiped card is off`() {
        // B-67: the spring back delays the throw, and the close-up with it.
        val state = ToonCardState(appear = false)
        state.drag(-150f)
        state.release(true)
        state.step(1000, 0f)
        var t = 0L
        while (t < 700) {
            state.step(1000 + t, 1f)
            val d = toonCardDismiss(t - state.throwShift)
            if (d.op > .01f) assertEquals(0f, state.closeUp(false), "closing up at $t ms while the card is drawn")
            t++
        }
        assertTrue(state.closedUp(false))
    }

    @Test
    fun `a card thrown from a list punches no hole, a card on its own does`() {
        // B-66: in a list the cards below close up over the slot at once, so the hole was covered by
        // the next card a few frames after it was punched: a black slab under a sliding card.
        for (inList in listOf(false, true)) {
            runComposeUiTest {
                val heard = mutableListOf<String>()
                setContent {
                    ToonTheme {
                        ToonFxHost(onEvent = { heard += it.name }) {
                            if (inList) {
                                ToonCardList(
                                    listOf(ToonCardListItem(0, "Pancake breakfast", "Kitchen, bring syrup", "9:00")),
                                    Modifier.testTag("card"),
                                    dismissible = true,
                                )
                            } else {
                                ToonCard("Pancake breakfast", Modifier.testTag("card"), dismissible = true)
                            }
                        }
                    }
                }
                onNodeWithTag("card").performTouchInput { swipeLeft() }
                waitForIdle()
                assertEquals(if (inList) emptyList() else listOf("card.gone"), heard, "in a list: $inList")
            }
        }
    }
}
