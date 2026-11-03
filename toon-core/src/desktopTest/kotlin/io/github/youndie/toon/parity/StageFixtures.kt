package io.github.youndie.toon.parity

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.theme.ToonStage
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import io.github.youndie.viddik.annotations.ViddikScreenshot

// B-13: the stage, 2 × 2 tiles of 224 dp (B-59), so both wraps of the gouache tile are in the picture; Bright
// Cel's is flat. Goldens only: the Chrome references draw cells on the flat stage colour.

@ViddikScreenshot(group = "Stage", name = "Tiled_InkGouacheLight", width = 448, height = 448)
@Composable
fun StageTiledInkGouacheLight() = Stage(inkGouacheLight)

@ViddikScreenshot(group = "Stage", name = "Tiled_InkGouacheNight", width = 448, height = 448)
@Composable
fun StageTiledInkGouacheNight() = Stage(inkGouacheNight)

@ViddikScreenshot(group = "Stage", name = "Flat_BrightCel", width = 448, height = 448)
@Composable
fun StageFlatBrightCel() = Stage(brightCel)

@Composable
private fun Stage(tokens: ToonTokens) = ToonTheme(tokens) { ToonStage(Modifier.size(448.dp)) }
