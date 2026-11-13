package io.github.youndie.toon.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * B-70: the speaker sticker against the desktop extension's `sticker` (`engine-goldens/desktop.json`,
 * group `SpeakerSticker`), and the item's other criterion — the 24 looks a named speaker can take are
 * told apart at the smallest size.
 */
@OptIn(ExperimentalTestApi::class)
class ToonSpeakerStickerTest {
    @Test
    fun `every kind at every size, and every shape, is the extension's frame layer by layer`() {
        val cells = EngineFrames.cells("SpeakerSticker")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                EngineFrames.compare(
                    cell,
                    toonSpeakerStickerScene(
                        cell.tokens,
                        o
                            .getValue("size")
                            .jsonPrimitive.int
                            .toFloat(),
                        ToonSpeakerKind.valueOf(
                            o
                                .getValue("kind")
                                .jsonPrimitive.content
                                .replaceFirstChar { it.uppercase() },
                        ),
                        o.getValue("shape").jsonPrimitive.int,
                        o.getValue("color").jsonPrimitive.int,
                        o.getValue("letter").jsonPrimitive.content,
                    ),
                )
            }
        println("speaker sticker frames: ${cells.size} cells, worst coordinate $worst dp")
        // Three kinds at three sizes and the seven other shapes, in three themes.
        assertEquals(48, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `a letter of two characters is set smaller, and shape and colour wrap`() {
        val one = toonSpeakerStickerScene(inkGouacheLight, 32f, ToonSpeakerKind.Named, 0, 0, "М")
        val two = toonSpeakerStickerScene(inkGouacheLight, 32f, ToonSpeakerKind.Named, 0, 0, "МК")
        assertEquals(
            16f,
            one.labels
                .single()
                .style.size,
        )
        assertEquals(
            12f,
            two.labels
                .single()
                .style.size,
        )
        val wrapped = toonSpeakerStickerScene(inkGouacheLight, 32f, ToonSpeakerKind.Named, 8 + 3, -2, "М")
        val plain = toonSpeakerStickerScene(inkGouacheLight, 32f, ToonSpeakerKind.Named, 3, 1, "М")
        assertEquals(plain.layers.map { it.numbers.toList() }, wrapped.layers.map { it.numbers.toList() })
        assertEquals(plain.layers.map { it.color }, wrapped.layers.map { it.color })
    }

    private fun BufferedImage.differs(o: BufferedImage): Int {
        var n = 0
        for (y in 0 until height) for (x in 0 until width) if (getRGB(x, y) != o.getRGB(x, y)) n++
        return n
    }

    @Test
    fun `the 24 looks of a named speaker are told apart at 24 dp in every theme`() =
        runComposeUiTest {
            val themes = listOf(inkGouacheLight, inkGouacheNight, brightCel)
            setContent {
                Column {
                    themes.forEachIndexed { t, tokens ->
                        ToonTheme(tokens) {
                            Row {
                                for (shape in 0 until 8) {
                                    for (color in 0 until 3) {
                                        ToonSpeakerSticker(
                                            "М",
                                            Modifier.testTag("s$t-$shape-$color"),
                                            shape = shape,
                                            color = color,
                                            size = ToonSpeakerSize.Small,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            themes.indices.forEach { t ->
                val shots =
                    (0 until 8).flatMap { shape ->
                        (0 until 3).map { color ->
                            "$shape/$color" to onNodeWithTag("s$t-$shape-$color").captureToImage().toAwtImage()
                        }
                    }
                val looks = shots.toMap()

                // The outline: where the sticker covers the canvas, at half coverage, so antialiasing
                // along one edge is not a difference. Each shape against each other, in one accent.
                fun BufferedImage.covered(
                    x: Int,
                    y: Int,
                ) = (getRGB(x, y) ushr 24) >= 128
                var shapes = Int.MAX_VALUE to ""
                for (a in 0 until 8) {
                    for (b in a + 1 until 8) {
                        val p = looks.getValue("$a/0")
                        val q = looks.getValue("$b/0")
                        var n = 0
                        for (y in 0 until p.height) {
                            for (x in 0 until p.width) {
                                if (p.covered(x, y) !=
                                    q.covered(x, y)
                                ) {
                                    n++
                                }
                            }
                        }
                        if (n < shapes.first) shapes = n to "$a and $b"
                    }
                }
                // The accent: the colour of the circle's body above its letter, below the rim's crescent.
                var accents = Int.MAX_VALUE to ""
                for (a in 0 until 3) {
                    for (b in a + 1 until 3) {
                        val p = looks.getValue("0/$a").getRGB(12, 5)
                        val q = looks.getValue("0/$b").getRGB(12, 5)
                        val d =
                            listOf(16, 8, 0).sumOf { s ->
                                val e = (p shr s and 0xFF) - (q shr s and 0xFF)
                                e * e
                            }
                        if (d < accents.first) accents = d to "$a and $b"
                    }
                }
                println(
                    "theme $t: closest outlines, shapes ${shapes.second}, ${shapes.first} px; closest accents ${accents.second}, distance² ${accents.first}",
                )
                // No two shapes share an outline. The closest pair is printed: the circle and the
                // four-petal shape differ in about 20 pixels at this size (B-70's findings).
                assertTrue(shapes.first > 0, "theme $t: shapes ${shapes.second} have one outline")
                // Three hues, not three shades of one: at least 60 apart in RGB, a seventh of black to white.
                assertTrue(accents.first >= MIN_ACCENT_DISTANCE_SQ, "theme $t: accents ${accents.second} are too close")
            }
        }

    private companion object {
        const val MIN_ACCENT_DISTANCE_SQ = 60 * 60
    }

    @Test
    fun `a sticker is its size, and says nothing unless described`() =
        runComposeUiTest {
            setContent {
                ToonTheme {
                    Row {
                        ToonSpeakerSticker("М", Modifier.testTag("plain"), size = ToonSpeakerSize.Large)
                        ToonSpeakerSticker(
                            "Я",
                            Modifier.testTag("me"),
                            kind = ToonSpeakerKind.Me,
                            contentDescription = "Me",
                        )
                    }
                }
            }
            onNodeWithTag("plain").assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
            onNodeWithTag("me").assertWidthIsEqualTo(32.dp)
            val described = SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription)
            assertFalse(
                described.matches(onNodeWithTag("plain").fetchSemanticsNode()),
                "an undescribed sticker is decoration",
            )
            assertTrue(described.matches(onNodeWithTag("me").fetchSemanticsNode()))
        }
}
