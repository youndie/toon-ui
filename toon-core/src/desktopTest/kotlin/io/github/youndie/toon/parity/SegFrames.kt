package io.github.youndie.toon.parity

import io.github.youndie.toon.component.ToonSegmentedLook
import io.github.youndie.toon.component.toonSegSwitch
import io.github.youndie.toon.component.toonSegmentedScene
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.tokens.ToonTokens

/** B-41: the v1 page's switch strip (`specs.seg`), from Day to Month, the page's 86.7 dp slots. */
internal object SegFrames {
    fun switch(
        tokens: ToonTokens,
        t: Float,
    ): ToonScene {
        val a = toonSegSwitch(t, 0, 2, 86.7f)
        return toonSegmentedScene(
            tokens,
            260f,
            listOf("Day", "Week", "Month"),
            ToonSegmentedLook(sel = a.sel!!, ia = a.ia, ib = a.ib, sq = a.sq),
        )
    }
}
