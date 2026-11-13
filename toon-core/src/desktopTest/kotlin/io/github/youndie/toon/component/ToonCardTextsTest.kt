package io.github.youndie.toon.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.parity.CardFrames
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.inkGouacheLight
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-83: a card's long time, title and meta stay inside the card, the time on a sticker that holds it. */
@OptIn(ExperimentalTestApi::class)
class ToonCardTextsTest {
    @Test
    fun `the sticker keeps the design's 64 dp for its times and grows for a longer one, up to its share`() {
        // The design's times measure 29 to 43 dp on desktop in the two faces.
        for (t in listOf(0f, 29f, 33f, 40f, 43f, 44f)) assertEquals(64f, toonCardStickerWidth(342f, t), "$t")
        assertEquals(135f, toonCardStickerWidth(358f, 115f), "\"12 ноября, 09:17\" plus 10 each side")
        assertEquals(358f * .45f, toonCardStickerWidth(358f, 400f), "never past 45 % of the card")
        var last = 0f
        for (t in 0..500) {
            val s = toonCardStickerWidth(358f, t.toFloat())
            assertTrue(s >= last, "the sticker narrows as the time widens at $t")
            last = s
        }
    }

    @Test
    fun `the design's cards draw their words with no cut, a long card cuts short of the sticker`() {
        val tokens = inkGouacheLight
        for (i in 0..2) {
            val (title, meta, time) = CardFrames.ITEMS[i]
            val design = toonCardScene(tokens, i, 342f, title, meta, time, ToonCardLook())
            design.labels.forEach {
                assertTrue(
                    it.maxWidth.isFinite(),
                    "card $i: «${it.text}» has no width to keep to",
                )
            }
            assertEquals(
                342f - 64f - 18f - 10f - 22f,
                design.labels[0].maxWidth,
                "the title ends 10 dp short of the sticker",
            )
            assertEquals(64f - 20f, design.labels[2].maxWidth)
        }
        val long = toonCardScene(tokens, 0, 358f, "T", "M", "12 ноября, 09:17", ToonCardLook(), 115f)
        assertEquals(358f - 135f - 18f - 10f - 22f, long.labels[0].maxWidth)
        assertEquals(358f - 135f - 18f - 10f - 22f, long.labels[1].maxWidth)
    }

    @Test
    fun `long words stay off the sticker and the time stays on it`() {
        val c = inkGouacheLight.colors

        fun near(
            a: Color,
            b: Color,
            by: Float = .12f,
        ) = abs(a.red - b.red) <= by && abs(a.green - b.green) <= by && abs(a.blue - b.blue) <= by
        val title = "Review the pull request that moves the board to the new kit"
        val meta = "Зелёный · PR 2, 1 падает · 177 из 184 — 96 % · ждут 1"
        for ((time, stw) in listOf(
            "12 ноября, 09:17" to 135f,
            "Понедельник, 12 ноября 2026 года, 09:17" to 358f * .45f,
        )) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                setContent {
                    ToonTheme {
                        ToonCard(
                            title,
                            Modifier.testTag("card"),
                            meta = meta,
                            time = time,
                            width = 358.dp,
                            tilt = false,
                        )
                    }
                }
                mainClock.advanceTimeBy(3_000)
                val image = onNodeWithTag("card").captureToImage().toPixelMap()
                val stx = (358f - stw - 18f).toInt()
                // The sticker's paper, along a row under its top edge, runs as wide as it was grown.
                val mid = (stx + stw / 2).toInt()
                val row = 27
                var left = mid
                while (near(image[left - 1, row], c.stickers[0], .05f)) left--
                var right = mid
                while (near(image[right + 1, row], c.stickers[0], .05f)) right++
                assertTrue(right - left + 1 >= stw - 12, "$time: the sticker's paper is ${right - left + 1} of $stw")
                // Between the words and the sticker no letter is drawn: not the title, the meta or the time.
                var ink = 0
                var muted = 0
                for (y in 16..70) {
                    for (x in stx - 8..stx - 2) {
                        if (near(image[x, y], c.text)) ink++
                        if (near(image[x, y], c.textMuted, .06f)) muted++
                    }
                }
                assertEquals(0, ink, "$time: the title or the time drawn between the words and the sticker")
                assertEquals(0, muted, "$time: the meta drawn between the words and the sticker")
            }
        }
    }
}
