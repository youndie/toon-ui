package io.github.youndie.toon.parity

import io.github.youndie.toon.component.ToonCardLook
import io.github.youndie.toon.component.toonCardAppear
import io.github.youndie.toon.component.toonCardDismiss
import io.github.youndie.toon.component.toonCardScene
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.fx.toonDustScene
import io.github.youndie.toon.fx.toonHoleScene
import io.github.youndie.toon.fx.toonSpeedScene
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.min

/**
 * B-21: the Core page's card cells and strips as the page composes them from the engine (`toon-ui
 * Core.dc.html`: `items`, `cCells`, `specs.card`, `cardStack`), for the fixtures and the frame test.
 * The page code is held to its text by scripts/component-cells.mjs.
 */
internal object CardFrames {
    /** The page's three demo items. */
    val ITEMS =
        listOf(
            Triple("Pancake breakfast", "Kitchen, bring syrup", "9:00"),
            Triple("Bike to the lake", "Check the tyre pump", "11:30"),
            Triple("Paint the fence", "Green this time", "14:00"),
        )

    private fun card(
        tokens: ToonTokens,
        i: Int,
        look: ToonCardLook,
    ): ToonScene = ITEMS[i].let { (title, meta, time) -> toonCardScene(tokens, i, 342f, title, meta, time, look) }

    /** `cCells`: a state cell, item `i % 3`, tilted by the theme's turn (one degree more when pressed). */
    fun state(
        tokens: ToonTokens,
        i: Int,
        look: ToonCardLook,
    ): ToonScene =
        card(tokens, i % 3, look).placed(
            0f,
            0f,
            1f,
            tokens.shape.tilts[i % 3] + (if (look.press > 0) -1f else 0f),
            171f,
            42f,
        )

    /** The appear strip at [t] ms: the first card falling, and its dust at both corners. */
    fun appear(
        tokens: ToonTokens,
        t: Float,
    ): ToonScene {
        val a = toonCardAppear(t, tokens.shape.tilts[0])
        return card(tokens, 0, ToonCardLook(sx = a.sx, sy = a.sy)).placed(0f, a.dy, a.op, a.rot, 171f, 42f) +
            toonDustScene(tokens.colors, a.du, 34f, 86f, 36f) +
            toonDustScene(tokens.colors, a.du, 308f, 86f, 36f)
    }

    /** The press strip at press [p]: leaning one degree. */
    fun press(
        tokens: ToonTokens,
        p: Float,
    ): ToonScene = card(tokens, 0, ToonCardLook(press = p)).placed(0f, 0f, 1f, tokens.shape.tilts[0] - p, 171f, 42f)

    /** `cardStack(T, t, 'appear')` (B-46): three cards falling 40 ms apart, each with its dust over all of them. */
    fun cascade(
        tokens: ToonTokens,
        t: Float,
    ): ToonScene {
        var parts = ToonScene.Empty
        var over = ToonScene.Empty
        for (i in 0 until 3) {
            val a = toonCardAppear(t - 40 * i, tokens.shape.tilts[i])
            val yy = i * 102f + a.dy
            if (a.op >
                .01f
            ) {
                parts +=
                    card(tokens, i, ToonCardLook(sx = a.sx, sy = a.sy)).placed(0f, yy, a.op, a.rot, 171f, 42f)
            }
            if (a.du > 0 && a.du < 1) {
                over +=
                    toonDustScene(tokens.colors, a.du, 34f, yy + 86, 36f) +
                    toonDustScene(tokens.colors, a.du, 308f, yy + 86, 36f)
            }
        }
        return parts + over
    }

    /**
     * `cardStack(T, t, 'dismiss')`: three cards, the middle one thrown, the one below closing up; without
     * the [hole] it punches into the paper, as a `ToonCardList` throws (B-66).
     */
    fun dismiss(
        tokens: ToonTokens,
        t: Float,
        hole: Boolean = true,
    ): ToonScene {
        val d = toonCardDismiss(t)
        var under = ToonScene.Empty
        var parts = ToonScene.Empty
        val rs = tokens.shape.radiusScale
        for (i in 0 until 3) {
            val base = i * 102f
            val rot = tokens.shape.tilts[i]
            var dx = 0f
            var sx = 1f
            var sy = 1f
            var op = 1f
            var yy = base
            if (i == 1) {
                dx = d.dx
                sx = d.sx
                sy = d.sy
                op = d.op
                under += toonSpeedScene(tokens.colors, d.lu, min(dx, 342f), base, 84f)
                val hu = (t - 170) / 300
                if (hole && hu >= 0 && hu < 1) {
                    under +=
                        toonHoleScene(
                            tokens.colors,
                            hu,
                            0f,
                            0f,
                            342f,
                            84f,
                            ToonRadii(11 * rs, 17 * rs, 13 * rs, 19 * rs),
                            5,
                        ).placed(0f, base, 1f, rot, 171f, 42f)
                }
            }
            if (i == 2) yy += d.n * 102
            if (op > .01f) parts += card(tokens, i, ToonCardLook(sx = sx, sy = sy)).placed(dx, yy, op, rot, 171f, 42f)
        }
        return under + parts
    }
}
