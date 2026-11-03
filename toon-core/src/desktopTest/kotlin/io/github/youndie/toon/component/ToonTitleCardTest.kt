package io.github.youndie.toon.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.parity.TitleFrames
import io.github.youndie.toon.theme.ToonTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
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
            ToonTitleCardMoment(toonSplashAt(240f).copy(phase = toonRingPhase(240f, false))),
            toonTitleCardFrameAt(240f, false),
        )
        assertEquals(ToonTitleCardMoment(rest, .5f), toonTitleCardFrameAt(60f, true))
        assertEquals(ToonTitleCardMoment(rest, 1f), toonTitleCardFrameAt(300f, true))
        assertEquals(0f, toonTitleCardFrameAt(60f, true).motion.stars)
        val clock = ToonTitleCardClock()
        assertEquals(0f, clock.age())
        clock.tick(1000)
        clock.tick(1899)
        assertEquals(899f, clock.age())
        clock.tick(1900)
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
    fun `the rings ease one period into where they rest, and looping keep flowing without a jump`() {
        // B-55: the engine's rings run at 0.04 dp a ms and then jump 14 dp to rest.
        fun ring(p: Float) = ((p % 34) + 34) % 34

        fun step(
            a: Float,
            b: Float,
        ) = minOf(abs(ring(a) - ring(b)), 34 - abs(ring(a) - ring(b)))
        val rest = ring(toonSplashAt(1e6f).phase)
        assertEquals(rest, ring(toonRingPhase(0f, false)), 1e-3f, "the entrance starts a whole period behind")
        assertEquals(rest, ring(toonTitleCardFrameAt(899.9f, false).motion.phase), 1e-2f, "and ends where it rests")
        for (loop in listOf(false, true)) {
            var last = toonRingPhase(0f, loop)
            for (t in 1..6000) {
                val p = toonRingPhase(t.toFloat(), loop)
                assertTrue(step(last, p) < .2f, "loop=$loop: the rings jump ${step(last, p)} dp at $t ms")
                last = p
            }
        }
        assertEquals(rest, ring(toonRingPhase(3000f, false)), "without the loop they stay at rest")
        assertTrue(step(rest, toonRingPhase(900f + 800f, true)) > 10f, "looping, they move")
        assertEquals(rest, ring(toonTitleCardFrameAt(3000f, true, loop = true).motion.phase), 1e-2f)
        // Reduce motion holds them still.
        assertEquals(toonSplashAt(1e6f), toonTitleCardFrameAt(3000f, true, loop = true).motion)
    }
}
