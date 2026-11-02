package io.github.youndie.toon.component

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
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.test.v2.runComposeUiTest
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
}
