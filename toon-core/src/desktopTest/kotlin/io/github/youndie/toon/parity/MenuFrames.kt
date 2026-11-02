package io.github.youndie.toon.parity

import io.github.youndie.toon.component.ToonIconButtonLook
import io.github.youndie.toon.component.ToonIconButtonVariant
import io.github.youndie.toon.component.ToonMenuItem
import io.github.youndie.toon.component.ToonMenuLook
import io.github.youndie.toon.component.toonIconButtonScene
import io.github.youndie.toon.component.toonMenuScene
import io.github.youndie.toon.component.toonMenuUnroll
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.tokens.ToonTokens

/** B-37: the v1 page's menu, its three items and their icons. */
internal object MenuFrames {
    val items =
        listOf(
            ToonMenuItem("Edit", ToonIcons.Edit),
            ToonMenuItem("Share", ToonIcons.Share),
            ToonMenuItem("Delete", ToonIcons.Delete),
        )

    /** The page's `menuScene` at [t] ms into an unroll ([open]) or a roll-up: the trigger, and the scroll under it. */
    fun strip(
        tokens: ToonTokens,
        open: Boolean,
        t: Float,
    ): ToonScene {
        val u = toonMenuUnroll(t, open)
        val trigger =
            toonIconButtonScene(
                tokens,
                ToonIcons.More,
                ToonIconButtonVariant.Standard,
                ToonIconButtonLook(),
            ).placed(186f, 4f)
        return if (u >
            .01f
        ) {
            trigger + toonMenuScene(tokens, 200f, items, ToonMenuLook(u = u)).placed(34f, 58f)
        } else {
            trigger
        }
    }
}
