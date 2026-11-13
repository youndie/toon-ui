package io.github.youndie.toon.parity

import io.github.youndie.toon.component.ToonCardLook
import io.github.youndie.toon.component.ToonDrawerItem
import io.github.youndie.toon.component.ToonDrawerLook
import io.github.youndie.toon.component.ToonDrawerMotion
import io.github.youndie.toon.component.toonCardScene
import io.github.youndie.toon.component.toonDrawerOpen
import io.github.youndie.toon.component.toonDrawerScene
import io.github.youndie.toon.draw.ToonAnchor
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.tokens.ToonTokens

/**
 * B-39: the v1 page's `drawerScene` — three cards behind, the door 280 dp wide on a 360 × 520
 * screen — for the fixtures and the frame test. The page code is held to its text by
 * scripts/component-cells.mjs.
 */
internal object DrawerFrames {
    private val CARDS =
        listOf(
            Triple("Pancake breakfast", "Kitchen, bring syrup", "9:00"),
            Triple("Bike to the lake", "Check the tyre pump", "11:30"),
            Triple("Paint the fence", "Green this time", "14:00"),
        )

    /** The kit's default destinations. */
    val items =
        listOf(
            ToonDrawerItem("Home", ToonIcons.Home),
            ToonDrawerItem("Weekend", ToonIcons.Calendar),
            ToonDrawerItem("Saved", ToonIcons.Heart),
            ToonDrawerItem("Settings", ToonIcons.Settings),
        )

    /** The page's open strip at [t] ms. */
    fun open(
        tokens: ToonTokens,
        t: Float,
    ): ToonScene = scene(tokens, toonDrawerOpen(t))

    fun scene(
        tokens: ToonTokens,
        a: ToonDrawerMotion,
        pressed: Int? = null,
        hovered: List<Float> = emptyList(),
    ): ToonScene {
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
        // The cards' text the door covers is not drawn.
        val edge = 280 * a.sx.coerceIn(0f, 1f)
        bg =
            ToonScene(
                bg.layers,
                bg.labels.filter {
                    val right = if (it.anchor == ToonAnchor.Middle) it.x + 30 else it.x
                    right > edge + 6 && it.x > edge + 6
                },
            )
        return bg +
            toonDrawerScene(
                tokens,
                280f,
                520f,
                360f,
                "Saturday",
                items,
                ToonDrawerLook(a, pressed = pressed, hovered = hovered),
            )
    }
}
