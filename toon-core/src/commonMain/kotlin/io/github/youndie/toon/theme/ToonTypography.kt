package io.github.youndie.toon.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.youndie.toon.tokens.ToonTypeSpec

/**
 * This step of the type scale as a [TextStyle] in the nearest [ToonTheme]'s faces — the display face
 * when [ToonTypeSpec.display], the body face otherwise — in the theme's text colour (B-29). Text set
 * with it outside a component, a screen's heading say, matches the components' own — in Latin. The
 * design faces hold no Cyrillic: Russian text goes through [ToonText], or [backed] for a string laid
 * out by hand, which draw it in the theme's backing faces (B-82).
 */
public val ToonTypeSpec.textStyle: TextStyle
    @Composable @ReadOnlyComposable
    get() =
        TextStyle(
            fontFamily = if (display) LocalToonFonts.current.display else LocalToonFonts.current.body,
            fontWeight = FontWeight(weight),
            fontSize = size.sp,
            lineHeight = lineHeight.sp,
            color = Toon.colors.text,
        )
