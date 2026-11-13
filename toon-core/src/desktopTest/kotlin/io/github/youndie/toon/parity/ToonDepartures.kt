package io.github.youndie.toon.parity

/**
 * Where the code departs from the design on purpose (research D13): a cell name prefix, as in the
 * engine goldens (`<group>_<cell>`) and the snapshot files, and the item that departed. The
 * engine-frame and ink-parity checks skip a listed cell and nothing else; every other cell is still
 * held to the design, and `ToonDeparturesTest` keeps each entry naming cells that exist.
 */
internal object ToonDepartures {
    val cells: Map<String, String> =
        mapOf(
            // The filled half of the rope hatched from the ball, so it moves with a drag.
            "Slider_" to "B-58",
            // The tail's paper reaches up into the balloon's, so no ink hairline crosses the joint.
            "Tooltip_" to "B-60",
            // The rolls lose the engine's crease, a 35 % ink line that read as a stripe through them;
            // every cell that shows the scroll. Unroll_0ms and RollUp_160ms show the trigger alone.
            "Menu_Open_" to "B-61",
            "Menu_Item" to "B-61",
            "Menu_Unrolling_" to "B-61",
            "Menu_Unroll_60ms_" to "B-61",
            "Menu_Unroll_120ms_" to "B-61",
            "Menu_Unroll_200ms_" to "B-61",
            "Menu_Unroll_360ms_" to "B-61",
            "Menu_RollUp_0ms_" to "B-61",
            "Menu_RollUp_60ms_" to "B-61",
            "Menu_RollUp_120ms_" to "B-61",
            "MenuProbe_" to "B-61",
            // The pointer rule's hover over those two (B-69) is drawn on the departed component.
            "Hover_Slider_" to "B-58",
            "Hover_Menu_" to "B-61",
        )

    /** The item a cell departed in, or null when it is still the design's. */
    fun of(cell: String): String? = cells.entries.firstOrNull { cell.startsWith(it.key) }?.value
}
