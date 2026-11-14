package io.github.youndie.toon.parity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.component.ToonButton
import io.github.youndie.toon.component.ToonListItem
import io.github.youndie.toon.component.ToonListTrailing
import io.github.youndie.toon.draw.toonFadingEdges
import io.github.youndie.toon.icon.ToonIconGlyph
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.theme.ToonStage
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import io.github.youndie.viddik.annotations.ViddikScreenshot

// B-92: a settings column on each theme's stage, faded at the edges it has scrolled past: at rest
// (the bottom only), in the middle (both) and at the end (the top only). The column is 12 dp in from
// the stage's sides and its fade has 12 dp of slack, so the switches' ink and the button's shadow at
// its sides are drawn, and faded with the rest. Goldens only: the design draws no scrolled column.

private const val W = 360
private const val H = 320
private const val SIDE = 12

@Composable
private fun FadingCell(
    tokens: ToonTokens,
    scrolled: Int,
) {
    ToonTheme(tokens) {
        ToonStage(Modifier.size(W.dp, H.dp)) {
            val scroll = rememberScrollState(scrolled)
            Column(
                Modifier
                    .padding(horizontal = SIDE.dp)
                    .fillMaxSize()
                    .toonFadingEdges(scroll, length = 32.dp, slack = SIDE.dp)
                    .verticalScroll(scroll)
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row("Weekend plans", ToonIcons.Calendar, "3 plans", ToonListTrailing.Chevron, false, 0)
                Row("Reminders", ToonIcons.Bell, "At noon", ToonListTrailing.Switch, true, 1)
                Row("Share plans", ToonIcons.Person, "With family", ToonListTrailing.Check, true, 2)
                Row("Reminders", ToonIcons.Bell, "At noon", ToonListTrailing.Switch, false, 3)
                Row("Weekend plans", ToonIcons.Calendar, "3 plans", ToonListTrailing.Chevron, false, 4)
                Row("Share plans", ToonIcons.Person, "With family", ToonListTrailing.Switch, true, 5)
                ToonButton("Add plan", {})
            }
        }
    }
}

@Composable
private fun Row(
    title: String,
    icon: ToonIconGlyph,
    sub: String,
    trailing: ToonListTrailing,
    on: Boolean,
    index: Int,
) = ToonListItem(title, icon = icon, sub = sub, trailing = trailing, on = on, index = index, width = (W - 2 * SIDE).dp)

@ViddikScreenshot(group = "FadingEdges", name = "Rest_InkGouacheLight", width = W, height = H)
@Composable
fun FadingEdgesRestInkGouacheLight() = FadingCell(inkGouacheLight, 0)

@ViddikScreenshot(group = "FadingEdges", name = "Middle_InkGouacheLight", width = W, height = H)
@Composable
fun FadingEdgesMiddleInkGouacheLight() = FadingCell(inkGouacheLight, 180)

@ViddikScreenshot(group = "FadingEdges", name = "End_InkGouacheLight", width = W, height = H)
@Composable
fun FadingEdgesEndInkGouacheLight() = FadingCell(inkGouacheLight, Int.MAX_VALUE)

@ViddikScreenshot(group = "FadingEdges", name = "Rest_InkGouacheNight", width = W, height = H)
@Composable
fun FadingEdgesRestInkGouacheNight() = FadingCell(inkGouacheNight, 0)

@ViddikScreenshot(group = "FadingEdges", name = "Middle_InkGouacheNight", width = W, height = H)
@Composable
fun FadingEdgesMiddleInkGouacheNight() = FadingCell(inkGouacheNight, 180)

@ViddikScreenshot(group = "FadingEdges", name = "End_InkGouacheNight", width = W, height = H)
@Composable
fun FadingEdgesEndInkGouacheNight() = FadingCell(inkGouacheNight, Int.MAX_VALUE)

@ViddikScreenshot(group = "FadingEdges", name = "Rest_BrightCel", width = W, height = H)
@Composable
fun FadingEdgesRestBrightCel() = FadingCell(brightCel, 0)

@ViddikScreenshot(group = "FadingEdges", name = "Middle_BrightCel", width = W, height = H)
@Composable
fun FadingEdgesMiddleBrightCel() = FadingCell(brightCel, 180)

@ViddikScreenshot(group = "FadingEdges", name = "End_BrightCel", width = W, height = H)
@Composable
fun FadingEdgesEndBrightCel() = FadingCell(brightCel, Int.MAX_VALUE)
