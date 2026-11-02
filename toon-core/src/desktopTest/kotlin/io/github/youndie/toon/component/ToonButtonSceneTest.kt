package io.github.youndie.toon.component

import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-14: every cell of the Core page's button, as the port builds it, against the frame the engine's
 * `btn` returns for it ([EngineFrames]).
 */
class ToonButtonSceneTest {
    private fun look(o: JsonObject) =
        ToonButtonLook(
            p = o.f("p") ?: 0f,
            pl = o.f("pl") ?: o.f("p") ?: 0f,
            br = o.f("br") ?: 1f,
            seed = o["seed"]?.jsonPrimitive?.int ?: 0,
            disabled = o["disabled"]?.jsonPrimitive?.boolean ?: false,
            focus = o["focus"]?.jsonPrimitive?.boolean ?: false,
            lt = o.f("lt"),
            spin = o.f("spin") ?: 0f,
        )

    @Test
    fun `every cell is the engine's frame layer by layer`() {
        val cells = EngineFrames.cells("Button")
        val worst =
            cells.maxOf { cell ->
                val variant =
                    ToonButtonVariant.valueOf(
                        cell.opts.getValue("v").jsonPrimitive.content.replaceFirstChar {
                            it.uppercase()
                        },
                    )
                EngineFrames.compare(
                    cell,
                    toonButtonScene(cell.tokens, variant, 150f, "Add plan", LABEL_WIDTH, look(cell.opts)),
                )
            }
        println("button frames: ${cells.size} cells, worst coordinate $worst dp")
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    private companion object {
        /** `tw` without a DOM: "Add plan".length × 18 × 0.55, the width the goldens were drawn with. */
        const val LABEL_WIDTH = 8 * 18 * .55f
    }
}
