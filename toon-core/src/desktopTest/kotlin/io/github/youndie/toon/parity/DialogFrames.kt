package io.github.youndie.toon.parity

import io.github.youndie.toon.component.ToonButtonLook
import io.github.youndie.toon.component.ToonButtonVariant
import io.github.youndie.toon.component.ToonCardLook
import io.github.youndie.toon.component.ToonDialogFocus
import io.github.youndie.toon.component.ToonDialogMotion
import io.github.youndie.toon.component.toonButtonScene
import io.github.youndie.toon.component.toonCardScene
import io.github.youndie.toon.component.toonDialogCardScene
import io.github.youndie.toon.component.toonDialogClose
import io.github.youndie.toon.component.toonDialogHeight
import io.github.youndie.toon.component.toonDialogOpen
import io.github.youndie.toon.component.toonDialogOverlay
import io.github.youndie.toon.draw.ToonLabel
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.tokens.ToonTokens
import kotlin.math.max

/**
 * B-24: the MVP page's dialog cells and strips as the engine's `dialogScene` composes them over its
 * demo screen (two cards and a button), for the fixtures and the frame test. The page code is held to
 * its text by scripts/component-cells.mjs. [labelWidth] measures a button label, which only the text
 * button's underline reads.
 */
internal object DialogFrames {
    private val ITEMS =
        listOf(
            Triple("Pancake breakfast", "Kitchen, bring syrup", "9:00"),
            Triple("Bike to the lake", "Check the tyre pump", "11:30"),
        )

    /** The page's four cells: destructive, confirm focused, cancel focused, and the basic one. */
    val CELLS =
        listOf(
            Cell("Delete plan?", listOf("“Paint the fence” will be gone", "for good."), "Delete", true, null),
            Cell(
                "Delete plan?",
                listOf("“Paint the fence” will be gone", "for good."),
                "Delete",
                true,
                ToonDialogFocus.Confirm,
            ),
            Cell(
                "Delete plan?",
                listOf("“Paint the fence” will be gone", "for good."),
                "Delete",
                true,
                ToonDialogFocus.Cancel,
            ),
            Cell("Leave the plan?", listOf("Changes you made will be", "kept as a draft."), "Leave", false, null),
        )

    class Cell(
        val title: String,
        val lines: List<String>,
        val confirm: String,
        val destructive: Boolean,
        val focus: ToonDialogFocus?,
    )

    private val AT_REST = ToonDialogMotion(190f, 1f, 1f, 1f, 0f)

    fun cell(
        tokens: ToonTokens,
        i: Int,
        labelWidth: (String) -> Float,
    ): ToonScene = scene(tokens, AT_REST, CELLS[i], labelWidth)

    fun open(
        tokens: ToonTokens,
        t: Float,
        labelWidth: (String) -> Float,
    ): ToonScene = scene(tokens, toonDialogOpen(t), CELLS[0], labelWidth)

    fun close(
        tokens: ToonTokens,
        t: Float,
        labelWidth: (String) -> Float,
    ): ToonScene = scene(tokens, toonDialogClose(t), CELLS[0], labelWidth)

    /** The engine's `dialogScene`: 360 × 460, the dialog centred on y 236. */
    private fun scene(
        tokens: ToonTokens,
        a: ToonDialogMotion,
        o: Cell,
        labelWidth: (String) -> Float,
    ): ToonScene {
        val cy = 236f
        // The demo screen's text dims to 35 % once the iris has closed in past 300.
        val k = if (a.r < 300) .35f else 1f
        var bg = ToonScene.Empty
        ITEMS.forEachIndexed { i, (title, meta, time) ->
            bg +=
                toonCardScene(
                    tokens,
                    i,
                    342f,
                    title,
                    meta,
                    time,
                    ToonCardLook(textOp = k),
                ).placed(9f, 24f + i * 102, 1f, tokens.shape.tilts[i], 171f, 42f)
        }
        bg +=
            toonButtonScene(
                tokens,
                ToonButtonVariant.Filled,
                342f,
                "Add plan",
                labelWidth("Add plan"),
                ToonButtonLook(),
            ).placed(9f, 384f)
        val card =
            toonDialogCardScene(
                tokens,
                o.title,
                o.lines,
                o.confirm,
                "Cancel",
                o.destructive,
                o.focus,
                labelWidth("Cancel"),
            )
        val h = toonDialogHeight(o.lines.size)
        // What the dialog covers is not drawn; the rest of the screen's text dims with it.
        val up = a.op > .01f && a.s > .05f
        val hh = h / 2 * max(1f, a.s) + 8
        val labels =
            bg.labels
                .filterNot { up && it.baseline > cy - hh && it.baseline < cy + hh + 16 }
                .map {
                    ToonLabel(
                        it.text,
                        it.x,
                        it.baseline,
                        it.color,
                        it.alpha * k,
                        it.style,
                        it.anchor,
                        it.maxWidth,
                        it.maxLines,
                    )
                }
        return ToonScene(bg.layers, labels) + toonDialogOverlay(tokens, 360f, 460f, cy, a.r, card, h, a)
    }
}
