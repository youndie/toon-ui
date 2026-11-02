package io.github.youndie.toon.component

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.theme.ToonTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-20: every MVP page chip cell and select frame against the engine's `chip`, `chipSelect` at every
 * millisecond, the 48 dp touch target round the 36 dp drawing, and the chip under a click.
 */
@OptIn(ExperimentalTestApi::class)
class ToonChipTest {
    @Test
    fun `every chip cell and select frame is the engine's`() {
        val cells = EngineFrames.cells("Chip")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts

                fun flag(k: String) = o[k]?.jsonPrimitive?.booleanOrNull ?: false
                val label = o.getValue("label").jsonPrimitive.content
                val variant =
                    ToonChipVariant.valueOf(
                        o
                            .getValue("variant")
                            .jsonPrimitive.content
                            .replaceFirstChar { it.uppercase() },
                    )
                val sel = flag("sel")
                val look =
                    ToonChipLook(
                        selected = sel,
                        scale = o.f("scale") ?: 1f,
                        swipe = o.f("swipe") ?: if (sel) 1f else 0f,
                        p = o.f("p") ?: 0f,
                        focus = flag("focus"),
                        disabled = flag("disabled"),
                    )
                val icon = ToonIcons.all.single { it.name == (o["icon"]?.jsonPrimitive?.content ?: "calendar") }
                EngineFrames.compare(
                    cell,
                    toonChipScene(cell.tokens, variant, label, label.length * 15 * .55f, icon, look),
                )
            }
        println("chip frames: ${cells.size} cells, worst coordinate $worst dp")
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `selection follows the engine's chipSelect at every millisecond`() {
        val c =
            Json
                .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText())
                .jsonObject
                .getValue("chipSelect(t)")
                .jsonObject
        val at = c.getValue("at").jsonArray.map { it.jsonPrimitive.float }
        val scale = c.getValue("scale").jsonArray.map { it.jsonPrimitive.float }
        val swipe = c.getValue("swipe").jsonArray.map { it.jsonPrimitive.float }
        val worst =
            at.indices.maxOf {
                maxOf(
                    abs(scale[it] - toonChipSelect(at[it]).scale),
                    abs(swipe[it] - toonChipSelect(at[it]).swipe),
                )
            }
        println("chipSelect: worst $worst")
        assertTrue(worst <= 1e-5f, "chipSelect off the engine by $worst")
    }

    @Test
    fun `under reduce motion a selected chip is painted at once`() {
        val look = toonChipFrameAt(selected = true, selectAgeMs = 60f, p = 0f, reduceMotion = true)
        assertEquals(1f, look.scale)
        assertEquals(1f, look.swipe)
        assertEquals(
            toonChipSelect(60f).swipe,
            toonChipFrameAt(selected = true, selectAgeMs = 60f, p = 0f, reduceMotion = false).swipe,
        )
    }

    @Test
    fun `the touch target is 48 dp round a 36 dp chip, and a filter chip toggles`() =
        runComposeUiTest {
            val changes = mutableListOf<Boolean>()
            setContent { ToonTheme { ToonChip("Outdoors", onSelectedChange = { changes += it }) } }
            val node = onNodeWithText("Outdoors")
            node.assertHeightIsEqualTo(48.dp)
            assertTrue(node.getBoundsInRoot().let { it.right - it.left } >= 48.dp)
            node.performClick()
            node.assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            assertEquals(listOf(true), changes)
        }
}
