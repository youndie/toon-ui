package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonTextRenderer
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.icon.ToonIconGlyph
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.theme.ToonTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B-26: every MVP page top app bar cell and its sway strip against the engine's frames, `sway` at
 * every millisecond, the kick's alternation and reduce motion, and the kit's tap targets.
 */
@OptIn(ExperimentalTestApi::class)
class ToonTopAppBarTest {
    private fun key(
        o: JsonObject,
        k: String,
    ): ToonTopBarKey? {
        val v = o[k]
        if (v == null || v is JsonNull) return null
        return if (v.jsonPrimitive.content == "nav") ToonTopBarKey.Nav else ToonTopBarKey.Action(v.jsonPrimitive.int)
    }

    @Test
    fun `every top app bar cell and sway frame is the engine's`() {
        val cells = EngineFrames.cells("TopAppBar")
        val worst =
            cells.maxOf { cell ->
                val o = cell.opts
                val center = o["variant"]?.jsonPrimitive?.content == "center"
                val look =
                    ToonTopBarLook(
                        o["scrolled"]?.jsonPrimitive?.booleanOrNull ?: false,
                        key(o, "pressed"),
                        key(o, "focus"),
                    )
                val rot =
                    if (o.getValue("kind").jsonPrimitive.content ==
                        "sway"
                    ) {
                        toonSway(o.f("t")!!, 28f)
                    } else {
                        o.f("rot")!!
                    }
                val scene =
                    toonTopBarScene(
                        cell.tokens,
                        390f,
                        o["title"]?.jsonPrimitive?.content ?: "Saturday",
                        if (center) ToonTopAppBarVariant.Center else ToonTopAppBarVariant.Small,
                        listOf(ToonIcons.Search, ToonIcons.More),
                        true,
                        look,
                    ).placed(0f, 0f, 1f, rot, 195f, -40f)
                EngineFrames.compare(cell, scene)
            }
        println("top app bar frames: ${cells.size} cells, worst coordinate $worst dp")
        assertEquals(39, cells.size)
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `the sway follows the engine's pendulum at every millisecond`() {
        val curve =
            Json
                .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText())
                .jsonObject
                .entries
                .first { it.key.startsWith("sway(") }
        val args = Regex("sway\\(t, ([-\\d.]+)\\)").find(curve.key)!!.groupValues[1].toFloat()
        val at =
            curve.value.jsonObject
                .getValue("at")
                .jsonArray
                .map { it.jsonPrimitive.content.toFloat() }
        val v =
            curve.value.jsonObject
                .getValue("value")
                .jsonArray
                .map { it.jsonPrimitive.content.toFloat() }
        var worst = 0f
        for (i in at.indices) worst = maxOf(worst, abs(v[i] - toonSway(at[i], args)))
        println("sway curve ${curve.key}: worst $worst")
        assertTrue(worst <= 1e-5f, "sway off the engine by $worst")
    }

    @Test
    fun `each kick swings the other way, for 900 ms, and not at all under reduce motion`() {
        val clock = ToonSwayClock()
        clock.kick()
        clock.tick(1000)
        clock.tick(1090)
        assertEquals(toonSway(90f, 24f), clock.angle(false), 1e-6f)
        assertEquals(0f, clock.angle(true))
        clock.tick(1890)
        assertEquals(toonSway(890f, 24f), clock.angle(false), 1e-6f, "still swinging at 890 ms")
        clock.tick(1900)
        assertEquals(0f, clock.angle(false), "at rest after 900 ms")
        clock.kick()
        clock.tick(2000)
        clock.tick(2090)
        assertEquals(toonSway(90f, -24f), clock.angle(false), 1e-6f)
    }

    @Test
    fun `the kit's targets are each action's 40 dp, else the first 76 dp for the back arrow`() {
        // 390 wide: "more" (index 1) at 298, "search" (index 0) at 338.
        assertEquals(ToonTopBarKey.Action(0), toonTopBarTarget(390f, 2, true, 330f))
        assertEquals(ToonTopBarKey.Action(0), toonTopBarTarget(390f, 2, true, 370f))
        assertEquals(ToonTopBarKey.Action(1), toonTopBarTarget(390f, 2, true, 290f))
        assertNull(toonTopBarTarget(390f, 2, true, 280f))
        assertEquals(ToonTopBarKey.Nav, toonTopBarTarget(390f, 2, true, 75f))
        assertNull(toonTopBarTarget(390f, 2, true, 77f))
        assertNull(toonTopBarTarget(390f, 2, false, 20f), "no back arrow")
    }

    @Test
    fun `taps go to the back arrow and to the action under them`() {
        runComposeUiTest {
            var navs = 0
            val acted = mutableListOf<ToonIconGlyph>()
            setContent {
                ToonTheme {
                    ToonTopAppBar("Saturday", Modifier.testTag("bar"), onNav = { navs++ }, onAction = { acted += it })
                }
            }
            val bar = onNodeWithTag("bar")
            bar.performTouchInput { click(Offset(30f, 38f)) }
            bar.performTouchInput { click(Offset(350f, 38f)) }
            bar.performTouchInput { click(Offset(305f, 38f)) }
            bar.performTouchInput { click(Offset(200f, 38f)) }
            assertEquals(1, navs)
            assertEquals(listOf(ToonIcons.Search, ToonIcons.More), acted)
        }
    }

    /** The bar drawn by [draw] at [rot] degrees, as pixels, on the light theme's stage. */
    private fun bar(
        rot: Float,
        draw: DrawScope.(ToonScene, Float, ToonTextRenderer) -> Unit,
    ): IntArray {
        lateinit var out: IntArray
        runComposeUiTest {
            setContent {
                ToonTheme {
                    val text = rememberToonText()
                    val tokens = Toon.tokens
                    Box(Modifier.size(410.dp, 140.dp).background(Toon.colors.stage).testTag("bar")) {
                        Canvas(Modifier.fillMaxSize()) {
                            inDp {
                                translate(10f, 50f) {
                                    draw(
                                        toonTopBarScene(
                                            tokens,
                                            390f,
                                            "Saturday",
                                            ToonTopAppBarVariant.Center,
                                            listOf(ToonIcons.Search),
                                            true,
                                            ToonTopBarLook(),
                                        ),
                                        rot,
                                        text,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            val m = onNodeWithTag("bar").captureToImage().toPixelMap()
            out = IntArray(m.width * m.height) { m[it % m.width, it / m.width].toArgb() }
        }
        return out
    }

    @Test
    fun `the title swings with the sign, and at rest the bar is drawn as before`() {
        // B-56: before, the sign's shapes turned and its title only moved, level.
        val level: DrawScope.(ToonScene, Float, ToonTextRenderer) -> Unit =
            { scene, rot, text -> drawScene(scene.placed(0f, 0f, 1f, rot, 195f, -40f), text) }
        val swung: DrawScope.(ToonScene, Float, ToonTextRenderer) -> Unit =
            { scene, rot, text -> drawToonTopBar(scene, rot, 390f, text) }
        val atRest = bar(0f, swung)
        val before = bar(0f, level)
        assertEquals(0, atRest.indices.count { atRest[it] != before[it] }, "at rest nothing changes")
        val tilted = bar(4f, swung)
        val old = bar(4f, level)
        val differ = tilted.indices.count { tilted[it] != old[it] }
        assertTrue(differ > 100, "at 4° the title is drawn turned: $differ px differ from the level title")
    }
}
