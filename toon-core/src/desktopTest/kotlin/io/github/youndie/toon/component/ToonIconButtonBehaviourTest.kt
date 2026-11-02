package io.github.youndie.toon.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.theme.ToonTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/** B-15: the icon button toggles and reports it, a disabled one does nothing; the badge says its count. */
@OptIn(ExperimentalTestApi::class)
class ToonIconButtonBehaviourTest {
    @Test
    fun `a toggle flips and reports on each click`() =
        runComposeUiTest {
            val changes = mutableListOf<Boolean>()
            var clicks = 0
            setContent {
                ToonTheme {
                    ToonIconButton(
                        ToonIcons.Heart,
                        "Save",
                        onClick = { clicks++ },
                        onSelectedChange = { changes += it },
                    )
                }
            }
            val b = onNodeWithContentDescription("Save")
            b.assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
            b.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
            b.performClick()
            b.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            b.performClick()
            assertEquals(listOf(true, false), changes)
            assertEquals(2, clicks)
        }

    @Test
    fun `a disabled icon button does not click`() =
        runComposeUiTest {
            var clicks = 0
            setContent {
                ToonTheme {
                    ToonIconButton(
                        ToonIcons.Edit,
                        "Edit",
                        onClick = { clicks++ },
                        enabled = false,
                        toggle = false,
                    )
                }
            }
            onNodeWithContentDescription("Edit").performClick()
            assertEquals(0, clicks)
        }

    @Test
    fun `the badge says its count, 99+ past 99`() =
        runComposeUiTest {
            var count by mutableIntStateOf(3)
            setContent { ToonTheme { ToonBadge(count, Modifier.testTag("badge")) } }
            onNodeWithTag(
                "badge",
            ).assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Text,
                    listOf(
                        androidx.compose.ui.text
                            .AnnotatedString("3"),
                    ),
                ),
            )
            count = 120
            onNodeWithTag(
                "badge",
            ).assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Text,
                    listOf(
                        androidx.compose.ui.text
                            .AnnotatedString("99+"),
                    ),
                ),
            )
        }
}
