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
// gouache tile over it at 256 px (`background-size: 256px 256px`, the 512 px tile at 2×), light by
// day and night by night; Bright Cel's backdrop is flat (`tex: 'data:,'`). The tiles are the owner's
// copies, vendored under reference/design-system/assets/textures and bundled byte for byte (research
// §1.1, H6). The paper tiles ship beside them, as the design keeps them, and nothing draws them yet.

/**
 * The theme's stage behind [content]: its background colour and, in Ink & Gouache, the gouache
 * texture tiled at 256 dp.
 */
@Composable
public fun ToonStage(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val tokens = Toon.tokens
    val tile = toonStageTexture(tokens)?.let { imageResource(it) }
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

/** The stage's tile, 512 px drawn at 256 dp. */
internal const val STAGE_TILE_DP = 256f

/** The background colour, then [tile] repeated over it at [STAGE_TILE_DP], as the page's CSS layers them. */
internal fun DrawScope.drawToonStage(
    tokens: ToonTokens,
    tile: ImageBitmap?,
) {
    drawRect(tokens.colors.stage)
    if (tile == null) return
    val k = STAGE_TILE_DP * density / tile.width
    scale(k, k, Offset.Zero) {
        drawRect(
            ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated)),
            size =
                Size(
                    size.width / k,
                    size.height / k,
                ),
        )
    }
}
