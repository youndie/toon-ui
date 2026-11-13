package io.github.youndie.toon.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.theme.ToonTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-85: content of the caller's in the dialog and the bottom sheet. */
@OptIn(ExperimentalTestApi::class)
class ToonOverlayContentTest {
    @Test
    fun `the dialog holds two radio groups between its body and its buttons, and grows for them`() {
        runComposeUiTest {
            setContent {
                ToonTheme {
                    ToonDialog(
                        "Appearance",
                        "Готово",
                        "Отмена",
                        Modifier.size(390.dp, 700.dp).testTag("dialog"),
                        body = "Skin and theme",
                    ) {
                        Column(Modifier.testTag("slot")) {
                            ToonRadioGroup(listOf("Toon", "Plakat"), 0, {})
                            ToonRadioGroup(listOf("Day", "Night"), 0, {})
                        }
                    }
                }
            }
            val slot = onNodeWithTag("slot").assertIsDisplayed().getBoundsInRoot()
            val slotH = (slot.bottom - slot.top).value
            assertTrue(slotH > 40f, "the slot laid out $slotH dp")
            val cardH = toonDialogHeight(1, slotH)
            val top = 700 / 2 - cardH / 2
            assertEquals(top + toonDialogSlotTop(1), slot.top.value, .6f, "the slot starts under the body")
            assertEquals((390 - 300) / 2f + 24, slot.left.value, .6f, "the slot starts at the body's margin")
            assertTrue(slot.bottom.value <= top + cardH - 72 - 20, "the slot ends above the buttons")
        }
    }

    private fun focusedText(test: androidx.compose.ui.test.ComposeUiTest): String =
        test.onNode(isFocused()).fetchSemanticsNode().config.let {
            it.getOrNull(SemanticsProperties.Text)?.joinToString() ?: it.getOrNull(SemanticsProperties.PaneTitle) ?: "?"
        }

    @Test
    fun `Tab walks the dialog's buttons, then its slot, and comes round to the buttons`() {
        runComposeUiTest {
            setContent {
                ToonTheme {
                    ToonDialog("Appearance", "Готово", "Отмена", Modifier.size(390.dp, 700.dp).testTag("dialog")) {
                        Column {
                            ToonRadioGroup(listOf("Toon", "Plakat"), 0, {})
                            ToonRadioGroup(listOf("Day", "Night"), 0, {})
                        }
                    }
                }
            }
            onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Appearance"))
                .performSemanticsAction(SemanticsActions.RequestFocus)
            val seen = mutableListOf(focusedText(this))
            repeat(6) {
                onNode(isFocused()).performKeyInput { pressKey(Key.Tab) }
                seen += focusedText(this)
            }
            // Confirm, Cancel (both the dialog's own node), then the four options, then round to the dialog.
            assertEquals(listOf("Appearance", "Appearance", "Toon", "Plakat", "Day", "Night", "Appearance"), seen)
        }
    }

    @Test
    fun `Escape in the dialog's slot cancels, and a closed dialog composes no slot`() {
        runComposeUiTest {
            var open by mutableStateOf(true)
            var cancels = 0
            setContent {
                ToonTheme {
                    ToonDialog("A", "OK", "No", Modifier.size(390.dp, 700.dp), open = open, onCancel = { cancels++ }) {
                        ToonRadioGroup(listOf("One", "Two"), 0, {}, Modifier.testTag("slot"))
                    }
                }
            }
            onNodeWithTag("slot").assertIsDisplayed()
            onNode(hasText("One")).performSemanticsAction(SemanticsActions.RequestFocus)
            onNode(isFocused()).performKeyInput { pressKey(Key.Escape) }
            assertEquals(1, cancels, "Escape in the dialog's slot")
            open = false
            waitForIdle()
            onNodeWithTag("slot").assertDoesNotExist()
        }
    }

    @Test
    fun `Escape in the sheet's slot closes, and a closed sheet composes no slot`() {
        runComposeUiTest {
            var open by mutableStateOf(true)
            var closes = 0
            setContent {
                ToonTheme {
                    ToonBottomSheetBox("B", Modifier.size(390.dp, 700.dp), open = open, onClose = { closes++ }) {
                        ToonRadioGroup(listOf("Three", "Four"), 0, {}, Modifier.testTag("slot"))
                    }
                }
            }
            onNodeWithTag("slot").assertIsDisplayed()
            onNode(hasText("Three")).performSemanticsAction(SemanticsActions.RequestFocus)
            onNode(isFocused()).performKeyInput { pressKey(Key.Escape) }
            assertEquals(1, closes, "Escape in the sheet's slot")
            open = false
            waitForIdle()
            onNodeWithTag("slot").assertDoesNotExist()
        }
    }

    @Test
    fun `the sheet holds its slot under its title and grows for it`() {
        runComposeUiTest {
            setContent {
                ToonTheme {
                    ToonBottomSheetBox("Appearance", Modifier.size(390.dp, 700.dp)) {
                        Column(Modifier.testTag("slot")) {
                            ToonRadioGroup(listOf("Toon", "Plakat"), 0, {})
                            ToonRadioGroup(listOf("Day", "Night"), 0, {})
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(1_000)
            val slot = onNodeWithTag("slot").assertIsDisplayed().getBoundsInRoot()
            val slotH = (slot.bottom - slot.top).value
            val top = 700 - toonSheetHeight(700f, 0, slotH)
            assertEquals(top + 70, slot.top.value, .6f, "the slot starts where the first row would")
            assertEquals(24f, slot.left.value, .6f)
            assertEquals(700f - 40, slot.bottom.value, .6f, "the sheet ends 40 dp under the slot")
        }
    }
}
