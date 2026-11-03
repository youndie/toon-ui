package io.github.youndie.toon.parity

import io.github.youndie.toon.draw.EngineFrames
import kotlin.test.Test
import kotlin.test.assertTrue

/** D13: a departure names cells that exist, so a stale entry cannot quietly skip nothing — or something new. */
class ToonDeparturesTest {
    @Test
    fun `every departure names a cell of the engine goldens or a reference`() {
        val names = EngineFrames.allCellNames() + InkRatio.cells(null).map { it.removeSuffix(".png") }
        for ((prefix, item) in ToonDepartures.cells) {
            assertTrue(names.any { it.startsWith(prefix) }, "$prefix ($item) names no cell")
        }
    }
}
