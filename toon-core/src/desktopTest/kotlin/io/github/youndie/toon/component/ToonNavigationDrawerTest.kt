package io.github.youndie.toon.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.parity.DrawerFrames
import io.github.youndie.toon.theme.ToonTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B-39: every v1 page navigation drawer cell and its open strip against the engine's frames, the
 * door's skew and shiver included; `drawerOpen` and `drawerClose` at every millisecond, reduce
 * motion, and choosing a row or closing by a release, by the keyboard and by a screen reader.
 */
@OptIn(ExperimentalTestApi::class)
class ToonNavigationDrawerTest {
    @Test
    fun `every navigation drawer cell and open frame is the engine's`() {
        val cells = EngineFrames.cells("NavigationDrawer") + EngineFrames.cells("NavigationDrawerProbe")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                val scene =
                    if (o.getValue("kind").jsonPrimitive.content == "open") {
                        DrawerFrames.open(cell.tokens, o.f("t")!!)
                    } else {
                        DrawerFrames.scene(
                            cell.tokens,
                            ToonDrawerMotion(o.f("sx")!!, 0f, o.f("scrim")!!, o.f("textOp")!!),
                            o["pressed"]?.jsonPrimitive?.intOrNull,
                        )
                    }
                EngineFrames.compare(cell, scene)
            }
        println("drawer frames: ${cells.size} cells, worst coordinate $worst dp")
        assertEquals(27, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the door follows the engine's curves at every millisecond, open and shut`() {
        val curves =
            Json
                .parseToJsonElement(
                    checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText(),
                ).jsonObject
        var worst = 0f
        for ((curve, f) in listOf<Pair<String, (Float) -> ToonDrawerMotion>>(
            "drawerOpen(t)" to ::toonDrawerOpen,
            "drawerClose(t)" to ::toonDrawerClose,
        )) {
            val c = curves.getValue(curve).jsonObject

            fun col(k: String) = c.getValue(k).jsonArray.map { it.jsonPrimitive.content.toFloat() }
            val at = col("at")
            for ((name, get) in listOf<Pair<String, (ToonDrawerMotion) -> Float>>(
                "sx" to { it.sx },
                "rot" to { it.rot },
                "scrim" to { it.scrim },
                "textOp" to { it.textOp },
            )) {
                val v = col(name)
                for (i in at.indices) worst = maxOf(worst, abs(v[i] - get(f(at[i]))))
            }
        }
        println("drawer curves: worst $worst")
        assertTrue(worst <= 1e-5f, "drawer curves off the engine by $worst")
    }

    @Test
    fun `the kit draws a close for 240 ms, and under reduce motion the door fades flat in 120`() {
        assertEquals(ToonDrawerMoment(ToonDrawerMotion()), toonDrawerFrameAt(true, null, false))
        assertNull(toonDrawerFrameAt(false, null, false))
        assertEquals(ToonDrawerMoment(toonDrawerOpen(80f)), toonDrawerFrameAt(true, 80f, false))
        assertEquals(ToonDrawerMoment(toonDrawerClose(240f)), toonDrawerFrameAt(false, 240f, false))
        assertNull(toonDrawerFrameAt(false, 241f, false))
        assertEquals(ToonDrawerMoment(ToonDrawerMotion(), .5f), toonDrawerFrameAt(true, 60f, true))
        assertEquals(ToonDrawerMoment(ToonDrawerMotion(), .5f), toonDrawerFrameAt(false, 60f, true))
        assertNull(toonDrawerFrameAt(false, 120f, true))
        assertEquals(ToonDrawerMoment(ToonDrawerMotion(), 1f), toonDrawerFrameAt(true, 300f, true))
    }

    @Test
    fun `the open it mounts with plays 600 ms and a change either way 400, then the drawer is at rest`() {
        val clock = ToonDrawerClock(true)
        assertEquals(0f, clock.age())
        clock.tick(1000)
        clock.tick(1599)
        assertEquals(599f, clock.age())
        clock.tick(1600)
        assertNull(clock.age())
        for (open in listOf(false, true)) {
            clock.changed(open)
            clock.tick(2000)
            clock.tick(2399)
            assertEquals(399f, clock.age())
            clock.tick(2400)
            assertNull(clock.age())
        }
        assertNull(ToonDrawerClock(false).age())
    }

    @Test
    fun `the door is 300 dp or the screen less 60, and its rows are 56 dp from 112`() {
        assertEquals(300f, toonDrawerWidth(390f))
        assertEquals(260f, toonDrawerWidth(320f))
        assertEquals(0, toonDrawerItemAt(300f, 4, 100f, 112f))
        assertEquals(0, toonDrawerItemAt(300f, 4, 100f, 167.9f))
        assertEquals(1, toonDrawerItemAt(300f, 4, 100f, 168f))
        assertEquals(3, toonDrawerItemAt(300f, 4, 100f, 335.9f))
        assertNull(toonDrawerItemAt(300f, 4, 100f, 111.9f))
        assertNull(toonDrawerItemAt(300f, 4, 100f, 336f))
        assertNull(toonDrawerItemAt(300f, 4, 300f, 150f))
    }

    @Test
    fun `a release on a row chooses it, right of the door closes, and a closed drawer takes nothing`() {
        runComposeUiTest {
            val heard = mutableListOf<String>()
            var open by mutableStateOf(true)
            setContent {
                ToonTheme {
                    Box(Modifier.size(360.dp, 520.dp)) {
                        ToonNavigationDrawer(
                            DrawerFrames.items,
                            Modifier.testTag("drawer"),
                            open = open,
                            onSelect = { heard += "select $it" },
                            onClose = { heard += "close" },
                        )
                    }
                }
            }
            val drawer = onNodeWithTag("drawer")
            drawer.performTouchInput { click(Offset(100f, 240f)) }
            drawer.performTouchInput { click(Offset(330f, 240f)) }
            drawer.performTouchInput { click(Offset(100f, 60f)) }
            assertEquals(listOf("select 2", "close"), heard)
            open = false
            waitForIdle()
            drawer.performTouchInput { click(Offset(330f, 240f)) }
            assertEquals(listOf("select 2", "close"), heard, "closed, it takes no input")
        }
    }

    @Test
    fun `the keyboard walks the rows, chooses and closes, and a screen reader can do both`() {
        runComposeUiTest {
            val heard = mutableListOf<String>()
            setContent {
                ToonTheme {
                    ToonNavigationDrawer(
                        DrawerFrames.items,
                        Modifier.testTag("drawer"),
                        selected = 1,
                        onSelect = { heard += "select $it" },
                        onClose = { heard += "close" },
                    )
                }
            }
            val drawer = onNodeWithTag("drawer")
            drawer.performSemanticsAction(SemanticsActions.RequestFocus)
            drawer.performKeyInput {
                pressKey(Key.Enter)
                pressKey(Key.DirectionDown)
                pressKey(Key.DirectionDown)
                pressKey(Key.DirectionDown)
                pressKey(Key.Spacebar)
                pressKey(Key.DirectionUp)
                pressKey(Key.Enter)
                pressKey(Key.Escape)
            }
            assertEquals(listOf("select 1", "select 3", "select 2", "close"), heard, "focus starts on the selected row")
            val config = drawer.fetchSemanticsNode().config
            assertEquals("Saturday", config.getOrNull(SemanticsProperties.PaneTitle))
            assertEquals("Weekend", config.getOrNull(SemanticsProperties.StateDescription))
            assertEquals(
                listOf("Home", "Weekend", "Saved", "Settings"),
                config.getOrNull(SemanticsActions.CustomActions).orEmpty().map { it.label },
            )
            drawer.performSemanticsAction(SemanticsActions.Dismiss)
            assertEquals("close", heard.last())
        }
    }
}
