package io.github.youndie.toon.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.down
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.parity.PullFrames
import io.github.youndie.toon.theme.ToonTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B-42: every v1 page pull-to-refresh cell and its pull and release strips against the engine's
 * frames, `pullRelease` at every millisecond, the kit's states and reduce motion, and a pull by a
 * finger and by a screen reader.
 */
@OptIn(ExperimentalTestApi::class)
class ToonPullToRefreshTest {
    @Test
    fun `every pull-to-refresh cell and strip frame is the engine's`() {
        val cells = EngineFrames.cells("PullToRefresh")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                val scene =
                    when (o.getValue("kind").jsonPrimitive.content) {
                        "pull" -> {
                            PullFrames.pull(cell.tokens, o.f("d")!!)
                        }

                        "release" -> {
                            PullFrames.release(cell.tokens, o.f("t")!!)
                        }

                        else -> {
                            val look =
                                ToonPullLook(
                                    d = o.f("d") ?: 0f,
                                    band = o["band"]?.jsonPrimitive?.booleanOrNull ?: true,
                                    blobY = o.f("blobY"),
                                    bsx = o.f("bsx") ?: 1f,
                                    bsy = o.f("bsy") ?: 1f,
                                )
                            o.f("off")?.let { PullFrames.scene(cell.tokens, look, it) }
                                ?: PullFrames.scene(cell.tokens, look)
                        }
                    }
                EngineFrames.compare(cell, scene)
            }
        println("pull frames: ${cells.size} cells, worst coordinate $worst dp")
        assertEquals(36, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the release follows the engine's curve at every millisecond`() {
        val c =
            Json
                .parseToJsonElement(
                    checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText(),
                ).jsonObject
                .getValue("pullRelease(t, 110)")
                .jsonObject

        fun col(k: String) = c.getValue(k).jsonArray.map { it.jsonPrimitive.content.toFloat() }
        val at = col("at")
        val band = c.getValue("band").jsonArray.map { it.jsonPrimitive.booleanOrNull!! }
        var worst = 0f
        for ((name, get) in listOf<Pair<String, (ToonPullLook) -> Float>>(
            "by" to { it.by!! },
            "blobY" to { it.blobY!! },
            "bsx" to { it.bsx },
            "bsy" to { it.bsy },
        )) {
            val v = col(name)
            for (i in at.indices) worst = maxOf(worst, abs(v[i] - get(toonPullRelease(at[i], 110f))))
        }
        for (i in at.indices) assertEquals(band[i], toonPullRelease(at[i], 110f).band, "band at ${at[i]} ms")
        println("pullRelease: worst $worst")
        assertTrue(worst <= 2e-5f, "pullRelease off the engine by $worst")
    }

    @Test
    fun `the kit's states, and under reduce motion a static spinner`() {
        val rest = ToonPullLook(by = 6f, blobY = -40f)
        assertEquals(rest, toonPullFrameAt(false, 0f, false, null, 0f, null, 0f, false))
        assertEquals(
            ToonPullLook(d = 60f, bsy = 1.25f, bsx = .9f),
            toonPullFrameAt(true, 60f, false, null, 0f, null, 500f, false),
        )
        assertEquals(
            toonPullRelease(120f, 90f).copy(ph = 700f),
            toonPullFrameAt(false, 0f, true, 120f, 90f, null, 700f, false),
        )
        // The fling is drawn for 400 ms, then the spinner turns at its spot.
        assertEquals(
            toonPullRelease(399f, 90f).copy(ph = 900f),
            toonPullFrameAt(false, 0f, true, 399f, 90f, null, 900f, false),
        )
        assertEquals(
            ToonPullLook(band = false, blobY = 40f, ph = 900f),
            toonPullFrameAt(false, 0f, true, 400f, 90f, null, 900f, false),
        )
        val s = 1 - 130f / 260
        assertEquals(
            ToonPullLook(band = false, blobY = 40f, ph = 900f, bsx = s, bsy = s),
            toonPullFrameAt(false, 0f, false, null, 0f, 130f, 900f, false),
        )
        assertEquals(rest, toonPullFrameAt(false, 0f, false, null, 0f, 260f, 900f, false))
        // Reduced: the finger still stretches the band; the release lands on a spinner that does not turn.
        assertEquals(
            ToonPullLook(d = 60f, bsy = 1.25f, bsx = .9f),
            toonPullFrameAt(true, 60f, false, null, 0f, null, 500f, true),
        )
        assertEquals(
            ToonPullLook(band = false, blobY = 40f),
            toonPullFrameAt(false, 0f, true, 120f, 90f, null, 700f, true),
        )
        assertEquals(rest, toonPullFrameAt(false, 0f, false, null, 0f, 130f, 900f, true))
        assertEquals(0f, toonPullDistance(-10f))
        assertEquals(40f, toonPullDistance(50f))
        assertEquals(120f, toonPullDistance(400f))
    }

    @Test
    fun `a pull past 40 dp refreshes until the caller is done, a short one does not`() {
        runComposeUiTest {
            // The spinner turns for as long as the refresh lasts: the clock moves only when told to.
            mainClock.autoAdvance = false
            var refreshing by mutableStateOf(false)
            val pulls = mutableListOf<Float>()
            var refreshes = 0
            setContent {
                ToonTheme {
                    ToonPullToRefresh(
                        refreshing,
                        {
                            refreshes++
                            refreshing = true
                        },
                        Modifier.testTag("pull"),
                        onPull = { pulls += it },
                    )
                }
            }
            mainClock.advanceTimeByFrame()
            val pull = onNodeWithTag("pull")
            pull.performTouchInput {
                down(Offset(180f, 10f))
                moveTo(Offset(180f, 50f))
                up()
            }
            assertEquals(0, refreshes, "32 dp is short of 40")
            assertEquals(32f, pulls.last(), 1e-3f)
            pull.performTouchInput {
                down(Offset(180f, 10f))
                moveTo(Offset(180f, 80f))
                up()
            }
            assertEquals(1, refreshes)
            assertEquals(56f, pulls.last(), 1e-3f)
            mainClock.advanceTimeBy(100)
            assertEquals(
                ProgressBarRangeInfo.Indeterminate,
                pull.fetchSemanticsNode().config.getOrNull(SemanticsProperties.ProgressBarRangeInfo),
            )
            pull.performTouchInput {
                down(Offset(180f, 10f))
                moveTo(Offset(180f, 100f))
                up()
            }
            assertEquals(1, refreshes, "refreshing, it takes no pull")
            refreshing = false
            mainClock.advanceTimeBy(400)
            assertNull(pull.fetchSemanticsNode().config.getOrNull(SemanticsProperties.ProgressBarRangeInfo))
            val action =
                pull
                    .fetchSemanticsNode()
                    .config
                    .getOrNull(SemanticsActions.CustomActions)
                    .orEmpty()
                    .single()
            assertEquals("Refresh", action.label)
            action.action()
            assertEquals(2, refreshes, "a screen reader refreshes without the drag")
        }
    }

    @Test
    fun `held content opens a gap that follows the drag, holds under the spinner, and closes without a jump`() {
        // B-64: the strip alone was a band pulled by its tip, with a spinner that shrank where it
        // hung. Over content, the gap above the content is the motion, and it never jumps.
        fun gap(
            dragging: Boolean = false,
            d: Float = 0f,
            refreshing: Boolean = false,
            release: Float? = null,
            done: Float? = null,
            back: Float? = null,
        ) = toonPullGapAt(dragging, d, refreshing, release, 90f, done, back, false)
        assertEquals(0f, gap())
        // Dragged, the gap grows with the band and makes room for the blob as it reaches 40 dp.
        var last = 0f
        for (d in 1..120) {
            val g = gap(dragging = true, d = d.toFloat())
            assertTrue(g > last && g - last < 2f, "dragging $d dp: the gap $last then $g")
            last = g
        }
        // Let go at 90: from the dragged gap to 60 on the release spring, then held.
        assertEquals(gap(dragging = true, d = 90f), gap(refreshing = true, release = 0f), 1e-3f)
        assertEquals(60f, gap(refreshing = true, release = 2000f), .5f)
        assertEquals(60f, gap(refreshing = true))
        // Done: held while the blob shrinks, then closed in 420 ms, never below 0, never in a jump.
        last = gap(refreshing = true)
        for (t in 0..800) {
            val g = gap(done = t.toFloat())
            assertTrue(g >= 0f && abs(g - last) < 2f, "done $t ms: the gap $last then $g")
            last = g
        }
        assertEquals(0f, gap(done = 800f))
        // A short pull springs straight back.
        assertEquals(gap(dragging = true, d = 90f), gap(back = 0f), 1e-3f)
        assertEquals(0f, gap(back = 500f))
        // Under reduce motion the gap is where it ends up, at once.
        assertEquals(60f, toonPullGapAt(false, 0f, true, 0f, 90f, null, null, true))
        assertEquals(0f, toonPullGapAt(false, 0f, false, null, 90f, 0f, null, true))
    }

    @Test
    fun `the content is what a pull moves, and it comes back once the refresh is done`() {
        runComposeUiTest {
            mainClock.autoAdvance = false
            var refreshing by mutableStateOf(false)
            var refreshes = 0
            setContent {
                ToonTheme {
                    ToonPullToRefreshBox(
                        refreshing,
                        {
                            refreshes++
                            refreshing = true
                        },
                        Modifier.size(300.dp, 240.dp).testTag("box"),
                    ) { Box(Modifier.size(300.dp, 100.dp).testTag("content")) }
                }
            }
            mainClock.advanceTimeByFrame()

            fun top() = onNodeWithTag("content").getBoundsInRoot().top.value
            val rest = top()
            val k = density.density
            onNodeWithTag("box").performTouchInput {
                down(Offset(150f * k, 20f * k))
                moveTo(Offset(150f * k, 140f * k))
            }
            mainClock.advanceTimeByFrame()
            assertTrue(top() - rest > 80f, "the content follows the finger: ${top() - rest} dp")
            onNodeWithTag("box").performTouchInput { up() }
            assertEquals(1, refreshes)
            mainClock.advanceTimeBy(1_000)
            assertEquals(60f, top() - rest, 1f, "refreshing, the content waits under the spinner")
            refreshing = false
            mainClock.advanceTimeBy(1_000)
            assertEquals(rest, top(), .5f, "done, it is back")
        }
    }
}
