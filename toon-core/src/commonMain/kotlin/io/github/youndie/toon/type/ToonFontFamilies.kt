package io.github.youndie.toon.type

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.youndie.toon.resources.Res
import io.github.youndie.toon.resources.figtree_wght
import io.github.youndie.toon.resources.fredoka_wdth_wght
import io.github.youndie.toon.resources.shrikhand_regular
import io.github.youndie.toon.resources.titan_one_regular
import org.jetbrains.compose.resources.Font

/**
 * A theme's two families: [display] for title cards and headlines, [body] for everything else
 * (research §1.4).
 *
 * | Theme | display | body |
 * |---|---|---|
 * | Ink & Gouache, day and night | Shrikhand | Figtree 400–800 |
 * | Bright Cel | Titan One | Fredoka 400–700 |
 *
 * Figtree and Fredoka ship as the unmodified variable files from google/fonts, and each weight is a
 * `wght` setting on the same file: cutting static instances would be a modified font. Fredoka's axis
 * stops at 700, so the engine's 800 resolves to 700, as it does in Chrome. None of the four has
 * Cyrillic; B-35 adds faces that do.
 */
@Immutable
internal class ToonFontFamilies(
    val display: FontFamily,
    val body: FontFamily,
)

/** The weights the engine asks the body face for (`fw` in toon-engine.js). */
private val figtreeWeights = listOf(400, 500, 600, 700, 800)
private val fredokaWeights = listOf(400, 500, 600, 700)

/**
 * Ink & Gouache's faces. `@Composable` because compose-resources reads a font asynchronously and
 * needs a composition to hang the load on, on every target alike.
 */
@Composable
internal fun inkAndGouacheFonts(): ToonFontFamilies =
    ToonFontFamilies(
        display = FontFamily(Font(Res.font.shrikhand_regular, FontWeight.Normal)),
        body = FontFamily(figtreeWeights.map { Font(Res.font.figtree_wght, FontWeight(it)) }),
    )

/** Bright Cel's faces. */
@Composable
internal fun brightCelFonts(): ToonFontFamilies =
    ToonFontFamilies(
        display = FontFamily(Font(Res.font.titan_one_regular, FontWeight.Normal)),
        body = FontFamily(fredokaWeights.map { Font(Res.font.fredoka_wdth_wght, FontWeight(it)) }),
    )
