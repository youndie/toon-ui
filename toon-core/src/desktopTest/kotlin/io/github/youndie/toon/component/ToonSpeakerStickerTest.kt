package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
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
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.wobbleAlongNormals
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.ToonTokens
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

    /** The circle's outline under [seedOf]'s wobble: what a shape would be if only its wobble told it apart. */
    private fun circleWobbledAs(
        tokens: ToonTokens,
        seedOf: Int,
    ): ToonScene {
        val k = 24f / 32
        val pts =
            wobbleAlongNormals(
                toonSpeakerBlobPoints(0, 12f, 12f, 11f),
                tokens.shape.wobble * .4f * k,
                toonSpeakerSeed(tokens, seedOf),
            )
        return ToonScene(
            listOf(smoothLayer(pts, tokens.colors.ink, dx = 2 * k, dy = 2 * k), smoothLayer(pts, tokens.colors.ink)),
            emptyList(),
        )
    }

    /** Where the image is covered, at half coverage, so antialiasing along one edge is not a difference. */
    private fun BufferedImage.outlineDiffers(o: BufferedImage): Int {
        var n = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (((getRGB(x, y) ushr 24) >= 128) !=
                    ((o.getRGB(x, y) ushr 24) >= 128)
                ) {
                    n++
                }
            }
        }
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
                                    Canvas(
                                        Modifier.size(24.dp).testTag("w$t-$shape"),
                                    ) { inDp { drawScene(circleWobbledAs(tokens, shape)) } }
                                }
                            }
                        }
                    }
                }
            }
            themes.indices.forEach { t ->
                fun shot(tag: String) = onNodeWithTag(tag).captureToImage().toAwtImage()
                val looks = (0 until 8).associateWith { s -> (0 until 3).map { c -> shot("s$t-$s-$c") } }
                val wobbled = (0 until 8).map { shot("w$t-$it") }
                // The control: one circle under the eight shapes' wobbles. What the wobble alone changes
                // in an outline, a shape must exceed to be told apart by its shape.
                val wobble =
                    (0 until 8)
                        .flatMap { a ->
                            (a + 1 until 8).map { b -> wobbled[a].outlineDiffers(wobbled[b]) }
                        }.max()
                val shapes =
                    (0 until 8)
                        .flatMap { a ->
                            (a + 1 until 8).map { b ->
                                Triple(a, b, looks.getValue(a)[0].outlineDiffers(looks.getValue(b)[0]))
                            }
                        }.minBy { it.third }
                // The accent: the colour of the circle's body above its letter, below the rim's crescent.
                val accents =
                    (0 until 3)
                        .flatMap { a ->
                            (a + 1 until 3).map { b ->
                                val p = looks.getValue(0)[a].getRGB(12, 5)
                                val q = looks.getValue(0)[b].getRGB(12, 5)
                                Triple(
                                    a,
                                    b,
                                    listOf(16, 8, 0).sumOf { s ->
                                        ((p shr s and 0xFF) - (q shr s and 0xFF)).let {
                                            it *
                                                it
                                        }
                                    },
                                )
                            }
                        }.minBy { it.third }
                println(
                    "theme $t: wobble alone changes up to $wobble outline px; closest shapes ${shapes.first} and " +
                        "${shapes.second}, ${shapes.third} px; closest accents ${accents.first} and ${accents.second}, distance² ${accents.third}",
                )
                assertTrue(
                    shapes.third > wobble,
                    "theme $t: shapes ${shapes.first} and ${shapes.second} differ no more than the wobble does",
                )
                // Three hues, not three shades of one: at least 60 apart in RGB, a seventh of black to white.
                assertTrue(
                    accents.third >= MIN_ACCENT_DISTANCE_SQ,
                    "theme $t: accents ${accents.first} and ${accents.second} are too close",
                )
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
