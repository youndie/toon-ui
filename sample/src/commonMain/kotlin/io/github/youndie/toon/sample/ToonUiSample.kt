package io.github.youndie.toon.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import io.github.youndie.toon.component.ToonBadge
import io.github.youndie.toon.component.ToonBottomSheet
import io.github.youndie.toon.component.ToonButton
import io.github.youndie.toon.component.ToonButtonVariant
import io.github.youndie.toon.component.ToonCardList
import io.github.youndie.toon.component.ToonCardListItem
import io.github.youndie.toon.component.ToonCheckbox
import io.github.youndie.toon.component.ToonChip
import io.github.youndie.toon.component.ToonChipVariant
import io.github.youndie.toon.component.ToonDialog
import io.github.youndie.toon.component.ToonDivider
import io.github.youndie.toon.component.ToonDrawerItem
import io.github.youndie.toon.component.ToonFab
import io.github.youndie.toon.component.ToonIconButton
import io.github.youndie.toon.component.ToonIconButtonVariant
import io.github.youndie.toon.component.ToonListItem
import io.github.youndie.toon.component.ToonListTrailing
import io.github.youndie.toon.component.ToonMenu
import io.github.youndie.toon.component.ToonMenuItem
import io.github.youndie.toon.component.ToonNavItem
import io.github.youndie.toon.component.ToonNavigationBar
import io.github.youndie.toon.component.ToonNavigationDrawer
import io.github.youndie.toon.component.ToonProgress
import io.github.youndie.toon.component.ToonProgressKind
import io.github.youndie.toon.component.ToonPullToRefresh
import io.github.youndie.toon.component.ToonRadioGroup
import io.github.youndie.toon.component.ToonSegmented
import io.github.youndie.toon.component.ToonSheetItem
import io.github.youndie.toon.component.ToonSlider
import io.github.youndie.toon.component.ToonSnackbarHost
import io.github.youndie.toon.component.ToonSnackbarHostState
import io.github.youndie.toon.component.ToonSnackbarType
import io.github.youndie.toon.component.ToonSwitch
import io.github.youndie.toon.component.ToonTabs
import io.github.youndie.toon.component.ToonTextField
import io.github.youndie.toon.component.ToonTitleCard
import io.github.youndie.toon.component.ToonTitleCardVariant
import io.github.youndie.toon.component.ToonTooltip
import io.github.youndie.toon.component.ToonTopAppBar
import io.github.youndie.toon.component.ToonTopAppBarVariant
import io.github.youndie.toon.component.ToonTransition
import io.github.youndie.toon.component.ToonTransitionHost
import io.github.youndie.toon.fx.ToonFxHost
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
import kotlinx.coroutines.launch

// The sample's front page, on the desktop and in the browser alike (B-32): every component of the
// library on one sheet, grouped as the design's pages group them, under the three themes and reduce
// motion; and beside it the Screens page's three phone screens (B-29).

private val LOOKS = listOf(ToonThemes.InkGouacheLight, ToonThemes.InkGouacheNight, ToonThemes.BrightCel)

private val PLAN_ITEMS =
    listOf(
        ToonCardListItem(0, "Pancake breakfast", "Kitchen, bring syrup", "9:00"),
        ToonCardListItem(1, "Bike to the lake", "Check the tyre pump", "11:30"),
        ToonCardListItem(2, "Paint the fence", "Green this time", "14:00"),
    )

private enum class Overlay { None, Dialog, Sheet, Drawer }

/** The whole sample: a header with the page, the theme and reduce motion, and the kit or the screens under it. */
@Composable
fun ToonUiSample() {
    var look by remember { mutableIntStateOf(0) }
    var calm by remember { mutableStateOf(false) }
    var page by remember { mutableIntStateOf(0) }
    var overlay by remember { mutableStateOf(Overlay.None) }
    ToonTheme(LOOKS[look], reduceMotion = calm) {
        ToonStage(Modifier.fillMaxSize()) {
            ToonFxHost(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        Header(page, { page = it }, look, { look = it }, calm, { calm = it })
                        when (page) {
                            0 -> {
                                Kit { overlay = it }
                            }

                            else -> {
                                Box(
                                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                                    Alignment.TopCenter,
                                ) { SampleApp() }
                            }
                        }
                    }
                    ToonDialog(
                        "Delete plan?",
                        open = overlay == Overlay.Dialog,
                        body = "Pancake breakfast will be gone for good.",
                        confirm = "Delete",
                        onConfirm = { overlay = Overlay.None },
                        onCancel = { overlay = Overlay.None },
                    )
                    ToonBottomSheet(
                        listOf(ToonSheetItem("Family", ToonIcons.Person), ToonSheetItem("Copy link", ToonIcons.Share)),
                        open = overlay == Overlay.Sheet,
                        onSelect = { overlay = Overlay.None },
                        onClose = { overlay = Overlay.None },
                    )
                    ToonNavigationDrawer(
                        listOf(
                            ToonDrawerItem("Home", ToonIcons.Home),
                            ToonDrawerItem("Saved", ToonIcons.Heart),
                            ToonDrawerItem("Settings", ToonIcons.Settings),
                        ),
                        open = overlay == Overlay.Drawer,
                        onSelect = { overlay = Overlay.None },
                        onClose = { overlay = Overlay.None },
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(
    page: Int,
    onPage: (Int) -> Unit,
    look: Int,
    onLook: (Int) -> Unit,
    calm: Boolean,
    onCalm: (Boolean) -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToonIcon(ToonIcons.Star, null, size = 34.dp, weight = ToonIconWeight.Bold, filled = true)
            BasicText("toon-ui", style = ToonTypeScale.Headline.textStyle)
        }
        ToonTabs(listOf("Kit", "Screens"), Modifier.testTag("page"), selected = page, width = 240.dp, onChange = onPage)
        ToonSegmented(
            listOf("Day", "Night", "Cel"),
            Modifier.testTag("theme"),
            selected = look,
            width = 260.dp,
            onChange = onLook,
        )
        ToonSwitch(calm, onCalm, "Reduce motion")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Kit(onOverlay: (Overlay) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Section("Actions") {
            Plate("Button") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToonButton("Add plan", {}, idle = true)
                    ToonButton("Add plan", {}, variant = ToonButtonVariant.Outlined)
                    ToonButton("Add plan", {}, variant = ToonButtonVariant.Text)
                    var loading by remember { mutableStateOf(false) }
                    LaunchedEffect(loading) {
                        if (loading) {
                            delay(2_000)
                            loading = false
                        }
                    }
                    ToonButton("Save", { loading = true }, loading = loading)
                }
            }
            Plate("Icon button · badge") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToonIconButton(ToonIcons.Heart, "Save")
                    ToonIconButton(ToonIcons.Bell, "Notifications", variant = ToonIconButtonVariant.Filled)
                    Box {
                        ToonIconButton(ToonIcons.Share, "Share", toggle = false)
                        Box(Modifier.align(Alignment.TopEnd)) { ToonBadge(3) }
                    }
                }
            }
            Plate("FAB") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToonFab({}, contentDescription = "New plan")
                    ToonFab({}, label = "New plan", delay = 150)
                }
            }
            Plate("Chip") {
                FlowRow(
                    Modifier.width(320.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ToonChip("Outdoors", selected = true, tilt = -1.5f)
                    ToonChip("Indoors")
                    ToonChip("Saturday", variant = ToonChipVariant.Assist, tilt = 1f)
                    ToonChip("With kids", variant = ToonChipVariant.Input, icon = ToonIcons.Person)
                }
            }
        }

        Section("Inputs") {
            Plate("Text field") {
                var name by remember { mutableStateOf("Pancakes") }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ToonTextField(name, { name = it }, "Plan name", width = 300.dp)
                    ToonTextField("Pan", {}, "Plan name", error = "At least 4 letters", width = 300.dp)
                }
            }
            Plate("Checkbox · radio · switch") {
                var snacks by remember { mutableStateOf(true) }
                var who by remember { mutableIntStateOf(0) }
                var reminders by remember { mutableStateOf(true) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToonCheckbox(snacks, { snacks = it }, "Bring snacks")
                    ToonRadioGroup(listOf("Just me", "Family", "Everyone"), who, { who = it })
                    ToonSwitch(reminders, { reminders = it }, "Reminders")
                }
            }
            Plate("Slider · segmented") {
                var value by remember { mutableFloatStateOf(.4f) }
                var steps by remember { mutableFloatStateOf(.5f) }
                var span by remember { mutableIntStateOf(0) }
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    ToonSlider(value, { value = it })
                    ToonSlider(steps, { steps = it }, steps = 4)
                    ToonSegmented(listOf("Day", "Week", "Month"), selected = span, onChange = { span = it })
                }
            }
        }

        Section("Content") {
            Plate("Card list") {
                val cards = remember { mutableStateListOf(*PLAN_ITEMS.toTypedArray()) }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ToonCardList(cards, appear = true, dismissible = true, onDismiss = { cards.remove(it) })
                    if (cards.size < PLAN_ITEMS.size) {
                        ToonButton("Put them back", {
                            PLAN_ITEMS.filter { it !in cards }.forEach {
                                cards.add(
                                    PLAN_ITEMS.indexOf(it).coerceAtMost(cards.size),
                                    it,
                                )
                            }
                        }, variant = ToonButtonVariant.Text)
                    }
                }
            }
            Plate("List item · divider") {
                var on by remember { mutableStateOf(true) }
                Column {
                    ToonListItem("Weekend plans", icon = ToonIcons.Calendar, sub = "3 plans")
                    ToonListItem(
                        "Reminders",
                        icon = ToonIcons.Bell,
                        sub = "At noon",
                        trailing = ToonListTrailing.Switch,
                        on = on,
                        index = 1,
                        onCheckedChange = { on = it },
                    )
                    ToonListItem(
                        "Share plans",
                        icon = ToonIcons.Person,
                        sub = "With family",
                        trailing = ToonListTrailing.Check,
                        on = true,
                        index = 2,
                        last = true,
                    )
                    ToonDivider(Modifier.padding(top = 12.dp))
                }
            }
            Plate("Progress") {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ToonProgress(value = .62f)
                    ToonProgress()
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        ToonProgress(kind = ToonProgressKind.Circular, value = .62f)
                        ToonProgress(kind = ToonProgressKind.Circular)
                    }
                }
            }
            Plate("Title card") {
                ToonTitleCard(
                    variant = ToonTitleCardVariant.Empty,
                    title = "Nothing yet",
                    sub = "Your weekend is wide open",
                    width = 320.dp,
                    height = 300.dp,
                )
            }
            Plate("Title card · loop") {
                ToonTitleCard(
                    title = "Saturday",
                    sub = "Plans for the good part of the week",
                    width = 320.dp,
                    height = 300.dp,
                    loop = true,
                )
            }
        }

        Section("Navigation") {
            Plate("Top app bar") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToonTopAppBar("Saturday", width = 390.dp)
                    ToonTopAppBar(
                        "Plans",
                        variant = ToonTopAppBarVariant.Center,
                        actions = listOf(ToonIcons.Search),
                        width = 390.dp,
                    )
                }
            }
            Plate("Tabs · navigation bar") {
                var tab by remember { mutableIntStateOf(0) }
                var nav by remember { mutableIntStateOf(0) }
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ToonTabs(listOf("Today", "Weekend", "Someday"), selected = tab, width = 390.dp, onChange = {
                        tab =
                            it
                    })
                    ToonNavigationBar(
                        listOf(
                            ToonNavItem(ToonIcons.Home, "Home"),
                            ToonNavItem(ToonIcons.Search, "Explore"),
                            ToonNavItem(ToonIcons.Heart, "Saved"),
                            ToonNavItem(ToonIcons.Person, "Profile"),
                        ),
                        selected = nav,
                        badgeIndex = 2,
                        onChange = { nav = it },
                    )
                }
            }
            Plate("Menu · drawer") {
                var open by remember { mutableStateOf(false) }
                var chosen by remember { mutableIntStateOf(-1) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ToonButton(
                            if (open) "Close menu" else "Open menu",
                            { open = !open },
                            variant = ToonButtonVariant.Outlined,
                        )
                        ToonButton("Drawer", { onOverlay(Overlay.Drawer) }, variant = ToonButtonVariant.Outlined)
                    }
                    Box(Modifier.size(220.dp, 210.dp)) {
                        ToonMenu(
                            listOf(
                                ToonMenuItem("Edit", ToonIcons.Edit),
                                ToonMenuItem("Share", ToonIcons.Share),
                                ToonMenuItem("Delete", ToonIcons.Delete),
                            ),
                            open = open,
                            selected = chosen,
                            onSelect = {
                                chosen = it
                                open = false
                            },
                        )
                    }
                }
            }
        }

        Section("Feedback") {
            Plate("Snackbar") {
                val host = remember { ToonSnackbarHostState() }
                val scope = rememberCoroutineScope()
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ToonButton(
                            "Info",
                            { scope.launch { host.show("Plan added", "Undo") } },
                            variant = ToonButtonVariant.Outlined,
                        )
                        ToonButton("Success", {
                            scope.launch {
                                host.show("Saved for Saturday", type = ToonSnackbarType.Success)
                            }
                        }, variant = ToonButtonVariant.Outlined)
                        ToonButton("Error", {
                            scope.launch { host.show("Couldn't save", "Retry", ToonSnackbarType.Error) }
                        }, variant = ToonButtonVariant.Outlined)
                    }
                    Box(Modifier.height(120.dp)) { ToonSnackbarHost(host) }
                }
            }
            Plate("Tooltip") {
                var show by remember { mutableStateOf(true) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToonButton(
                        if (show) "Hide tip" else "Show tip",
                        { show = !show },
                        variant = ToonButtonVariant.Outlined,
                    )
                    Box(Modifier.size(260.dp, 90.dp)) { ToonTooltip("Tap to add a plan", show = show) }
                }
            }
            Plate("Dialog · bottom sheet") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToonButton("Dialog", { onOverlay(Overlay.Dialog) })
                    ToonButton("Sheet", { onOverlay(Overlay.Sheet) }, variant = ToonButtonVariant.Outlined)
                }
            }
            Plate("Pull to refresh") {
                var refreshing by remember { mutableStateOf(false) }
                LaunchedEffect(refreshing) {
                    if (refreshing) {
                        delay(1_600)
                        refreshing = false
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Note("Drag down on the band")
                    ToonPullToRefresh(refreshing, { refreshing = true }, Modifier.height(140.dp))
                }
            }
        }

        Section("Transitions") {
            Plate("Screen transition host") {
                var how by remember { mutableIntStateOf(0) }
                var screen by remember { mutableIntStateOf(0) }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ToonSegmented(listOf("Push", "Scene", "Hole"), selected = how, onChange = { how = it })
                    ToonButton("Next screen", { screen = (screen + 1) % 3 })
                    ToonTransitionHost(
                        screen,
                        Modifier.size(300.dp, 360.dp),
                        transition = ToonTransition.entries[how],
                    ) {
                        ToonStage(Modifier.fillMaxSize()) {
                            ToonTitleCard(
                                title = listOf("Saturday", "Sunday", "Someday")[it],
                                sub = "Plans for the good part of the week",
                                width = 300.dp,
                                height = 360.dp,
                            )
                        }
                    }
                }
            }
        }
        Box(Modifier.height(40.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BasicText(title, style = ToonTypeScale.Headline.textStyle)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun Plate(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText(
            label.uppercase(),
            style = ToonTypeScale.Label.textStyle.copy(color = Toon.colors.textMuted, letterSpacing = .12.em),
        )
        Box(Modifier.padding(start = 6.dp)) { content() }
    }
}

@Composable
private fun Note(text: String) =
    BasicText(text, style = ToonTypeScale.Body.textStyle.copy(color = Toon.colors.textMuted))
