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
}
