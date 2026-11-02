package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonSegments
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.inkLinePoints
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens

// A port of the engine's `divider` and the kit's `C.divider` (B-22): a tapering ink stroke, thickest
// in the middle, with no motion of its own.

/** The `weight` prop of `ToonDivider.dc.html`: the stroke's thickness at its middle. */
public enum class ToonDividerWeight(
    internal val thickness: Float,
) {
    S(2.4f),
    M(3f),
    L(4.6f),
}

/** The design's divider: an ink stroke [width] wide, starting 56 dp in when [inset]; [seed] varies its wobble. */
@Composable
public fun ToonDivider(
    modifier: Modifier = Modifier,
    width: Dp = 342.dp,
    inset: Boolean = false,
    weight: ToonDividerWeight = ToonDividerWeight.M,
    seed: Int = 0,
) {
    val tokens = Toon.tokens
    val w = width.value
    Canvas(modifier.size(width, DIVIDER_H.dp)) {
        // The engine draws in dp (research D5); the canvas is pixels (B-47).
        inDp {
            drawScene(toonDividerScene(tokens, if (inset) INSET else 0f, w, weight.thickness, seed))
        }
    }
}

/** The kit's box: 12 dp, the stroke on its middle. */
private const val DIVIDER_H = 12f
private const val INSET = 56f

/** The engine's `divider`: `inkLine` from [x0] to [x1] on y 6, [th] thick. */
@OptIn(InternalToonApi::class)
internal fun toonDividerScene(
    tokens: ToonTokens,
    x0: Float,
    x1: Float,
    th: Float,
    seed: Int,
): ToonScene =
    ToonScene(
        listOf(
            ToonLayer(
                inkLinePoints(x0, x1, 6f, th, tokens.shape.seed + 970 + seed),
                ToonSegments.Line,
                true,
                tokens.colors.ink,
            ),
        ),
        emptyList(),
    )
