package io.github.youndie.toon.parity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.drawFocusRing
import io.github.youndie.toon.draw.drawInkBody
import io.github.youndie.toon.draw.place
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import io.github.youndie.viddik.annotations.ViddikScreenshot

// B-09: the drawing primitives against the engine's own `body` and `ring`, drawn in Chrome by
// scripts/design-references.mjs. The body is the Principles page's `blk()`: 96 × 52, the button's
// radii, the theme's primary fill, its shadow, and the night theme's rim. The focused one adds the
// ring at a 6 dp outset with the radii grown by 6, as `btn` draws it.

private const val W = 118
private const val H = 74

@Composable
private fun BodyCell(
    tokens: ToonTokens,
    focused: Boolean,
) {
    Canvas(Modifier.size(W.dp, H.dp).background(tokens.colors.stage)) {
        place(10f, 10f) {
            if (focused) {
                drawFocusRing(
                    -6f,
                    -6f,
                    108f,
                    64f,
                    ToonRadii(22f, 17f, 24f, 18f),
                    tokens.colors,
                    tokens.shape,
                    tokens.shape.seed + 1403,
                )
            }
            drawInkBody(
                0f,
                0f,
                96f,
                52f,
                ToonRadii(16f, 11f, 18f, 12f),
                tokens.shape.seed + 1400,
                tokens.colors.primary,
                tokens.colors,
                tokens.shape,
            )
        }
    }
}

@ViddikScreenshot(group = "Body", name = "Plain_InkGouacheLight", width = W, height = H)
@Composable
fun BodyPlainInkGouacheLight() = BodyCell(inkGouacheLight, focused = false)

@ViddikScreenshot(group = "Body", name = "Focused_InkGouacheLight", width = W, height = H)
@Composable
fun BodyFocusedInkGouacheLight() = BodyCell(inkGouacheLight, focused = true)

@ViddikScreenshot(group = "Body", name = "Plain_InkGouacheNight", width = W, height = H)
@Composable
fun BodyPlainInkGouacheNight() = BodyCell(inkGouacheNight, focused = false)

@ViddikScreenshot(group = "Body", name = "Focused_InkGouacheNight", width = W, height = H)
@Composable
fun BodyFocusedInkGouacheNight() = BodyCell(inkGouacheNight, focused = true)

@ViddikScreenshot(group = "Body", name = "Plain_BrightCel", width = W, height = H)
@Composable
fun BodyPlainBrightCel() = BodyCell(brightCel, focused = false)

@ViddikScreenshot(group = "Body", name = "Focused_BrightCel", width = W, height = H)
@Composable
fun BodyFocusedBrightCel() = BodyCell(brightCel, focused = true)
