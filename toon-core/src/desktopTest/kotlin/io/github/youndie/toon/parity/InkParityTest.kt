package io.github.youndie.toon.parity

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-12: parity measured against the ink, not the cell, for every reference the floors do not come
 * from. B-14 adds a second floor for cells with text (see [WITH_TEXT]). viddik's percentage is of the whole cell, and
 * an effect covers a small part of its 260 × 150 cell, so a wrong effect stays under the Divider's
 * 1.54 % floor: stars spinning one way measured 0.56 %, sweat drops without their stretch 0.50 %.
 * Here a cell's differing pixels are counted against the pixels either image inks (differs from
 * the stage), and the floor is that same ratio over the Divider cells, the rasterisers' difference
 * alone (research D3). Both sides are committed files: the Chrome reference and the viddik golden.
 */
class InkParityTest {
    @Test
    fun `every cell is within its ink floor`() {
        val floor = InkRatio.shapes
        val textFloor = InkRatio.text
        val all =
            InkRatio
                .cells(null)
                .filterNot { it.startsWith("Divider_") || it.startsWith("TextFloor_") }
                .associateWith(InkRatio::ofCell)

        fun floorOf(cell: String) = if (cell.substringBefore('_') in WITH_TEXT) textFloor else floor
        for ((label, cells) in all.entries.groupBy { it.key.substringBefore('_') in WITH_TEXT }) {
            val worst = cells.maxBy { it.value }
            println(
                "${if (label) "with text" else "shapes"}: floor ${pct(
                    if (label) textFloor else floor,
                )}, worst of ${cells.size}: ${worst.key} ${pct(worst.value)}",
            )
        }
        val over = all.filter { (cell, ratio) -> ratio > floorOf(cell) }
        assertTrue(
            over.isEmpty(),
            "over the ink floor: ${over.mapValues { "${pct(it.value)} > ${pct(floorOf(it.key))}" }}",
        )
    }

    private fun pct(v: Double) = "%.2f %%".format(v * 100)

    private companion object {
        /**
         * Groups whose cells carry text, read against the text floor: the `TextFloor` cells, a label
         * alone, which Chrome inks through CoreText and Compose through Skia with whole-pixel advances.
         * The button and the tabs, whose labels carry most of their ink, have a twin without them
         * (`ButtonShape`, `TabsShape`), held to the Divider's floor.
         */
        val WITH_TEXT =
            setOf("Button", "Badge", "Checkbox", "Radio", "TextField", "Chip", "Card", "ListItem", "Tabs")
    }
}
