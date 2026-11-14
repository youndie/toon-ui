package io.github.youndie.toon.draw

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.component.ToonListItem
import io.github.youndie.toon.component.ToonListTrailing
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.theme.ToonStage
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-92: the fade is an alpha mask over the scrolled content, as deep at each edge as the content has
 * moved past it, up to its length. The probe is a solid red column over white, so a pixel's green
 * channel reads the mask: red kept whole is green 0, red masked out is white, green 1.
 */
@OptIn(ExperimentalTestApi::class)
class ToonFadingEdgesTest {
    @Test
    fun `at rest the column is faded at the bottom only, over the length`() {
        val shot = column(scroll = 0)
        assertNear(1f, shot.alpha(0), "the top row, at rest")
        assertNear(1f, shot.alpha(H - LENGTH - 1), "the row above the bottom band")
        assertNear(.5f, shot.alpha(H - LENGTH / 2), "the middle of the bottom band")
        assertNear(0f, shot.alpha(H - 1), "the bottom row")
    }

    @Test
    fun `scrolled by a share of the length, the top is faded by that share, at densities 1 and 2`() {
        for (density in listOf(1f, 2f)) {
            for (dp in listOf(5, 10, 20, 30)) {
                val shot = column(scroll = (dp * density).toInt(), density = density)
                val share = dp / LENGTH.toFloat()
                assertNear(1f - share, shot.alpha(0), "the top row scrolled by $dp dp at density $density")
                assertNear(1f, shot.alpha((LENGTH * density).toInt() + 1), "below the top band, $dp dp at $density")
            }
        }
    }

    @Test
    fun `the top fade comes in with the scroll, never by more than the scroll's share`() {
        runComposeUiTest {
            val scroll = ScrollState(0)
            setContent { Probe(scroll) }
            var before = alpha(0)
            for (px in 1..60) {
                runBlocking { scroll.scrollTo(px) }
                waitForIdle()
                val now = alpha(0)
                assertTrue(
                    before - now <= 1f / LENGTH + TOLERANCE,
                    "${before - now} more fade from ${px - 1} px to $px px",
                )
                assertTrue(now <= before + TOLERANCE, "the fade went back at $px px")
                before = now
            }
            assertNear(0f, before, "the top row past the length")
        }
    }

    @Test
    fun `at the end the column is faded at the top only, and 7 px before it by 7 px of the length`() {
        val end = column(scroll = Int.MAX_VALUE)
        assertNear(0f, end.alpha(0), "the top row at the end")
        assertNear(1f, end.alpha(H - 1), "the bottom row at the end")
        runComposeUiTest {
            val scroll = ScrollState(Int.MAX_VALUE)
            setContent { Probe(scroll) }
            runBlocking { scroll.scrollTo(scroll.maxValue - 7) }
            waitForIdle()
            assertEquals(7f, scroll.scrolledPast().second, "past the bottom, 7 px before the end")
            assertNear(1f - 7f / LENGTH, alpha(H - 1), "the bottom row 7 px before the end")
        }
    }

    @Test
    fun `content that fits is drawn as it is, and so is any column with a length of 0`() {
        // The red is one colour, so a column of length 0 draws the same red wherever it is scrolled.
        val plain = shot { Probe(ScrollState(0), length = 0.dp) }
        assertEquals(0, differing(plain, column(scroll = 300, length = 0.dp)), "length 0")
        assertTrue(differing(plain, column(scroll = 300)) > 0, "the same column with a length is faded")
        // Exactly the box's height: it fits, and its red reaches both bands, where a fade would show.
        val fits = shot { Probe(ScrollState(0), content = H.dp) }
        val fitsPlain = shot { Probe(ScrollState(0), length = 0.dp, content = H.dp) }
        assertEquals(0, differing(fits, fitsPlain), "a column that fits")
    }

    @Test
    fun `a length over half the box fades each half, and no size throws`() {
        val shot = column(scroll = 300, length = 400.dp)
        // Each fade is half the box: they meet in the middle, whole there and nowhere else.
        assertNear(1f, shot.alpha(H / 2), "the middle")
        assertNear(.5f, shot.alpha(H / 4), "a quarter down")
        assertNear(.5f, shot.alpha(H * 3 / 4), "three quarters down")
        runComposeUiTest {
            var box by mutableStateOf(0.dp)
            setContent {
                Box(Modifier.size(300.dp).testTag(CELL)) {
                    val scroll = rememberScrollState(Int.MAX_VALUE)
                    Column(Modifier.size(box).toonFadingEdges(scroll, 40.dp, slack = 8.dp).verticalScroll(scroll)) {
                        Box(Modifier.size(50.dp, 2000.dp).background(Color.Red))
                    }
                }
            }
            for (b in listOf(0.dp, 1.dp, 2.dp, 79.dp, 300.dp)) {
                box = b
                waitForIdle()
                onNodeWithTag(CELL).captureToImage()
            }
        }
    }

    @Test
    fun `the slack keeps ink past the column's sides, fades it, and moves nothing`() {
        // The red is 10 dp wider than the column at each side; the scroll does not clip it there.
        fun wide(slack: Dp) = shot { Probe(ScrollState(0), slack = slack, overflow = 10.dp) }
        val cut = wide(0.dp)
        val kept = wide(12.dp)
        for ((side, x) in listOf("start" to SIDE - 5, "end" to W - SIDE + 4)) {
            assertNear(0f, cut.alpha(H / 2, x), "past the $side with no slack: the layer cuts it")
            assertNear(1f, kept.alpha(H / 2, x), "past the $side with 12 dp of slack")
            assertNear(.5f, kept.alpha(H - LENGTH / 2, x), "past the $side, in the bottom band")
        }
        runComposeUiTest {
            var slack by mutableStateOf(0.dp)
            setContent { Probe(ScrollState(0), slack = slack) }
            val bounds = onNodeWithTag(COLUMN).getBoundsInRoot()
            slack = 12.dp
            waitForIdle()
            assertEquals(bounds, onNodeWithTag(COLUMN).getBoundsInRoot(), "the slack moves the box")
        }
    }

    @Test
    fun `a lazy list is faded as a column is, at rest, by a share of the length and at the end`() {
        runComposeUiTest {
            val list = LazyListState()
            setContent { Lazy(list) }
            assertNear(1f, alpha(0), "the top row at rest")
            assertNear(0f, alpha(H - 1), "the bottom row at rest")
            runBlocking { list.scrollBy(10f) }
            waitForIdle()
            assertNear(.75f, alpha(0), "the top row scrolled by a quarter of the length")
            runBlocking { list.scrollToItem(ITEMS - 1) }
            waitForIdle()
            assertNear(0f, alpha(0), "the top row at the end")
            assertNear(1f, alpha(H - 1), "the bottom row at the end")
        }
    }

    @Test
    fun `a lazy list reads the distance past each edge, its content padding and reversed layout included`() {
        runComposeUiTest {
            val list = LazyListState()
            var reverse by mutableStateOf(false)
            setContent { Lazy(list, padding = PaddingValues(top = 16.dp, bottom = 24.dp), reverse = reverse) }
            assertEquals(0f, list.scrolledPast().first, "at rest, past the top")
            assertEquals(Float.POSITIVE_INFINITY, list.scrolledPast().second, "at rest, past the bottom")
            runBlocking { list.scrollBy(13f) }
            waitForIdle()
            assertEquals(13f, list.scrolledPast().first, "13 px down")
            runBlocking { list.scrollToItem(ITEMS - 1) }
            waitForIdle()
            assertEquals(0f, list.scrolledPast().second, "at the end, past the bottom")
            runBlocking { list.scrollBy(-9f) }
            waitForIdle()
            assertEquals(9f, list.scrolledPast().second, "9 px before the end, past the bottom")
            reverse = true
            waitForIdle()
            runBlocking { list.scrollToItem(0) }
            waitForIdle()
            // Reversed, the list's start is at the bottom: at rest only the top is faded.
            assertEquals(Float.POSITIVE_INFINITY to 0f, list.scrolledPast(), "reversed at rest")
            // Read clear of the content paddings, which the list leaves white at its ends.
            assertNear(1f, alpha(H - 25), "reversed at rest: above the bottom padding")
            assertNear(20.5f / LENGTH, alpha(20), "reversed at rest: below the top padding")
        }
    }

    @Test
    fun `over each theme's paper the faded content is the content laid over the stage by the mask`() {
        for (tokens in listOf(inkGouacheLight, inkGouacheNight, brightCel)) {
            fun paper(
                length: Dp,
                rows: Boolean,
            ) = shot {
                ToonTheme(tokens) {
                    ToonStage(Modifier.size(W.dp, H.dp).testTag(CELL)) {
                        val scroll = rememberScrollState()
                        Column(Modifier.fillMaxSize().toonFadingEdges(scroll, length).verticalScroll(scroll)) {
                            if (rows) {
                                repeat(8) {
                                    ToonListItem(
                                        "Reminders",
                                        icon = ToonIcons.Bell,
                                        sub = "At noon",
                                        trailing = ToonListTrailing.Switch,
                                        on = it % 2 == 0,
                                        index = it,
                                        width = W.dp,
                                    )
                                }
                            } else {
                                Box(Modifier.fillMaxWidth().height(1000.dp))
                            }
                        }
                    }
                }
            }
            val faded = paper(LENGTH.dp, rows = true)
            val whole = paper(0.dp, rows = true)
            val stage = paper(LENGTH.dp, rows = false)
            var worst = 0f
            var inked = 0
            var loose = 0
            for (y in H - LENGTH until H) {
                val a = (H - y - .5f) / LENGTH
                for (x in 0 until W) {
                    val w = whole[x, y]
                    val s = stage[x, y]
                    if (w != s) inked++
                    val f = faded[x, y]
                    val off =
                        maxOf(
                            abs(f.red - (a * w.red + (1 - a) * s.red)),
                            abs(f.green - (a * w.green + (1 - a) * s.green)),
                            abs(f.blue - (a * w.blue + (1 - a) * s.blue)),
                        )
                    if (off > 2f / 255) loose++
                    worst = maxOf(worst, off)
                }
            }
            println(
                "${tokens.name}: worst channel off the mask ${worst * 255} / 255, $loose past 2 / 255, over $inked inked pixels",
            )
            assertTrue(inked > 1000, "${tokens.name}: the band has content to fade ($inked pixels)")
            // Three 8-bit roundings stand between the two: the layer's premultiplied store, the DstIn
            // product and the composite over the stage. An edge's antialiased pixel, partly transparent in
            // the layer, can gather them to 4 levels (one Bright Cel pixel does); the rest stay within 2.
            assertTrue(
                worst <= 4f / 255,
                "${tokens.name}: a channel ${worst * 255} / 255 off the content over the stage",
            )
            assertTrue(loose * 1000 <= W * LENGTH, "${tokens.name}: $loose pixels past 2 / 255")
        }
    }

    // A white box, the column in it SIDE dp in from each side, the red content in the column.
    @Composable
    private fun Probe(
        scroll: ScrollState,
        length: Dp = LENGTH.dp,
        slack: Dp = 0.dp,
        content: Dp = 1000.dp,
        overflow: Dp = 0.dp,
    ) {
        Box(Modifier.size(W.dp, H.dp).background(Color.White).testTag(CELL)) {
            Column(
                Modifier
                    .padding(horizontal = SIDE.dp)
                    .fillMaxSize()
                    .testTag(COLUMN)
                    .toonFadingEdges(scroll, length, slack)
                    .verticalScroll(scroll),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .requiredWidth(
                            (W - 2 * SIDE).dp + overflow * 2,
                        ).height(content)
                        .background(Color.Red),
                )
            }
        }
    }

    @Composable
    private fun Lazy(
        list: LazyListState,
        padding: PaddingValues = PaddingValues(),
        reverse: Boolean = false,
    ) {
        Box(Modifier.size(W.dp, H.dp).background(Color.White).testTag(CELL)) {
            LazyColumn(
                Modifier.padding(horizontal = SIDE.dp).fillMaxSize().toonFadingEdges(list, LENGTH.dp),
                list,
                contentPadding = padding,
                reverseLayout = reverse,
            ) {
                items(ITEMS) { Box(Modifier.fillMaxWidth().height(40.dp).background(Color.Red)) }
            }
        }
    }

    private fun column(
        scroll: Int,
        density: Float = 1f,
        length: Dp = LENGTH.dp,
    ): PixelMap = shot(density) { Probe(ScrollState(scroll), length) }

    private fun shot(
        density: Float = 1f,
        content: @Composable () -> Unit,
    ): PixelMap {
        lateinit var shot: PixelMap
        runComposeUiTest {
            setContent { CompositionLocalProvider(LocalDensity provides Density(density)) { content() } }
            shot = onNodeWithTag(CELL).captureToImage().toPixelMap()
        }
        return shot
    }

    /** The mask's alpha at row [y], read in the middle of the column, or at [x]. */
    private fun PixelMap.alpha(
        y: Int,
        x: Int = W / 2,
    ): Float = 1f - this[x, y].green

    private fun ComposeUiTest.alpha(y: Int): Float =
        onNodeWithTag(CELL)
            .captureToImage()
            .toPixelMap()
            .alpha(y)

    private fun assertNear(
        expected: Float,
        actual: Float,
        what: String,
    ) = assertTrue(abs(expected - actual) <= TOLERANCE, "$what: alpha $actual, expected $expected")

    private fun differing(
        a: PixelMap,
        b: PixelMap,
    ): Int {
        var n = 0
        for (y in 0 until a.height) for (x in 0 until a.width) if (a[x, y] != b[x, y]) n++
        return n
    }

    private companion object {
        const val CELL = "cell"
        const val COLUMN = "column"
        const val W = 240
        const val H = 200
        const val SIDE = 40
        const val LENGTH = 40
        const val ITEMS = 30

        // A pixel's half within the gradient, and the 8-bit channel's rounding.
        const val TOLERANCE = .5f / LENGTH + 2f / 255
    }
}
