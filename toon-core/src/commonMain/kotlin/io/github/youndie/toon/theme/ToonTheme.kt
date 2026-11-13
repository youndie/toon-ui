package io.github.youndie.toon.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.youndie.toon.tokens.ToonColors
import io.github.youndie.toon.tokens.ToonShape
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import io.github.youndie.toon.type.ToonFontFamilies
import io.github.youndie.toon.type.brightCelFonts
import io.github.youndie.toon.type.inkAndGouacheFonts

/** The design's three themes (research §1.3). */
public object ToonThemes {
    /** Ink & Gouache, the base, by day. */
    public val InkGouacheLight: ToonTokens get() = inkGouacheLight

    /** Ink & Gouache at night: the navy stage, the pale rim. */
    public val InkGouacheNight: ToonTokens get() = inkGouacheNight

    /** Bright Cel, the second light theme. */
    public val BrightCel: ToonTokens get() = brightCel
}

internal val LocalToonTokens = staticCompositionLocalOf<ToonTokens> { error("Wrap the content in ToonTheme { }") }
internal val LocalToonStageTexture = staticCompositionLocalOf { true }

internal val LocalToonFonts = staticCompositionLocalOf<ToonFontFamilies> { error("Wrap the content in ToonTheme { }") }
internal val LocalToonReduceMotion = staticCompositionLocalOf { false }
internal val LocalToonIdleBudget = staticCompositionLocalOf { ToonIdleBudget(capacity = 0) }

/**
 * Provides a theme's tokens and fonts, whether motion is reduced, and the idle budget.
 *
 * [reduceMotion] null follows the platform's setting (Android's animator scale at 0, iOS's Reduce
 * Motion, the browser's `prefers-reduced-motion`; off on the desktop JVM). Each component then plays
 * the reduce-motion word its spec names (research D8). [idleBudget] is how many idle animations may
 * run at once inside this theme; the brief allows two.
 */
@Composable
public fun ToonTheme(
    tokens: ToonTokens = ToonThemes.InkGouacheLight,
    reduceMotion: Boolean? = null,
    idleBudget: Int = 2,
    stageTexture: Boolean = true,
    content: @Composable () -> Unit,
) {
    val fonts =
        if (tokens.displayFamily ==
            ToonThemes.BrightCel.displayFamily
        ) {
            brightCelFonts()
        } else {
            inkAndGouacheFonts()
        }
    val reduced = reduceMotion ?: platformReducedMotion()
    val budget = remember(idleBudget) { ToonIdleBudget(idleBudget) }
    CompositionLocalProvider(
        LocalToonTokens provides tokens,
        LocalToonFonts provides fonts,
        LocalToonReduceMotion provides reduced,
        LocalToonIdleBudget provides budget,
        LocalToonStageTexture provides stageTexture,
        content = content,
    )
}

/** What the nearest [ToonTheme] provides. */
public object Toon {
    public val tokens: ToonTokens
        @Composable @ReadOnlyComposable
        get() = LocalToonTokens.current

    public val colors: ToonColors
        @Composable @ReadOnlyComposable
        get() = LocalToonTokens.current.colors

    public val shape: ToonShape
        @Composable @ReadOnlyComposable
        get() = LocalToonTokens.current.shape

    /** Whether animations play their reduce-motion word instead of their motion. */
    public val reduceMotion: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalToonReduceMotion.current

    public val idleBudget: ToonIdleBudget
        @Composable @ReadOnlyComposable
        get() = LocalToonIdleBudget.current

    /**
     * Whether [ToonStage] draws its theme's texture, or only the flat stage colour: `ToonTheme`'s
     * `stageTexture`, false for screenshots meant to match across machines (B-86).
     */
    public val stageTexture: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalToonStageTexture.current

    internal val fonts: ToonFontFamilies
        @Composable @ReadOnlyComposable
        get() = LocalToonFonts.current
}
