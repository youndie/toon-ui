package io.github.youndie.toon.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.theme.ToonTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-25: every MVP page progress cell and its worm and spin loops against the engine's frames, `worm`
 * at every millisecond of its loop, and what a screen reader is told.
 */
@OptIn(ExperimentalTestApi::class)
class ToonProgressTest {
    @Test
    fun `every progress cell and loop frame is the engine's`() {
        val cells = EngineFrames.cells("Progress")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                val scene =
                    when (o.getValue("kind").jsonPrimitive.content) {
                        "worm" -> toonWormScene(cell.tokens, o.f("w")!!, o.f("t")!!)
                        "linear" -> toonLinearScene(cell.tokens, o.f("w")!!, 0f, o.f("b")!!, worm = false)
                        else -> toonCircularScene(cell.tokens, o.f("value"), o.f("ph") ?: 0f)
                    }
                EngineFrames.compare(cell, scene)
            }
        println("progress frames: ${cells.size} cells, worst coordinate $worst dp")
        assertEquals(39, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the worm follows the engine's curve at every millisecond`() {
        val curve =
            Json
                .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText())
                .jsonObject
                .getValue("worm(t)")
                .jsonObject

        fun col(name: String) = curve.getValue(name).jsonArray.map { it.jsonPrimitive.content.toFloat() }
        val at = col("at")
        val a = col("a")
        val b = col("b")
        var worst = 0f
        for (i in at.indices) {
            val w = toonWorm(at[i])
            worst = maxOf(worst, abs(a[i] - w.a), abs(b[i] - w.b))
        }
        println("worm curve: worst $worst")
        assertTrue(worst <= 1e-5f, "worm off the engine by $worst")
        // The loop repeats every 1200 ms.
        assertEquals(toonWorm(300f), toonWorm(1500f))
    }

    @Test
    fun `a screen reader hears the value, or that it is not known`() {
        runComposeUiTest {
            // The worm crawls for ever: the clock is advanced by hand, or the test never idles.
            mainClock.autoAdvance = false
            setContent {
                ToonTheme {
                    ToonProgress(Modifier.testTag("worm"))
                    ToonProgress(Modifier.testTag("ring"), kind = ToonProgressKind.Circular, value = .65f)
                }
            }
            onNodeWithTag(
                "worm",
            ).assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo.Indeterminate,
                ),
            )
            onNodeWithTag("ring").assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(
                        .65f,
                        0f..1f,
                    ),
                ),
            )
        }
    }
}
