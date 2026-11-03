package io.github.youndie.toon.parity

import io.github.youndie.toon.component.toonTipInflate
import io.github.youndie.toon.component.toonTooltipScene
import io.github.youndie.toon.component.toonTooltipWidth
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.tokens.ToonTokens

/**
 * B-40: the v1 page's tooltip cells and inflate strip. The balloon is as wide as its text, which the
 * engine measures: on a canvas in Chrome, as length × 14 × 0.55 in Node. [textWidth] is that width,
 * Compose's own measure for the fixtures and Node's for the frame test. [asDesigned] draws the tail as
 * the engine does (B-60), for the frame test.
 */
internal object TipFrames {
    const val TEXT = "Tap to add a plan"

    /** A page cell: the balloon placed with its tail's tip at the bottom middle of a box its width. */
    fun cell(
        tokens: ToonTokens,
        text: String,
        s: Float,
        textWidth: Float,
        asDesigned: Boolean = false,
    ): ToonScene = toonTooltipScene(tokens, text, textWidth, s, asDesigned).placed(toonTooltipWidth(textWidth) / 2, 46f)

    /** The page's inflate strip at [t] ms, the tip at (100, 52). */
    fun inflate(
        tokens: ToonTokens,
        t: Float,
        textWidth: Float,
        asDesigned: Boolean = false,
    ): ToonScene = toonTooltipScene(tokens, TEXT, textWidth, toonTipInflate(t, true), asDesigned).placed(100f, 52f)
}
