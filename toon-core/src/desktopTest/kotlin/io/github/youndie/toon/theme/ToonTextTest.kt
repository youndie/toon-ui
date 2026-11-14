package io.github.youndie.toon.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.ToonTypeScale
import io.github.youndie.toon.tokens.ToonTypeSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * B-82: a consumer's own text, in Russian, set with the kit's public type: [ToonText] draws its
 * Cyrillic in the theme's backing face, as the components do, where [textStyle] alone falls back to
 * the platform's font.
 */
@OptIn(ExperimentalTestApi::class)
class ToonTextTest {
    private val themes = listOf(ToonThemes.InkGouacheLight, ToonThemes.InkGouacheNight, ToonThemes.BrightCel)

    /**
     * The ink of [content] drawn alone in a 320 × 80 box in [tokens]' theme, cropped to its bounds: a
     * run of backing face in a paragraph of the design face sits on the paragraph's line, which can
     * stand a pixel off a paragraph set in the backing face alone, so the shapes are compared, not the
     * places.
     */
    private fun ink(
        tokens: ToonTokens,
        content: @Composable () -> Unit,
    ): List<List<Int>> {
        var out = emptyList<List<Int>>()
        runComposeUiTest {
            setContent { ToonTheme(tokens) { Box(Modifier.size(320.dp, 80.dp).testTag("t")) { content() } } }
            waitForIdle()
            val map = onNodeWithTag("t").captureToImage().toPixelMap()
            val inked = { x: Int, y: Int -> map[x, y].alpha > 0f }
            val ys = (0 until map.height).filter { y -> (0 until map.width).any { inked(it, y) } }
            val xs = (0 until map.width).filter { x -> (0 until map.height).any { inked(x, it) } }
            out =
                if (ys.isEmpty()) {
                    emptyList()
                } else {
                    (ys.first()..ys.last()).map { y ->
                        (xs.first()..xs.last()).map { x -> map[x, y].hashCode() }
                    }
                }
        }
        return out
    }

    private fun differing(
        a: List<List<Int>>,
        b: List<List<Int>>,
    ): Int =
        if (a.size != b.size || a.firstOrNull()?.size != b.firstOrNull()?.size) {
            Int.MAX_VALUE
        } else {
            a.indices.sumOf { y -> a[y].indices.count { x -> a[y][x] != b[y][x] } }
        }

    @Composable
    private fun inBackingFace(
        text: String,
        spec: ToonTypeSpec,
    ) {
        // The backing face set by hand, as the components' renderer sets a run of Cyrillic.
        val fonts = Toon.fonts
        val backing = if (spec.display) fonts.displayCyrillic else fonts.bodyCyrillic
        BasicText(
            text,
            style =
                spec.textStyle.copy(
                    fontFamily = backing.family,
                    fontWeight = FontWeight(backing.weight(spec.weight)),
                    fontSize = (spec.size * backing.scale).sp,
                ),
        )
    }

    @Test
    fun `Russian set with ToonText is drawn in the backing face, not the platform's`() {
        for (tokens in themes) {
            for ((word, spec) in listOf("Вид" to ToonTypeScale.Headline, "Готово" to ToonTypeScale.Body)) {
                val kit = ink(tokens) { ToonText(word, spec) }
                val backing = ink(tokens) { inBackingFace(word, spec) }
                val fallback = ink(tokens) { BasicText(word, style = spec.textStyle) }
                assertEquals(0, differing(backing, kit), "${tokens.name}: «$word» in ToonText is not the backing face")
                assertTrue(
                    differing(fallback, kit) > 0,
                    "${tokens.name}: «$word» in ToonText is the platform's fallback",
                )
            }
        }
    }

    @Test
    fun `Latin set with ToonText is the design face, as textStyle draws it`() {
        for (tokens in themes) {
            for (spec in listOf(ToonTypeScale.Headline, ToonTypeScale.Body)) {
                val plain = ink(tokens) { BasicText("Saturday", style = spec.textStyle) }
                val kit = ink(tokens) { ToonText("Saturday", spec) }
                assertTrue(plain.isNotEmpty())
                assertEquals(0, differing(plain, kit), "${tokens.name}: Latin in ToonText moved")
            }
        }
    }

    @Test
    fun `a step of a consumer's own sets its weight between the scale's, in Latin and backed Cyrillic`() {
        // B-91: the desktop design sets words at 15 sp and 500, which the scale has no step for.
        val medium = ToonTypeScale.Body.copy(weight = 500)
        assertEquals(ToonTypeSpec(15f, 22f, 500, display = false), medium)
        assertEquals(ToonTypeScale.Body, ToonTypeScale.Body.copy())
        for (tokens in themes) {
            for (word in listOf("Saturday", "Вид")) {
                val (regular, mid, bold) =
                    listOf(400, 500, 700).map { w ->
                        ink(tokens) { ToonText(word, ToonTypeScale.Body.copy(weight = w)) }.sumOf { row ->
                            row.count {
                                it !=
                                    0
                            }
                        }
                    }
                assertTrue(regular < mid && mid < bold, "${tokens.name} «$word»: inked pixels $regular, $mid, $bold")
                val made = ink(tokens) { ToonText(word, ToonTypeSpec(15f, 22f, 500, display = false)) }
                assertEquals(0, differing(ink(tokens) { ToonText(word, medium) }, made), "${tokens.name} «$word»")
            }
        }
    }

    @Test
    fun `a step out of range is refused where it is made`() {
        for ((size, line, weight) in listOf(
            Triple(0f, 22f, 400),
            Triple(15f, -1f, 400),
            Triple(15f, 22f, 0),
            Triple(15f, 22f, 1001),
        )) {
            assertFailsWith<IllegalArgumentException>("$size / $line, $weight") {
                ToonTypeSpec(size, line, weight, display = false)
            }
        }
        assertFailsWith<IllegalArgumentException> { ToonTypeScale.Body.copy(weight = 1200) }
    }

    @Test
    fun `a mixed line keeps its Latin in the design face and its Cyrillic backed`() {
        val tokens = ToonThemes.InkGouacheLight
        val spec = ToonTypeScale.Body
        var runs = emptyList<String>()
        runComposeUiTest {
            setContent {
                ToonTheme(tokens) {
                    val s = spec.backed("PR 2, 1 падает")
                    runs = s.spanStyles.map { s.text.substring(it.start, it.end) }
                }
            }
            waitForIdle()
        }
        assertEquals(listOf("падает"), runs)
    }
}
