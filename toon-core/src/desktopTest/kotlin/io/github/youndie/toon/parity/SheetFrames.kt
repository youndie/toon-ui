package io.github.youndie.toon.parity

import io.github.youndie.toon.component.ToonCardLook
import io.github.youndie.toon.component.ToonListItemLook
import io.github.youndie.toon.component.ToonListTrailing
import io.github.youndie.toon.component.ToonSheetMotion
import io.github.youndie.toon.component.toonCardScene
import io.github.youndie.toon.component.toonListItemScene
import io.github.youndie.toon.component.toonSheetMove
import io.github.youndie.toon.component.toonSheetScene
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.tokens.ToonTokens

/**
 * B-38: the v1 page's `sheetScene` — three cards behind, the sheet 260 dp tall on a 360 × 520
 * screen, and two rows on it — for the fixtures and the frame test. The page code is held to its text
 * by scripts/component-cells.mjs.
 */
internal object SheetFrames {
    private val CARDS =
        listOf(
            Triple("Pancake breakfast", "Kitchen, bring syrup", "9:00"),
            Triple("Bike to the lake", "Check the tyre pump", "11:30"),
            Triple("Paint the fence", "Green this time", "14:00"),
        )

    /** The page's open strip at [t] ms. */
    fun open(
        tokens: ToonTokens,
        t: Float,
    ): ToonScene = scene(tokens, toonSheetMove(t, true, 260f))

    fun scene(
        tokens: ToonTokens,
        a: ToonSheetMotion,
    ): ToonScene {
        val sheet = toonSheetScene(tokens, 360f, 520f, 260f, "Share plan", a)
        val y = 520f - 260f + a.dy
        var bg = ToonScene.Empty
        CARDS.forEachIndexed { i, (title, meta, time) ->
            bg +=
                toonCardScene(
                    tokens,
                    i,
                    324f,
                    title,
                    meta,
                    time,
                    ToonCardLook(),
                ).placed(18f, 24f + i * 102, 1f, tokens.shape.tilts[i], 162f, 42f)
        }
        // The cards' text under the sheet is not drawn.
        bg = ToonScene(bg.layers, bg.labels.filter { it.baseline < y - 20 })
        val rows =
            toonListItemScene(
                tokens,
                312f,
                ToonIcons.Person,
                "Family",
                "4 people",
                ToonListTrailing.Check,
                ToonListItemLook(on = true),
            ).placed(24f, y + 70) +
                toonListItemScene(
                    tokens,
                    312f,
                    ToonIcons.Share,
                    "Copy link",
                    "",
                    ToonListTrailing.Chevron,
                    ToonListItemLook(seed = 1, last = true),
                ).placed(24f, y + 142)
        return bg + sheet + rows
    }
}
