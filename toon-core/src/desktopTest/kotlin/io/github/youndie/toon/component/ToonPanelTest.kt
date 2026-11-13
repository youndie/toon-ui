package io.github.youndie.toon.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.parity.InkRatio
import io.github.youndie.toon.parity.SceneCell
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-89: the panel against the desktop extension's `sheet` as the design's screens call it
 * (`engine-goldens/desktop.json`, group `Panel`), and what the composable does with it: it draws that
 * scene in the size its layout gives it, in dp, under its content, which has the panel's whole box.
 */
@OptIn(ExperimentalTestApi::class)
class ToonPanelTest {
    @Test
    fun `the panel is the extension's sheet layer by layer at every size and seed`() {
        val cells = EngineFrames.cells("Panel")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                EngineFrames.compare(cell, toonPanelScene(cell.tokens, o.f("w")!!, o.f("h")!!, o.f("seed")!!.toInt()))
            }
        println("panel frames: ${cells.size} cells, worst coordinate $worst dp")
        // Four sizes and seeds in three themes.
        assertEquals(12, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the goldens are within the shapes floor of Chrome's rendering, counted over the outline alone`() {
        // InkParityTest counts a cell's differing pixels over every pixel either image inks, and a
        // panel's ink is mostly paper: a whole outline drawn wrong would stay far under the floor. Here
        // the pixels that differ are counted over those that are neither stage nor paper, the outline
        // and its shadow, as the Divider's floor counts them over a line.
        val cells = InkRatio.cells("Panel")
        assertEquals(6, cells.size)
        val ratios =
            cells.associateWith { name ->
                val ref = ImageIO.read(File(SNAPSHOTS, "design/$name"))
                val golden = ImageIO.read(File(SNAPSHOTS, name))
                val stage = ref.getRGB(0, 0)
                val paper = ref.getRGB(ref.width / 2, ref.height / 2)
                val line = { p: Int -> InkRatio.differs(p, stage) && InkRatio.differs(p, paper) }
                var outline = 0
                var differ = 0
                for (y in 0 until ref.height) {
                    for (x in 0 until ref.width) {
                        val p = ref.getRGB(x, y)
                        val q = golden.getRGB(x, y)
                        if (line(p) || line(q)) outline++
                        if (InkRatio.differs(p, q)) differ++
                    }
                }
                differ.toDouble() / outline
            }
        println("panel outline parity: ${ratios.mapValues { "%.2f %%".format(it.value * 100) }}")
        val over = ratios.filterValues { it > InkRatio.shapes }
        assertTrue(over.isEmpty(), "over the shapes floor ${InkRatio.shapes}: $over")
    }

    @Test
    fun `the composable draws the panel's scene in its own size, in every theme and at density 2`() {
        for (tokens in listOf(inkGouacheLight, inkGouacheNight, brightCel)) {
            for ((density, w, h, seed) in listOf(Look(1f, 320, 200, 0), Look(1f, 180, 260, 5), Look(2f, 160, 100, 3))) {
                val panel = capture(density) { PanelOnStage(tokens, w, h) { ToonPanel(it, seed = seed) {} } }
                val scene =
                    capture(density) {
                        Box(Modifier.testTag(CELL)) {
                            SceneCell(
                                tokens,
                                w + PAD + END,
                                h + PAD + END,
                                PAD,
                                PAD,
                            ) { toonPanelScene(tokens, w.toFloat(), h.toFloat(), seed) }
                        }
                    }
                assertEquals(scene.width, panel.width, "${tokens.name} $w × $h at $density: width")
                val differ = differing(panel, scene)
                assertEquals(0, differ, "${tokens.name} $w × $h seed $seed at density $density: pixels off the scene")
            }
        }
    }

    @Test
    fun `the seed and the theme redraw the panel when they change`() {
        var tokens by mutableStateOf(inkGouacheLight)
        var seed by mutableIntStateOf(0)
        runComposeUiTest {
            setContent { PanelOnStage(tokens, 240, 140) { ToonPanel(it, seed = seed) {} } }
            val first = onNodeWithTag(CELL).captureToImage().toPixelMap()
            seed = 4
            waitForIdle()
            val reseeded = onNodeWithTag(CELL).captureToImage().toPixelMap()
            assertTrue(differing(first, reseeded) > 0, "a new seed draws another outline")
            assertEquals(0, differing(reseeded, sceneOf(inkGouacheLight, 240, 140, 4)), "the new seed's scene")
            tokens = inkGouacheNight
            waitForIdle()
            assertEquals(
                0,
                differing(onNodeWithTag(CELL).captureToImage().toPixelMap(), sceneOf(inkGouacheNight, 240, 140, 4)),
                "the night's scene",
            )
        }
    }

    @Test
    fun `the content has the panel's whole box, from its corner, with no padding`() {
        runComposeUiTest {
            setContent {
                ToonTheme {
                    Box(Modifier.size(500.dp, 400.dp)) {
                        ToonPanel(Modifier.offset(30.dp, 20.dp).size(300.dp, 200.dp).testTag("panel")) {
                            Box(Modifier.fillMaxSize().testTag("fill"))
                            Box(Modifier.size(10.dp).testTag("corner"))
                        }
                    }
                }
            }
            val panel = onNodeWithTag("panel").getBoundsInRoot()
            assertEquals(DpRect(30.dp, 20.dp, 330.dp, 220.dp), panel)
            assertEquals(panel, onNodeWithTag("fill").getBoundsInRoot(), "fillMaxSize content fills the panel")
            assertEquals(DpRect(30.dp, 20.dp, 40.dp, 30.dp), onNodeWithTag("corner").getBoundsInRoot(), "no padding")
        }
    }

    @Test
    fun `with no size of its own the panel is its content's size, and draws there`() {
        runComposeUiTest {
            setContent {
                ToonTheme {
                    Box(Modifier.size(300.dp, 200.dp).background(inkGouacheLight.colors.stage).testTag(CELL)) {
                        ToonPanel(
                            Modifier.offset(PAD.dp, PAD.dp).testTag("panel"),
                        ) { Box(Modifier.size(150.dp, 90.dp)) }
                    }
                }
            }
            assertEquals(DpRect(12.dp, 12.dp, 162.dp, 102.dp), onNodeWithTag("panel").getBoundsInRoot())
            val shot = onNodeWithTag(CELL).captureToImage().toPixelMap()
            assertEquals(
                0,
                differing(shot, sceneOf(inkGouacheLight, 150, 90, 0, 300, 200)),
                "the 150 × 90 panel's scene",
            )
        }
    }

    @Test
    fun `the shadow reaches past the box at the end and the bottom, as the design's drawing does`() {
        val shot = capture(1f) { PanelOnStage(inkGouacheLight, 200, 120) { ToonPanel(it) {} } }
        val ink = inkGouacheLight.colors.ink
        // The panel's box is 12..212 × 12..132; its shadow is the theme's 4 dp plus 2.
        val pastEnd = (40 until 120).count { y -> (213 until 218).any { x -> shot[x, y] == ink } }
        val pastBottom = (40 until 180).count { x -> (133 until 138).any { y -> shot[x, y] == ink } }
        assertTrue(pastEnd > 70, "ink past the end on $pastEnd of 80 rows")
        assertTrue(pastBottom > 120, "ink past the bottom on $pastBottom of 140 columns")
    }

    @Test
    fun `no size a layout gives it throws, unbounded width included`() {
        val sizes = listOf(0 to 0, 1 to 1, 2 to 300, 3000 to 20, 40 to 4000)
        runComposeUiTest {
            var size by mutableStateOf(sizes.first())
            setContent {
                ToonTheme {
                    Box(Modifier.size(400.dp, 300.dp).testTag(CELL)) {
                        ToonPanel(Modifier.size(size.first.dp, size.second.dp)) {}
                    }
                }
            }
            for (s in sizes) {
                size = s
                waitForIdle()
                // Drawing runs only with a frame, so take one.
                onNodeWithTag(CELL).captureToImage()
            }
        }
        runComposeUiTest {
            setContent {
                ToonTheme {
                    Row(Modifier.size(400.dp, 300.dp).horizontalScroll(rememberScrollState()).testTag(CELL)) {
                        ToonPanel(Modifier.fillMaxWidth().testTag("panel")) { Box(Modifier.size(1200.dp, 100.dp)) }
                    }
                }
            }
            // The scroll clips the bounds in the root to its 400 dp; the node itself is the content's.
            assertEquals(1200, onNodeWithTag("panel").fetchSemanticsNode().size.width)
            onNodeWithTag(CELL).captureToImage()
        }
    }

    @Test
    fun `a click on the panel goes to what is under it`() {
        runComposeUiTest {
            var clicks = 0
            setContent {
                ToonTheme {
                    Box(Modifier.size(300.dp, 200.dp).clickable { clicks++ }) {
                        ToonPanel(Modifier.size(200.dp, 120.dp).testTag("panel")) {}
                    }
                }
            }
            onNodeWithTag("panel", useUnmergedTree = true).performClick()
            assertEquals(1, clicks)
        }
    }

    private data class Look(
        val density: Float,
        val w: Int,
        val h: Int,
        val seed: Int,
    )

    /** A cell as [SceneCell] lays one out: the panel [w] × [h] at [PAD], on the theme's stage. */
    @Composable
    private fun PanelOnStage(
        tokens: ToonTokens,
        w: Int,
        h: Int,
        panel: @Composable (Modifier) -> Unit,
    ) {
        ToonTheme(tokens) {
            Box(Modifier.size((w + PAD + END).dp, (h + PAD + END).dp).background(tokens.colors.stage).testTag(CELL)) {
                panel(Modifier.offset(PAD.dp, PAD.dp).size(w.dp, h.dp))
            }
        }
    }

    private fun capture(
        density: Float,
        content: @Composable () -> Unit,
    ): PixelMap {
        lateinit var shot: PixelMap
        runComposeUiTest {
            setContent { CompositionLocalProvider(LocalDensity provides Density(density)) { content() } }
            shot = onNodeWithTag(CELL).captureToImage().toPixelMap()
        }
        return shot
    }

    private fun ComposeUiTest.sceneOf(
        tokens: ToonTokens,
        w: Int,
        h: Int,
        seed: Int,
        cellW: Int = w + PAD + END,
        cellH: Int = h + PAD + END,
    ): PixelMap =
        capture(1f) {
            Box(Modifier.testTag(CELL)) {
                SceneCell(tokens, cellW, cellH, PAD, PAD) { toonPanelScene(tokens, w.toFloat(), h.toFloat(), seed) }
            }
        }

    private fun differing(
        a: PixelMap,
        b: PixelMap,
    ): Int {
        assertEquals(b.width to b.height, a.width to a.height, "capture size")
        var n = 0
        for (y in 0 until a.height) for (x in 0 until a.width) if (a[x, y] != b[x, y]) n++
        return n
    }

    private companion object {
        const val CELL = "cell"
        const val PAD = 12
        const val END = 16
        val SNAPSHOTS = File("src/desktopTest/snapshots")
    }
}
