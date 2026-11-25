package io.github.youndie.toon.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.inkGouacheLight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** B-97: a dialog whose choice applies at once has one button; outside and Escape still close it. */
@OptIn(ExperimentalTestApi::class)
class ToonDialogOneButtonTest {
    @Test
    fun `with no cancel the card draws one button and no cancel word`() {
        val two = toonDialogCardScene(inkGouacheLight, "Вид", emptyList(), "Готово", "Закрыть", false, null, 60f)
        val one = toonDialogCardScene(inkGouacheLight, "Вид", emptyList(), "Готово", null, false, null, 0f)
        assertTrue(two.labels.any { it.text == "Закрыть" })
        assertTrue(one.labels.none { it.text == "Закрыть" })
        assertTrue(one.labels.any { it.text == "Готово" })
        assertTrue(one.layers.size < two.layers.size, "${one.layers.size} layers against ${two.layers.size}")
        // The confirm button stands where it stands beside a cancel: the same layers, the same numbers.
        assertEquals(
            two.layers.takeLast(one.layers.size - BODY_LAYERS).map { it.numbers.toList() },
            one.layers.drop(BODY_LAYERS).map { it.numbers.toList() },
        )
    }

    @Test
    fun `with no cancel the cancel button's place is the card's and does nothing`() {
        val h = toonDialogHeight(0)
        val y0 = 350 - h / 2
        val row = y0 + h - 72 + 26
        assertEquals(ToonDialogHit.Cancel, toonDialogHit(390f, 700f, h, 150f, row), "two buttons: cancel")
        assertNull(toonDialogHit(390f, 700f, h, 150f, row, oneButton = true), "one button: the card")
        assertEquals(ToonDialogHit.Confirm, toonDialogHit(390f, 700f, h, 260f, row, oneButton = true))
        assertEquals(ToonDialogHit.Cancel, toonDialogHit(390f, 700f, h, 20f, 350f, oneButton = true), "outside")
    }

    @Test
    fun `a one-button dialog confirms, and a tap outside and Escape still cancel`() {
        runComposeUiTest {
            var confirms = 0
            var cancels = 0
            setContent {
                ToonTheme {
                    ToonDialog(
                        "Вид",
                        "Готово",
                        null,
                        Modifier.size(390.dp, 700.dp).testTag("dialog"),
                        destructive = false,
                        onConfirm = { confirms++ },
                        onCancel = { cancels++ },
                    )
                }
            }
            val h = toonDialogHeight(0)
            val row = 350 - h / 2 + h - 72 + 26
            val dialog = onNodeWithTag("dialog")
            dialog.performTouchInput { click(Offset(150f, row)) }
            assertEquals(0 to 0, confirms to cancels, "where cancel would stand")
            dialog.performTouchInput { click(Offset(260f, row)) }
            assertEquals(1 to 0, confirms to cancels)
            dialog.performTouchInput { click(Offset(20f, 20f)) }
            assertEquals(1 to 1, confirms to cancels, "a tap outside")
            dialog.performSemanticsAction(SemanticsActions.RequestFocus)
            dialog.performKeyInput {
                pressKey(Key.Tab)
                pressKey(Key.Enter)
            }
            assertEquals(2 to 1, confirms to cancels, "Tab keeps the focus on the one button")
            dialog.performKeyInput { pressKey(Key.Escape) }
            assertEquals(2 to 2, confirms to cancels, "Escape cancels")
            val actions =
                dialog
                    .fetchSemanticsNode()
                    .config
                    .getOrNull(SemanticsActions.CustomActions)
                    .orEmpty()
            assertEquals(listOf("Готово"), actions.map { it.label }, "a reader is offered the one button")
        }
    }

    @Test
    fun `in a one-button dialog Tab goes from the button to the slot and round`() {
        runComposeUiTest {
            setContent {
                ToonTheme {
                    ToonDialog("Вид", "Готово", null, Modifier.size(390.dp, 700.dp), destructive = false) {
                        Column { ToonRadioGroup(listOf("Toon", "Plakat"), 0, {}) }
                    }
                }
            }

            fun focused() =
                onNode(isFocused()).fetchSemanticsNode().config.let {
                    it.getOrNull(SemanticsProperties.Text)?.joinToString()
                        ?: it.getOrNull(SemanticsProperties.PaneTitle)
                        ?: "?"
                }
            onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Вид"))
                .performSemanticsAction(SemanticsActions.RequestFocus)
            val seen = mutableListOf(focused())
            repeat(3) {
                onNode(isFocused()).performKeyInput { pressKey(Key.Tab) }
                seen += focused()
            }
            // The one button (the dialog's own node), then the two options, then round to the button: no second stop.
            assertEquals(listOf("Вид", "Toon", "Plakat", "Вид"), seen)
        }
    }

    private companion object {
        /** The card's own body: the layers before the buttons. */
        val BODY_LAYERS =
            toonDialogCardScene(inkGouacheLight, "Вид", emptyList(), "Готово", null, false, null, 0f).let { one ->
                one.layers.size -
                    toonButtonScene(
                        inkGouacheLight,
                        ToonButtonVariant.Filled,
                        128f,
                        "Готово",
                        0f,
                        ToonButtonLook(),
                    ).layers.size
            }
    }
}
