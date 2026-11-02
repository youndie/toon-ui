package io.github.youndie.toon.a11y

import androidx.compose.ui.graphics.Color
import io.github.youndie.toon.tokens.ToonColors
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-30: every text a component draws, on what it draws it on, holds the brief's 4.5:1 (WCAG 2) in
 * every theme. The pairs are read off the engine's renderers, the colour each `X` is filled with on
 * the layer under it; a disabled control's text is out of scope, as WCAG has it. One pair is the
 * owner's accepted exception (2026-11-02, B-24): `onDanger` on `danger` in Ink & Gouache light, 4.38,
 * kept by the owner for every use of the pair — the destructive button, the badge's number and the
 * error snackbar.
 */
class ToonContrastTest {
    private class Pair(
        val where: String,
        val text: (ToonColors) -> Color,
        val on: (ToonColors) -> Color,
    )

    private val pairs: List<Pair> =
        buildList {
            add(Pair("button, filled", { it.onPrimary }, { it.primary }))
            add(Pair("button, outlined", { it.text }, { it.surface }))
            add(Pair("button, text", { it.text }, { it.stage }))
            add(Pair("FAB label", { it.onPrimary }, { it.primary }))
            add(Pair("badge number", { it.onDanger }, { it.danger }))
            add(Pair("checkbox, radio, switch label", { it.text }, { it.stage }))
            add(Pair("checkbox label in error", { it.dangerText }, { it.stage }))
            add(Pair("text field value", { it.text }, { it.field }))
            add(Pair("text field label at rest", { it.textMuted }, { it.field }))
            add(Pair("text field label lifted", { it.text }, { it.stage }))
            add(Pair("text field helper", { it.textMuted }, { it.stage }))
            add(Pair("text field error", { it.dangerText }, { it.stage }))
            add(Pair("chip", { it.text }, { it.surface }))
            add(Pair("chip, selected", { it.ink }, { it.chipSelected }))
            for (i in 0 until 3) {
                add(Pair("card $i title", { it.text }, { it.cardFills[i] }))
                add(Pair("card $i meta", { it.textMuted }, { it.cardFills[i] }))
                add(Pair("card $i time", { it.ink }, { it.stickers[i] }))
            }
            add(Pair("list item title", { it.text }, { it.stage }))
            add(Pair("list item sub", { it.textMuted }, { it.stage }))
            add(Pair("list item title, selected", { it.text }, { it.surfacePressed }))
            add(Pair("list item sub, selected", { it.textMuted }, { it.surfacePressed }))
            add(Pair("snackbar, info", { it.ink }, { it.warning }))
            add(Pair("snackbar, success", { it.ink }, { it.successLight }))
            add(Pair("snackbar, error", { it.onDanger }, { it.danger }))
            add(Pair("dialog title, body, cancel", { it.text }, { it.surface }))
            add(Pair("dialog body", { it.textMuted }, { it.surface }))
            add(Pair("dialog confirm", { it.onPrimary }, { it.primary }))
            add(Pair("dialog confirm, destructive", { it.onDanger }, { it.danger }))
            add(Pair("top app bar title", { it.text }, { it.surface }))
            add(Pair("navigation bar label", { it.text }, { it.surface }))
            add(Pair("tab, selected", { it.ink }, { it.chipSelected }))
            add(Pair("tab", { it.text }, { it.stage }))
        }

    private fun lum(c: Color): Double {
        fun ch(v: Float) = if (v <= .03928f) v / 12.92 else ((v + .055) / 1.055).pow(2.4)
        return .2126 * ch(c.red) + .7152 * ch(c.green) + .0722 * ch(c.blue)
    }

    private fun ratio(
        a: Color,
        b: Color,
    ): Double {
        val x = lum(a)
        val y = lum(b)
        return (maxOf(x, y) + .05) / (minOf(x, y) + .05)
    }

    /** The owner's one accepted pair (B-24): the danger fill under its own text, in this theme only. */
    private fun accepted(
        tokens: ToonTokens,
        p: Pair,
    ) = tokens === inkGouacheLight && p.text(tokens.colors) == tokens.colors.onDanger &&
        p.on(tokens.colors) == tokens.colors.danger

    @Test
    fun `every text on what it is drawn on holds 4_5 to 1, but the one accepted pair`() {
        val low = mutableListOf<String>()
        for (tokens in listOf(inkGouacheLight, inkGouacheNight, brightCel)) {
            for (p in pairs) {
                val r = ratio(p.text(tokens.colors), p.on(tokens.colors))
                if (r < 4.5 && !accepted(tokens, p)) low += "${tokens.name}: ${p.where} %.2f".format(r)
                if (accepted(tokens, p)) println("accepted: ${tokens.name}: ${p.where} %.2f".format(r))
            }
        }
        assertTrue(low.isEmpty(), "below 4.5:1:\n" + low.joinToString("\n"))
    }
}
