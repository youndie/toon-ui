package io.github.youndie.toon.parity

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.component.ToonTitleCard
import io.github.youndie.toon.component.ToonTitleCardVariant
import io.github.youndie.toon.theme.ToonStage
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import io.github.youndie.viddik.annotations.ViddikScreenshot

// B-99: the empty state as pyatiletka's board draws it (#72), a 340 × 300 card with a sentence for a sub: wrapped onto
// lines inside a taller plaque, not shrunk. Goldens only: the design's subs fit one line.

private const val W = 340
private const val H = 300

@Composable
private fun LongSubCell(tokens: ToonTokens) {
    ToonTheme(tokens, reduceMotion = true) {
        ToonStage(Modifier.size(W.dp, H.dp)) {
            ToonTitleCard(
                variant = ToonTitleCardVariant.Empty,
                title = "Репозиториев нет",
                sub = "У владельца youndie нет репозиториев. Добавьте владельцев в настройках.",
                width = W.dp,
                height = H.dp,
            )
        }
    }
}

@ViddikScreenshot(group = "TitleCard", name = "LongSub_InkGouacheLight", width = W, height = H)
@Composable
fun TitleCardLongSubInkGouacheLight() = LongSubCell(inkGouacheLight)

@ViddikScreenshot(group = "TitleCard", name = "LongSub_InkGouacheNight", width = W, height = H)
@Composable
fun TitleCardLongSubInkGouacheNight() = LongSubCell(inkGouacheNight)

@ViddikScreenshot(group = "TitleCard", name = "LongSub_BrightCel", width = W, height = H)
@Composable
fun TitleCardLongSubBrightCel() = LongSubCell(brightCel)
