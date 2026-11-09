package io.github.youndie.toon.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.theme.Toon
import kotlin.math.min

// A column of cards as the Core page's `cardStack` draws it and the Screens page lays it out (B-46): each
// card drops in 40 ms after the one above it, and when one is thrown away the cards below slide up over
// its slot with the engine's overshoot (`cardDismiss`'s `n`) instead of jumping. The design has no file
// of its own for the list; the cards are `ToonCard`s, and only the cascade and the close-up are the list's.
// A thrown card punches no hole into the paper here: the cards below close up over its slot at once and
// only cover it (B-66).

/** One card of a [ToonCardList]: its [key], unique in the list, and what the card shows. */
public class ToonCardListItem(
    public val key: Any,
    public val title: String,
    public val meta: String = "",
    public val time: String = "",
)

/**
 * The design's list of cards: a [ToonCard] per [items], [width] wide and 18 dp apart. With [appear] the
 * cards first shown drop in one after another, 40 ms apart; one added later drops in on its own. With
 * [dismissible] a swipe throws a card away and reports it to [onDismiss] as the card goes, 200 ms in;
 * the list keeps its slot while the cards below close up over it, 520 ms in all, whether or not the
 * caller has removed it by then. A dismissed card stays out of the list until it is removed and added
 * back. Each card keeps the colour and tilt of the place it was first shown in.
 */
@Composable
public fun ToonCardList(
    items: List<ToonCardListItem>,
    modifier: Modifier = Modifier,
    width: Dp = 342.dp,
    tilt: Boolean = true,
    appear: Boolean = false,
    dismissible: Boolean = false,
    onDismiss: (ToonCardListItem) -> Unit = {},
    onClick: (ToonCardListItem) -> Unit = {},
) {
    val reduce = Toon.reduceMotion
    val list = remember { ToonCardListState() }
    val shown = list.shown(items, reduce)
    val first = list.slots.isEmpty()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CARD_LIST_GAP.dp)) {
        shown.forEachIndexed { position, item ->
            val slot = list.slot(item.key, position, if (appear && first) position * CASCADE_MS else 0, appear)
            key(item.key) {
                ToonCard(
                    slot.state,
                    item.title,
                    Modifier.graphicsLayer {
                        // The cards above that are being thrown, each pulling this one up by its close-up.
                        translationY = list.closeUp(shown, position, reduce) * CARD_LIST_STEP * density
                    },
                    item.meta,
                    item.time,
                    slot.index,
                    tilt,
                    slot.delay,
                    width,
                    selected = false,
                    enabled = true,
                    dismissible = dismissible,
                    onDismiss = { onDismiss(item) },
                    onClick = { onClick(item) },
                    hole = false,
                )
            }
        }
    }
    SideEffect { list.settle(items, shown) }
}

/** The page's cascade: each card of a list starts 40 ms after the one above it. */
internal const val CASCADE_MS = 40

/** The page's gap between cards, and the step `cardStack` closes up by: an 84 dp card and 18 dp. */
private const val CARD_LIST_GAP = 18f
internal const val CARD_LIST_STEP = 102f

/** A card's clock, and the colour and delay it was given when first shown. */
internal class ToonCardListSlot(
    val state: ToonCardState,
    val index: Int,
    val delay: Int,
)

/** The list's cards by key, and what it showed last, so a card the caller removed mid-throw keeps its slot. */
internal class ToonCardListState {
    val slots = mutableMapOf<Any, ToonCardListSlot>()
    private var last: List<ToonCardListItem> = emptyList()

    fun slot(
        key: Any,
        position: Int,
        delay: Int,
        appear: Boolean,
    ): ToonCardListSlot = slots.getOrPut(key) { ToonCardListSlot(ToonCardState(appear), position, delay) }

    /**
     * What the list shows: the caller's [items] less those whose throw has played out, with each card
     * the caller has removed while its cards below still close up put back where it was.
     */
    fun shown(
        items: List<ToonCardListItem>,
        reduceMotion: Boolean,
    ): List<ToonCardListItem> {
        fun done(key: Any) = slots[key]?.state?.closedUp(reduceMotion) ?: false
        val keys = items.mapTo(HashSet()) { it.key }
        val out = items.filterNot { done(it.key) }.toMutableList()
        last.forEachIndexed { p, it ->
            val thrown = slots[it.key]?.state?.dismissing ?: false
            if (it.key !in keys && thrown && !done(it.key)) out.add(min(p, out.size), it)
        }
        return out
    }

    /** How many steps the card at [position] of [shown] is pulled up by the throws above it. */
    fun closeUp(
        shown: List<ToonCardListItem>,
        position: Int,
        reduceMotion: Boolean,
    ): Float {
        var n = 0f
        for (p in 0 until min(position, shown.size)) n += slots[shown[p].key]?.state?.closeUp(reduceMotion) ?: 0f
        return n
    }

    /** After a composition: forget the cards that are gone from both the caller's list and the screen. */
    fun settle(
        items: List<ToonCardListItem>,
        shown: List<ToonCardListItem>,
    ) {
        last = shown
        val keep = (items.asSequence() + shown.asSequence()).mapTo(HashSet()) { it.key }
        slots.keys.retainAll(keep)
    }
}
