package io.github.youndie.toon.parity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonTextRenderer
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.ToonTokens

/** What a cell's frame is built from: the theme's [tokens], and [text] to measure its words with. */
internal class SceneCellScope(
    val tokens: ToonTokens,
    val text: ToonTextRenderer,
)

/**
 * B-15: a spec page's cell as `E.cell` lays it out — [width] × [height] on the theme's stage, the
 * frame's origin [left] and [top] in from the corner — with the frame [scene] builds for the theme.
 */
@Composable
internal fun SceneCell(
    tokens: ToonTokens,
    width: Int,
    height: Int,
    left: Int,
    top: Int,
    scene: SceneCellScope.() -> ToonScene,
) {
    ToonTheme(tokens) {
        val text = rememberToonText()
        Canvas(Modifier.size(width.dp, height.dp).background(tokens.colors.stage)) {
            drawScene(SceneCellScope(tokens, text).scene().placed(left.toFloat(), top.toFloat()), text)
        }
    }
}
