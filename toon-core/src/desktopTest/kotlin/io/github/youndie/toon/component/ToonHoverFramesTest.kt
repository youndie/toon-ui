package io.github.youndie.toon.component

import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.parity.CardFrames
import io.github.youndie.toon.parity.MenuFrames
import io.github.youndie.toon.tokens.ToonTokens
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-69: the pointer rule's hover and focus on every kit component the pointer page draws
 * (`Desktop Pointer & Keyboard.dc.html`, section 01), against the frames the engine and its desktop
 * extension return for them (`engine-goldens/desktop.json`, groups `Hover` and `Focus`). Each cell is
 * one of the component's own rest cells, hovered or focused, built here as that component's frame test
 * builds the rest cell.
 */
class ToonHoverFramesTest {
    private fun JsonObject.flag(k: String) = this[k]?.jsonPrimitive?.booleanOrNull ?: false

    private fun JsonObject.text(k: String) = this[k]?.jsonPrimitive?.content

    private fun scene(
        name: String,
        tokens: ToonTokens,
        o: JsonObject,
        hv: Float,
    ): ToonScene =
        when (name.substringBefore('_')) {
            "Button" -> {
                toonButtonScene(
                    tokens,
                    ToonButtonVariant.valueOf(o.text("v")!!.replaceFirstChar { it.uppercase() }),
                    150f,
                    "Add plan",
                    BUTTON_LABEL_WIDTH,
                    ToonButtonLook(focus = o.flag("focus"), hv = hv),
                )
            }

            "IconButton" -> {
                toonIconButtonScene(
                    tokens,
                    ToonIcons.all.single { it.name == o.text("icon") },
                    if (o.text("variant") == "filled") ToonIconButtonVariant.Filled else ToonIconButtonVariant.Standard,
                    ToonIconButtonLook(hv = hv),
                )
            }

            "Fab" -> {
                toonFabScene(tokens, ToonIcons.Plus, "", 0f, ToonFabLook(hv = hv))
            }

            "Chip" -> {
                val label = o.text("label")!!
                val sel = o.flag("sel")
                toonChipScene(
                    tokens,
                    ToonChipVariant.valueOf(o.text("variant")!!.replaceFirstChar { it.uppercase() }),
                    label,
                    label.length * 15 * .55f,
                    ToonIcons.all.single { it.name == (o.text("icon") ?: "calendar") },
                    ToonChipLook(selected = sel, hv = hv),
                )
            }

            "Card" -> {
                CardFrames.state(
                    tokens,
                    o.getValue("i").jsonPrimitive.int,
                    ToonCardLook(focus = o.flag("focus"), hv = hv),
                )
            }

            "Slider" -> {
                toonSliderScene(tokens, 260f, o.f("value")!!, ToonSliderLook(hv = hv), asDesigned = true)
            }

            "Menu" -> {
                val over = o["hover"]?.jsonPrimitive?.intOrNull
                toonMenuScene(
                    tokens,
                    200f,
                    MenuFrames.items,
                    ToonMenuLook(hovered = MenuFrames.items.indices.map { if (it == over) hv else 0f }),
                    asDesigned = true,
                )
            }

            "ListItem" -> {
                toonListItemScene(
                    tokens,
                    342f,
                    ToonIcons.all.single { it.name == o.text("icon") },
                    o.text("title")!!,
                    o.text("sub") ?: "",
                    ToonListTrailing.valueOf(o.text("trailing")!!.replaceFirstChar { it.uppercase() }),
                    ToonListItemLook(seed = o.getValue("seed").jsonPrimitive.int, hv = hv),
                )
            }

            else -> {
                error("no scene for the hover cell $name")
            }
        }

    @Test
    fun `every hovered component is the page's frame layer by layer`() {
        // Hovered: the slider and the menu are drawn as designed, as their own frame tests draw them.
        val cells = EngineFrames.cells("Hover", asDesigned = true)
        val worst = cells.maxOf { EngineFrames.compare(it, scene(it.name, it.tokens, it.opts, hv = 1f)) }
        println("hover frames: ${cells.size} cells, worst coordinate $worst dp")
        assertEquals(42, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `hovered and pressed all the way down, a body is the kit's pressed body`() {
        // The lift fades as the press drops the shadow, the extension's `liftBody`: nothing of it is
        // left at p = 1, so the pressed cells hold it.
        EngineFrames.cells("Button").filter { it.name.startsWith("Filled_Pressed_") }.forEach { cell ->
            EngineFrames.compare(
                cell,
                toonButtonScene(
                    cell.tokens,
                    ToonButtonVariant.Filled,
                    150f,
                    "Add plan",
                    BUTTON_LABEL_WIDTH,
                    ToonButtonLook(p = 1f, hv = 1f),
                ),
            )
        }
    }

    @Test
    fun `the card's focus ring is the page's`() {
        val cells = EngineFrames.cells("Focus")
        val worst = cells.maxOf { EngineFrames.compare(it, scene(it.name, it.tokens, it.opts, hv = 0f)) }
        assertEquals(3, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `away from the pointer every hovered cell is its rest cell`() {
        // hv 0 draws what the component drew before B-69: the rest cell the hover cell was made from.
        EngineFrames.cells("Hover", asDesigned = true).forEach { hovered ->
            val base = hovered.name.substringBefore('_')
            val rest =
                EngineFrames
                    .cells(base, asDesigned = true)
                    .single { it.name == hovered.name.removePrefix("${base}_") }
            EngineFrames.compare(
                rest,
                scene(hovered.name, hovered.tokens, hovered.opts.minus("hover").let(::JsonObject), hv = 0f),
            )
        }
    }

    private companion object {
        /** `tw` without a DOM: "Add plan".length × 18 × 0.55, the width the goldens were drawn with. */
        const val BUTTON_LABEL_WIDTH = 8 * 18 * .55f
    }
}
