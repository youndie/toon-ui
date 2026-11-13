package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonInsets
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.ringLayer
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.icon.ToonIconGlyph
import io.github.youndie.toon.icon.ToonIconWeight
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.icon.iconLayers
import io.github.youndie.toon.interaction.ToonPointerTarget
import io.github.youndie.toon.interaction.rememberToonHoverState
import io.github.youndie.toon.interaction.rememberToonPressState
import io.github.youndie.toon.interaction.toonHoverable
import io.github.youndie.toon.interaction.toonPlateLayer
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.type.withBacking
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// A port of `ToonTranscriptTurn.dc.html` (B-78, research §1.8): one turn of a transcript — the
// speaker's sticker, name and time over the words said — and of the extension's `plate` and
// `iconBtn32` it draws with. The reading area is still: no idle, no tilt, no FX over the text, and the
// hover plate is instant (the design's README: in the reading zone the plate is always instant). The
// words are HTML over the drawing in the design; here the header's are laid out by its flex row's
// numbers and the paragraphs are real, wrapping, selectable text.

/** What a [ToonTranscriptTurn] asks its caller to do: the design's `onAction`. */
public enum class ToonTurnAction {
    /** Play the recording from this turn. */
    Play,

    /** Copy the turn's text. */
    Copy,

    /** Give the turn to another speaker. */
    Reassign,

    /** Rename the turn's speaker. */
    Rename,

    /** Name an unnamed voice. */
    Name,
}

/** The words of a [ToonTranscriptTurn]'s actions, its menu and its naming link; English by default. */
@Stable
public class ToonTranscriptTurnLabels(
    public val play: String = "Play from here",
    public val copy: String = "Copy",
    public val more: String = "More",
    public val nameVoice: String = "Name this voice",
    public val reassign: String = "Reassign turn",
    public val rename: String = "Rename speaker",
)

/**
 * One turn of a transcript: [speaker]'s sticker ([letter] on [shape] in [color], or [kind]'s capsule
 * or dashed outline), name and [time] over [paragraphs] at most 640 dp wide, 16/1.6.
 *
 * - [playing] marks the turn being played: its time in bold and a wave beside it.
 * - [mark] highlights each match in the text, to the end of the word it starts, in an accent; the
 *   match numbered [currentMark] (counted through the turn's paragraphs) is in `primary` with an ink
 *   outline, and the turn holding it lies on its plate. -1: this turn holds no current match.
 * - A mouse over the turn lays its plate under it, at once, and shows three actions: play (disabled
 *   without [audio]), copy and more, which opens the menu. Keyboard focus rings it. Every action is
 *   on the keyboard too — Space plays, ⌘C or Ctrl+C copies, Shift+F10 or the menu key opens the menu,
 *   Escape closes it, the arrows move to the turn above or below — and in the context menu.
 * - An unnamed voice carries a link to name it, and its menu names it where a named one renames.
 *
 * The text's selection is drawn in `chipSelected`, never mistaken for a match; wrap the list in a
 * `SelectionContainer` to let it be selected. [onAction] reports what the reader asked for.
 */
@Composable
public fun ToonTranscriptTurn(
    speaker: String,
    paragraphs: List<String>,
    modifier: Modifier = Modifier,
    letter: String = speaker.take(1),
    kind: ToonSpeakerKind = ToonSpeakerKind.Named,
    shape: Int = 0,
    color: Int = 0,
    time: String = "",
    playing: Boolean = false,
    audio: Boolean = true,
    mark: String = "",
    currentMark: Int = -1,
    onAction: (ToonTurnAction) -> Unit = {},
    labels: ToonTranscriptTurnLabels = ToonTranscriptTurnLabels(),
    seed: Int = 0,
) {
    val tokens = Toon.tokens
    val c = tokens.colors
    val reduce = Toon.reduceMotion
    val text = rememberToonText()
    val fire by rememberUpdatedState(onAction)
    val focusManager = LocalFocusManager.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val rootFocus = remember { FocusRequester() }
    val menuFocus = remember { FocusRequester() }
    var hovered by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var menuShown by remember { mutableStateOf(false) }
    var menuByKeys by remember { mutableStateOf(false) }
    val found = currentMark >= 0
    val vis = toonTurnVisibility(hovered, focused, menu, playing, found)
    val geometry = remember { ToonTurnGeometry() }
    val menuActions = toonTurnMenuActions(kind, audio)

    fun act(a: ToonTurnAction) {
        if (a == ToonTurnAction.Play && !audio) return
        fire(a)
    }

    fun openMenu(byKeys: Boolean) {
        menuByKeys = byKeys
        menu = true
    }

    LaunchedEffect(menu) {
        if (menu) {
            menuShown = true
        } else if (menuShown) {
            // Focus comes back to the turn the keys opened the menu from.
            if (menuByKeys) rootFocus.requestFocus()
            // The kit's menu rolls up in 160 ms before it goes.
            if (!reduce) delay(MENU_ROLL_UP_MS)
            menuShown = false
        }
    }

    val marks = remember(paragraphs, mark, currentMark) { toonTurnMarks(paragraphs, mark, currentMark) }
    val back =
        Modifier.drawWithCache {
            val look =
                ToonTurnLook(
                    w = size.width / density,
                    h = size.height / density,
                    kind = kind,
                    shape = shape,
                    color = color,
                    letter = letter,
                    seed = seed,
                    plate = vis.plate,
                    ring = vis.ring,
                    micX = geometry.micX,
                    waveX = geometry.waveX,
                )
            val scene = toonTranscriptTurnScene(tokens, look)
            val link = geometry.link?.let { toonWavyUnderline(it.x, it.y, it.w) }
            onDrawBehind {
                inDp {
                    drawScene(scene, text)
                    if (link != null) {
                        drawPath(link, c.primary, style = Stroke(width = LINK_UNDERLINE))
                    }
                }
            }
        }

    Box(modifier) {
        Layout(
            content = {
                Words(
                    speaker,
                    NAME_STYLE,
                    if (kind ==
                        ToonSpeakerKind.Unnamed
                    ) {
                        c.textMuted
                    } else {
                        c.text
                    },
                    Modifier.layoutId(ID_NAME),
                )
                Words(
                    time,
                    if (playing) TIME_PLAYING_STYLE else TIME_STYLE,
                    if (playing) c.text else c.textMuted,
                    Modifier.layoutId(ID_TIME),
                )
                if (kind == ToonSpeakerKind.Unnamed) {
                    Words(
                        labels.nameVoice,
                        LINK_STYLE,
                        c.text,
                        Modifier
                            .layoutId(ID_LINK)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .semantics {
                                role = Role.Button
                                onClick {
                                    act(ToonTurnAction.Name)
                                    true
                                }
                            }.pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown()
                                    if (waitForUpOrCancellation() != null) act(ToonTurnAction.Name)
                                }
                            },
                    )
                }
                CompositionLocalProvider(
                    LocalTextSelectionColors provides TextSelectionColors(c.primary, c.chipSelected),
                ) {
                    Column(Modifier.layoutId(ID_TEXT), verticalArrangement = Arrangement.spacedBy(PARA_GAP.dp)) {
                        paragraphs.forEachIndexed { i, p ->
                            TurnParagraph(p, marks.filter { it.paragraph == i })
                        }
                    }
                }
                if (vis.actions) {
                    Row(Modifier.layoutId(ID_ACTIONS), horizontalArrangement = Arrangement.spacedBy(ACTION_GAP.dp)) {
                        TurnAction(ToonIcons.Play, labels.play, 0, enabled = audio) { act(ToonTurnAction.Play) }
                        TurnAction(ToonIcons.Copy, labels.copy, 1) { act(ToonTurnAction.Copy) }
                        TurnAction(ToonIcons.More, labels.more, 2) {
                            if (menu) menu = false else openMenu(byKeys = false)
                        }
                    }
                }
            },
            modifier =
                back
                    .semantics {
                        contentDescription = if (time.isEmpty()) speaker else "$speaker, $time"
                        customActions =
                            menuActions.map { a ->
                                CustomAccessibilityAction(labelOf(a, labels)) {
                                    act(a)
                                    true
                                }
                            }
                    }.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val e = awaitPointerEvent()
                                val mouse = e.changes.any { it.type == PointerType.Mouse }
                                when (e.type) {
                                    PointerEventType.Enter, PointerEventType.Move -> {
                                        if (mouse) hovered = true
                                    }

                                    PointerEventType.Exit -> {
                                        hovered = false
                                    }

                                    PointerEventType.Press -> {
                                        if (mouse &&
                                            e.buttons.isSecondaryPressed
                                        ) {
                                            openMenu(byKeys = false)
                                        }
                                    }
                                }
                            }
                        }
                    }.onKeyEvent { e ->
                        if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                        when {
                            e.key == Key.Menu || (e.isShiftPressed && e.key == Key.F10) -> {
                                openMenu(byKeys = true)
                                true
                            }

                            e.key == Key.Escape && menu -> {
                                menu = false
                                true
                            }

                            e.key == Key.Spacebar -> {
                                act(ToonTurnAction.Play)
                                true
                            }

                            e.key == Key.DirectionDown -> {
                                focusManager.moveFocus(FocusDirection.Down)
                            }

                            e.key == Key.DirectionUp -> {
                                focusManager.moveFocus(FocusDirection.Up)
                            }

                            e.key == Key.C && (e.isMetaPressed || e.isCtrlPressed) -> {
                                act(ToonTurnAction.Copy)
                                true
                            }

                            else -> {
                                false
                            }
                        }
                    }.focusRequester(rootFocus)
                    .focusable(interactionSource = interaction),
        ) { measurables, constraints ->
            val byId = measurables.associateBy { it.layoutId }
            val w =
                if (constraints.hasBoundedWidth) {
                    constraints.maxWidth
                } else {
                    (PAD_START + TEXT_MAX + PAD_END)
                        .dp
                        .roundToPx()
                }
            val wDp = w / density
            val contentW = max(0f, wDp - PAD_START - PAD_END)
            val loose = constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Int.MAX_VALUE)
            val time = byId.getValue(ID_TIME).measure(loose.copy(maxWidth = Int.MAX_VALUE))
            val link = byId[ID_LINK]?.measure(loose.copy(maxWidth = Int.MAX_VALUE))
            val actions = byId[ID_ACTIONS]?.measure(loose.copy(maxWidth = Int.MAX_VALUE))
            // The header's room: the column, less the actions' while they show (`headPad`).
            val head = contentW - if (vis.actions) HEAD_PAD else 0f
            val row =
                toonTurnHeaderRow(
                    timeW = time.width / density,
                    linkW = link?.let { it.width / density },
                    me = kind == ToonSpeakerKind.Me,
                    playing = playing,
                )
            val name =
                byId.getValue(ID_NAME).measure(
                    loose.copy(maxWidth = max(0f, head - row.after).dp.roundToPx()),
                )
            val nameW = name.width / density
            val textW = min(TEXT_MAX, contentW)
            val body = byId.getValue(ID_TEXT).measure(loose.copy(maxWidth = textW.dp.roundToPx()))
            val headH = maxOf(HEAD_MIN, name.height / density, time.height / density, (link?.height ?: 0) / density)
            val textTop = PAD_TOP + headH + HEAD_GAP
            val h = textTop + body.height / density + PAD_BOTTOM
            val x0 = PAD_START + nameW
            geometry.micX = row.micX?.let { x0 + it }
            geometry.waveX = row.waveX?.let { x0 + it }
            geometry.link =
                if (link == null || row.linkX == null) {
                    null
                } else {
                    ToonTurnLink(
                        x0 + row.linkX,
                        PAD_TOP + (headH - link.height / density) / 2 + link[FirstBaseline] / density,
                        link.width / density,
                    )
                }

            fun centred(p: Placeable) = (PAD_TOP + (headH - p.height / density) / 2).dp.roundToPx()
            layout(w, h.dp.roundToPx()) {
                name.place(PAD_START.dp.roundToPx(), centred(name))
                time.place((x0 + row.timeX).dp.roundToPx(), centred(time))
                if (link != null && row.linkX != null) link.place((x0 + row.linkX).dp.roundToPx(), centred(link))
                body.place(PAD_START.dp.roundToPx(), textTop.dp.roundToPx())
                actions?.place((wDp - ACTIONS_END).dp.roundToPx() - actions.width, ACTIONS_TOP.dp.roundToPx())
            }
        }
        if (menuShown) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = with(LocalDensity.current) { IntOffset(-MENU_END.dp.roundToPx(), MENU_TOP.dp.roundToPx()) },
                onDismissRequest = { menu = false },
                properties = PopupProperties(focusable = menu),
            ) {
                ToonMenu(
                    items = menuActions.map { ToonMenuItem(labelOf(it, labels), iconOf(it)) },
                    modifier =
                        Modifier.focusRequester(menuFocus).onKeyEvent { e ->
                            if (e.type == KeyEventType.KeyDown && e.key == Key.Escape) {
                                menu = false
                                true
                            } else {
                                false
                            }
                        },
                    open = menu,
                    width = MENU_W.dp,
                    onSelect = { i ->
                        menu = false
                        act(menuActions[i])
                    },
                )
                LaunchedEffect(menu) {
                    if (menu && menuByKeys) menuFocus.requestFocus()
                }
            }
        }
    }
}

/** One of the header's lines, ending in an ellipsis where it must. */
@Composable
private fun Words(
    s: String,
    style: TurnTextStyle,
    color: Color,
    modifier: Modifier,
) {
    val backing = Toon.fonts.bodyCyrillic
    BasicText(
        withBacking(s, backing, style.size, style.weight),
        modifier,
        TextStyle(
            fontFamily = Toon.fonts.body,
            fontWeight = FontWeight(style.weight),
            fontSize = style.size.sp,
            color = color,
            fontFeatureSettings = if (style.tabular) "tnum" else null,
        ),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        softWrap = false,
    )
}

/** A paragraph of the turn, 16/1.6, its [marks] highlighted behind the words. */
@Composable
private fun TurnParagraph(
    s: String,
    marks: List<ToonTurnMark>,
) {
    val tokens = Toon.tokens
    val c = tokens.colors
    val backing = Toon.fonts.bodyCyrillic
    val style =
        TextStyle(
            fontFamily = Toon.fonts.body,
            fontWeight = FontWeight(PARA_WEIGHT),
            fontSize = PARA_SIZE.sp,
            color = c.text,
            // CSS's half-leading: the line's extra height shared above and below the face.
            lineHeight = (PARA_SIZE * PARA_LINE).sp,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        )
    val words =
        remember(s, marks, backing, c) {
            val base = withBacking(s, backing, PARA_SIZE, PARA_WEIGHT)
            if (marks.isEmpty()) {
                base
            } else {
                AnnotatedString
                    .Builder(base)
                    .apply {
                        marks.forEach { addStyle(SpanStyle(color = c.ink), it.start, it.end) }
                    }.toAnnotatedString()
            }
        }
    // A span's background covers its face's ascent and descent, not the line: the face, measured alone.
    val measurer = rememberTextMeasurer()
    val face = remember(measurer, style) { measurer.measure("Ag", style.copy(lineHeight = TextUnit.Unspecified)) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val accent = accentsOf(tokens)[1]
    BasicText(
        words,
        Modifier.widthIn(max = TEXT_MAX.dp).drawBehind {
            val l = layout ?: return@drawBehind
            val asc = face.firstBaseline
            val desc = face.size.height - face.firstBaseline
            for (m in marks) {
                for (r in toonTurnMarkRects(l, m.start, m.end, asc, desc)) {
                    if (m.current) {
                        val o = (MARK_RING + MARK_SPREAD).dp.toPx()
                        drawRoundRect(
                            c.ink,
                            r.first - Offset(o, o),
                            Size(
                                r.second.width + 2 * o,
                                r.second.height + 2 * o,
                            ),
                            CornerRadius((MARK_RADIUS + MARK_RING + MARK_SPREAD).dp.toPx()),
                        )
                    }
                    val o = MARK_SPREAD.dp.toPx()
                    drawRoundRect(
                        if (m.current) c.primary else accent,
                        r.first - Offset(o, o),
                        Size(r.second.width + 2 * o, r.second.height + 2 * o),
                        CornerRadius((MARK_RADIUS + MARK_SPREAD).dp.toPx()),
                    )
                }
            }
        },
        style,
        onTextLayout = { layout = it },
    )
}

/** Each line's piece of the range [start] to [end] in [l]: its top left and size, the face's height. */
internal fun toonTurnMarkRects(
    l: TextLayoutResult,
    start: Int,
    end: Int,
    ascent: Float,
    descent: Float,
): List<Pair<Offset, Size>> {
    if (end <= start) return emptyList()
    val out = mutableListOf<Pair<Offset, Size>>()
    for (line in l.getLineForOffset(start)..l.getLineForOffset(end - 1)) {
        val a = max(start, l.getLineStart(line))
        val b = min(end, l.getLineEnd(line, visibleEnd = true))
        if (b <= a) continue
        val x0 = l.getHorizontalPosition(a, true)
        val x1 = l.getBoundingBox(b - 1).right
        val base = l.getLineBaseline(line)
        out += Offset(x0, base - ascent) to Size(x1 - x0, ascent + descent)
    }
    return out
}

/** One of the three actions: the extension's `iconBtn32`, lifted by a mouse over it, squashed by a press. */
@Composable
private fun TurnAction(
    icon: ToonIconGlyph,
    label: String,
    seed: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val tokens = Toon.tokens
    val hover = rememberToonHoverState()
    val press = rememberToonPressState()
    val click by rememberUpdatedState(onClick)
    Canvas(
        Modifier
            .size(ToonPointerTarget.ICON.dp)
            .semantics {
                role = Role.Button
                contentDescription = label
                onClick {
                    if (enabled) click()
                    enabled
                }
                if (!enabled) disabled()
            }.toonHoverable(hover, enabled)
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .pointerInput(enabled) {
                awaitEachGesture {
                    awaitFirstDown()
                    if (!enabled) return@awaitEachGesture
                    press.press()
                    val up = waitForUpOrCancellation()
                    if (press.release() && up != null) click()
                }
            },
    ) {
        inDp {
            drawScene(
                toonIconButton32Scene(
                    tokens,
                    ToonIconButton32Look(icon, hv = hover.value, p = press.progress, disabled = !enabled, seed = seed),
                ),
            )
        }
    }
}

/** What shows, by the design's `vis()`: the plate, the focus ring and the actions. */
internal data class ToonTurnVisibility(
    val plate: Boolean,
    val ring: Boolean,
    val actions: Boolean,
)

/**
 * The design's `vis()`, with its `state` read from the turn: [playing] is its `playing`, a turn
 * holding the current match ([found]) its `found`. The plate shows under a mouse, on focus, with the
 * menu open, and on a playing or found turn; the ring on focus without the menu; the actions under a
 * mouse, or on focus and with the menu unless the turn plays or is found.
 */
internal fun toonTurnVisibility(
    hovered: Boolean,
    focused: Boolean,
    menu: Boolean,
    playing: Boolean,
    found: Boolean,
): ToonTurnVisibility {
    val hv = hovered || playing || found
    return ToonTurnVisibility(
        plate = hv || focused || menu,
        ring = focused && !menu,
        actions = ((hv || focused || menu) && !playing && !found) || hovered,
    )
}

/** The menu's actions: play while there is audio, copy, reassign, and rename a named speaker or name a voice. */
internal fun toonTurnMenuActions(
    kind: ToonSpeakerKind,
    audio: Boolean,
): List<ToonTurnAction> =
    listOfNotNull(
        ToonTurnAction.Play.takeIf { audio },
        ToonTurnAction.Copy,
        ToonTurnAction.Reassign,
        if (kind == ToonSpeakerKind.Unnamed) ToonTurnAction.Name else ToonTurnAction.Rename,
    )

private fun labelOf(
    a: ToonTurnAction,
    l: ToonTranscriptTurnLabels,
): String =
    when (a) {
        ToonTurnAction.Play -> l.play
        ToonTurnAction.Copy -> l.copy
        ToonTurnAction.Reassign -> l.reassign
        ToonTurnAction.Rename -> l.rename
        ToonTurnAction.Name -> l.nameVoice
    }

/** The menu's icons, the design's `play`, `copy`, `person` and `edit`. */
private fun iconOf(a: ToonTurnAction): ToonIconGlyph =
    when (a) {
        ToonTurnAction.Play -> ToonIcons.Play
        ToonTurnAction.Copy -> ToonIcons.Copy
        ToonTurnAction.Reassign -> ToonIcons.Person
        ToonTurnAction.Rename, ToonTurnAction.Name -> ToonIcons.Edit
    }

/** A match of the turn's search: in [paragraph], from [start] to [end], and whether it is the [current] one. */
internal data class ToonTurnMark(
    val paragraph: Int,
    val start: Int,
    val end: Int,
    val current: Boolean,
)

/**
 * The design's highlighting: each place [mark] occurs in [paragraphs], ignoring case, up to the end of
 * the word it starts in (a letter, a figure or a hyphen continues it). Matches are numbered through
 * the paragraphs; the one numbered [current] is current. A match covers at least the query, which the
 * design's loop does not hold for a query that ends in the middle of a word or starts with a space.
 */
internal fun toonTurnMarks(
    paragraphs: List<String>,
    mark: String,
    current: Int,
): List<ToonTurnMark> {
    if (mark.isEmpty()) return emptyList()
    val out = mutableListOf<ToonTurnMark>()
    paragraphs.forEachIndexed { p, tx ->
        var i = 0
        while (true) {
            val j = tx.indexOf(mark, i, ignoreCase = true)
            if (j < 0) break
            var e = j
            while (e < tx.length && tx[e].continuesWord()) e++
            val end = max(e, j + mark.length)
            out += ToonTurnMark(p, j, end, out.size == current)
            i = end
        }
    }
    return out
}

/** The design's `[\p{L}\p{N}-]`. */
private fun Char.continuesWord(): Boolean =
    isLetter() || this == '-' ||
        category == CharCategory.DECIMAL_DIGIT_NUMBER ||
        category == CharCategory.LETTER_NUMBER ||
        category == CharCategory.OTHER_NUMBER

/** Where the header's pieces stand after the name, as offsets from its end, and their room ([after]). */
internal data class ToonTurnHeaderRow(
    val micX: Float?,
    val waveX: Float?,
    val timeX: Float,
    val linkX: Float?,
    val after: Float,
)

/**
 * The design's header row, a flex row 10 dp apart: the name; the mic of the user's own turn, pulled
 * 4 dp back; the playing turn's wave; the time; the unnamed voice's link. Offsets from the name's end.
 */
internal fun toonTurnHeaderRow(
    timeW: Float,
    linkW: Float?,
    me: Boolean,
    playing: Boolean,
): ToonTurnHeaderRow {
    var x = HEAD_GAP_X
    var mic: Float? = null
    var wave: Float? = null
    if (me) {
        x += MIC_PULL
        mic = x
        x += MIC + HEAD_GAP_X
    }
    if (playing) {
        wave = x
        x += WAVE + HEAD_GAP_X
    }
    val timeX = x
    x += timeW
    var link: Float? = null
    if (linkW != null) {
        x += HEAD_GAP_X
        link = x
        x += linkW
    }
    return ToonTurnHeaderRow(mic, wave, timeX, link, x)
}

/** Where the drawn pieces of the header stand, written by the layout and read by the drawing. */
@Stable
internal class ToonTurnGeometry {
    var micX: Float? by mutableStateOf(null)
    var waveX: Float? by mutableStateOf(null)
    var link: ToonTurnLink? by mutableStateOf(null)
}

/** The naming link's line: from [x], [w] wide, its baseline at [y]. */
internal data class ToonTurnLink(
    val x: Float,
    val y: Float,
    val w: Float,
)

/** The link's wavy underline in `primary`, 2 dp thick, 5 dp under the baseline, as Chrome draws a `wavy` decoration. */
private fun toonWavyUnderline(
    x: Float,
    baseline: Float,
    w: Float,
): Path =
    Path().apply {
        val y = baseline + LINK_OFFSET
        val n = max(1, (w / LINK_WAVE * 8).roundToInt())
        moveTo(x, y)
        for (i in 1..n) {
            val t = w * i / n
            lineTo(x + t, y + LINK_AMP * sin(2 * PI.toFloat() * t / LINK_WAVE))
        }
    }

/** What one frame of a turn draws: the design's `paint()` with the header's layout. */
internal data class ToonTurnLook(
    val w: Float,
    val h: Float,
    val kind: ToonSpeakerKind = ToonSpeakerKind.Named,
    val shape: Int = 0,
    val color: Int = 0,
    val letter: String = "",
    val seed: Int = 0,
    val plate: Boolean = false,
    val ring: Boolean = false,
    val micX: Float? = null,
    val waveX: Float? = null,
    /** The three actions, at the turn's top end; null while they are hidden. */
    val actions: List<ToonIconButton32Look>? = null,
)

/**
 * The design's drawing of a turn [ToonTurnLook.w] by [ToonTurnLook.h] dp, from its top left: the
 * plate and the focus ring in the turn's own box, the 32 dp sticker 12 dp in and 10 down, the mic and
 * the wave in the 32 dp header row, and the actions 10 dp from the top and the end, 6 apart. No words.
 */
@OptIn(InternalToonApi::class)
internal fun toonTranscriptTurnScene(
    tokens: ToonTokens,
    o: ToonTurnLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val back = mutableListOf<ToonLayer>()
    if (o.plate) back += toonPlateLayer(tokens, 0f, 2f, o.w, o.h - 4, 1f, seed = o.seed % 5)
    if (o.ring) back += ringLayer(-5f, -3f, o.w + 10, o.h + 6, RING_RADII, c, sh.wobble, sh.seed + 3520)
    var scene =
        ToonScene(back, emptyList()) +
            toonSpeakerStickerScene(
                tokens,
                STICKER,
                o.kind,
                o.shape,
                o.color,
                o.letter,
            ).placed(PAD_START - GUTTER, PAD_TOP)
    o.micX?.let {
        scene +=
            ToonScene(iconLayers(ToonIcons.Mic, 0f, 0f, c.textMuted, size = MIC), emptyList()).placed(
                it,
                PAD_TOP + (HEAD_MIN - MIC) / 2,
            )
    }
    o.waveX?.let {
        scene +=
            ToonScene(
                iconLayers(ToonIcons.Wave, 0f, 0f, c.text, size = WAVE, weight = ToonIconWeight.Bold),
                emptyList(),
            ).placed(it, PAD_TOP + (HEAD_MIN - WAVE) / 2)
    }
    o.actions?.forEachIndexed { i, a ->
        val x =
            o.w - ACTIONS_END - 3 * ToonPointerTarget.ICON - 2 * ACTION_GAP + i * (ToonPointerTarget.ICON + ACTION_GAP)
        scene += toonIconButton32Scene(tokens, a).placed(x, ACTIONS_TOP)
    }
    return scene
}

/** What one frame of an `iconBtn32` draws: its icon, hover, press, whether it is disabled, its seed. */
internal data class ToonIconButton32Look(
    val icon: ToonIconGlyph,
    val hv: Float = 0f,
    val p: Float = 0f,
    val disabled: Boolean = false,
    val seed: Int = 0,
)

/**
 * The extension's `iconBtn32`, standard: a 32 dp paper button whose press squashes it and drops its
 * 2.5 dp shadow, whose hover lifts the body (the shadow stays), its 20 dp icon riding the body. A
 * disabled one is flat, grey and faint, and does not lift.
 */
@OptIn(InternalToonApi::class)
internal fun toonIconButton32Scene(
    tokens: ToonTokens,
    o: ToonIconButton32Look,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val d = ToonPointerTarget.ICON
    val p = o.p.coerceIn(0f, 1f)
    val bw = d * (1 + .12f * p)
    val bh = d * (1 - .15f * p)
    val x = (d - bw) / 2
    val y = d - bh
    val dis = o.disabled
    val ins = sh.insets.let { ToonInsets(it.top * .7f, it.right * .7f, it.bottom * .7f, it.left * .7f) }
    val g = shapePoints(x, y, bw, bh, BUTTON_RADII, ins, sh.wobble * .7f, sh.seed + 3200 + o.seed, 3f)
    val inkAlpha = if (dis) .35f else 1f
    val shadow = if (dis) 0f else BUTTON_SHADOW * (1 - p)
    val lift = if (dis) 0f else 2 * o.hv * (1 - p)
    val layers = mutableListOf<ToonLayer>()
    if (shadow > .05f) layers += bodyLayers(g, c.ink, c, shadow = shadow, inkAlpha = inkAlpha, rim = null).first()
    val body =
        bodyLayers(
            g,
            if (dis) c.disabled else c.surface,
            c,
            shadow = 0f,
            inkAlpha = inkAlpha,
            rim = if (dis) null else c.rim,
        )
    layers += if (lift != 0f) body.map { it.moved(-lift, -lift) } else body
    layers +=
        iconLayers(
            o.icon,
            6 - lift,
            6 + (d - bh) - lift + 1.2f * p,
            c.text,
            size = BUTTON_ICON,
            alpha = if (dis) .4f else 1f,
        )
    return ToonScene(layers, emptyList())
}

/** A header word's face: its weight and size, and whether its figures are of one width. */
private class TurnTextStyle(
    val weight: Int,
    val size: Float,
    val tabular: Boolean = false,
)

/** The header's words: the name 15 bold, the time 13 medium (extra-bold while playing), the link 14 bold. */
private val NAME_STYLE = TurnTextStyle(700, 15f)
private val TIME_STYLE = TurnTextStyle(500, 13f, tabular = true)
private val TIME_PLAYING_STYLE = TurnTextStyle(800, 13f, tabular = true)
private val LINK_STYLE = TurnTextStyle(700, 14f)

/** The paragraphs: 16, regular, 1.6 apart, 4 dp between them, at most the extension's `textMax`. */
private const val PARA_SIZE = 16f
private const val PARA_WEIGHT = 400
private const val PARA_LINE = 1.6f
private const val PARA_GAP = 4f
internal const val TEXT_MAX = 640f

/** The box: `padding: 10px 12px 12px 12px`, the sticker's 44 dp column (the extension's `gutter`). */
internal const val PAD_TOP = 10f
private const val PAD_END = 12f
private const val PAD_BOTTOM = 12f
internal const val GUTTER = 44f
internal const val PAD_START = 12f + GUTTER

/** The header row: at least 32 dp, 10 dp apart, 4 dp above the text; 120 dp kept for the actions. */
internal const val HEAD_MIN = 32f
private const val HEAD_GAP_X = 10f
private const val HEAD_GAP = 4f
private const val HEAD_PAD = 120f
private const val MIC = 16f
private const val MIC_PULL = -4f
private const val WAVE = 18f
private const val STICKER = 32f

/** The actions: 10 dp from the top and the end, 6 apart. */
private const val ACTIONS_TOP = 10f
private const val ACTIONS_END = 10f
private const val ACTION_GAP = 6f

/** `iconBtn32`: its radii (not the theme's), its shadow and its 20 dp icon. */
private val BUTTON_RADII = ToonRadii(13f, 11f, 14f, 10f)
private const val BUTTON_SHADOW = 2.5f
private const val BUTTON_ICON = 20f

/** The focus ring's radii. */
private val RING_RADII = ToonRadii(16f, 12f, 18f, 13f)

/** A match: its 3 dp corners, the 2 dp its colour spreads, the current one's 2 dp of ink past that. */
private const val MARK_RADIUS = 3f
private const val MARK_SPREAD = 2f
private const val MARK_RING = 2f

/** The link's underline: 2 dp thick, 5 dp under the baseline; the wave's length and height. */
private const val LINK_UNDERLINE = 2f
private const val LINK_OFFSET = 5f
private const val LINK_WAVE = 8f
private const val LINK_AMP = 1.5f

/** The menu: 272 dp wide, 4 dp from the end and 46 from the top; it rolls up in 160 ms. */
private const val MENU_W = 272f
private const val MENU_END = 4f
private const val MENU_TOP = 46f
private const val MENU_ROLL_UP_MS = 160L

private const val ID_NAME = "name"
private const val ID_TIME = "time"
private const val ID_LINK = "link"
private const val ID_TEXT = "text"
private const val ID_ACTIONS = "actions"
