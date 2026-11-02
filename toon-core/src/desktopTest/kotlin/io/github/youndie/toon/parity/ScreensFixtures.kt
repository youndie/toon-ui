package io.github.youndie.toon.parity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.youndie.toon.component.ToonButton
import io.github.youndie.toon.component.ToonButtonVariant
import io.github.youndie.toon.component.ToonCard
import io.github.youndie.toon.component.ToonCheckbox
import io.github.youndie.toon.component.ToonChip
import io.github.youndie.toon.component.ToonDivider
import io.github.youndie.toon.component.ToonFab
import io.github.youndie.toon.component.ToonListItem
import io.github.youndie.toon.component.ToonListTrailing
import io.github.youndie.toon.component.ToonNavigationBar
import io.github.youndie.toon.component.ToonProgress
import io.github.youndie.toon.component.ToonRadioGroup
import io.github.youndie.toon.component.ToonTabs
import io.github.youndie.toon.component.ToonTextField
import io.github.youndie.toon.component.ToonTopAppBar
import io.github.youndie.toon.icon.ToonIcon
import io.github.youndie.toon.icon.ToonIconWeight
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.theme.ToonStage
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import io.github.youndie.toon.type.inkAndGouacheFonts
import io.github.youndie.viddik.annotations.ViddikScreenshot

// B-29: the Screens page's three phone screens in each theme, built from the library's components at
// the page's places (`toon-ui Screens.dc.html`), against the page itself rendered by the design's
// runtime with reduced motion (scripts/design-references.mjs). Reduced motion leaves every animation
// at its last frame, which here is the components drawn without their entrances; the page's own
// text is set in Shrikhand and Figtree in every theme, as the page sets it.

private val PAGE = Color(0xFFE6E2D8)

/** The page's demo plans (`ITEMS`), four on a phone. */
internal val SCREEN_PLANS =
    listOf(
        Triple("Pancake breakfast", "Kitchen, bring syrup", "9:00"),
        Triple("Bike to the lake", "Check the tyre pump", "11:30"),
        Triple("Paint the fence", "Green this time", "14:00"),
        Triple("Picnic by the lake", "Blanket and lemonade", "16:00"),
        Triple("Movie night", "Popcorn duty: you", "20:00"),
    )

/** A phone screen on the page: 390 × 844, its corners rounded 32 over the page's paper. */
@Composable
private fun Phone(
    tokens: ToonTokens,
    content: @Composable BoxScope.() -> Unit,
) = ToonTheme(tokens, reduceMotion = true) {
    Box(Modifier.size(390.dp, 844.dp).background(PAGE)) {
        Box(Modifier.size(390.dp, 844.dp).clip(RoundedCornerShape(32.dp))) {
            ToonStage(Modifier.size(390.dp, 844.dp))
            content()
        }
    }
}

/** An element placed as the page's `position:absolute; left; top`. */
@Composable
private fun At(
    x: Int,
    y: Int,
    content: @Composable () -> Unit,
) = Box(Modifier.offset(x.dp, y.dp)) { content() }

/**
 * The page's own text: Shrikhand or Figtree at a CSS `font` and `line-height`, uppercase when set.
 * A CSS line box may be shorter than the face's own line, which Compose never draws below: then the
 * text keeps its natural line and sits centred on the shorter box, as CSS's negative half-leading
 * puts it.
 */
@Composable
private fun PageText(
    text: String,
    weight: Int,
    size: Float,
    lineHeight: Float,
    color: Color,
    display: Boolean = false,
    tracking: TextUnit = TextUnit.Unspecified,
    modifier: Modifier = Modifier,
) {
    val fonts = inkAndGouacheFonts()
    BasicText(
        text,
        modifier.layout { measurable, constraints ->
            val p = measurable.measure(constraints)
            val box = (size * lineHeight).dp.roundToPx()
            val lines = (p.height / box).coerceAtLeast(1)
            if (lines > 1 || p.height <= box) {
                layout(p.width, p.height) { p.place(0, 0) }
            } else {
                layout(p.width, box) { p.place(0, (box - p.height) / 2) }
            }
        },
        style =
            TextStyle(
                fontFamily = if (display) fonts.display else fonts.body,
                fontWeight = FontWeight(weight),
                fontSize = size.sp,
                lineHeight = (size * lineHeight).sp,
                letterSpacing = tracking,
                color = color,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
            ),
        overflow = TextOverflow.Visible,
    )
}

@Composable
private fun SignIn(tokens: ToonTokens) =
    Phone(tokens) {
        val c = Toon.colors
        Column(Modifier.offset(28.dp, 118.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToonIcon(ToonIcons.Star, null, size = 32.dp, weight = ToonIconWeight.Bold, filled = true)
                PageText("WELCOME BACK", 500, 14f, 1f, c.textMuted, tracking = .14.em)
            }
            PageText("Saturday", 400, 60f, 1f, c.text, display = true)
            PageText(
                "Plans for the good part of the week.",
                500,
                16f,
                1.45f,
                c.textMuted,
                modifier = Modifier.width(300.dp),
            )
        }
        At(24, 330) { ToonTextField("pavel@example.com", {}, "Email") }
        At(24, 430) { ToonTextField("", {}, "Password", helper = "At least 8 characters") }
        At(16, 544) { ToonCheckbox(true, {}, "Keep me signed in") }
        At(24, 640) { ToonButton("Sign in", {}, width = 342.dp, idle = true) }
        At(24, 708) { ToonButton("Create an account", {}, variant = ToonButtonVariant.Text, width = 342.dp) }
    }

@Composable
private fun PlanList(tokens: ToonTokens) =
    Phone(tokens) {
        At(0, 28) { ToonTopAppBar("Saturday", actions = listOf(ToonIcons.Search, ToonIcons.More), nav = false) }
        At(24, 112) { ToonTabs(listOf("Today", "Weekend", "Someday")) }
        Row(Modifier.offset(20.dp, 180.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToonChip("Outdoors", selected = true, tilt = -1.5f)
            ToonChip("Indoors")
            ToonChip("With kids", tilt = 1f)
        }
        Column(Modifier.offset(24.dp, 258.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            SCREEN_PLANS.take(4).forEachIndexed { j, (title, meta, time) ->
                ToonCard(title, meta = meta, time = time, index = j % 3, dismissible = true)
            }
        }
        Box(Modifier.fillMaxWidth().padding(end = 24.dp).offset(y = 676.dp), contentAlignment = Alignment.TopEnd) {
            ToonFab({}, label = "New plan", land = false)
        }
        At(0, 764) { ToonNavigationBar(NavFrames.ITEMS, badgeIndex = 2) }
    }

@Composable
private fun Settings(
    tokens: ToonTokens,
    radio: Int,
) = Phone(tokens) {
    At(0, 28) { ToonTopAppBar("Settings", actions = emptyList()) }
    Column(Modifier.offset(24.dp, 120.dp)) {
        ToonListItem(
            "Reminders",
            icon = ToonIcons.Bell,
            sub = "At noon on Saturday",
            trailing = ToonListTrailing.Switch,
            on = true,
        )
        ToonListItem(
            "Weekend starts",
            icon = ToonIcons.Calendar,
            sub = "Saturday",
            trailing = ToonListTrailing.Chevron,
            index = 1,
        )
        ToonListItem(
            "Share plans",
            icon = ToonIcons.Person,
            sub = "With family",
            trailing = ToonListTrailing.Check,
            on = true,
            index = 2,
        )
        ToonListItem(
            "Calm motion",
            icon = ToonIcons.Info,
            sub = "Fewer bounces, no effects",
            trailing = ToonListTrailing.Switch,
            last = true,
        )
    }
    At(24, 430) { ToonDivider(width = 342.dp) }
    At(28, 462) { PageText("LOOK", 800, 12f, 1f, Toon.colors.textMuted, tracking = .12.em) }
    At(24, 484) { ToonRadioGroup(listOf("Ink & Gouache", "Night scene", "Bright Cel"), radio, {}) }
    At(24, 660) { ToonButton("Sign out", {}, variant = ToonButtonVariant.Outlined, width = 342.dp) }
    At(0, 764) { ToonNavigationBar(NavFrames.ITEMS, selected = 3) }
}

@ViddikScreenshot(group = "Screens", name = "SignIn_InkGouacheLight", width = 390, height = 844)
@Composable
fun ScreensSignInInkGouacheLight() = SignIn(inkGouacheLight)

@ViddikScreenshot(group = "Screens", name = "List_InkGouacheLight", width = 390, height = 844)
@Composable
fun ScreensListInkGouacheLight() = PlanList(inkGouacheLight)

@ViddikScreenshot(group = "Screens", name = "Settings_InkGouacheLight", width = 390, height = 844)
@Composable
fun ScreensSettingsInkGouacheLight() = Settings(inkGouacheLight, 0)

@ViddikScreenshot(group = "Screens", name = "SignIn_InkGouacheNight", width = 390, height = 844)
@Composable
fun ScreensSignInInkGouacheNight() = SignIn(inkGouacheNight)

@ViddikScreenshot(group = "Screens", name = "List_InkGouacheNight", width = 390, height = 844)
@Composable
fun ScreensListInkGouacheNight() = PlanList(inkGouacheNight)

@ViddikScreenshot(group = "Screens", name = "Settings_InkGouacheNight", width = 390, height = 844)
@Composable
fun ScreensSettingsInkGouacheNight() = Settings(inkGouacheNight, 1)

@ViddikScreenshot(group = "Screens", name = "SignIn_BrightCel", width = 390, height = 844)
@Composable
fun ScreensSignInBrightCel() = SignIn(brightCel)

@ViddikScreenshot(group = "Screens", name = "List_BrightCel", width = 390, height = 844)
@Composable
fun ScreensListBrightCel() = PlanList(brightCel)

@ViddikScreenshot(group = "Screens", name = "Settings_BrightCel", width = 390, height = 844)
@Composable
fun ScreensSettingsBrightCel() = Settings(brightCel, 2)

/** The page's desktop layout: 1280 × 800, the same components with wider props and no bottom bar. */
@Composable
private fun Desktop() =
    ToonTheme(inkGouacheLight, reduceMotion = true) {
        val muted = Toon.colors.textMuted
        Box(Modifier.size(1280.dp, 800.dp).background(PAGE)) {
            Box(Modifier.size(1280.dp, 800.dp).clip(RoundedCornerShape(18.dp))) {
                ToonStage(Modifier.size(1280.dp, 800.dp))
                At(16, 16) {
                    ToonTopAppBar(
                        "Saturday",
                        actions = listOf(ToonIcons.Settings, ToonIcons.Search),
                        nav = false,
                        width = 1248.dp,
                    )
                }
                At(40, 112) { ToonTabs(listOf("Today", "Weekend", "Someday"), width = 480.dp) }
                Row(Modifier.offset(36.dp, 178.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ToonChip("Outdoors", selected = true, tilt = -1.5f)
                    ToonChip("Indoors")
                    ToonChip("With kids", tilt = 1f)
                }
                // A grid of two 410 dp columns, 22 dp between rows and 28 between columns.
                val plans = SCREEN_PLANS + Triple("Garden swap", "Tomato seedlings", "Sun")
                Column(Modifier.offset(40.dp, 254.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    plans.chunked(2).forEachIndexed { row, pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                            pair.forEachIndexed { col, (title, meta, time) ->
                                val j = row * 2 + col
                                ToonCard(
                                    title,
                                    meta = meta,
                                    time = time,
                                    index = j % 3,
                                    width = 410.dp,
                                    dismissible = true,
                                )
                            }
                        }
                    }
                }
                At(704, 700) { ToonFab({}, label = "New plan", land = false) }
                Column(
                    Modifier.offset(924.dp, 112.dp).width(316.dp).padding(top = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PageText(
                        "THIS WEEKEND",
                        800,
                        12f,
                        1f,
                        muted,
                        tracking = .12.em,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
                    )
                    ToonListItem(
                        "Reminders",
                        icon = ToonIcons.Bell,
                        sub = "At noon",
                        trailing = ToonListTrailing.Switch,
                        on = true,
                        width = 316.dp,
                    )
                    ToonListItem(
                        "Share plans",
                        icon = ToonIcons.Person,
                        sub = "With family",
                        trailing = ToonListTrailing.Check,
                        on = true,
                        index = 1,
                        width = 316.dp,
                    )
                    ToonListItem(
                        "Weekend starts",
                        icon = ToonIcons.Calendar,
                        sub = "Saturday",
                        index = 2,
                        last = true,
                        width = 316.dp,
                    )
                    Box(Modifier.size(1.dp, 18.dp))
                    // On the page the 14 dp bar sits in a line box as tall as the body text's strut: the
                    // page's Figtree at 16 px and `line-height: normal`, 1.2 of it.
                    Box(Modifier.height(19.2.dp)) { ToonProgress(value = .6f, width = 300.dp) }
                    PageText(
                        "3 of 5 plans done",
                        500,
                        13f,
                        1.4f,
                        muted,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
                    )
                    Box(Modifier.size(1.dp, 18.dp))
                    ToonButton("Share the weekend", {}, variant = ToonButtonVariant.Outlined, width = 316.dp)
                }
            }
        }
    }

@ViddikScreenshot(group = "Screens", name = "Desktop_InkGouacheLight", width = 1280, height = 800)
@Composable
fun ScreensDesktopInkGouacheLight() = Desktop()
