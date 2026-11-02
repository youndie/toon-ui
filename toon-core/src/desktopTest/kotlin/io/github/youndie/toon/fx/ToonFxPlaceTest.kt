package io.github.youndie.toon.fx

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.theme.ToonTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-48: the host draws each effect at the point and size its event was sent with — root pixels moved
 * into its own dp — and at the FX page's place on the element when the event names none; at densities
 * 1 and 2. The dialog's pop lands on the dialog.
 */
@OptIn(ExperimentalTestApi::class)
class ToonFxPlaceTest {
    /** The host at root (40, 30) dp; an element at host (100, 60)–(180, 108) dp; a point at host (150, 140) dp. */
    private fun at(px: Float) =
        Triple(
            Offset(40f * px, 30f * px),
            Rect(140f * px, 90f * px, 220f * px, 138f * px),
            Offset(190f * px, 170f * px),
        )

    @Test
    fun `an event's own point and size are where the effect goes, at densities 1 and 2`() {
        for (px in listOf(1f, 2f)) {
            val (o, b, p) = at(px)
            val sent = Offset(150f, 140f)

            fun check(
                e: ToonFxEvent,
                size: Float,
            ) {
                val place = toonFxPlace(e, o, px)
                assertEquals(size, place.size, "${e.name} at $px: size")
                for (q in place.points) {
                    assertEquals(sent.x, q.x, 1e-3f, "${e.name} at $px: x")
                    assertEquals(sent.y, q.y, 1e-3f, "${e.name} at $px: y")
                }
                assertTrue(place.points.isNotEmpty(), "${e.name}: no point")
            }
            check(ToonFxEvent.Land("card", b, puffs = listOf(p, p), spread = 36f), 36f)
            check(ToonFxEvent.Hit("card", b, at = p, spread = 30f), 30f)
            check(ToonFxEvent.FieldError(b, at = p, spread = 26f), 26f)
            check(ToonFxEvent.DialogPop(b, at = p, spread = 70f), 70f)
            check(ToonFxEvent.Pop("badge", b, at = p, size = 15f), 15f)
            check(ToonFxEvent.Success(b, at = p, size = 20f), 20f)
        }
    }

    @Test
    fun `without a point an effect sits where the FX page sets it on the element, its offsets in dp`() {
        for (px in listOf(1f, 2f)) {
            val (o, b, _) = at(px)
            assertEquals(
                listOf(Offset(108f, 110f), Offset(172f, 110f)),
                toonFxPlace(ToonFxEvent.Land("card", b), o, px).points,
                "dust at $px",
            )
            assertEquals(46f, toonFxPlace(ToonFxEvent.Land("card", b), o, px).size)
            assertEquals(
                listOf(Offset(140f, 54f)),
                toonFxPlace(ToonFxEvent.Hit("card", b), o, px).points,
                "stars at $px",
            )
            assertEquals(40f, toonFxPlace(ToonFxEvent.DialogPop(b), o, px).size)
            assertEquals(
                listOf(Offset(140f, 82f)),
                toonFxPlace(ToonFxEvent.Pop("badge", b), o, px).points,
                "flash at $px",
            )
            assertEquals(46f, toonFxPlace(ToonFxEvent.Success(b), o, px).size)
        }
    }

    @Test
    fun `the dialog's pop lands on the dialog, not above its layer, at density 2`() =
        runComposeUiTest {
            var emitter: ToonFxEmitter? = null
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(2f)) {
                    ToonTheme {
                        ToonFxHost(Modifier.size(300.dp, 400.dp).testTag("host")) {
                            emitter = Toon.fx
                            Box(Modifier.fillMaxSize().background(Toon.colors.stage))
                        }
                    }
                }
            }
            waitForIdle()
            // The layer is the whole host (600 × 800 px); the dialog's centre is at (300, 500) px.
            runOnIdle {
                checkNotNull(
                    emitter,
                ).emit(ToonFxEvent.DialogPop(Rect(0f, 0f, 600f, 800f), at = Offset(300f, 500f), spread = 70f))
            }
            mainClock.advanceTimeBy(150)
            val image = onNodeWithTag("host").captureToImage().toPixelMap()
            val star =
                io.github.youndie.toon.tokens.inkGouacheLight.colors.warning
                    .toArgb()
            var n = 0
            var sx = 0L
            var sy = 0L
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    val c = image[x, y].toArgb()
                    if ((0..16 step 8).all { s -> abs((c shr s and 0xFF) - (star shr s and 0xFF)) <= 8 }) {
                        n++
                        sx += x
                        sy += y
                    }
                }
            }
            assertTrue(n > 50, "no stars drawn ($n px)")
            val cx = sx.toFloat() / n
            val cy = sy.toFloat() / n
            // Stars on an arc of 70 dp over the dialog's centre, 150 ms in: their centres, in px.
            val stars =
                toonStarsScene(
                    io.github.youndie.toon.tokens.inkGouacheLight.colors,
                    150f / ToonFx.Stars.durationMs,
                    150f,
                    250f,
                    70f,
                )
            val want =
                stars.layers
                    .map { it.dy }
                    .average()
                    .toFloat() * 2
            assertEquals(300f, cx, 12f, "stars centred over the dialog")
            assertEquals(want, cy, 12f, "stars at y $cy px, where an arc of 70 dp over the dialog has them")
        }

    /** The ink a host draws for [event] [ms] in, at density 2, as its box in px: left, top, right, bottom. */
    private fun inked(
        event: ToonFxEvent,
        ms: Long,
    ): IntArray {
        lateinit var box: IntArray
        runComposeUiTest {
            var emitter: ToonFxEmitter? = null
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(2f)) {
                    ToonTheme {
                        ToonFxHost(Modifier.size(300.dp, 400.dp).testTag("host")) {
                            emitter = Toon.fx
                            Box(Modifier.fillMaxSize().background(Toon.colors.stage))
                        }
                    }
                }
            }
            waitForIdle()
            runOnIdle { checkNotNull(emitter).emit(event) }
            mainClock.advanceTimeBy(ms)
            val image = onNodeWithTag("host").captureToImage().toPixelMap()
            val stage = image[0, 0]
            box = intArrayOf(Int.MAX_VALUE, Int.MAX_VALUE, -1, -1)
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    if (image[x, y] == stage) continue
                    box[0] = minOf(box[0], x)
                    box[1] = minOf(box[1], y)
                    box[2] = maxOf(box[2], x)
                    box[3] = maxOf(box[3], y)
                }
            }
        }
        return box
    }

    @Test
    fun `a flash is as big as its event says and where, at density 2`() {
        val small = inked(ToonFxEvent.Pop("badge", Rect(0f, 0f, 600f, 800f), at = Offset(300f, 400f), size = 15f), 90)
        val big = inked(ToonFxEvent.Pop("badge", Rect(0f, 0f, 600f, 800f), at = Offset(300f, 400f), size = 46f), 90)
        assertTrue(small[2] > 0, "no flash drawn")
        assertEquals(300f, (small[0] + small[2]) / 2f, 6f, "flash centred on its point")
        assertEquals(400f, (small[1] + small[3]) / 2f, 6f, "flash centred on its point")
        val ratio = (small[2] - small[0]).toFloat() / (big[2] - big[0])
        assertEquals(15f / 46, ratio, .08f, "a 15 dp flash against a 46 dp one")
    }

    @Test
    fun `dust rises from the event's puffs over its spread, at density 2`() {
        val puffs = listOf(Offset(200f, 600f), Offset(400f, 600f))
        val narrow = inked(ToonFxEvent.Land("card", Rect(0f, 0f, 600f, 800f), puffs = puffs, spread = 20f), 100)
        val wide = inked(ToonFxEvent.Land("card", Rect(0f, 0f, 600f, 800f), puffs = puffs, spread = 60f), 100)
        assertTrue(narrow[2] > 0, "no dust drawn")
        assertEquals(300f, (narrow[0] + narrow[2]) / 2f, 8f, "dust about its two puffs")
        assertTrue(narrow[3] in 560..680, "dust at the puffs' height, its bottom at ${narrow[3]}")
        assertTrue(wide[2] - wide[0] > narrow[2] - narrow[0] + 40, "a 60 dp spread wider than a 20 dp one")
    }
}
