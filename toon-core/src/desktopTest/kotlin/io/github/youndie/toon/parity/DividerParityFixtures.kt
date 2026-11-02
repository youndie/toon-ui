package io.github.youndie.toon.parity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.inkLinePoints
import io.github.youndie.toon.draw.place
import io.github.youndie.toon.draw.polygonPath
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import io.github.youndie.viddik.annotations.ViddikScreenshot

// B-06: the parity floor. The engine's `divider` is one filled `inkLine`, and B-04 showed the port's
// points match the engine's within 0.05 px, so whatever these fixtures differ from their Chrome
// references by is the two rasterisers, not the geometry. Each fixture is the MVP page's Divider cell
// (342 × 12, 4 px padding) on its theme's stage colour, drawn through B-09's primitives and B-07's
// tokens; the component itself is B-22's.

private const val W = 350
private const val H = 20

@Composable
private fun DividerCell(
    tokens: ToonTokens,
    index: Int,
    x0: Float,
    x1: Float,
    th: Float,
) {
    Canvas(Modifier.size(W.dp, H.dp).background(tokens.colors.stage)) {
        place(4f, 4f) {
            drawPath(polygonPath(inkLinePoints(x0, x1, 6f, th, tokens.shape.seed + 970 + index)), tokens.colors.ink)
        }
    }
}

@ViddikScreenshot(group = "Divider", name = "Full_InkGouacheLight", width = W, height = H)
@Composable
fun DividerFullInkGouacheLight() = DividerCell(inkGouacheLight, 0, 0f, 342f, 3f)

@ViddikScreenshot(group = "Divider", name = "Inset_InkGouacheLight", width = W, height = H)
@Composable
fun DividerInsetInkGouacheLight() = DividerCell(inkGouacheLight, 1, 56f, 342f, 3f)

@ViddikScreenshot(group = "Divider", name = "Thick_InkGouacheLight", width = W, height = H)
@Composable
fun DividerThickInkGouacheLight() = DividerCell(inkGouacheLight, 2, 0f, 342f, 4.6f)

@ViddikScreenshot(group = "Divider", name = "Short_InkGouacheLight", width = W, height = H)
@Composable
fun DividerShortInkGouacheLight() = DividerCell(inkGouacheLight, 3, 0f, 120f, 2.4f)

@ViddikScreenshot(group = "Divider", name = "Full_InkGouacheNight", width = W, height = H)
@Composable
fun DividerFullInkGouacheNight() = DividerCell(inkGouacheNight, 0, 0f, 342f, 3f)

@ViddikScreenshot(group = "Divider", name = "Inset_InkGouacheNight", width = W, height = H)
@Composable
fun DividerInsetInkGouacheNight() = DividerCell(inkGouacheNight, 1, 56f, 342f, 3f)

@ViddikScreenshot(group = "Divider", name = "Thick_InkGouacheNight", width = W, height = H)
@Composable
fun DividerThickInkGouacheNight() = DividerCell(inkGouacheNight, 2, 0f, 342f, 4.6f)

@ViddikScreenshot(group = "Divider", name = "Short_InkGouacheNight", width = W, height = H)
@Composable
fun DividerShortInkGouacheNight() = DividerCell(inkGouacheNight, 3, 0f, 120f, 2.4f)

@ViddikScreenshot(group = "Divider", name = "Full_BrightCel", width = W, height = H)
@Composable
fun DividerFullBrightCel() = DividerCell(brightCel, 0, 0f, 342f, 3f)

@ViddikScreenshot(group = "Divider", name = "Inset_BrightCel", width = W, height = H)
@Composable
fun DividerInsetBrightCel() = DividerCell(brightCel, 1, 56f, 342f, 3f)

@ViddikScreenshot(group = "Divider", name = "Thick_BrightCel", width = W, height = H)
@Composable
fun DividerThickBrightCel() = DividerCell(brightCel, 2, 0f, 342f, 4.6f)

@ViddikScreenshot(group = "Divider", name = "Short_BrightCel", width = W, height = H)
@Composable
fun DividerShortBrightCel() = DividerCell(brightCel, 3, 0f, 120f, 2.4f)
