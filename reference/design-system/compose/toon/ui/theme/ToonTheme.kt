package toon.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Mirrors THEMES in toon-engine.js. Names follow toon/color/*, toon/stroke/*, toon/motion/*.
@Immutable
data class ToonColors(
    val ink: Color, val stage: Color, val stageWash: Color, val surface: Color, val surfacePressed: Color,
    val text: Color, val textMuted: Color, val primary: Color, val onPrimary: Color, val primaryHighlight: Color,
    val danger: Color, val onDanger: Color, val dangerText: Color, val success: Color, val successLight: Color,
    val warning: Color, val chipSelected: Color, val field: Color, val trackOn: Color, val trackOff: Color,
    val thumb: Color, val focus: Color, val disabled: Color, val nail: Color,
    val cel: List<Color>,             // sticker / card accents, 3 entries
    val cardFills: List<Color>,       // per-card surface (Bright Cel colours each card)
    val rim: Color?,                  // night only: 1.6 dp highlight along the top edge
    val fieldRim: Color?,
    val fxLine: Color, val fxDust: Color, val fxSweat: Color, val holeEdge: Color,
)

@Immutable
data class ToonShape(
    val wobble: Float,                // dp, toon/wobble/amp
    val insets: Insets,               // toon/stroke/m: fill inset per side → variable ink weight
    val buttonRadii: Radii,
    val radiusScale: Float,
    val shadow: Dp,                   // toon/shadow/s ; l = shadow + 2.dp
    val tilts: List<Float>,           // toon/tilt/set, degrees
    val seed: Int,
) {
    @Immutable data class Insets(val top: Float, val right: Float, val bottom: Float, val left: Float) {
        operator fun times(k: Float) = Insets(top * k, right * k, bottom * k, left * k)
    }
    @Immutable data class Radii(val tl: Float, val tr: Float, val br: Float, val bl: Float) {
        operator fun times(k: Float) = Radii(tl * k, tr * k, br * k, bl * k)
        fun plus(d: Float) = Radii(tl + d, tr + d, br + d, bl + d)
    }
}

@Immutable
data class ToonType(val display: TextStyle, val headline: TextStyle, val title: TextStyle, val body: TextStyle, val label: TextStyle)

@Immutable
data class ToonTheme(val colors: ToonColors, val shape: ToonShape, val type: ToonType, val name: String)

private fun c(hex: Long) = Color(0xFF000000 or hex)

/** Fonts: bundle Shrikhand, Figtree, Titan One, Fredoka (all SIL OFL) via compose-resources and pass them in. */
class ToonFonts(val display: FontFamily, val body: FontFamily, val celDisplay: FontFamily = display, val celBody: FontFamily = body)

private fun type(display: FontFamily, body: FontFamily, ink: Color) = ToonType(
    display = TextStyle(fontFamily = display, fontSize = 40.sp, lineHeight = 44.sp, color = ink),
    headline = TextStyle(fontFamily = display, fontSize = 28.sp, lineHeight = 32.sp, color = ink),
    title = TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp, color = ink),
    body = TextStyle(fontFamily = body, fontSize = 15.sp, lineHeight = 22.sp, color = ink),
    label = TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp, color = ink),
)

private val baseShape = ToonShape(1.1f, ToonShape.Insets(2f, 2.6f, 4f, 2.2f), ToonShape.Radii(16f, 11f, 18f, 12f), 1f, 4.dp, listOf(-1.2f, 0f, 1.5f), 11)

fun inkAndGouacheLight(f: ToonFonts) = ToonTheme(
    ToonColors(
        ink = c(0x2B211C), stage = c(0xCBC3A6), stageWash = c(0xBDB493), surface = c(0xF4EBD6), surfacePressed = c(0xE6D9BC),
        text = c(0x2B211C), textMuted = c(0x51463D), primary = c(0xE2643C), onPrimary = c(0x2B211C), primaryHighlight = c(0xE2643C),
        danger = c(0xC23F2C), onDanger = c(0xF4EBD6), dangerText = c(0x8E2A1C), success = c(0x3E8F5E), successLight = c(0x8FC79E),
        warning = c(0xE9B53A), chipSelected = c(0xE9B53A), field = c(0xF4EBD6), trackOn = c(0x3E8F5E), trackOff = c(0xDCD2B8),
        thumb = c(0xF4EBD6), focus = c(0x2F6FB0), disabled = c(0xE0D6BE), nail = c(0x8A7D70),
        cel = listOf(c(0xE9B53A), c(0x7FB2D6), c(0xE07F9C)), cardFills = List(3) { c(0xF4EBD6) },
        rim = null, fieldRim = null, fxLine = c(0x2B211C), fxDust = c(0xEFE6CF), fxSweat = c(0xA9D3E8), holeEdge = c(0xF4EBD6),
    ), baseShape, type(f.display, f.body, c(0x2B211C)), "light")

fun inkAndGouacheNight(f: ToonFonts) = ToonTheme(
    ToonColors(
        ink = c(0x120D0F), stage = c(0x1A2140), stageWash = c(0x202A4E), surface = c(0x2C3760), surfacePressed = c(0x36426E),
        text = c(0xF1E6CC), textMuted = c(0xBDB6CF), primary = c(0xD9693F), onPrimary = c(0x120D0F), primaryHighlight = c(0xF2A57E),
        danger = c(0xE06A57), onDanger = c(0x120D0F), dangerText = c(0xF29A88), success = c(0x4E9A6C), successLight = c(0x79B98A),
        warning = c(0xD2A955), chipSelected = c(0xD2A955), field = c(0x232D52), trackOn = c(0x4E9A6C), trackOff = c(0x3A4570),
        thumb = c(0xF1E6CC), focus = c(0x9FB4F0), disabled = c(0x2A3355), nail = c(0x8392C4),
        cel = listOf(c(0xD2A955), c(0x79A3C2), c(0xC27891)), cardFills = List(3) { c(0x2C3760) },
        rim = c(0x8392C4), fieldRim = c(0x6E7BAA), fxLine = c(0xC9C2DA), fxDust = c(0x56608A), fxSweat = c(0x7FA9CF), holeEdge = c(0x8392C4),
    ), baseShape, type(f.display, f.body, c(0xF1E6CC)), "night")

fun brightCel(f: ToonFonts) = ToonTheme(
    ToonColors(
        ink = c(0x231B17), stage = c(0xBCD3D6), stageWash = c(0xC9DDDF), surface = c(0xFFF6E2), surfacePressed = c(0xFFE9B8),
        text = c(0x231B17), textMuted = c(0x3A2C24), primary = c(0xFF5A36), onPrimary = c(0x231B17), primaryHighlight = c(0xFF5A36),
        danger = c(0xD42A20), onDanger = c(0xFFF6E2), dangerText = c(0xA11D14), success = c(0x2FB35A), successLight = c(0x7FD69A),
        warning = c(0xFFCE3A), chipSelected = c(0xFFCE3A), field = c(0xFFF6E2), trackOn = c(0x2FB35A), trackOff = c(0xEDE3CC),
        thumb = c(0xFFF6E2), focus = c(0x2D6FD6), disabled = c(0xE4DCCB), nail = c(0x6B5F55),
        cel = List(3) { c(0xFFF6E2) }, cardFills = listOf(c(0xFFCE3A), c(0xFF8FB1), c(0x6EC3F0)),
        rim = null, fieldRim = null, fxLine = c(0x231B17), fxDust = c(0xFFFFFF), fxSweat = c(0xBDE6F7), holeEdge = c(0xFFF6E2),
    ), ToonShape(.55f, ToonShape.Insets(2.6f, 3f, 4.6f, 2.6f), ToonShape.Radii(22f, 16f, 24f, 18f), 1.25f, 6.dp, listOf(1f, -1.5f, 0f), 23),
    type(f.celDisplay, f.celBody, c(0x231B17)), "cel")

/** toon/motion/* */
object ToonMotion {
    const val SNAP = 80; const val QUICK = 160; const val NORMAL = 260; const val SLOW = 420; const val SCENE = 600
    const val CASCADE_STEP = 40; const val HOLD = 60   // snap-hold pause in the air
    // springs: (dampingRatio, angular frequency ω in rad/s). Compose: spring(dampingRatio, stiffness = ω²)
    val bouncy = ToonSpring(.35f, 45f); val wobbly = ToonSpring(.25f, 30f); val stiff = ToonSpring(.7f, 60f)
    val pendulum = ToonSpring(.25f, 14f)  // TopAppBar sway
}
@Immutable data class ToonSpring(val zeta: Float, val omega: Float) { val stiffness get() = omega * omega }

val LocalToonTheme = staticCompositionLocalOf<ToonTheme> { error("Wrap content in ToonTheme { }") }
val LocalReduceMotion = staticCompositionLocalOf { false }
val LocalIdleBudget = staticCompositionLocalOf { 2 }  // max idle-animated elements per screen

@Composable
fun ToonTheme(theme: ToonTheme, reduceMotion: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalToonTheme provides theme, LocalReduceMotion provides reduceMotion, content = content)
}

object Toon {
    val theme @Composable get() = LocalToonTheme.current
    val colors @Composable get() = LocalToonTheme.current.colors
    val shape @Composable get() = LocalToonTheme.current.shape
    val type @Composable get() = LocalToonTheme.current.type
}
