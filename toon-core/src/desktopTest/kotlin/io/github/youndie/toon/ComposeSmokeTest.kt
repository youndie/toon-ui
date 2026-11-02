package io.github.youndie.toon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test

/**
 * B-01: the desktop test toolchain composes and lays out off-screen. Every screenshot and parity
 * test after it stands on this; if it fails, nothing visual in the suite means anything.
 */
@OptIn(ExperimentalTestApi::class)
class ComposeSmokeTest {
    @Test
    fun `a composable lays out at the size it asks for`() =
        runComposeUiTest {
            setContent { Box(Modifier.size(48.dp).testTag("box")) }
            onNodeWithTag("box").assertHeightIsEqualTo(48.dp)
        }
}
