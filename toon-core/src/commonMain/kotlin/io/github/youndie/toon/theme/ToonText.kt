package io.github.youndie.toon.theme

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.github.youndie.toon.tokens.ToonTypeSpec
import io.github.youndie.toon.type.withBacking

/**
 * [text] set in this step of the type scale the way the kit's components set theirs: Latin in the
 * theme's design face, and each run of Cyrillic in the face that backs it (B-82). None of the design
 * faces holds Cyrillic, so [textStyle] alone draws Russian in the platform's fallback font, or in
 * nothing at all on wasm; text a consumer sets with [ToonText], or lays out with this string and
 * [textStyle], matches the components' own.
 */
@Composable
@ReadOnlyComposable
public fun ToonTypeSpec.backed(text: String): AnnotatedString {
    val fonts = LocalToonFonts.current
    return withBacking(text, if (display) fonts.displayCyrillic else fonts.bodyCyrillic, size, weight)
}

/**
 * [text] in [spec] of the nearest [ToonTheme], Cyrillic in the theme's backing faces as the kit's
 * components draw it (B-82), in [color] — the theme's text colour when unspecified. The rest are
 * [BasicText]'s.
 */
@Composable
public fun ToonText(
    text: String,
    spec: ToonTypeSpec,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    val style = spec.textStyle
    BasicText(
        spec.backed(text),
        modifier,
        style.merge(color = color, textAlign = textAlign ?: style.textAlign),
        onTextLayout,
        overflow,
        softWrap,
        maxLines,
        minLines,
    )
}
