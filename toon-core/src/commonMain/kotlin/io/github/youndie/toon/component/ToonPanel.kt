package io.github.youndie.toon.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonStroke
import io.github.youndie.toon.tokens.ToonTokens

// A port of the desktop extension's panel `sheet(T, w, h, o)` (B-89, research §1.8): the paper body a
// window lays each content pane on, with the hand-drawn ink outline and a shadow one step deeper than
// the theme's. The extension's screens call it with a seed and the renderer's defaults otherwise; the
// sidebar (B-77) calls it with radii, a shadow and a seed of its own. In the design it overwrites the
// kit's bottom sheet; here it is a scene of its own, and `ToonBottomSheet` is unchanged.

/**
 * The desktop design's paper panel, the surface a window lays each content pane on: a paper body with
 * the theme's hand-drawn ink outline and a hard shadow one step deeper than a button's, in the size its
 * layout gives it. [seed] picks the outline's wobble, so two panels side by side are not drawn alike.
 *
 * [content] stands over the panel in its box, from its top start corner, with no padding: each of the
 * design's panes places its own, from the 28 dp in and 22 dp down the extension's layout names. The
 * shadow (6 dp in Ink & Gouache) and the outline's wobble reach past the box at the end and bottom, as
 * the design's drawing does, so leave that much room beside it. The panel itself takes no input and says
 * nothing to accessibility: it is a surface, not a control.
 */
@Composable
public fun ToonPanel(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = Toon.tokens
    Box(
        modifier.drawWithCache {
            // The engine draws in dp (research D5); the canvas is pixels (B-47). The scene is built
            // again only when the size, the theme or the seed changes.
            val scene = toonPanelScene(tokens, size.width / density, size.height / density, seed)
            onDrawBehind { inDp { drawScene(scene) } }
        },
        content = content,
    )
}

/**
 * The extension's `sheet`: a body [w] by [h] dp at the origin with the renderer's [radii] scaled by the
 * theme, in [fill], its shadow [shadow] dp down and to the end, wobbling from the theme's seed plus
 * 3800 plus [seed]. The defaults are the renderer's own, as the design's screens call it; the sidebar
 * passes its own.
 */
@OptIn(InternalToonApi::class)
internal fun toonPanelScene(
    tokens: ToonTokens,
    w: Float,
    h: Float,
    seed: Int = 0,
    radii: ToonRadii = PANEL_RADII,
    shadow: Float = tokens.shape.shadow + ToonStroke.SHADOW_L_EXTRA,
    fill: Color = tokens.colors.surface,
): ToonScene {
    val sh = tokens.shape
    val rs = sh.radiusScale
    val r = ToonRadii(radii.topLeft * rs, radii.topRight * rs, radii.bottomRight * rs, radii.bottomLeft * rs)
    return ToonScene(
        bodyLayers(
            shapePoints(0f, 0f, w, h, r, sh.insets, sh.wobble, sh.seed + PANEL_SEED + seed),
            fill,
            tokens.colors,
            shadow,
        ),
        emptyList(),
    )
}

/** The renderer's literals: its default radii, and the seed offset of its wobble. */
private val PANEL_RADII = ToonRadii(20f, 14f, 24f, 16f)
private const val PANEL_SEED = 3800
