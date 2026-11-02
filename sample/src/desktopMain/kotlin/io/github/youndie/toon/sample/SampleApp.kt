package io.github.youndie.toon.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import io.github.youndie.toon.component.ToonNavItem
import io.github.youndie.toon.component.ToonNavigationBar
import io.github.youndie.toon.component.ToonRadioGroup
import io.github.youndie.toon.component.ToonSnackbar
import io.github.youndie.toon.component.ToonTabs
import io.github.youndie.toon.component.ToonTextField
import io.github.youndie.toon.component.ToonTopAppBar
import io.github.youndie.toon.icon.ToonIcon
import io.github.youndie.toon.icon.ToonIconWeight
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.theme.ToonStage
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.theme.ToonThemes
import io.github.youndie.toon.theme.textStyle
import io.github.youndie.toon.tokens.ToonTypeScale
import kotlinx.coroutines.delay

// B-29: the Screens page's three screens, live. Sign in leads to the list; a card swiped away shows
// the snackbar, whose Undo puts it back; the navigation bar's Profile opens the settings, whose "Look"
// switches the theme, and Sign out goes back to the start. Laid out at the page's places on a phone.

/** The page's demo plans (`ITEMS`). */
private val PLANS =
    listOf(
        Triple("Pancake breakfast", "Kitchen, bring syrup", "9:00"),
        Triple("Bike to the lake", "Check the tyre pump", "11:30"),
        Triple("Paint the fence", "Green this time", "14:00"),
        Triple("Picnic by the lake", "Blanket and lemonade", "16:00"),
        Triple("Movie night", "Popcorn duty: you", "20:00"),
    )

private val DESTINATIONS =
    listOf(
        ToonNavItem(ToonIcons.Home, "Home"),
        ToonNavItem(ToonIcons.Search, "Explore"),
        ToonNavItem(ToonIcons.Heart, "Saved"),
        ToonNavItem(ToonIcons.Person, "Profile"),
    )

private val THEMES = listOf(ToonThemes.InkGouacheLight, ToonThemes.InkGouacheNight, ToonThemes.BrightCel)

private enum class Screen { SignIn, List, Settings }

/** The sample: one phone screen at a time, in the theme the settings pick. */
@Composable
fun SampleApp() {
    var screen by remember { mutableStateOf(Screen.SignIn) }
    var look by remember { mutableIntStateOf(0) }
    ToonTheme(THEMES[look]) {
        ToonStage(Modifier.size(390.dp, 844.dp)) {
            when (screen) {
                Screen.SignIn -> {
                    SignIn { screen = Screen.List }
                }

                Screen.List -> {
                    PlanList { if (it == 3) screen = Screen.Settings }
                }

                Screen.Settings -> {
                    Settings(look, { look = it }, { screen = Screen.SignIn }) {
                        if (it ==
                            0
                        ) {
                            screen = Screen.List
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun At(
    x: Int,
    y: Int,
    content: @Composable () -> Unit,
) = Box(Modifier.offset(x.dp, y.dp)) { content() }

@Composable
private fun SignIn(onSignIn: () -> Unit) {
    var email by remember { mutableStateOf("pavel@example.com") }
    var password by remember { mutableStateOf("") }
    var keep by remember { mutableStateOf(true) }
    val muted = Toon.colors.textMuted
    Column(Modifier.offset(28.dp, 118.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToonIcon(ToonIcons.Star, null, size = 32.dp, weight = ToonIconWeight.Bold, filled = true)
            BasicText("WELCOME BACK", style = ToonTypeScale.Label.textStyle.copy(color = muted, letterSpacing = .14.em))
        }
        BasicText("Saturday", style = ToonTypeScale.Display.textStyle.copy(fontSize = 60.sp, lineHeight = 64.sp))
        BasicText(
            "Plans for the good part of the week.",
            Modifier.width(300.dp),
            style = ToonTypeScale.Body.textStyle.copy(color = muted),
        )
    }
    At(24, 330) { ToonTextField(email, { email = it }, "Email") }
    At(24, 430) { ToonTextField(password, { password = it }, "Password", helper = "At least 8 characters") }
    At(16, 544) { ToonCheckbox(keep, { keep = it }, "Keep me signed in") }
    At(24, 640) { ToonButton("Sign in", onSignIn, Modifier.testTag("sign-in"), width = 342.dp, idle = true) }
    At(24, 708) { ToonButton("Create an account", {}, variant = ToonButtonVariant.Text, width = 342.dp) }
}

@Composable
private fun PlanList(onNavigate: (Int) -> Unit) {
    val plans = remember { mutableStateListOf(0, 1, 2, 3, 4) }
    var removed by remember { mutableStateOf<Int?>(null) }
    var snack by remember { mutableStateOf(false) }
    // The page hides the snackbar 2.6 s after a dismissal.
    LaunchedEffect(snack, removed) {
        if (!snack) return@LaunchedEffect
        delay(2_600)
        snack = false
    }
    At(0, 28) { ToonTopAppBar("Saturday", actions = listOf(ToonIcons.Search, ToonIcons.More), nav = false) }
    At(24, 112) { ToonTabs(listOf("Today", "Weekend", "Someday")) }
    Row(Modifier.offset(20.dp, 180.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ToonChip("Outdoors", selected = true, tilt = -1.5f)
        ToonChip("Indoors")
        ToonChip("With kids", tilt = 1f)
    }
    Column(Modifier.offset(24.dp, 258.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        plans.take(4).forEachIndexed { j, i ->
            val (title, meta, time) = PLANS[i]
            androidx.compose.runtime.key(i) {
                ToonCard(
                    title,
                    Modifier.testTag("card-$i"),
                    meta = meta,
                    time = time,
                    index = j % 3,
                    appear = true,
                    delay = j * 40,
                    dismissible = true,
                    onDismiss = {
                        plans.remove(i)
                        removed = i
                        snack = true
                    },
                )
            }
        }
    }
    Box(Modifier.fillMaxWidth().padding(end = 24.dp).offset(y = 676.dp), contentAlignment = Alignment.TopEnd) {
        ToonFab({}, label = "New plan", delay = 300)
    }
    At(24, 600) {
        ToonSnackbar(
            "Plan removed",
            Modifier.testTag("snackbar"),
            action = "Undo",
            shown = snack,
            onAction = {
                removed?.let { if (it !in plans) plans.add(0, it) }
                snack = false
            },
        )
    }
    At(0, 764) { ToonNavigationBar(DESTINATIONS, Modifier.testTag("nav"), badgeIndex = 2, onChange = onNavigate) }
}

@Composable
private fun Settings(
    look: Int,
    onLook: (Int) -> Unit,
    onSignOut: () -> Unit,
    onNavigate: (Int) -> Unit,
) {
    var reminders by remember { mutableStateOf(true) }
    var calm by remember { mutableStateOf(false) }
    At(0, 28) { ToonTopAppBar("Settings", actions = emptyList()) }
    Column(Modifier.offset(24.dp, 120.dp)) {
        ToonListItem("Reminders", icon = ToonIcons.Bell, sub = "At noon on Saturday", trailing = ToonListTrailing.Switch, on = reminders, onCheckedChange = {
            reminders =
                it
        })
        ToonListItem("Weekend starts", icon = ToonIcons.Calendar, sub = "Saturday", index = 1)
        ToonListItem(
            "Share plans",
            icon = ToonIcons.Person,
            sub = "With family",
            trailing = ToonListTrailing.Check,
            on = true,
            index = 2,
        )
        ToonListItem("Calm motion", icon = ToonIcons.Info, sub = "Fewer bounces, no effects", trailing = ToonListTrailing.Switch, on = calm, last = true, onCheckedChange = {
            calm =
                it
        })
    }
    At(24, 430) { ToonDivider(width = 342.dp) }
    At(28, 462) {
        BasicText(
            "LOOK",
            style =
                ToonTypeScale.Label.textStyle.copy(
                    color = Toon.colors.textMuted,
                    fontSize = 12.sp,
                    letterSpacing = .12.em,
                ),
        )
    }
    At(24, 484) {
        ToonRadioGroup(listOf("Ink & Gouache", "Night scene", "Bright Cel"), look, onLook, Modifier.testTag("look"))
    }
    At(24, 660) { ToonButton("Sign out", onSignOut, variant = ToonButtonVariant.Outlined, width = 342.dp) }
    At(0, 764) { ToonNavigationBar(DESTINATIONS, selected = 3, onChange = onNavigate) }
}
