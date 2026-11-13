package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonInsets
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonSegments
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.inkLinePoints
import io.github.youndie.toon.draw.ringLayer
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.draw.sizeDp
import io.github.youndie.toon.draw.smoothLayer
import io.github.youndie.toon.draw.wobblePoints
import io.github.youndie.toon.icon.ToonIconGlyph
import io.github.youndie.toon.icon.iconLayers
import io.github.youndie.toon.interaction.ToonPointerTarget
import io.github.youndie.toon.interaction.hoverLifted
import io.github.youndie.toon.interaction.rememberToonHoverState
import io.github.youndie.toon.interaction.toonHoverable
import io.github.youndie.toon.interaction.toonPlateLayer
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.type.withBacking
import kotlin.math.roundToInt

// A port of `ToonSidebar.dc.html` and of the desktop extension's panel `sheet`, `navPlate` and
// `liftBody` (B-77, research §1.8): a persistent side panel — always there, 240 to 360 dp wide, not the
// modal drawer — with a search field, a recording pinned above a list of grouped rows, and navigation
// rows at its foot. The extension's `sheet` overwrites the kit's bottom sheet in the design; here the
// panel is a scene of its own beside it. The search field (B-71) and the scrollbar (B-72) are the
// kit's own. The renderers draw shapes only: the words are HTML over them in the design, and here
// texts laid out by that HTML's numbers.

/** One row of the list: [title] over [meta], and a search result's [snippet] under them. */
public class ToonSidebarItem(
    public val id: String,
    public val title: String,
    public val meta: String = "",
    public val snippet: ToonSidebarSnippet? = null,
)

/** A search result's line: [before], the match [hit] highlighted, [after]. */
public class ToonSidebarSnippet(
    public val before: String,
    public val hit: String,
    public val after: String,
)

/** A group of rows under its [label]. */
public class ToonSidebarGroup(
    public val label: String,
    public val items: List<ToonSidebarItem>,
)

/** The recording in progress, pinned above the list: [title], and [elapsed] · [source] under it. */
public class ToonSidebarRecording(
    public val id: String,
    public val title: String,
    public val elapsed: String,
    public val source: String = "",
)

/** A navigation row at the sidebar's foot. */
public class ToonSidebarNav(
    public val id: String,
    public val label: String,
    public val icon: ToonIconGlyph,
)

/**
 * The design's sidebar, [width] wide (held to its 240–360 dp) and as tall as it is given: a search
 * field over a list of [groups], [recording] pinned above them, and [nav] rows at the foot.
 *
 * - [query] is the field's value, reported to [onQuery], with [count] beside it and [keyHint] while it
 *   is empty; while [results] is set, the list shows them instead of [groups], each row with its
 *   snippet and the match highlighted.
 * - A row, or the recording, is [selected] by its id, and a click, Enter or Space on it calls
 *   [onSelect]; a navigation row is [selectedNav], and calls [onNav]. While a navigation row is
 *   selected, no list row shows as selected. A selected row is a plate, not a colour alone.
 * - Up and Down move the keyboard focus from row to row, so Enter selects by keys alone.
 * - With nothing to list, [emptyTitle] and [emptyText] stand where the list would.
 *
 * A mouse over a row lays a plate under it, and over the recording lifts it; keyboard focus rings a
 * row. The recording's mark breathes on a 1.2 s cycle, and holds its full size with reduced motion.
 */
@Composable
public fun ToonSidebar(
    groups: List<ToonSidebarGroup>,
    selected: String?,
    onSelect: (String) -> Unit,
    query: String,
    onQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
    results: List<ToonSidebarGroup>? = null,
    count: String = "",
    recording: ToonSidebarRecording? = null,
    nav: List<ToonSidebarNav> = emptyList(),
    selectedNav: String? = null,
    onNav: (String) -> Unit = {},
    placeholder: String = "Search",
    keyHint: String = "⌘K",
    emptyTitle: String = "Nothing here yet",
    emptyText: String = "New items appear here.",
    width: Dp = 300.dp,
    listState: LazyListState = rememberLazyListState(),
) {
    val tokens = Toon.tokens
    val c = tokens.colors
    val w = width.value.coerceIn(MIN_W, MAX_W)
    val box = toonSidebarBox(w, recording != null, nav.size)
    val shown = results ?: groups
    val navSelected = selectedNav != null

    Box(modifier.width(w.dp).fillMaxHeight()) {
        Canvas(Modifier.matchParentSize()) {
            // The engine draws in dp (research D5); the canvas is pixels (B-47).
            inDp { drawScene(toonSidebarPanelScene(tokens, w, sizeDp.height, nav.size)) }
        }
        ToonSearchField(
            query,
            onQuery,
            placeholder,
            Modifier.offset(FIELD_X.dp, FIELD_Y.dp),
            count = count,
            keyHint = keyHint,
            width = (w - 2 * FIELD_X).dp,
        )
        if (recording != null) {
            SidebarRecordingRow(
                recording,
                thick = navSelected || selected == recording.id,
                onPick = { onSelect(recording.id) },
                modifier = Modifier.offset(ROW_X.dp, REC_Y.dp).size(box.navW.dp, REC_H.dp),
            )
        }
        if (shown.isEmpty()) {
            Column(
                Modifier.padding(start = EMPTY_X.dp, end = EMPTY_X.dp, top = (box.listTop + EMPTY_TOP).dp),
                verticalArrangement = Arrangement.spacedBy(EMPTY_GAP.dp),
            ) {
                SidebarWords(AnnotatedString(emptyTitle), EMPTY_TITLE, c.text)
                SidebarWords(AnnotatedString(emptyText), EMPTY_TEXT, c.textMuted, lines = Int.MAX_VALUE)
            }
        }
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(start = ROW_X.dp, end = LIST_END.dp, top = box.listTop.dp, bottom = box.footH.dp),
            state = listState,
            contentPadding = PaddingValues(bottom = LIST_BOTTOM.dp),
        ) {
            shown.forEachIndexed { gi, group ->
                item(key = "g$gi") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = PAD.dp, end = PAD.dp, top = HEADER_TOP.dp, bottom = HEADER_BOTTOM.dp),
                    ) {
                        SidebarWords(AnnotatedString(group.label.uppercase()), HEADER, c.textMuted)
                    }
                }
                itemsIndexed(group.items, key = { _, it -> "$gi/${it.id}" }) { j, item ->
                    val sel = item.id == selected && !navSelected
                    val h = if (item.snippet != null) SNIPPET_ROW else ToonPointerTarget.ROW
                    SidebarPlateRow(
                        selected = sel,
                        seed = j,
                        onPick = { onSelect(item.id) },
                        modifier = Modifier.fillMaxWidth().height(h.dp),
                    ) {
                        Column(
                            Modifier.align(Alignment.CenterStart).padding(horizontal = PAD.dp),
                            verticalArrangement = Arrangement.spacedBy(ROW_GAP.dp),
                        ) {
                            SidebarItemWords(tokens, item, sel)
                        }
                    }
                }
            }
        }
        // The design sets the 14 dp bar 4 dp from the sidebar's edge, over the list from 8 dp below its
        // top to 8 dp above its end. The scrollbar keeps 6 dp after its bar, so it reaches 2 dp past the
        // edge: that strip draws nothing and takes no press.
        ToonScrollbar(
            listState,
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = SCROLL_OVERHANG.dp)
                .padding(top = box.listTop.dp, bottom = box.footH.dp),
        )
        if (nav.isNotEmpty()) {
            Column(Modifier.align(Alignment.BottomStart).padding(start = ROW_X.dp, bottom = NAV_BOTTOM.dp)) {
                nav.forEachIndexed { j, n ->
                    val sel = n.id == selectedNav
                    SidebarPlateRow(
                        selected = sel,
                        seed = j + NAV_SEED,
                        onPick = { onNav(n.id) },
                        modifier = Modifier.size(box.navW.dp, ToonPointerTarget.NAV_ROW.dp),
                        icon = n.icon,
                    ) {
                        Box(Modifier.align(Alignment.CenterStart).padding(start = NAV_LABEL_X.dp)) {
                            SidebarWords(AnnotatedString(n.label), NAV_LABEL, if (sel) c.ink else c.text)
                        }
                    }
                }
            }
        }
    }
}

/** Where the sidebar's parts stand, from the design's `renderVals` and `dims`. */
internal data class ToonSidebarBox(
    /** The list's top: under the field, and under the recording when there is one. */
    val listTop: Float,
    /** What the foot keeps under the list: the navigation rows, the rule over them and the margins. */
    val footH: Float,
    /** A list row's width (`rw`) and a navigation row's or the recording's (`nw`). */
    val rowW: Float,
    val navW: Float,
)

internal fun toonSidebarBox(
    w: Float,
    recording: Boolean,
    navRows: Int,
): ToonSidebarBox =
    ToonSidebarBox(
        listTop = LIST_TOP + if (recording) REC_ROOM else 0f,
        footH = if (navRows > 0) NAV_BOTTOM + navRows * ToonPointerTarget.NAV_ROW + FOOT_RULE else NAV_BOTTOM,
        rowW = w - ROW_X - LIST_END,
        navW = w - 2 * ROW_X,
    )

/** The rule over the navigation rows: 8 dp above them, the design's `fy`. */
internal fun toonSidebarRuleY(
    h: Float,
    navRows: Int,
): Float = h - NAV_BOTTOM - navRows * ToonPointerTarget.NAV_ROW - 8

/**
 * The panel: the extension's `sheet` [w] by [h] dp — a paper body with the theme's shadow — and the
 * ink rule over the [navRows] navigation rows when there are any.
 */
@OptIn(InternalToonApi::class)
internal fun toonSidebarPanelScene(
    tokens: ToonTokens,
    w: Float,
    h: Float,
    navRows: Int,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val rs = sh.radiusScale
    val r =
        ToonRadii(
            PANEL_RADII.topLeft * rs,
            PANEL_RADII.topRight * rs,
            PANEL_RADII.bottomRight * rs,
            PANEL_RADII.bottomLeft * rs,
        )
    val layers =
        bodyLayers(
            shapePoints(0f, 0f, w, h, r, sh.insets, sh.wobble, sh.seed + PANEL_SEED),
            c.surface,
            c,
            shadow = sh.shadow,
        ).toMutableList()
    if (navRows > 0) {
        layers +=
            ToonLayer(
                inkLinePoints(RULE_X, w - RULE_X, toonSidebarRuleY(h, navRows), 2.6f, sh.seed + RULE_SEED),
                ToonSegments.Line,
                true,
                c.ink,
            )
    }
    return ToonScene(layers, emptyList())
}

/** A row's look: `navPlate`'s options. */
internal data class ToonSidebarRowLook(
    val selected: Boolean = false,
    val hv: Float = 0f,
    val focus: Boolean = false,
    val seed: Int = 0,
)

/**
 * The extension's `navPlate`: a row [w] by [h] dp from its top left — a `chipSel` plate when selected,
 * else the hover's plate, and the focus ring round the row.
 */
@OptIn(InternalToonApi::class)
internal fun toonSidebarRowScene(
    tokens: ToonTokens,
    w: Float,
    h: Float,
    o: ToonSidebarRowLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val layers = mutableListOf<ToonLayer>()
    if (o.selected) {
        // Its radii and wobble are absolute, not the theme's.
        layers +=
            smoothLayer(
                wobblePoints(2f, 3f, w - 4, h - 6, PLATE_RADII, SELECTED_WOBBLE, sh.seed + SELECTED_SEED + o.seed, 4f),
                c.chipSelected,
            )
    } else if (o.hv > .01f) {
        layers += toonPlateLayer(tokens, 2f, 3f, w - 4, h - 6, o.hv, seed = o.seed)
    }
    if (o.focus) layers += ringLayer(-2f, -1f, w + 4, h + 2, RING_RADII, c, sh.wobble, sh.seed + RING_SEED)
    return ToonScene(layers, emptyList())
}

/** A navigation row: its `navPlate`, and [icon] 22 dp at 12, 11 — ink over paper when selected. */
internal fun toonSidebarNavScene(
    tokens: ToonTokens,
    w: Float,
    icon: ToonIconGlyph,
    o: ToonSidebarRowLook,
): ToonScene {
    val c = tokens.colors
    val plate = toonSidebarRowScene(tokens, w, ToonPointerTarget.NAV_ROW, o)
    val glyph =
        iconLayers(
            icon,
            PAD,
            (ToonPointerTarget.NAV_ROW - ICON) / 2,
            if (o.selected) c.ink else c.text,
            size = ICON,
            fill = if (o.selected) c.surface else null,
        )
    return ToonScene(plate.layers + glyph, emptyList())
}

/**
 * The recording pinned above the list, [w] dp wide and 60 tall: the extension's `liftBody` in `danger`
 * with a 3 dp shadow, its ink thicker when [thick], and the record mark 5 dp plus 1.6 of [pulse] in
 * radius. A hover lifts the body and its mark [hv] × 2 dp; the shadow stays.
 */
@OptIn(InternalToonApi::class)
internal fun toonSidebarRecordingScene(
    tokens: ToonTokens,
    w: Float,
    pulse: Float,
    thick: Boolean,
    hv: Float = 0f,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val k = if (thick) THICK else 1f
    val ins = sh.insets.let { ToonInsets(it.top * k, it.right * k, it.bottom * k, it.left * k) }
    val body =
        ToonScene(
            bodyLayers(
                shapePoints(2f, 3f, w - 4, REC_H - 6, PLATE_RADII, ins, sh.wobble, sh.seed + REC_SEED),
                c.danger,
                c,
                shadow = REC_SHADOW,
                rim = null,
            ),
            emptyList(),
        )
    val r = 5 + 1.6f * pulse
    val g =
        shapePoints(
            20 - r,
            30 - r,
            2 * r,
            2 * r,
            ToonRadii(r, r * .95f, r, r * .9f),
            MARK_INSETS,
            .35f,
            sh.seed + MARK_SEED,
            2f,
        )
    val mark = ToonScene(listOf(smoothLayer(g.ink, c.ink), smoothLayer(g.fill, c.surface)), emptyList())
    val lift = 2 * hv
    return body.hoverLifted(lift, c.ink) + mark.hoverLifted(lift, c.ink)
}

/**
 * The recording's row: its scene with the mark at `recPulse` of the ms since it was pinned (full with
 * reduced motion), the focus ring, and its words, which ride the body when a hover lifts it.
 */
@Composable
private fun SidebarRecordingRow(
    recording: ToonSidebarRecording,
    thick: Boolean,
    onPick: () -> Unit,
    modifier: Modifier,
) {
    val tokens = Toon.tokens
    val c = tokens.colors
    val reduce = Toon.reduceMotion
    val hover = rememberToonHoverState()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    // Ms since the recording was pinned, on the frame clock, read only in the draw.
    var age by remember { mutableLongStateOf(0L) }
    LaunchedEffect(recording.id, reduce) {
        age = 0L
        if (reduce) return@LaunchedEffect
        val start = withFrameMillis { it }
        while (true) withFrameMillis { age = it - start }
    }
    val density = LocalDensity.current
    Box(modifier.sidebarPressable(selected = thick, onPick = onPick, hover = hover, interaction = interaction)) {
        Canvas(Modifier.matchParentSize()) {
            inDp {
                val w = sizeDp.width
                val pulse = if (reduce) 1f else toonRecPulse(age.toFloat())
                var scene = toonSidebarRecordingScene(tokens, w, pulse, thick, hover.value)
                if (focused) scene += toonSidebarRowScene(tokens, w, REC_H, ToonSidebarRowLook(focus = true))
                drawScene(scene)
            }
        }
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = REC_TEXT_X.dp, end = PAD.dp)
                // The words ride the body when it lifts.
                .offset {
                    val d = with(density) { (-HOVER_LIFT * hover.value).dp.roundToPx() }
                    IntOffset(d, d)
                },
            verticalArrangement = Arrangement.spacedBy(ROW_GAP.dp),
        ) {
            SidebarWords(AnnotatedString(recording.title), REC_TITLE, c.onDanger)
            val meta =
                if (recording.source.isEmpty()) recording.elapsed else "${recording.elapsed} · ${recording.source}"
            SidebarWords(AnnotatedString(meta), REC_META, c.onDanger)
        }
    }
}

/** A list or navigation row: its plate, its keys, its hover and focus, and [content] over them. */
@Composable
private fun SidebarPlateRow(
    selected: Boolean,
    seed: Int,
    onPick: () -> Unit,
    modifier: Modifier,
    icon: ToonIconGlyph? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = Toon.tokens
    val hover = rememberToonHoverState()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(modifier.sidebarPressable(selected, onPick, hover, interaction)) {
        Canvas(Modifier.matchParentSize()) {
            inDp {
                val look = ToonSidebarRowLook(selected, hover.value, focused, seed)
                val scene =
                    if (icon != null) {
                        toonSidebarNavScene(tokens, sizeDp.width, icon, look)
                    } else {
                        toonSidebarRowScene(tokens, sizeDp.width, sizeDp.height, look)
                    }
                drawScene(scene)
            }
        }
        content()
    }
}

/**
 * A row's input, as the design's rows take it: a click, Enter or Space picks it; Up and Down move the
 * keyboard focus to the row before or after; a mouse over it sets [hover].
 */
@Composable
private fun Modifier.sidebarPressable(
    selected: Boolean,
    onPick: () -> Unit,
    hover: io.github.youndie.toon.interaction.ToonHoverState,
    interaction: MutableInteractionSource,
): Modifier {
    val pick by rememberUpdatedState(onPick)
    val focusManager = LocalFocusManager.current
    return this
        .semantics(mergeDescendants = true) {
            this.selected = selected
            onClick {
                pick()
                true
            }
        }.toonHoverable(hover)
        .onPreviewKeyEvent { e ->
            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (e.key) {
                Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                    pick()
                    true
                }

                Key.DirectionDown -> {
                    focusManager.moveFocus(FocusDirection.Down)
                }

                Key.DirectionUp -> {
                    focusManager.moveFocus(FocusDirection.Up)
                }

                else -> {
                    false
                }
            }
        }.focusable(interactionSource = interaction)
        .pointerInput(Unit) { detectTapGestures(onTap = { pick() }) }
}

/** A list row's words: the title, the meta, and a result's snippet with its match on a plate. */
@Composable
private fun ColumnScope.SidebarItemWords(
    tokens: ToonTokens,
    item: ToonSidebarItem,
    selected: Boolean,
) {
    val c = tokens.colors
    val backing = Toon.fonts.bodyCyrillic
    val tc = if (selected) c.ink else c.text
    SidebarWords(AnnotatedString(item.title), ITEM_TITLE, tc)
    SidebarWords(AnnotatedString(item.meta), ITEM_META, if (selected) c.ink else c.textMuted)
    val snip = item.snippet ?: return
    val mark = accentsOf(tokens)[1]
    val text =
        buildAnnotatedString {
            append(withBacking(snip.before, backing, SNIPPET.size, SNIPPET.weight))
            withStyle(SpanStyle(fontWeight = FontWeight(HIT_WEIGHT), color = c.ink)) {
                append(withBacking(snip.hit, backing, SNIPPET.size, HIT_WEIGHT))
            }
            append(withBacking(snip.after, backing, SNIPPET.size, SNIPPET.weight))
        }
    val start = snip.before.length
    SidebarWords(text, SNIPPET, tc, highlight = mark to (start until start + snip.hit.length))
}

/** A text style of the design's HTML: weight, size, `line-height` (0 for `normal`) and spacing. */
internal class ToonSidebarText(
    val weight: Int,
    val size: Float,
    val lineHeight: Float = 0f,
    val letterSpacing: Float = 0f,
    val tabular: Boolean = false,
)

/**
 * One block of the design's words: a single line ended by an ellipsis, or up to [lines] lines. A line
 * of a set `line-height` is a box that tall with the text centred in it, as CSS centres a line; a
 * [highlight]'s range sits on a plate in its colour, the span's 3 dp corners and 2 dp spread.
 */
@Composable
private fun SidebarWords(
    text: AnnotatedString,
    style: ToonSidebarText,
    color: Color,
    lines: Int = 1,
    highlight: Pair<Color, IntRange>? = null,
) {
    val backing = Toon.fonts.bodyCyrillic
    val words = if (text.spanStyles.isEmpty()) withBacking(text.text, backing, style.size, style.weight) else text
    val base =
        TextStyle(
            fontFamily = Toon.fonts.body,
            fontWeight = FontWeight(style.weight),
            fontSize = style.size.sp,
            color = color,
            letterSpacing = style.letterSpacing.em,
            fontFeatureSettings = if (style.tabular) "tnum" else null,
        )
    if (lines > 1) {
        // A paragraph: each line `line-height` tall, the text centred in it.
        val ts =
            if (style.lineHeight > 0) {
                base.copy(
                    lineHeight = style.lineHeight.em,
                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
                )
            } else {
                base
            }
        BasicText(words, style = ts, maxLines = lines)
        return
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current
    // A line `line-height` tall with the text centred in it; `normal` is the text's own height.
    val box =
        if (style.lineHeight >
            0
        ) {
            Modifier.height(with(density) { (style.size * style.lineHeight).sp.toDp() })
        } else {
            Modifier
        }
    Box(box, contentAlignment = Alignment.CenterStart) {
        BasicText(
            words,
            Modifier
                .wrapContentHeight(unbounded = true)
                .drawBehind {
                    val (fill, range) = highlight ?: return@drawBehind
                    val l = layout ?: return@drawBehind
                    if (range.isEmpty()) return@drawBehind
                    val b = l.getPathForRange(range.first, range.last + 1).getBounds()
                    val s = with(density) { HIT_SPREAD.dp.toPx() }
                    drawRoundRect(
                        fill,
                        Offset(b.left - s, b.top - s),
                        Size(b.width + 2 * s, b.height + 2 * s),
                        CornerRadius(with(density) { (HIT_RADIUS + HIT_SPREAD).dp.toPx() }),
                    )
                },
            style = base,
            onTextLayout = { layout = it },
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** The design's numbers: the panel, the field and the rows. */
private const val MIN_W = 240f
private const val MAX_W = 360f
private const val FIELD_X = 12f
private const val FIELD_Y = 12f
private const val ROW_X = 6f
private const val LIST_TOP = 62f
private const val LIST_END = 20f
private const val LIST_BOTTOM = 12f
private const val REC_Y = 62f
private const val REC_H = 60f
private const val REC_ROOM = 66f
private const val REC_TEXT_X = 40f
private const val REC_SHADOW = 3f
private const val THICK = 1.4f
private const val NAV_BOTTOM = 10f
private const val FOOT_RULE = 18f
private const val RULE_X = 14f
private const val PAD = 12f
private const val ROW_GAP = 5f
private const val SNIPPET_ROW = 80f
private const val HEADER_TOP = 16f
private const val HEADER_BOTTOM = 6f
private const val EMPTY_X = 18f
private const val EMPTY_TOP = 22f
private const val EMPTY_GAP = 6f
private const val ICON = 22f
private const val NAV_LABEL_X = PAD + ICON + 12f
private const val NAV_SEED = 3
private const val SCROLL_OVERHANG = 2f
private const val HOVER_LIFT = 2f
private const val HIT_WEIGHT = 700
private const val HIT_RADIUS = 3f
private const val HIT_SPREAD = 2f

/** The renderers' literals: radii, wobble and seed offsets. */
private val PANEL_RADII = ToonRadii(18f, 12f, 22f, 14f)
private val PLATE_RADII = ToonRadii(14f, 10f, 16f, 11f)
private val RING_RADII = ToonRadii(16f, 12f, 18f, 13f)
private val MARK_INSETS = ToonInsets(1.4f, 1.6f, 2.2f, 1.4f)
private const val SELECTED_WOBBLE = 1.3f
private const val PANEL_SEED = 3801
private const val RULE_SEED = 3950
private const val SELECTED_SEED = 3500
private const val RING_SEED = 3510
private const val REC_SEED = 3530
private const val MARK_SEED = 3310

/** The words' styles, from the design's HTML. */
private val HEADER = ToonSidebarText(800, 11f, 1f, letterSpacing = .12f)
private val ITEM_TITLE = ToonSidebarText(700, 15f, 1.2f)
private val ITEM_META = ToonSidebarText(500, 13f, 1.2f, tabular = true)
private val SNIPPET = ToonSidebarText(500, 13f, 1.3f)
private val REC_TITLE = ToonSidebarText(800, 15f, 1.2f)
private val REC_META = ToonSidebarText(700, 13f, 1.2f, tabular = true)
private val NAV_LABEL = ToonSidebarText(700, 15f)
private val EMPTY_TITLE = ToonSidebarText(800, 15f)
private val EMPTY_TEXT = ToonSidebarText(500, 14f, 1.45f)
