package io.github.youndie.toon.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.parity.TitleFrames
import io.github.youndie.toon.theme.ToonTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B-43: the v1 page's splash and empty state and its enter strip against the engine's frames, rings,
 * plaque, stars and text; `splashAt` at every millisecond; the kit's rest and reduce motion; and what a
 * screen reader hears.
 */
@OptIn(ExperimentalTestApi::class)
class ToonTitleCardTest {
    /** `tw` without a DOM: length × 18 × 0.55, the width the goldens were drawn with. */
    private val noDom: (String) -> Float = { it.length * 18 * .55f }

    @Test
    fun `every title card cell and enter frame is the engine's`() {
        val cells = EngineFrames.cells("TitleCard") + EngineFrames.cells("TitleCardProbe")
        val worst =
            cells.maxOf { cell ->
                val scene =
                    when (
                        cell.opts
                            .getValue("kind")
                            .jsonPrimitive.content
                    ) {
                        "splash" -> {
                            TitleFrames.splash(cell.tokens)
                        }

                        "empty" -> {
                            TitleFrames.empty(cell.tokens, noDom)
                        }

                        "noSub" -> {
                            toonTitleCardScene(cell.tokens, 360f, 520f, ToonTitleLook())
                        }

                        "kitEmpty" -> {
                            toonTitleCardKitScene(
                                cell.tokens,
                                360f,
                                420f,
                                true,
                                "Nothing yet",
                                "Your weekend is wide open",
                                toonSplashAt(cell.opts.f("t")!!),
                            )
                        }

                        else -> {
                            TitleFrames.enter(cell.tokens, cell.opts.f("t")!!)
                        }
                    }
                EngineFrames.compare(cell, scene)
            }
        println("title card frames: ${cells.size} cells, worst coordinate $worst dp")
        assertEquals(27, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the entrance follows the engine's curve at every millisecond`() {
        val c =
            Json
                .parseToJsonElement(
                    checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText(),
                ).jsonObject
                .getValue("splashAt(t)")
                .jsonObject

        fun col(k: String) = c.getValue(k).jsonArray.map { it.jsonPrimitive.content.toFloat() }
        val at = col("at")
        var worst = 0f
        for ((name, get) in listOf<Pair<String, (ToonSplashMotion) -> Float>>(
            "s" to { it.s },
            "phase" to { it.phase },
            "textOp" to { it.textOp },
            "stars" to { it.stars },
        )) {
            val v = col(name)
            for (i in at.indices) worst = maxOf(worst, abs(v[i] - get(toonSplashAt(at[i]))))
        }
        println("splashAt: worst $worst")
        assertTrue(worst <= 1e-5f, "splashAt off the engine by $worst")
    }

    @Test
    fun `the entrance plays 900 ms, rests where the kit's rings stop, and under reduce motion fades`() {
        val rest = toonSplashAt(1e6f)
        // A long time after, the rings stand 16 dp along their 34: 40 000 dp of travel.
        assertEquals(16f, rest.phase % 34)
        assertEquals(ToonTitleCardMoment(rest), toonTitleCardFrameAt(null, false))
        // The live card's rings are its own (B-55); the rest of the entrance is the engine's.
        assertEquals(
            ToonTitleCardMoment(toonSplashAt(240f).copy(phase = toonRings(240f, false).phase)),
            toonTitleCardFrameAt(240f, false),
        )
        assertEquals(ToonTitleCardMoment(rest, .5f), toonTitleCardFrameAt(60f, true))
        assertEquals(ToonTitleCardMoment(rest, 1f), toonTitleCardFrameAt(300f, true))
        assertEquals(0f, toonTitleCardFrameAt(60f, true).motion.stars)
        val clock = ToonTitleCardClock()
        assertEquals(0f, clock.age())
        clock.tick(1000)
        clock.tick(2399)
        // The plaque lands at 900 ms; the rings ease on to 1400 (B-55).
        assertEquals(1399f, clock.age())
        clock.tick(2400)
        assertNull(clock.age())
        clock.restart()
        clock.tick(3000)
        assertEquals(0f, clock.age())
    }

    @Test
    fun `the empty state is the card smaller and higher, the stars where the splash has them`() {
        val tokens = io.github.youndie.toon.tokens.inkGouacheLight
        val a = toonSplashAt(300f)
        val kit = toonTitleCardKitScene(tokens, 360f, 420f, true, "Nothing yet", "Your weekend is wide open", a)
        val own =
            toonTitleCardScene(
                tokens,
                360f,
                420f,
                ToonTitleLook(
                    "Nothing yet",
                    "Your weekend is wide open",
                    168f,
                    260f,
                    110f,
                    34f,
                    0f,
                    a.s,
                    a.phase,
                    a.textOp,
                ),
            )
        assertEquals(own.layers.size + 6, kit.layers.size, "three stars, two layers each")
        assertEquals(
            34f,
            kit.labels
                .first()
                .style.size,
        )
    }

    @Test
    fun `a screen reader hears a heading with the card's words, and a click plays it again`() {
        runComposeUiTest {
            var clicks = 0
            setContent {
                ToonTheme {
                    ToonTitleCard(
                        Modifier.testTag("card"),
                        variant = ToonTitleCardVariant.Empty,
                        onClick = { clicks++ },
                    )
                }
            }
            val card = onNodeWithTag("card")
            val config = card.fetchSemanticsNode().config
            assertTrue(config.contains(SemanticsProperties.Heading))
            assertEquals(
                listOf("Nothing yet. Your weekend is wide open"),
                config.getOrNull(SemanticsProperties.ContentDescription),
            )
            assertTrue(config.contains(SemanticsActions.OnClick))
            card.performClick()
            assertEquals(1, clicks)
        }
    }

    @Test
    fun `the rings never jump, ease two periods into where they rest, and looping swell and surge`() {
        // B-55: the engine's rings run at 0.04 dp a ms and then jump 14 dp to rest; and a ring's
        // colour taken from the phase modulo one period swapped every colour when it wrapped.
        val base = hypot(360f, 520f) * .62f
        val samples = (0 until 700).map { 30f + it * .5f }

        // The colour at each distance from the centre: the innermost ring that reaches it.
        fun picture(m: ToonSplashMotion): List<Boolean?> {
            val rings = toonRingRadii(base, m.phase, m.warp, m.swirl)
            return samples.map { d -> rings.lastOrNull { it.first >= d }?.second }
        }

        fun changed(
            a: List<Boolean?>,
            b: List<Boolean?>,
        ) = a.indices.count { a[it] != b[it] }
        val rest = picture(toonSplashAt(1e6f))
        assertEquals(
            rest,
            picture(toonTitleCardFrameAt(0f, false).motion),
            "the entrance starts on the resting picture",
        )
        assertEquals(rest, picture(toonTitleCardFrameAt(null, false).motion))
        for (loop in listOf(false, true)) {
            var last = picture(toonTitleCardFrameAt(0f, false, loop).motion)
            for (t in 1..12000) {
                val now = picture(toonTitleCardFrameAt(t.toFloat(), false, loop).motion)
                // An edge moving under a sample flips it; a whole-picture swap flips hundreds.
                assertTrue(
                    changed(last, now) <= 40,
                    "loop=$loop: ${changed(last, now)} of ${samples.size} samples flip at $t ms",
                )
                last = now
            }
        }
        assertEquals(rest, picture(toonTitleCardFrameAt(1400f, false).motion), "without the loop they rest")
        // Looping: the speed surges and ebbs, never backwards, and the widths swell and shrink.
        val speeds = (1500..12000 step 10).map { toonRings(it + 1f, true).phase - toonRings(it.toFloat(), true).phase }
        assertTrue(speeds.all { it > 0 }, "the rings never flow backwards")
        assertTrue(speeds.max() / speeds.min() > 3, "the speed surges: ${speeds.min()}..${speeds.max()} dp a ms")
        val widths =
            (3000..9000 step 500).flatMap { t ->
                val m = toonRings(t.toFloat(), true)
                toonRingRadii(base, m.phase, m.warp, m.swirl).zipWithNext { a, b -> a.first - b.first }
            }
        assertTrue(widths.all { it > 0 }, "the rings stay in order")
        assertTrue(widths.max() / widths.min() > 2.5, "the widths swell: ${widths.min()}..${widths.max()} dp")
        // Reduce motion holds them still.
        assertEquals(toonSplashAt(1e6f), toonTitleCardFrameAt(3000f, true, loop = true).motion)
    }

    @Test
    fun `the plaque grows to hold long words, never past the card, and the design's words keep its plaque`() {
        for (empty in listOf(false, true)) {
            val least = if (empty) 260f else 280f
            for (w in listOf(240f, 280f, 360f, 600f)) {
                var last = 0f
                for (i in 0..120) {
                    val words = i * 10f
                    for ((tw, sw) in listOf(words to 0f, 0f to words, words to words * .7f)) {
                        val fit = toonTitlePlaqueFit(w, empty, tw, sw)
                        assertTrue(fit.pw >= least, "$w $tw: plaque ${fit.pw} under the design's $least")
                        assertTrue(fit.pw <= max(least, w - 32f), "$w $tw: plaque ${fit.pw} past the card")
                        assertTrue(tw * fit.titleScale <= fit.pw - 48f + .01f, "$w $tw: title wider than its room")
                        assertTrue(sw * fit.subScale <= fit.pw - 48f + .01f, "$w $sw: sub wider than its room")
                        assertTrue(fit.titleScale in 0f..1f && fit.subScale in 0f..1f)
                    }
                    val pw = toonTitlePlaqueFit(w, empty, words, 0f).pw
                    assertTrue(pw >= last, "$w: the plaque narrows as the title widens, $last then $pw")
                    last = pw
                }
            }
        }
        // The kit's scene draws the fit: a 600-wide title on a 360 card is set smaller, the sub too.
        val fit = toonTitlePlaqueFit(360f, false, 600f, 400f)
        val kit =
            toonTitleCardKitScene(
                io.github.youndie.toon.tokens.inkGouacheLight,
                360f,
                520f,
                false,
                "T",
                "S",
                toonSplashAt(1e6f),
                600f,
                400f,
            )
        assertEquals(44f * fit.titleScale, kit.labels[0].style.size)
        assertEquals(14f * fit.subScale, kit.labels[1].style.size)
        // The design's words, measured on desktop, keep the design's plaque and sizes.
        for ((empty, tw, sw) in listOf(Triple(false, 216f, 225f), Triple(true, 206f, 173f))) {
            val fit = toonTitlePlaqueFit(360f, empty, tw, sw)
            assertEquals(if (empty) 260f else 280f, fit.pw)
            assertEquals(1f, fit.titleScale)
            assertEquals(1f, fit.subScale)
        }
    }

    @Test
    fun `a title wider than the design's plaque is drawn inside the plaque`() {
        val c =
            io.github.youndie.toon.tokens.inkGouacheLight.colors

        fun near(
            a: Color,
            b: Color,
            by: Float,
        ) = abs(a.red - b.red) <= by && abs(a.green - b.green) <= by && abs(a.blue - b.blue) <= by
        for ((width, title, grows) in listOf(
            Triple(360, "ПЯТИЛЕТКА", true),
            Triple(280, "ПЯТИЛЕТКА", false),
            Triple(360, "The quick brown fox jumps over lazy dogs", false),
        )) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                setContent {
                    ToonTheme {
                        ToonTitleCard(Modifier.testTag("card"), title = title, sub = "", width = width.dp)
                    }
                }
                mainClock.advanceTimeBy(3_000)
                val image = onNodeWithTag("card").captureToImage().toPixelMap()
                // The plaque at rest stands 158 to 278 (120 tall about 0.42 of 520); the row 247 is under
                // the title's descenders, above the dashed frame, and over the bottom star.
                val row = 247
                val mid = width / 2
                var left = mid
                while (left > 0 && near(image[left - 1, row], c.surface, .05f)) left--
                var right = mid
                while (right < width - 1 && near(image[right + 1, row], c.surface, .05f)) right++
                val plaque = right - left + 1
                if (grows) assertTrue(plaque > 299 + 10, "$title at $width: the plaque is $plaque wide")

                // The text is the ink of the plaque's outline, so a title past the plaque shows as more
                // ink before the plaque's paper begins on a title row than on the row under the title.
                fun inkBeforePaper(
                    y: Int,
                    xs: IntProgression,
                ): Int {
                    var ink = 0
                    for (x in xs) {
                        if (near(image[x, y], c.surface, .05f)) return ink
                        if (near(image[x, y], c.text, .2f)) ink++
                    }
                    return ink
                }
                for (xs in listOf(0 until width, (width - 1) downTo 0)) {
                    val edge = inkBeforePaper(row, xs)
                    val worst = (175..240).maxOf { inkBeforePaper(it, xs) }
                    assertTrue(
                        worst <= edge + 3,
                        "$title at $width: $worst ink before the paper on a title row, $edge at the edge",
                    )
                }
            }
        }
    }
}
