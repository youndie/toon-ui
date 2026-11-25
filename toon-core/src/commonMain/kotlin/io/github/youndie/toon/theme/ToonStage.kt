package io.github.youndie.toon.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import io.github.youndie.toon.resources.Res
import io.github.youndie.toon.resources.gouache_light
import io.github.youndie.toon.resources.gouache_night
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource

// B-13: the stage the components stand on. The Screens page paints it as the theme's `bg` with the
// gouache tile over it at 2× (`background-size: 256px 256px` for its 512 px tile), light by day and
// night by night; Bright Cel's backdrop is flat (`tex: 'data:,'`). The design's tiles, vendored under
// reference/design-system/assets/textures, do not wrap cleanly; the bundled ones are made seamless
// from them by `make tiles` (B-59, a departure), 448 px, so the stage repeats every 224 dp. The paper
// tiles ship beside them, as the design keeps them, and nothing draws them yet.

/**
 * The theme's stage behind [content]: its background colour and, in Ink & Gouache, the gouache
 * texture tiled at 2 px a dp, every 224 dp — flat under `ToonTheme(stageTexture = false)` (B-86).
 */
@Composable
public fun ToonStage(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val tokens = Toon.tokens
    val tile = toonStageTexture(tokens)?.takeIf { Toon.stageTexture }?.let { imageResource(it) }
    Box(modifier.drawBehind { drawToonStage(tokens, tile) }) { content() }
}

/** The tile behind [tokens]' stage, or null for a flat one: Bright Cel's, and any theme of the caller's own. */
internal fun toonStageTexture(tokens: ToonTokens): DrawableResource? =
    when (tokens.colors.stage) {
        brightCel.colors.stage -> null
        inkGouacheNight.colors.stage -> Res.drawable.gouache_night
        inkGouacheLight.colors.stage -> Res.drawable.gouache_light
        else -> null
    }

/** The stage's tile is drawn at 2 px a dp, as the design draws its own: 448 px every 224 dp (B-59). */
internal const val STAGE_TILE_PX_PER_DP = 2f

/**
 * B-98: how far the tile is drawn off the canvas's origin, in texels, so that no pixel's centre falls on the line between
 * two texels. Unshifted, at 1 px a dp every centre lands exactly on that line (texel 2x + 1), and nearest sampling breaks
 * the tie by the last bit of the arithmetic: under viddik's capture matrix, whose perspective term is 1e-9, Intel and AMD
 * runners broke it apart (#71). Shifted by an eighth, a centre stands at least 0.008 texel from a line at every density
 * from 1 to 4 in quarters and at 2.625 and 2.75, where a shift of a quarter or a half lands on one at some density (B-98).
 * At 1 and 2 px a dp it is the texel the Mac took before, so nothing drawn on it moved.
 */
internal const val STAGE_TILE_SHIFT = 0.125f

/** The background colour, then [tile] repeated over it at [STAGE_TILE_PX_PER_DP], as the page's CSS layers them. */
internal fun DrawScope.drawToonStage(
    tokens: ToonTokens,
    tile: ImageBitmap?,
) {
    drawRect(tokens.colors.stage)
    if (tile == null) return
    val k = density / STAGE_TILE_PX_PER_DP
    scale(k, k, Offset.Zero) {
        // The shader's origin moves with the canvas: moved by the shift, a pixel samples the tile that much earlier, and
        // the rect, drawn from minus the shift, covers the same box.
        translate(STAGE_TILE_SHIFT, STAGE_TILE_SHIFT) {
            drawRect(
                ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated)),
                topLeft = Offset(-STAGE_TILE_SHIFT, -STAGE_TILE_SHIFT),
                size =
                    Size(
                        size.width / k + STAGE_TILE_SHIFT,
                        size.height / k + STAGE_TILE_SHIFT,
                    ),
            )
        }
    }
}
