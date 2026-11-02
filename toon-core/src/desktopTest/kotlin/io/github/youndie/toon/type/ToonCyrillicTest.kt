package io.github.youndie.toon.type

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-35: a label's Cyrillic runs go to the backing face, at its size and weight; the rest stays. */
class ToonCyrillicTest {
    private val backing = ToonBackingFace(FontFamily.Monospace, 1.25f) { it + 200 }

    @Test
    fun `only the Cyrillic runs take the backing face, its size and its weight`() {
        val s = withBacking("Plan: Удалить план?", backing, 16f, 600)
        assertEquals("Plan: Удалить план?", s.text)
        val runs = s.spanStyles.map { s.text.substring(it.start, it.end) }
        // The space between two Cyrillic words, and the question mark, stay in the design's face.
        assertEquals(listOf("Удалить", "план"), runs)
        for (r in s.spanStyles) {
            assertEquals(FontFamily.Monospace, r.item.fontFamily)
            assertEquals(FontWeight(800), r.item.fontWeight)
            assertEquals(20.sp, r.item.fontSize)
        }
    }

    @Test
    fun `a label with no Cyrillic is left as it is`() {
        assertTrue(withBacking("Add plan", backing, 18f, 700).spanStyles.isEmpty())
    }

    @Test
    fun `Cyrillic is the basic block, its supplement and both extensions, and nothing next to them`() {
        for (c in listOf('Ѐ', 'я', 'ё', 'Ԁ', 'ԯ', 'ᲀ', 'ⷠ', 'Ꙁ')) assertTrue(isCyrillic(c), "$c")
        for (c in listOf('A', 'z', ' ', '?', '«', 'Ω', 'ԯ' + 1, 'Ͽ')) assertTrue(!isCyrillic(c), "$c")
    }
}
