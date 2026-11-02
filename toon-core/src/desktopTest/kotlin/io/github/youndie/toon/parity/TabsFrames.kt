package io.github.youndie.toon.parity

import io.github.youndie.toon.component.ToonTabsLook
import io.github.youndie.toon.component.toonTabsScene
import io.github.youndie.toon.component.toonTabsSwitch
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.tokens.ToonTokens

/** B-28: the MVP page's switch strip (`specs.tabs`), from the first tab to the second, 114 dp slots. */
internal fun toonTabsSwitchScene(
    tokens: ToonTokens,
    t: Float,
): ToonScene {
    val a = toonTabsSwitch(t, 0, 1, 114f)
    return toonTabsScene(
        tokens,
        342f,
        listOf("Today", "Weekend", "Someday"),
        ToonTabsLook(sel = a.sel!!, ia = a.ia, ib = a.ib),
    )
}
