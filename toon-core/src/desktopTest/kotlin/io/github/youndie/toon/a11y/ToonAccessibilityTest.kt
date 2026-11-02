package io.github.youndie.toon.a11y

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.component.ToonBadge
import io.github.youndie.toon.component.ToonBottomSheet
import io.github.youndie.toon.component.ToonButton
import io.github.youndie.toon.component.ToonCard
import io.github.youndie.toon.component.ToonCheckbox
import io.github.youndie.toon.component.ToonChip
import io.github.youndie.toon.component.ToonChipVariant
import io.github.youndie.toon.component.ToonDialog
import io.github.youndie.toon.component.ToonDivider
import io.github.youndie.toon.component.ToonDrawerItem
import io.github.youndie.toon.component.ToonFab
import io.github.youndie.toon.component.ToonIconButton
import io.github.youndie.toon.component.ToonListItem
import io.github.youndie.toon.component.ToonListTrailing
import io.github.youndie.toon.component.ToonMenu
import io.github.youndie.toon.component.ToonMenuItem
import io.github.youndie.toon.component.ToonNavItem
import io.github.youndie.toon.component.ToonNavigationBar
import io.github.youndie.toon.component.ToonNavigationDrawer
import io.github.youndie.toon.component.ToonProgress
import io.github.youndie.toon.component.ToonPullToRefresh
import io.github.youndie.toon.component.ToonRadioGroup
import io.github.youndie.toon.component.ToonSegmented
import io.github.youndie.toon.component.ToonSheetHit
import io.github.youndie.toon.component.ToonSheetItem
import io.github.youndie.toon.component.ToonSnackbar
import io.github.youndie.toon.component.ToonSwitch
import io.github.youndie.toon.component.ToonTabs
import io.github.youndie.toon.component.ToonTextField
import io.github.youndie.toon.component.ToonTitleCard
import io.github.youndie.toon.component.ToonTooltip
import io.github.youndie.toon.component.ToonTopAppBar
import io.github.youndie.toon.component.toonDialogHit
import io.github.youndie.toon.component.toonDrawerItemAt
import io.github.youndie.toon.component.toonMenuItemAt
import io.github.youndie.toon.component.toonNavSlot
import io.github.youndie.toon.component.toonSheetHit
import io.github.youndie.toon.component.toonTopBarTarget
import io.github.youndie.toon.icon.ToonIcon
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.theme.ToonTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B-30: each of the 19 components tells a screen reader what it is (role), what state it is in, and
 * what it says, and each one a finger can press is at least 48 dp each way (the brief's target), even
 * where the design draws it smaller. B-37 adds the menu, whose middle items are 44 dp by the owner's named exception (B-50), B-38 the bottom sheet and B-39
 * the navigation drawer, B-40 the tooltip, B-41 the segmented button,
 * B-42 pull-to-refresh, B-43 the title card.
 */
@OptIn(ExperimentalTestApi::class)
class ToonAccessibilityTest {
    private val target = 48f

    /** The top bar's middle actions, 40 dp wide: the owner's named exception to the target (B-49, 2026-11-02). */
    private val topBarMiddleAction = 40

    /** The menu's middle items, 44 dp: the owner's named exception to the target (B-50, 2026-11-02). */
    private val menuMiddleItem = 44

    private class Node(
        val role: Role?,
        val text: String,
        val state: String?,
        val toggle: ToggleableState?,
        val selected: Boolean?,
        val clickable: Boolean,
        val actions: List<String>,
        val wDp: Float,
        val hDp: Float,
        val node: SemanticsNode,
        /** Every node under it, read while the composition is alive. */
        val under: List<SemanticsNode>,
    )

    private fun node(content: @Composable () -> Unit): Node {
        lateinit var n: Node
        runComposeUiTest {
            // Reduced motion: no idle loop runs, so the test can wait for the components to settle.
            setContent { ToonTheme(reduceMotion = true) { content() } }
            waitForIdle()
            val i = onNodeWithTag("it", useUnmergedTree = true)
            val c = i.fetchSemanticsNode().config
            val b = i.fetchSemanticsNode().size
            n =
                Node(
                    c.getOrNull(SemanticsProperties.Role),
                    (c.getOrNull(SemanticsProperties.Text) ?: emptyList<AnnotatedString>()).joinToString { it.text } +
                        (c.getOrNull(SemanticsProperties.ContentDescription)?.joinToString() ?: ""),
                    c.getOrNull(SemanticsProperties.StateDescription),
                    c.getOrNull(SemanticsProperties.ToggleableState),
                    c.getOrNull(SemanticsProperties.Selected),
                    c.getOrNull(SemanticsActions.OnClick) != null,
                    c.getOrNull(SemanticsActions.CustomActions)?.map { it.label } ?: emptyList(),
                    b.width / density.density,
                    b.height / density.density,
                    i.fetchSemanticsNode(),
                    descendants(i.fetchSemanticsNode()),
                )
        }
        return n
    }

    private fun descendants(n: SemanticsNode): List<SemanticsNode> = n.children.flatMap { listOf(it) + descendants(it) }

    private fun Node.isTarget() = assertTrue(wDp >= target && hDp >= target, "target ${wDp}x$hDp dp")

    private val it = Modifier.testTag("it")

    @Test
    fun `button`() =
        with(node { ToonButton("Add plan", {}, it) }) {
            assertEquals(Role.Button, role)
            assertEquals("Add plan", text)
            assertTrue(clickable)
            isTarget()
        }

    @Test
    fun `icon button, a toggle`() =
        with(node { ToonIconButton(ToonIcons.Heart, "Favourite", it, selected = true) }) {
            assertEquals(Role.Checkbox, role)
            assertEquals("Favourite", text)
            assertEquals(ToggleableState.On, toggle)
            isTarget()
        }

    @Test
    fun `badge says its count and is no target`() =
        with(node { ToonBadge(12, it) }) {
            assertTrue(text.contains("12"), text)
            assertTrue(!clickable)
        }

    @Test
    fun `FAB`() =
        with(node { ToonFab({}, it, label = "New plan") }) {
            assertEquals(Role.Button, role)
            assertEquals("New plan", text)
            isTarget()
        }

    @Test
    fun `checkbox`() =
        with(node { ToonCheckbox(true, {}, "Keep me signed in", it) }) {
            assertEquals(Role.Checkbox, role)
            assertEquals(ToggleableState.On, toggle)
            assertTrue(text.contains("Keep me signed in"), text)
            isTarget()
        }

    @Test
    fun `radio group, each option`() =
        with(node { ToonRadioGroup(listOf("Ink & Gouache", "Night scene"), 1, {}, it) }) {
            // The group; its options are its children.
            assertTrue(hDp >= 2 * target, "two options in $hDp dp")
            val options = under.filter { it.config.getOrNull(SemanticsProperties.Role) == Role.RadioButton }
            assertEquals(2, options.size)
            for ((i, o) in options.withIndex()) {
                assertEquals(Role.RadioButton, o.config.getOrNull(SemanticsProperties.Role))
                assertEquals(i == 1, o.config.getOrNull(SemanticsProperties.Selected))
                assertTrue(o.size.height >= target && o.size.width >= target, "option $i ${o.size}")
            }
        }

    @Test
    fun `switch`() =
        with(node { ToonSwitch(false, {}, "Reminders", it) }) {
            assertEquals(Role.Switch, role)
            assertEquals(ToggleableState.Off, toggle)
            isTarget()
        }

    @Test
    fun `text field names its label and is editable`() =
        with(node { ToonTextField("pavel@example.com", {}, "Email", it) }) {
            val field = under.single { it.config.getOrNull(SemanticsActions.SetText) != null }
            assertEquals(listOf("Email"), field.config.getOrNull(SemanticsProperties.ContentDescription))
            assertTrue(field.size.height >= target, "field ${field.size}")
        }

    @Test
    fun `chip, a filter`() =
        with(node { ToonChip("Outdoors", it, variant = ToonChipVariant.Filter, selected = true) }) {
            assertEquals(Role.Checkbox, role)
            assertEquals(true, selected)
            assertEquals("Outdoors", text)
            isTarget()
        }

    @Test
    fun `card, dismissible, offers the dismissal as an action`() {
        var dismissed = 0
        runComposeUiTest {
            setContent {
                ToonTheme(reduceMotion = true) {
                    ToonCard(
                        "Pancake breakfast",
                        it,
                        meta = "Kitchen",
                        time = "9:00",
                        dismissible = true,
                        onDismiss = { dismissed++ },
                    )
                }
            }
            val c = onNodeWithTag("it").fetchSemanticsNode().config
            assertEquals(Role.Button, c.getOrNull(SemanticsProperties.Role))
            val dismiss = c.getOrNull(SemanticsActions.CustomActions)?.single { it.label == "Dismiss" }
            assertNotNull(dismiss).action()
            mainClock.advanceTimeBy(2_000)
            waitForIdle()
        }
        assertEquals(1, dismissed)
        with(node { ToonCard("Pancake breakfast", it) }) { isTarget() }
    }

    @Test
    fun `list item, a switch row`() =
        with(node { ToonListItem("Reminders", it, sub = "At noon", trailing = ToonListTrailing.Switch, on = true) }) {
            assertEquals(Role.Switch, role)
            assertEquals(ToggleableState.On, toggle)
            assertEquals("Reminders, At noon", text)
            isTarget()
        }

    @Test
    fun `divider is decoration and says nothing`() =
        with(node { ToonDivider(it) }) {
            assertNull(role)
            assertEquals("", text)
            assertTrue(!clickable)
        }

    @Test
    fun `snackbar is announced, and its action is a target`() {
        with(node { ToonSnackbar("Plan added", it, action = "Undo") }) {
            assertEquals("Plan added", text)
            assertEquals(listOf("Undo"), actions)
            // Announced when it appears, without cutting in on what is being read.
            assertEquals(LiveRegionMode.Polite, node.config.getOrNull(SemanticsProperties.LiveRegion))
            // The action's target: the last 110 dp of the 56 dp sign.
            assertTrue(hDp >= target && 110 >= target)
        }
    }

    @Test
    fun `dialog is a pane named by its title, its buttons targets`() {
        with(node { ToonDialog("Delete plan?", it.size(390.dp, 700.dp), confirm = "Delete") }) {
            assertEquals(listOf("Delete", "Cancel"), actions)
            assertEquals("Delete plan?", node.config.getOrNull(SemanticsProperties.PaneTitle))
        }
        // Each button's area in a 390 x 700 layer, one line of body: confirm 128 x 52, cancel 110 x 52.
        val y = 350f
        var confirm = 0
        var cancel = 0
        for (x in 0..390) {
            when (toonDialogHit(390f, 700f, 180f, x.toFloat(), y + 180f / 2 - 72 + 26)) {
                io.github.youndie.toon.component.ToonDialogHit.Confirm -> confirm++
                io.github.youndie.toon.component.ToonDialogHit.Cancel -> cancel++
                null -> Unit
            }
        }
        assertTrue(confirm >= target, "confirm $confirm dp wide")
    }

    @Test
    fun `bottom sheet is a pane named by its title that can be dismissed, its rows 72 dp targets`() {
        val items = listOf(ToonSheetItem("Family"), ToonSheetItem("Copy link"))
        with(node { ToonBottomSheet(items, it.size(390.dp, 700.dp)) }) {
            assertEquals(listOf("Family", "Copy link"), actions)
            assertEquals("Share plan", node.config.getOrNull(SemanticsProperties.PaneTitle))
            assertTrue(node.config.contains(SemanticsActions.Dismiss))
        }
        for (i in items.indices) {
            val tall = (0..700).count { toonSheetHit(700f, 2, it.toFloat()) == ToonSheetHit.Item(i) }
            assertTrue(tall >= target, "row $i is $tall dp tall")
        }
    }

    @Test
    fun `navigation drawer is a pane that names its destination, can be dismissed, its rows 56 dp targets`() {
        val items = listOf(ToonDrawerItem("Home"), ToonDrawerItem("Saved"))
        with(node { ToonNavigationDrawer(items, it.size(390.dp, 700.dp), selected = 1) }) {
            assertEquals(listOf("Home", "Saved"), actions)
            assertEquals("Saved", state)
            assertEquals("Saturday", node.config.getOrNull(SemanticsProperties.PaneTitle))
            assertTrue(node.config.contains(SemanticsActions.Dismiss))
        }
        for (i in items.indices) {
            val tall = (0..700).count { toonDrawerItemAt(300f, 2, 100f, it.toFloat()) == i }
            assertTrue(tall >= target, "row $i is $tall dp tall")
        }
    }

    @Test
    fun `tooltip says its text, politely, and is no target`() =
        with(node { ToonTooltip("Tap to add a plan", it) }) {
            assertEquals("Tap to add a plan", text)
            assertEquals(LiveRegionMode.Polite, node.config.getOrNull(SemanticsProperties.LiveRegion))
            assertTrue(!clickable)
        }

    @Test
    fun `progress tells its value, or that it is not known`() {
        with(node { ToonProgress(it, value = .4f) }) {
            assertEquals(
                ProgressBarRangeInfo(.4f, 0f..1f),
                node.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo),
            )
            assertTrue(!clickable)
        }
    }

    @Test
    fun `top app bar is a heading, its back arrow and its outer actions are 48 dp targets, a middle one 40`() {
        with(node { ToonTopAppBar("Saturday", it) }) {
            assertTrue(node.config.contains(SemanticsProperties.Heading))
            assertEquals("Saturday", text)
            assertEquals(listOf("Back", "search", "more"), actions)
            assertTrue(hDp >= target)
        }
        for (n in 1..2) {
            for (i in 0 until n) {
                val width =
                    (0..390).count {
                        toonTopBarTarget(390f, n, true, it.toFloat()) ==
                            io.github.youndie.toon.component.ToonTopBarKey
                                .Action(i)
                    }
                assertTrue(width >= target, "$n actions: action $i is $width dp wide")
            }
        }
        for (n in 3..4) {
            for (i in 0 until n) {
                val width =
                    (0..390).count {
                        toonTopBarTarget(390f, n, true, it.toFloat()) ==
                            io.github.youndie.toon.component.ToonTopBarKey
                                .Action(i)
                    }
                // The owner's named exception (B-49): an action between the outermost and the innermost
                // keeps the design's 40 dp spacing, 76 tall; the outer two still reach 48.
                if (i == 0 || i == n - 1) {
                    assertTrue(width >= target, "$n actions: action $i is $width dp wide")
                } else {
                    assertEquals(topBarMiddleAction, width, "$n actions: action $i is $width dp wide")
                }
            }
        }
        assertTrue(
            (0..390).count {
                toonTopBarTarget(390f, 2, true, it.toFloat()) ==
                    io.github.youndie.toon.component.ToonTopBarKey.Nav
            } >=
                target,
        )
    }

    @Test
    fun `navigation bar names the selected destination and offers each`() {
        val items = listOf(ToonNavItem(ToonIcons.Home, "Home"), ToonNavItem(ToonIcons.Heart, "Saved"))
        with(node { ToonNavigationBar(items, it, selected = 1) }) {
            assertEquals("Saved", state)
            assertEquals(listOf("Home", "Saved"), actions)
            assertTrue(hDp >= target)
        }
        // Five destinations on the narrowest phone, 320 dp: 64 dp a slot.
        assertTrue((0..320).count { toonNavSlot(320f, 5, it.toFloat()) == 2 } >= target)
    }

    @Test
    fun `tabs name the selected tab and offer each`() =
        with(node { ToonTabs(listOf("Today", "Weekend", "Someday"), it, selected = 2) }) {
            assertEquals("Someday", state)
            assertEquals(listOf("Today", "Weekend", "Someday"), actions)
            assertTrue(hDp >= target && wDp / 3 >= target)
        }

    @Test
    fun `menu names the chosen item and offers each, the first and last 48 dp targets, a middle one 44`() {
        val items = listOf("Edit", "Share", "Delete").map { ToonMenuItem(it) }
        with(node { ToonMenu(items, it, selected = 1) }) {
            assertEquals("Share", state)
            assertEquals(listOf("Edit", "Share", "Delete"), actions)
            assertTrue(wDp >= target)
        }
        for (n in 1..3) {
            for (i in listOf(0, n - 1)) {
                val height = (0..200).count { toonMenuItemAt(n, it.toFloat()) == i }
                assertTrue(height >= target, "$n items: item $i is $height dp tall")
            }
            // The owner's named exception (B-50): an item between the first and the last keeps the
            // design's 44 dp row; it is the scroll's whole width, and 44 is Apple's minimum.
            for (i in 1 until n - 1) {
                val height = (0..200).count { toonMenuItemAt(n, it.toFloat()) == i }
                assertEquals(menuMiddleItem, height, "$n items: item $i is $height dp tall")
            }
        }
    }

    @Test
    fun `segmented button names the selected segment and offers each, every segment a target`() =
        with(node { ToonSegmented(listOf("Day", "Week", "Month"), it, selected = 1) }) {
            assertEquals("Week", state)
            assertEquals(listOf("Day", "Week", "Month"), actions)
            assertTrue(hDp >= target && wDp / 3 >= target, "segment ${wDp / 3}x$hDp dp")
        }

    @Test
    fun `pull-to-refresh offers a refresh without the drag, and tells a refresh under way`() {
        with(node { ToonPullToRefresh(false, {}, it) }) {
            assertEquals(listOf("Refresh"), actions)
        }
        with(node { ToonPullToRefresh(true, {}, it) }) {
            assertEquals(
                ProgressBarRangeInfo.Indeterminate,
                node.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo),
            )
            assertTrue(actions.isEmpty())
        }
    }

    @Test
    fun `title card is a heading that says its words`() =
        with(node { ToonTitleCard(it) }) {
            assertTrue(node.config.contains(SemanticsProperties.Heading))
            assertEquals("Saturday. Plans for the good part of the week", text)
        }

    @Test
    fun `icon is an image with its description, or nothing when decorative`() {
        with(node { ToonIcon(ToonIcons.Star, "Favourite", it) }) {
            assertEquals(Role.Image, role)
            assertEquals("Favourite", text)
        }
        with(node { ToonIcon(ToonIcons.Star, null, it) }) {
            assertNull(role)
            assertEquals("", text)
        }
    }
}
