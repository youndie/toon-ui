package io.github.youndie.toon.parity

import io.github.youndie.toon.component.ToonSnackbarType
import io.github.youndie.toon.component.toonSnackbarFrameAt
import io.github.youndie.toon.component.toonSnackbarScene
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.tokens.ToonTokens

/**
 * B-23: the Core page's snackbar strips (`specs.snk`) as the kit draws them at each age, 40 dp down in
 * the page's 120 dp frame. The page code is held to its text by scripts/component-cells.mjs.
 */
internal object SnackFrames {
    /** A show ([shown]) or hide frame [t] ms after the change: the info sign "Plan added" / "Undo". */
    fun strip(
        tokens: ToonTokens,
        shown: Boolean,
        t: Float,
    ): ToonScene {
        val moment = checkNotNull(toonSnackbarFrameAt(shown, t, reduceMotion = false)) { "nothing drawn at $t ms" }
        return toonSnackbarScene(tokens, 342f, "Plan added", "Undo", ToonSnackbarType.Info, moment).placed(0f, 40f)
    }
}
