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
        )

    /** The item a cell departed in, or null when it is still the design's. */
    fun of(cell: String): String? = cells.entries.firstOrNull { cell.startsWith(it.key) }?.value
}
