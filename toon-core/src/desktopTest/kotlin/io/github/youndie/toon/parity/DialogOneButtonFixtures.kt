package io.github.youndie.toon.parity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.component.ToonDialog
import io.github.youndie.toon.component.ToonRadioGroup
import io.github.youndie.toon.theme.ToonStage
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import io.github.youndie.viddik.annotations.ViddikScreenshot

// B-97: the dialog with one button, as pyatiletka's board draws its appearance choice (#70): a skin and a theme, each a
// radio group in the content slot, applied at once, and «Готово» alone. Goldens only: the design draws no such dialog.

private const val W = 390
private const val H = 560

@Composable
private fun OneButtonCell(tokens: ToonTokens) {
    ToonTheme(tokens, reduceMotion = true) {
        ToonStage(Modifier.size(W.dp, H.dp)) {
            ToonDialog("Вид", "Готово", null, Modifier.fillMaxSize(), destructive = false) {
                Column {
                    ToonRadioGroup(listOf("Toon", "Plakat"), 0, {})
                    ToonRadioGroup(listOf("День", "Ночь"), 1, {})
                }
            }
        }
    }
}

@ViddikScreenshot(group = "Dialog", name = "OneButton_InkGouacheLight", width = W, height = H)
@Composable
fun DialogOneButtonInkGouacheLight() = OneButtonCell(inkGouacheLight)

@ViddikScreenshot(group = "Dialog", name = "OneButton_InkGouacheNight", width = W, height = H)
@Composable
fun DialogOneButtonInkGouacheNight() = OneButtonCell(inkGouacheNight)

@ViddikScreenshot(group = "Dialog", name = "OneButton_BrightCel", width = W, height = H)
@Composable
fun DialogOneButtonBrightCel() = OneButtonCell(brightCel)
