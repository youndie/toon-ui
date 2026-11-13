package io.github.youndie.toon.component

import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.parity.CardFrames
import io.github.youndie.toon.parity.DrawerFrames
import io.github.youndie.toon.parity.MenuFrames
import io.github.youndie.toon.parity.NavFrames
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
 *
 * B-79: the same for the pressables the page does not draw — checkbox, radio, switch, segmented
 * button, tabs, navigation bar, drawer and text field — each look this repository's decision, made
 * in the engine's own renderer under the page's two rules (scripts/desktop-cells.mjs says how).
 */
class ToonHoverFramesTest {
    private fun JsonObject.flag(k: String) = this[k]?.jsonPrimitive?.booleanOrNull ?: false

    private fun JsonObject.text(k: String) = this[k]?.jsonPrimitive?.content

    /** Each of [n] slots' hover: [hv] on the one the cell's `hover` names, 0 elsewhere. */
    private fun JsonObject.slots(
        n: Int,
        hv: Float,
    ): List<Float> {
        val over = this["hover"]?.jsonPrimitive?.intOrNull
        return List(n) { if (it == over) hv else 0f }
    }

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

            "Checkbox" -> {
                toonCheckboxScene(
                    tokens,
                    o.text("label")!!,
                    ToonCheckboxLook(on = o.flag("on"), focus = o.flag("focus"), hv = hv),
                )
            }

            "Radio" -> {
                toonRadioScene(
                    tokens,
                    o.text("label")!!,
                    ToonRadioLook(selected = o.flag("sel"), focus = o.flag("focus"), hv = hv),
                )
            }

            "Switch" -> {
                toonSwitchScene(tokens, ToonSwitchLook(on = o.flag("on"), focus = o.flag("focus"), hv = hv))
            }

            "Segmented" -> {
                toonSegmentedScene(
                    tokens,
                    260f,
                    listOf("Day", "Week", "Month"),
                    ToonSegmentedLook(o.getValue("sel").jsonPrimitive.int, hovered = o.slots(3, hv)),
                )
            }

            "Tabs" -> {
                toonTabsScene(
                    tokens,
                    342f,
                    listOf("Today", "Weekend", "Someday"),
                    ToonTabsLook(o.getValue("sel").jsonPrimitive.int, hovered = o.slots(3, hv)),
                )
            }

            "NavigationBar" -> {
                toonNavBarScene(
                    tokens,
                    390f,
                    NavFrames.ITEMS,
                    ToonNavLook(o.getValue("sel").jsonPrimitive.int, hovered = o.slots(NavFrames.ITEMS.size, hv)),
                )
            }

            "NavigationDrawer" -> {
                DrawerFrames.scene(tokens, ToonDrawerMotion(), hovered = o.slots(DrawerFrames.items.size, hv))
            }

            "TextField" -> {
                toonFieldScene(
                    tokens,
                    280f,
                    ToonFieldLook(
                        label = o.text("label")!!,
                        value = o.text("value") ?: "",
                        helper = o.text("helper") ?: "",
                        focus = o.flag("focus"),
                        error = o.flag("error"),
                        caretX = if (o.flag("caret")) engineCaretX(o.text("value") ?: "") else null,
                        hv = hv,
                    ),
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
        assertEquals(90, cells.size)
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

    private fun rest(
        group: String,
        name: String,
    ) = EngineFrames
        .cells(group)
        .filter { it.name.removePrefix("${name}_") in THEMES }
        .also { assertEquals(3, it.size, name) }

    @Test
    fun `hovered and pressed all the way down, a lifted control is its page's pressed cell`() {
        // The lift fades as the press goes down, as the extension's `liftBody` has it (B-79).
        rest("Checkbox", "Pressed").forEach {
            val look = toonCheckboxFrameAt(on = false, toggleAgeMs = null, p = 1f, reduceMotion = false, hv = 1f)
            EngineFrames.compare(it, toonCheckboxScene(it.tokens, "Bring snacks", look))
        }
        rest("Radio", "Pressed").forEach {
            val look = toonRadioFrameAt(0, 0, -1, null, p = 1f, reduceMotion = false, hv = 1f)
            EngineFrames.compare(it, toonRadioScene(it.tokens, "Noon", look))
        }
        rest("Switch", "Pressed").forEach {
            val look = toonSwitchFrameAt(on = true, toggleAgeMs = null, p = 1f, reduceMotion = false, hv = 1f)
            EngineFrames.compare(it, toonSwitchScene(it.tokens, look))
        }
    }

    @Test
    fun `a disabled control shows no hover`() {
        listOf(
            "Checkbox" to "Disabled",
            "Checkbox" to "Disabled_Checked",
            "Radio" to "Disabled",
            "Switch" to "Disabled",
        ).forEach { (group, name) ->
            rest(group, name).forEach {
                val o = it.opts
                val scene =
                    when (group) {
                        "Checkbox" -> {
                            toonCheckboxScene(
                                it.tokens,
                                "Bring snacks",
                                ToonCheckboxLook(on = o.flag("on"), disabled = true, hv = 1f),
                            )
                        }

                        "Radio" -> {
                            toonRadioScene(it.tokens, "Noon", ToonRadioLook(disabled = true, hv = 1f))
                        }

                        else -> {
                            toonSwitchScene(it.tokens, ToonSwitchLook(disabled = true, hv = 1f))
                        }
                    }
                EngineFrames.compare(it, scene)
            }
        }
        rest("TextField", "Disabled").forEach {
            EngineFrames.compare(
                it,
                toonFieldScene(it.tokens, 280f, ToonFieldLook(label = "Plan name", disabled = true, hv = 1f)),
            )
        }
        rest("Segmented", "Disabled").forEach {
            val look = ToonSegmentedLook(0, disabled = true, hovered = listOf(0f, 1f, 1f))
            EngineFrames.compare(it, toonSegmentedScene(it.tokens, 260f, listOf("Day", "Week", "Month"), look))
        }
        rest("Tabs", "SomedayDisabled").forEach {
            val look = ToonTabsLook(0, disabled = 2, hovered = listOf(0f, 0f, 1f))
            EngineFrames.compare(it, toonTabsScene(it.tokens, 342f, listOf("Today", "Weekend", "Someday"), look))
        }
        rest("NavigationBar", "SavedDisabled").forEach {
            val look = ToonNavLook(0, disabled = 2, hovered = listOf(0f, 0f, 1f, 0f))
            EngineFrames.compare(it, toonNavBarScene(it.tokens, 390f, NavFrames.ITEMS, look))
        }
    }

    @Test
    fun `a focused field shows no hover`() {
        rest("TextField", "Focused").forEach {
            val look =
                ToonFieldLook(
                    label = "Plan name",
                    value = "Pancakes",
                    focus = true,
                    caretX = engineCaretX("Pancakes"),
                    hv = 1f,
                )
            EngineFrames.compare(it, toonFieldScene(it.tokens, 280f, look))
        }
    }

    @Test
    fun `the selected slot, which a press leaves as it is, takes no plate`() {
        val on = listOf(1f, 0f, 0f, 0f)
        rest("Segmented", "Day").forEach {
            val look = ToonSegmentedLook(0, hovered = on.take(3))
            EngineFrames.compare(it, toonSegmentedScene(it.tokens, 260f, listOf("Day", "Week", "Month"), look))
        }
        rest("Tabs", "Today").forEach {
            val look = ToonTabsLook(0, hovered = on.take(3))
            EngineFrames.compare(it, toonTabsScene(it.tokens, 342f, listOf("Today", "Weekend", "Someday"), look))
        }
        rest("NavigationBar", "HomeSelected").forEach {
            EngineFrames.compare(it, toonNavBarScene(it.tokens, 390f, NavFrames.ITEMS, ToonNavLook(0, hovered = on)))
        }
        rest("NavigationDrawer", "Open").forEach {
            EngineFrames.compare(it, DrawerFrames.scene(it.tokens, ToonDrawerMotion(), hovered = on))
        }
    }

    @Test
    fun `a pressed slot under the pointer is its page's pressed cell`() {
        // The press plate is whole; the hover adds nothing to it.
        rest("Tabs", "SomedayPressed").forEach {
            val look = ToonTabsLook(0, pressed = 2, hovered = listOf(0f, 0f, .4f))
            EngineFrames.compare(it, toonTabsScene(it.tokens, 342f, listOf("Today", "Weekend", "Someday"), look))
        }
        rest("NavigationDrawer", "ItemPressed").forEach {
            EngineFrames.compare(
                it,
                DrawerFrames.scene(it.tokens, ToonDrawerMotion(), pressed = 2, hovered = listOf(0f, 0f, .4f, 0f)),
            )
        }
    }

    private companion object {
        /** The three themes a cell's name ends in. */
        val THEMES = setOf("InkGouacheLight", "InkGouacheNight", "BrightCel")

        /** `tw` without a DOM: "Add plan".length × 18 × 0.55, the width the goldens were drawn with. */
        const val BUTTON_LABEL_WIDTH = 8 * 18 * .55f
    }
}
