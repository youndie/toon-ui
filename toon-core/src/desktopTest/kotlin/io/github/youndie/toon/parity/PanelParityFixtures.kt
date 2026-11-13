package io.github.youndie.toon.parity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.component.ToonPanel
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import io.github.youndie.viddik.annotations.ViddikScreenshot

// B-89: the panel against the desktop extension's own `sheet`, drawn in Chrome by
// scripts/design-references.mjs (group `Panel`): the composable itself, sized by its layout, on the
// theme's stage, 12 dp in from the cell's top start and 16 dp short of its end and bottom, where the
// shadow lands. No text, so it is read against the Divider's floor.

private const val TOP = 12
private const val END = 16

@Composable
private fun PanelCell(
    tokens: ToonTokens,
    w: Int,
    h: Int,
    seed: Int,
) {
    ToonTheme(tokens) {
        Box(Modifier.size((w + TOP + END).dp, (h + TOP + END).dp).background(tokens.colors.stage)) {
            ToonPanel(Modifier.offset(TOP.dp, TOP.dp).size(w.dp, h.dp), seed = seed) {}
        }
    }
}

@ViddikScreenshot(group = "Panel", name = "Card_InkGouacheLight", width = 348, height = 228)
@Composable
fun PanelCardInkGouacheLight() = PanelCell(inkGouacheLight, 320, 200, 0)

@ViddikScreenshot(group = "Panel", name = "Wide_InkGouacheLight", width = 508, height = 188)
@Composable
fun PanelWideInkGouacheLight() = PanelCell(inkGouacheLight, 480, 160, 5)

@ViddikScreenshot(group = "Panel", name = "Card_InkGouacheNight", width = 348, height = 228)
@Composable
fun PanelCardInkGouacheNight() = PanelCell(inkGouacheNight, 320, 200, 0)

@ViddikScreenshot(group = "Panel", name = "Wide_InkGouacheNight", width = 508, height = 188)
@Composable
fun PanelWideInkGouacheNight() = PanelCell(inkGouacheNight, 480, 160, 5)

@ViddikScreenshot(group = "Panel", name = "Card_BrightCel", width = 348, height = 228)
@Composable
fun PanelCardBrightCel() = PanelCell(brightCel, 320, 200, 0)

@ViddikScreenshot(group = "Panel", name = "Wide_BrightCel", width = 508, height = 188)
@Composable
fun PanelWideBrightCel() = PanelCell(brightCel, 480, 160, 5)
