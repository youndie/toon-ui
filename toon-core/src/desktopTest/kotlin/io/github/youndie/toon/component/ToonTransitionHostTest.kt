package io.github.youndie.toon.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.fx.toonIrisAt
import io.github.youndie.toon.theme.ToonTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-44: the three transitions' motion against the engine at every millisecond — `pushAt`, `irisAt`,
 * and the hole's growth and the new screen's fade read off `holeScene`'s own frames — and the host
 * composing both screens while one plays, only the new one after.
 */
@OptIn(ExperimentalTestApi::class)
class ToonTransitionHostTest {
    private val curves =
        Json
            .parseToJsonElement(
                checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText(),
            ).jsonObject

    private fun col(
        curve: String,
        k: String,
    ) = curves
        .getValue(curve)
        .jsonObject
        .getValue(k)
        .jsonArray
        .map { if (it is JsonNull) null else it.jsonPrimitive.content.toFloat() }

    @Test
    fun `the push follows the engine at every millisecond`() {
        val at = col("pushAt(t, 180)", "at").map { it!! }
        var worst = 0f
        for ((k, get) in listOf<Pair<String, (ToonPushMotion) -> Float>>(
            "ax" to { it.ax },
            "asx" to { it.asx },
            "bx" to { it.bx },
        )) {
            val v = col("pushAt(t, 180)", k)
            for (i in at.indices) worst = maxOf(worst, abs(v[i]!! - get(toonPushAt(at[i], 180f))))
        }
        println("pushAt: worst $worst")
        // The old screen leaves to -186, where a Float's step is 1.5·10⁻⁵: two steps of it.
        val bound = 2 * Math.ulp(186f)
        assertTrue(worst <= bound, "pushAt off the engine by $worst, over $bound")
    }

    @Test
    fun `the scene change is the FX iris, at every millisecond`() {
        val at = col("irisAt(t)", "at").map { it!! }
        val k = col("irisAt(t)", "k")
        val scene = col("irisAt(t)", "scene")
        var worst = 0f
        for (i in at.indices) {
            worst = maxOf(worst, abs(k[i]!! - toonIrisAt(at[i]).k))
            assertEquals(scene[i]!!.toInt(), toonIrisAt(at[i]).scene, "scene at ${at[i]} ms")
        }
        assertTrue(worst <= 1e-5f, "irisAt off the engine by $worst")
    }

    @Test
    fun `the hole grows and the new screen fades in as the engine's frames have them`() {
        val at = col("holeScene(t)", "at").map { it!! }
        val scale = col("holeScene(t)", "scale")
        val newOp = col("holeScene(t)", "newOp")
        var worstScale = 0f
        var worstOp = 0f
        var drawn = 0
        for (i in at.indices) {
            val m = toonHoleAt(at[i])
            scale[i]?.let {
                worstScale = maxOf(worstScale, abs(it - m.scale))
                drawn++
            }
            worstOp = maxOf(worstOp, abs(newOp[i]!! - m.newOp))
        }
        println("hole: worst scale $worstScale over $drawn ms, worst fade $worstOp")
        assertEquals(419, drawn, "the silhouette is drawn from 1 ms to 419")
        // The engine writes the scale to three decimals.
        assertTrue(worstScale <= 5e-4f + 1e-5f, "hole scale off the engine by $worstScale")
        assertTrue(worstOp <= 1e-5f, "new screen's fade off the engine by $worstOp")
        val tokens = io.github.youndie.toon.tokens.inkGouacheLight
        assertTrue(toonHoleTransitionScene(tokens.colors, 0f, 14f, 82f, 152f, 46f).layers.isEmpty())
        assertTrue(toonHoleTransitionScene(tokens.colors, 419f, 14f, 82f, 152f, 46f).layers.isNotEmpty())
        assertTrue(toonHoleTransitionScene(tokens.colors, 420f, 14f, 82f, 152f, 46f).layers.isEmpty())
    }

    @Test
    fun `each transition plays its page duration`() {
        assertEquals(450f, toonTransitionMs(ToonTransition.Push))
        assertEquals(600f, toonTransitionMs(ToonTransition.Scene))
        assertEquals(460f, toonTransitionMs(ToonTransition.Hole))
        val state = ToonTransitionState("a")
        state.go("a", ToonTransition.Hole)
        assertEquals(null, state.age(), "the same screen plays nothing")
        state.go("b", ToonTransition.Hole)
        state.tick(1000)
        state.tick(1459)
        assertEquals(459f, state.age())
        assertEquals("a", state.from)
        state.tick(1460)
        assertEquals(null, state.age())
    }

    @Test
    fun `the host composes both screens while a transition plays, and only the new one after`() {
        for (kind in ToonTransition.entries) {
            for (reduce in listOf(false, true)) {
                runComposeUiTest {
                    mainClock.autoAdvance = false
                    var screen by mutableStateOf("list")
                    setContent {
                        ToonTheme(reduceMotion = reduce) {
                            ToonTransitionHost(screen, Modifier.size(180.dp, 320.dp), transition = kind) {
                                Box(Modifier.fillMaxSize().testTag(it))
                            }
                        }
                    }
                    mainClock.advanceTimeByFrame()
                    assertEquals(1, onAllNodesWithTagCount("list"))
                    screen = "plan"
                    mainClock.advanceTimeBy(100)
                    assertEquals(1, onAllNodesWithTagCount("list"), "$kind, reduced $reduce: the old screen during")
                    assertEquals(1, onAllNodesWithTagCount("plan"), "$kind, reduced $reduce: the new screen during")
                    mainClock.advanceTimeBy(700)
                    assertEquals(0, onAllNodesWithTagCount("list"), "$kind, reduced $reduce: the old screen after")
                    assertEquals(1, onAllNodesWithTagCount("plan"))
                }
            }
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.onAllNodesWithTagCount(tag: String) =
        onAllNodes(
            androidx.compose.ui.test
                .hasTestTag(tag),
        ).fetchSemanticsNodes().size
}
