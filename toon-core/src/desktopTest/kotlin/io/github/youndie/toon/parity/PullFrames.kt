package io.github.youndie.toon.parity

import io.github.youndie.toon.component.ToonCardLook
import io.github.youndie.toon.component.ToonPullLook
import io.github.youndie.toon.component.toonCardScene
import io.github.youndie.toon.component.toonPullRelease
import io.github.youndie.toon.component.toonPullScene
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.max

/**
 * B-42: the v1 page's `pullScene` — its three cards moved down by [off] (70 % of the pull unless the
 * page says otherwise) under the strip, 360 × 260 — for the fixtures and the frame test. The page code
 * is held to its text by scripts/component-cells.mjs.
 */
internal object PullFrames {
    private val CARDS =
        listOf(
            Triple("Pancake breakfast", "Kitchen, bring syrup", "9:00"),
            Triple("Bike to the lake", "Check the tyre pump", "11:30"),
            Triple("Paint the fence", "Green this time", "14:00"),
        )

    fun scene(
        tokens: ToonTokens,
        look: ToonPullLook,
        off: Float = look.d * .7f,
    ): ToonScene {
        var bg = ToonScene.Empty
        CARDS.forEachIndexed { i, (title, meta, time) ->
            bg +=
                toonCardScene(
                    tokens,
                    i,
                    324f,
                    title,
                    meta,
                    time,
                    ToonCardLook(),
                ).placed(18f, 24f + i * 102, 1f, tokens.shape.tilts[i], 162f, 42f)
        }
        return bg.placed(0f, 30 + off) + toonPullScene(tokens, 360f, look)
    }

    /** The page's pull strip, [d] dp down. */
    fun pull(
        tokens: ToonTokens,
        d: Float,
    ): ToonScene = scene(tokens, ToonPullLook(d = d, bsy = 1 + d / 240, bsx = 1 - d / 600))

    /** The page's release strip, [t] ms after letting go 110 dp down. */
    fun release(
        tokens: ToonTokens,
        t: Float,
    ): ToonScene = scene(tokens, toonPullRelease(t, 110f), 54 + (77 - 54) * max(0f, 1 - t / 160))
}
