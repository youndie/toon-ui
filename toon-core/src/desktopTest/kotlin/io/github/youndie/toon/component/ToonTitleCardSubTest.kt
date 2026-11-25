package io.github.youndie.toon.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.youndie.toon.draw.ToonTextRenderer
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.inkGouacheLight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-99: a sub line wider than the widest plaque wraps, and is set smaller only down to a floor. */
@OptIn(ExperimentalTestApi::class)
class ToonTitleCardSubTest {
    /** The issue's two sentences (#72), an empty state's sub as the board writes it. */
    private val owners = "У владельца youndie нет репозиториев. Добавьте владельцев в настройках."
    private val search = "По запросу «zzz» среди 12 репозиториев ничего нет."

    /** A stand-in measure: 0.5 of the size a letter. */
    private val fake: (String, Float) -> Float = { s, size -> s.length * size * .5f }

    /** The kit's own measure, in the theme's sub face, as the card measures. */
    private fun withTexts(block: (ToonTextRenderer) -> Unit) =
        runComposeUiTest {
            setContent {
                ToonTheme {
                    val texts = rememberToonText()
                    block(texts)
                }
            }
        }

    @Test
    fun `the issue's sentences wrap onto at most three lines at a readable size, each inside the plaque`() {
        withTexts { texts ->
            val measure: (
                String,
                Float,
            ) -> Float = { s, size -> texts.measure(s, toonTitleSubStyle(size)).multiParagraph.width }
            for (card in listOf(340f, 390f)) {
                // The card wraps 8 dp inside the words' room either side, clear of the lower star.
                val room = toonTitleWordRoom(card, empty = true) - 16f
                for (sub in listOf(owners, search)) {
                    val fit = toonTitleSubFit(sub, room, measure)
                    assertTrue(fit.lines.size in 1..3, "$card: ${fit.lines}")
                    assertTrue(fit.size >= 12f, "$card: «$sub» at ${fit.size} sp")
                    fit.lines.forEach { assertTrue(measure(it, fit.size) <= room, "$card: «$it» is wider than $room") }
                    assertEquals(sub.split(' '), fit.lines.flatMap { it.split(' ') }, "every word is kept, in order")
                }
            }
        }
    }

    @Test
    fun `a sub that fits one line is the design's, at 14, and the scene does not change`() {
        val fit = toonTitleSubFit("Your weekend is wide open", 260f, fake)
        assertEquals(listOf("Your weekend is wide open"), fit.lines)
        assertEquals(14f, fit.size)
        val a = toonTitleCardFrameAt(null, false).motion
        val plain = toonTitleCardKitScene(inkGouacheLight, 360f, 520f, true, "Nothing yet", fit.lines[0], a, 206f, 173f)
        val fitted =
            toonTitleCardKitScene(inkGouacheLight, 360f, 520f, true, "Nothing yet", fit.lines[0], a, 206f, 173f, fit)
        assertEquals(
            plain.labels.map {
                listOf(it.text, it.x, it.baseline, it.style.size, it.style.weight)
            },
            fitted.labels.map { listOf(it.text, it.x, it.baseline, it.style.size, it.style.weight) },
        )
        assertEquals(plain.layers.map { it.numbers.toList() }, fitted.layers.map { it.numbers.toList() })
    }

    @Test
    fun `past three lines it is set smaller, and at the floor the last line ends in an ellipsis`() {
        val long = (1..40).joinToString(" ") { "word$it" }
        val fit = toonTitleSubFit(long, 200f, fake)
        assertEquals(12f, fit.size, "set down to the floor")
        assertEquals(3, fit.lines.size)
        assertTrue(fit.lines.last().endsWith("…"), fit.lines.last())
        fit.lines.forEach { assertTrue(fake(it, 12f) <= 200f, "«$it»") }
        // Smaller before cut: ten words that need four lines at 14 and 13 take three at 12, whole.
        val smaller = toonTitleSubFit((1..10).joinToString(" ") { "word$it" }, 140f, fake)
        assertEquals(12f, smaller.size)
        assertEquals(listOf("word1 word2 word3 word4", "word5 word6 word7 word8", "word9 word10"), smaller.lines)
    }

    @Test
    fun `a word wider than the room is cut with an ellipsis, never shrunk past the floor`() {
        val fit = toonTitleSubFit("x".repeat(200), 100f, fake)
        assertEquals(12f, fit.size)
        assertEquals(1, fit.lines.size)
        assertTrue(fit.lines[0].endsWith("…") && fake(fit.lines[0], 12f) <= 100f, fit.lines[0])
        assertEquals(emptyList(), toonTitleSubFit("", 100f, fake).lines, "an empty sub is no line")
    }

    @Test
    fun `the plaque grows by a line for each line past the first, and every line stands inside it`() {
        val a = toonTitleCardFrameAt(null, false).motion
        val fit = ToonTitleSub(listOf("one line", "two line", "three line"), 12f)
        val scene = toonTitleCardKitScene(inkGouacheLight, 340f, 300f, true, "Репозиториев нет", "x", a, 0f, 0f, fit)
        val subs = scene.labels.filter { it.text in fit.lines }
        assertEquals(3, subs.size)
        assertEquals(listOf(18f, 18f), subs.zipWithNext { p, q -> q.baseline - p.baseline }, "18 dp apart")
        assertTrue(subs.all { it.style.size == 12f })
        // The plaque's body is the surface-coloured shape; its lowest point stands below the last line.
        val surface = inkGouacheLight.colors.surface
        val plaque = scene.layers.filter { it.color == surface }
        assertTrue(plaque.isNotEmpty())
        val bottom = plaque.maxOf { l -> l.numbers.filterIndexed { i, _ -> i % 2 == 1 }.maxOf { it + l.dy } }
        assertTrue(
            subs.last().baseline + 8f < bottom,
            "the last line at ${subs.last().baseline}, the plaque ends at $bottom",
        )
        // One line less, the plaque is 18 dp shorter.
        val two =
            toonTitleCardKitScene(
                inkGouacheLight,
                340f,
                300f,
                true,
                "Репозиториев нет",
                "x",
                a,
                0f,
                0f,
                fit.copy(lines = fit.lines.take(2)),
            )
        val shorter =
            two.layers.filter { it.color == surface }.maxOf { l ->
                l.numbers
                    .filterIndexed { i, _ ->
                        i % 2 ==
                            1
                    }.maxOf { it + l.dy }
            }
        assertEquals(18f, bottom - shorter, 1f)
    }
}
