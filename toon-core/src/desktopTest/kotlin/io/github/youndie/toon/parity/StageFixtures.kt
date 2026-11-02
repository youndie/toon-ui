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

// B-13: the stage, 2 × 2 tiles of 256 dp, so both wraps of the gouache tile are in the picture; Bright
// Cel's is flat. Goldens only: the Chrome references draw cells on the flat stage colour.

@ViddikScreenshot(group = "Stage", name = "Tiled_InkGouacheLight", width = 512, height = 512)
@Composable
fun StageTiledInkGouacheLight() = Stage(inkGouacheLight)

@ViddikScreenshot(group = "Stage", name = "Tiled_InkGouacheNight", width = 512, height = 512)
@Composable
fun StageTiledInkGouacheNight() = Stage(inkGouacheNight)

@ViddikScreenshot(group = "Stage", name = "Flat_BrightCel", width = 512, height = 512)
@Composable
fun StageFlatBrightCel() = Stage(brightCel)

@Composable
private fun Stage(tokens: ToonTokens) = ToonTheme(tokens) { ToonStage(Modifier.size(512.dp)) }
