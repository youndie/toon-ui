package io.github.youndie.toon.icon

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.theme.ToonTheme
import kotlin.test.Test

/** B-11: the icon takes the size it is given, and says what it is only when it is told to. */
@OptIn(ExperimentalTestApi::class)
class ToonIconTest {
    @Test
    fun `a described icon is an image of its size`() =
        runComposeUiTest {
            setContent {
                ToonTheme { ToonIcon(ToonIcons.Heart, contentDescription = "Saved", size = 48.dp) }
            }
            onNodeWithContentDescription("Saved")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
                .assertWidthIsEqualTo(48.dp)
                .assertHeightIsEqualTo(48.dp)
        }

    @Test
    fun `a decorative icon carries no description`() =
        runComposeUiTest {
            setContent {
                ToonTheme { ToonIcon(ToonIcons.Heart, contentDescription = null, modifier = Modifier.testTag("icon")) }
            }
            onNodeWithTag("icon")
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
                .assertWidthIsEqualTo(24.dp)
        }
}
