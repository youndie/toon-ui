package io.github.youndie.toon.component

import io.github.youndie.toon.draw.EngineFrames
import io.github.youndie.toon.draw.EngineFrames.f
import io.github.youndie.toon.icon.ToonIcons
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B-15: every MVP page cell of the icon button and the badge against the engine's `iconBtn` and
 * `badge` frames, and their curves — `iconBounce`, `badgeAppear`, `badgeCount` — against the engine's
 * at every millisecond.
 */
class ToonIconButtonAndBadgeTest {
    private fun JsonObject.flag(key: String) = this[key]?.jsonPrimitive?.boolean ?: false

    private fun iconButtonScene(
        tokens: io.github.youndie.toon.tokens.ToonTokens,
        o: JsonObject,
    ) = toonIconButtonScene(
        tokens,
        ToonIcons.all.single { it.name == o.getValue("icon").jsonPrimitive.content },
        if (o["variant"]?.jsonPrimitive?.content ==
            "filled"
        ) {
            ToonIconButtonVariant.Filled
        } else {
            ToonIconButtonVariant.Standard
        },
        ToonIconButtonLook(
            p = o.f("p") ?: 0f,
            iy = o.f("iy") ?: 0f,
            selected = o.flag("sel"),
            disabled = o.flag("disabled"),
            focus = o.flag("focus"),
        ),
    )

    private fun badgeLook(o: JsonObject) =
        ToonBadgeLook(
            n = o["n"]?.jsonPrimitive?.content ?: "",
            dot = o.flag("dot"),
            s = o.f("s") ?: 1f,
            numOp = o.f("numOp") ?: 1f,
            numDy = o.f("numDy") ?: 0f,
            old = o["old"]?.jsonPrimitive?.content,
            oldOp = o.f("oldOp") ?: 0f,
            oldDy = o.f("oldDy") ?: 0f,
            dx = o.f("dx") ?: 0f,
        )

    @Test
    fun `every icon button cell is the engine's frame`() {
        val cells = EngineFrames.cells("IconButton")
        val worst = cells.maxOf { EngineFrames.compare(it, iconButtonScene(it.tokens, it.opts)) }
        println("icon button frames: ${cells.size} cells, worst coordinate $worst dp")
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    @Test
    fun `every badge cell is the engine's frame`() {
        val cells = EngineFrames.cells("Badge")
        val worst =
            cells.maxOf { cell ->
                val scene =
                    if (cell.opts.flag("onIconButton")) {
                        toonIconButtonScene(
                            cell.tokens,
                            ToonIcons.Bell,
                            ToonIconButtonVariant.Standard,
                            ToonIconButtonLook(),
                        ) +
                            toonBadgeScene(cell.tokens, ToonBadgeLook("3")).placed(40f, 6f)
                    } else {
                        toonBadgeScene(cell.tokens, badgeLook(cell.opts))
                    }
                EngineFrames.compare(cell, scene)
            }
        println("badge frames: ${cells.size} cells, worst coordinate $worst dp")
        assertTrue(worst <= EngineFrames.TOLERANCE, "worst coordinate $worst dp")
    }

    private val curves =
        Json
            .parseToJsonElement(checkNotNull(javaClass.getResource("/engine-goldens/curves.json")).readText())
            .jsonObject

    private fun column(
        curve: String,
        name: String,
    ) = curves
        .getValue(curve)
        .jsonObject
        .getValue(name)
        .jsonArray
        .map { it.jsonPrimitive.float }

    private fun worst(
        expected: List<Float>,
        actual: (Int) -> Float,
    ) = expected.indices.maxOf { abs(expected[it] - actual(it)) }

    @Test
    fun `the hop, the pop and the count swap follow the engine's curves`() {
        val at = column("iconBounce(t)", "at")
        val bounce = worst(column("iconBounce(t)", "value")) { toonIconBounce(at[it]) }
        val ap = column("badgeAppear(t)", "at")
        val appear =
            maxOf(
                worst(column("badgeAppear(t)", "s")) { toonBadgeAppear(ap[it]).s },
                worst(column("badgeAppear(t)", "numOp")) { toonBadgeAppear(ap[it]).numOp },
            )
        val ct = column("badgeCount(t)", "at")
        val count =
            listOf<(ToonBadgeCount) -> Float>({ it.oldDy }, { it.oldOp }, { it.numDy }, { it.numOp }, { it.dx })
                .zip(listOf("oldDy", "oldOp", "numDy", "numOp", "dx"))
                .maxOf { (get, name) -> worst(column("badgeCount(t)", name)) { get(toonBadgeCount(ct[it])) } }
        println("iconBounce $bounce, badgeAppear $appear, badgeCount $count")
        assertTrue(
            maxOf(bounce, appear, count) <= CURVE_TOLERANCE,
            "curves off the engine by ${maxOf(bounce, appear, count)}",
        )
    }

    @Test
    fun `under reduce motion the hop and the pops are already over`() {
        val ib = toonIconButtonFrameAt(bounceAgeMs = 70f, popAgeMs = 70f, reduceMotion = true)
        assertEquals(0f, ib.iy)
        assertNull(ib.flash)
        val live = toonIconButtonFrameAt(bounceAgeMs = 70f, popAgeMs = 70f, reduceMotion = false)
        assertEquals(toonIconBounce(70f), live.iy)
        assertEquals(70f / 220, live.flash!!, 1e-6f)
        val badge =
            toonBadgeFrameAt(
                4,
                3,
                dot = false,
                appearAgeMs = 42f,
                countAgeMs = 60f,
                hideAgeMs = null,
                reduceMotion = true,
            )
        assertEquals(1f, badge.look!!.s, 1e-6f)
        assertEquals(1f, badge.look.numOp)
        assertNull(badge.look.old)
        assertNull(badge.flash)
    }

    @Test
    fun `past 99 the badge says 99+, and at 0 it shrinks away`() {
        assertEquals("99+", badgeLabel(120))
        assertEquals("99", badgeLabel(99))
        val hiding =
            toonBadgeFrameAt(
                0,
                5,
                dot = false,
                appearAgeMs = null,
                countAgeMs = null,
                hideAgeMs = 60f,
                reduceMotion = false,
            )
        assertEquals(.5f, hiding.look!!.s, 1e-6f)
        assertEquals("5", hiding.look.n)
        assertNull(
            toonBadgeFrameAt(
                0,
                5,
                dot = false,
                appearAgeMs = null,
                countAgeMs = null,
                hideAgeMs = 130f,
                reduceMotion = false,
            ).look,
        )
    }

    private companion object {
        /** The goldens' six decimals and Float arithmetic, as for the other curves (B-12, B-14). */
        const val CURVE_TOLERANCE = 1e-5f
    }
}
