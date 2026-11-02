package io.github.youndie.toon.type

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import io.github.youndie.toon.resources.Res
import io.github.youndie.toon.resources.figtree_wght
import io.github.youndie.toon.resources.fredoka_wdth_wght
import io.github.youndie.toon.resources.nunito_wght
import io.github.youndie.toon.resources.onest_wght
import io.github.youndie.toon.resources.rubik_italic_wght
import io.github.youndie.toon.resources.shrikhand_regular
import io.github.youndie.toon.resources.titan_one_regular
import org.jetbrains.compose.resources.Font

/**
 * A theme's two families: [display] for title cards and headlines, [body] for everything else
 * (research §1.4).
 *
 * | Theme | display | body | Cyrillic display | Cyrillic body |
 * |---|---|---|---|---|
 * | Ink & Gouache, day and night | Shrikhand | Figtree 400–800 | Rubik Italic 900 | Onest 400–800 |
 * | Bright Cel | Titan One | Fredoka 400–700 | Nunito 1000 | Nunito 600–1000 |
 *
 * Figtree and Fredoka ship as the unmodified variable files from google/fonts, and each weight is a
 * `wght` setting on the same file: cutting static instances would be a modified font. Fredoka's axis
 * stops at 700, so the engine's 800 resolves to 700, as it does in Chrome. None of the four has
 * Cyrillic, so each role is backed by a face that does (B-35, research §1.4): the design's face
 * draws Latin, and a run of Cyrillic is drawn in [displayCyrillic] or [bodyCyrillic].
 */
@Immutable
internal class ToonFontFamilies(
    val display: FontFamily,
    val body: FontFamily,
    val displayCyrillic: ToonBackingFace,
    val bodyCyrillic: ToonBackingFace,
)

/**
 * A face that draws the Cyrillic its role's face lacks: [family], at [scale] times the size so its
 * x-height meets the design face's (CSS's `size-adjust`), and at [weight] for the weight asked of the
 * design face. A display backing is one instance (Rubik at 900, Nunito at 1000), which every weight
 * asked of it resolves to.
 */
@Immutable
internal class ToonBackingFace(
    val family: FontFamily,
    val scale: Float,
    val weight: (Int) -> Int,
)

/** Whether [c] is Cyrillic: the basic block, its supplement and the two extensions. */
internal fun isCyrillic(c: Char): Boolean =
    c in '\u0400'..'\u052F' || c in '\u1C80'..'\u1C8F' || c in '\u2DE0'..'\u2DFF' || c in '\uA640'..'\uA69F'

/**
 * [text] with each run of Cyrillic in [backing], the rest left to the paragraph's own face, at [size]
 * sp and [weight] — what a browser's fallback from the design face to the backing one draws.
 */
internal fun withBacking(
    text: String,
    backing: ToonBackingFace,
    size: Float,
    weight: Int,
): AnnotatedString {
    if (text.none(::isCyrillic)) return AnnotatedString(text)
    val style =
        SpanStyle(
            fontFamily = backing.family,
            fontWeight = FontWeight(backing.weight(weight)),
            fontSize =
                (
                    size *
                        backing.scale
                ).sp,
        )
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val cyr = isCyrillic(text[i])
            var j = i
            while (j < text.length && isCyrillic(text[j]) == cyr) j++
            if (cyr) withStyle(style) { append(text, i, j) } else append(text, i, j)
            i = j
        }
    }
}

/** The weights the engine asks the body face for (`fw` in toon-engine.js). */
private val figtreeWeights = listOf(400, 500, 600, 700, 800)
private val fredokaWeights = listOf(400, 500, 600, 700)

/**
 * The backing faces' x-height ratios, measured in Chrome at 100 px (research §1.4): Shrikhand 52.9
 * over Rubik Italic 52.0, Figtree 50.0 over Onest 52.7, Titan One 58.0 over Nunito 49.7, and Fredoka
 * 51.1 over Nunito 49.7 at 700, the weight its labels mostly take.
 */
private const val SHRIKHAND_OVER_RUBIK = 52.9f / 52.0f
private const val FIGTREE_OVER_ONEST = 50.0f / 52.7f
private const val TITAN_OVER_NUNITO = 58.0f / 49.7f
private const val FREDOKA_OVER_NUNITO = 51.1f / 49.7f

/** Fredoka draws heavier than Nunito at one weight: Fredoka 700 reads as Nunito 900. */
private const val NUNITO_UNDER_FREDOKA = 200
private val onestWeights = figtreeWeights
private val nunitoWeights = (fredokaWeights + listOf(800, 900, 1000)).distinct()

/**
 * Ink & Gouache's faces. `@Composable` because compose-resources reads a font asynchronously and
 * needs a composition to hang the load on, on every target alike.
 */
@Composable
internal fun inkAndGouacheFonts(): ToonFontFamilies =
    ToonFontFamilies(
        display = FontFamily(Font(Res.font.shrikhand_regular, FontWeight.Normal)),
        body = FontFamily(figtreeWeights.map { Font(Res.font.figtree_wght, FontWeight(it)) }),
        // Shrikhand is a heavy slanted brush: the closest Cyrillic is Rubik's black italic.
        displayCyrillic =
            ToonBackingFace(
                FontFamily(Font(Res.font.rubik_italic_wght, FontWeight(900))),
                SHRIKHAND_OVER_RUBIK,
            ) { 900 },
        bodyCyrillic =
            ToonBackingFace(
                FontFamily(onestWeights.map { Font(Res.font.onest_wght, FontWeight(it)) }),
                FIGTREE_OVER_ONEST,
            ) { it },
    )

/** Bright Cel's faces. */
@Composable
internal fun brightCelFonts(): ToonFontFamilies =
    ToonFontFamilies(
        display = FontFamily(Font(Res.font.titan_one_regular, FontWeight.Normal)),
        body = FontFamily(fredokaWeights.map { Font(Res.font.fredoka_wdth_wght, FontWeight(it)) }),
        // Titan One and Fredoka are both round; Nunito is too, at its black for the display.
        displayCyrillic =
            ToonBackingFace(FontFamily(Font(Res.font.nunito_wght, FontWeight(1000))), TITAN_OVER_NUNITO) { 1000 },
        bodyCyrillic =
            ToonBackingFace(
                FontFamily(nunitoWeights.map { Font(Res.font.nunito_wght, FontWeight(it)) }),
                FREDOKA_OVER_NUNITO,
            ) { minOf(1000, it + NUNITO_UNDER_FREDOKA) },
    )
