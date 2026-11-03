package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.parity.CardFrames
import io.github.youndie.toon.parity.InkRatio
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.inkGouacheLight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-46: the card list's cascade against the page's `cardStack(T, t, 'appear')`, frame by frame, and
 * the live list against the Core page's cascade and dismiss strips, as pixels within the shapes floor.
 */
@OptIn(ExperimentalTestApi::class)
class ToonCardListTest {
    private val items =
        CardFrames.ITEMS.mapIndexed { i, (title, meta, time) -> ToonCardListItem(i, title, meta, time) }

    @Test
    fun `the cascade's frames are the engine's`() {
        val cells = EngineFrames.cells("CardCascade")
        val worst = cells.maxOf { EngineFrames.compare(it, CardFrames.cascade(it.tokens, it.opts.f("t")!!)) }
        println("card cascade frames: ${cells.size} cells, worst coordinate $worst dp")
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    /** The cell the page's strips draw in: 342 × 288 inside its 60 dp left and 24 dp top padding. */
    private fun ComposeUiTest.capture(tag: String): IntArray {
        val m = onNodeWithTag(tag).captureToImage().toPixelMap()
        return IntArray(m.width * m.height) { m[it % m.width, it / m.width].toArgb() }
    }

    private fun frame(scene: ToonScene): IntArray {
        lateinit var out: IntArray
        runComposeUiTest {
            setContent {
                ToonTheme {
                    val text = rememberToonText()
                    Box(Modifier.size(W.dp, H.dp).background(Toon.colors.stage).testTag("cell")) {
                        Canvas(Modifier.fillMaxSize()) { inDp { translate(LEFT, TOP) { drawScene(scene, text) } } }
                    }
                }
            }
            out = capture("cell")
        }
        return out
    }

    private fun ratio(
        live: IntArray,
        frame: IntArray,
    ): Double = InkRatio.of(W, H, live[0], { x, y -> live[y * W + x] }, { x, y -> frame[y * W + x] })

    @Test
    fun `a list shown with appear cascades as the page's stack, 40 ms a card`() {
        val ages = listOf(48, 96, 144, 224, 304, 384)
        val live = mutableMapOf<Int, IntArray>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                ToonTheme {
                    Box(Modifier.size(W.dp, H.dp).background(Toon.colors.stage).testTag("cell")) {
                        ToonCardList(items, Modifier.offset(LEFT.dp, TOP.dp), appear = true)
                    }
                }
            }
            var age = -FIRST
            while (age < ages.last()) {
                mainClock.advanceTimeByFrame()
                age += FRAME
                if (age in ages) live[age] = capture("cell")
            }
        }
        for (t in ages) {
            val r = ratio(live.getValue(t), frame(CardFrames.cascade(inkGouacheLight, t.toFloat())))
            println("cascade at $t ms: ${"%.2f".format(r * 100)} %")
            assertTrue(r <= InkRatio.shapes, "cascade at $t ms: ${r * 100} % over the floor ${InkRatio.shapes * 100} %")
        }
    }

    /** Shows the three page items, throws the middle one as a screen reader would, and hands each frame's age to [frame]. */
    private fun dismissing(
        reduceMotion: Boolean,
        until: Int,
        heard: MutableList<Int> = mutableListOf(),
        frame: ComposeUiTest.(age: Int) -> Unit,
    ) = runComposeUiTest {
        mainClock.autoAdvance = false
        val shown = mutableStateListOf(*items.toTypedArray())
        setContent {
            ToonTheme(reduceMotion = reduceMotion) {
                Box(Modifier.size(W.dp, H.dp).background(Toon.colors.stage).testTag("cell")) {
                    ToonCardList(shown, Modifier.offset(LEFT.dp, TOP.dp), dismissible = true, onDismiss = {
                        heard += it.key as Int
                        // The caller drops the card as it goes, as the Screens page does.
                        shown.remove(it)
                    })
                }
            }
        }
        mainClock.advanceTimeByFrame()
        onNode(hasText("Bike to the lake", substring = true)).performCustomAccessibilityActionWithLabel("Dismiss")
        var age = -ACTION_FIRST
        while (age < until) {
            mainClock.advanceTimeByFrame()
            age += FRAME
            if (age >= 0) frame(age)
        }
    }

    private fun ComposeUiTest.top(title: String): Float =
        onNode(hasText(title, substring = true)).getBoundsInRoot().top.value - TOP

    @Test
    fun `a dismissed card's cards below close up over it as the page's stack does`() {
        // Before 170 ms the thrown card is the kit's own throw (B-21), not the strip's: it leaves from
        // the finger with no pull-back and smears the placed card about its centre.
        val ages = listOf(176, 240, 288, 352, 432, 512)
        val live = mutableMapOf<Int, IntArray>()
        val heard = mutableListOf<Int>()
        dismissing(false, ages.last(), heard) { age -> if (age in ages) live[age] = capture("cell") }
        assertEquals(listOf(1), heard)
        for (t in ages) {
            val r = ratio(live.getValue(t), frame(CardFrames.dismiss(inkGouacheLight, t.toFloat())))
            println("dismiss at $t ms: ${"%.2f".format(r * 100)} %")
            assertTrue(r <= InkRatio.shapes, "dismiss at $t ms: ${r * 100} % over the floor ${InkRatio.shapes * 100} %")
        }
    }

    @Test
    fun `the card below follows the close-up at every frame and settles in the slot it took`() {
        val tops = mutableMapOf<Int, Float>()
        dismissing(false, 640) { age -> tops[age] = top("Paint the fence") }
        for ((t, y) in tops) {
            val want =
                if (t <
                    520
                ) {
                    2 * CARD_LIST_STEP + toonCardDismiss(t.toFloat()).n * CARD_LIST_STEP
                } else {
                    CARD_LIST_STEP
                }
            assertEquals(want, y, .01f, "the card below at $t ms")
        }
        assertTrue(tops.filterKeys { it < 520 }.values.any { it < CARD_LIST_STEP }, "no overshoot past the slot")
    }

    @Test
    fun `under reduce motion the card fades for 120 ms and the cards below move up at once`() {
        val tops = mutableMapOf<Int, Float>()
        dismissing(true, 320) { age -> tops[age] = top("Paint the fence") }
        for ((t, y) in tops) {
            assertEquals(
                if (t <
                    120
                ) {
                    2 * CARD_LIST_STEP
                } else {
                    CARD_LIST_STEP
                },
                y,
                .01f,
                "the card below at $t ms",
            )
        }
    }

    private companion object {
        const val W = 342 + 60 + 20
        const val H = 288 + 24 + 24
        const val LEFT = 60f
        const val TOP = 24f
        const val FRAME = 16

        /** The appear's clock starts on the first frame after the list is shown. */
        const val FIRST = 16

        /** A screen reader's action recomposes on the next frame, and the throw's clock starts on the one after. */
        const val ACTION_FIRST = 32
    }
}
