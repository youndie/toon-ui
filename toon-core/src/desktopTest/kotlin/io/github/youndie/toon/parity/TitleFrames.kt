package io.github.youndie.toon.parity

import io.github.youndie.toon.component.ToonButtonLook
import io.github.youndie.toon.component.ToonButtonVariant
import io.github.youndie.toon.component.ToonTitleLook
import io.github.youndie.toon.component.toonButtonScene
import io.github.youndie.toon.component.toonSplashAt
import io.github.youndie.toon.component.toonTitleCardScene
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.fx.toonStarsScene
import io.github.youndie.toon.tokens.ToonTokens

/**
 * B-43: the v1 page's title card cells and its enter strip — the page's `splashScene` on a 360 × 520
 * screen, and the empty state with its button. The page code is held to its text by
 * scripts/component-cells.mjs. [labelWidth] measures the button's label.
 */
internal object TitleFrames {
    private const val SPLASH_SUB = "Plans for the good part of the week"

    /** The splash cell: the card at rest, its rings unmoved. */
    fun splash(tokens: ToonTokens): ToonScene = toonTitleCardScene(tokens, 360f, 520f, ToonTitleLook(sub = SPLASH_SUB))

    /** The page's enter strip at [t] ms: `splashAt`, and the stars bursting over the plaque. */
    fun enter(
        tokens: ToonTokens,
        t: Float,
    ): ToonScene {
        val a = toonSplashAt(t)
        return toonTitleCardScene(
            tokens,
            360f,
            520f,
            ToonTitleLook(sub = SPLASH_SUB, s = a.s, phase = a.phase, textOp = a.textOp),
        ) +
            toonStarsScene(tokens.colors, a.stars, 180f, 520 * .42f - 60, 90f)
    }

    /** The empty state cell, 360 × 420, with its "Add a plan" button. */
    fun empty(
        tokens: ToonTokens,
        labelWidth: (String) -> Float,
    ): ToonScene =
        toonTitleCardScene(
            tokens,
            360f,
            420f,
            ToonTitleLook(
                title = "Nothing yet",
                sub = "Your weekend is wide open",
                cy = 170f,
                pw = 260f,
                ph = 110f,
                fs = 34f,
            ),
        ) +
            toonButtonScene(
                tokens,
                ToonButtonVariant.Filled,
                220f,
                "Add a plan",
                labelWidth("Add a plan"),
                ToonButtonLook(),
            ).placed(70f, 300f)
}
