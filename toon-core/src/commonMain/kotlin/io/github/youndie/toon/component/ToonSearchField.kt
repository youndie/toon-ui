package io.github.youndie.toon.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.toon.InternalToonApi
import io.github.youndie.toon.draw.ToonAnchor
import io.github.youndie.toon.draw.ToonInsets
import io.github.youndie.toon.draw.ToonLabel
import io.github.youndie.toon.draw.ToonLayer
import io.github.youndie.toon.draw.ToonRadii
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonTextRenderer
import io.github.youndie.toon.draw.ToonTextStyle
import io.github.youndie.toon.draw.bodyLayers
import io.github.youndie.toon.draw.drawScene
import io.github.youndie.toon.draw.inDp
import io.github.youndie.toon.draw.rememberToonText
import io.github.youndie.toon.draw.ringLayer
import io.github.youndie.toon.draw.shapePoints
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.icon.iconLayers
import io.github.youndie.toon.interaction.rememberToonHoverState
import io.github.youndie.toon.interaction.toonHoverable
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.type.withBacking

// A port of the desktop extension's `searchBox` and of `ToonSearchField.dc.html` (B-71, research
// §1.8): the renderer draws the box, the magnifier and the key hint's plate; the words — the
// placeholder, the value, the count and the key hint — are HTML over it there, and here a real
// `BasicTextField` and labels placed by that HTML's numbers.

/**
 * The design's search field: [value] typed into a hand-drawn box [width] wide (held to the design's
 * 160–600 dp), [placeholder] while it is empty.
 *
 * - [count] — the matches, as the caller words it ("3 / 12") — shows only beside a value.
 * - [keyHint] — the shortcut that reaches the field, which the caller binds — shows only while the
 *   field is empty and unfocused; an empty hint shows nothing.
 * - Enter calls [onNext] with 1 and Shift+Enter with -1, to step through the matches; Escape clears
 *   the field and leaves it.
 *
 * A mouse over it fills it, focus thickens its ink and rings it. The highlight of a match in the text
 * searched is not this component's.
 */
@Composable
public fun ToonSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    count: String = "",
    keyHint: String = "⌘K",
    onNext: (Int) -> Unit = {},
    width: Dp = 276.dp,
) {
    val tokens = Toon.tokens
    val text = rememberToonText()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hover = rememberToonHoverState()
    val focusManager = LocalFocusManager.current
    val w = width.value.coerceIn(MIN_W, MAX_W)
    val parts = toonSearchFieldParts(value, focused, keyHint, count)
    val backing = Toon.fonts.bodyCyrillic
    val cyrillic =
        remember(backing) {
            VisualTransformation {
                TransformedText(
                    withBacking(it.text, backing, VALUE_STYLE.size, VALUE_STYLE.weight),
                    OffsetMapping.Identity,
                )
            }
        }

    Box(modifier.size(w.dp, H.dp).toonHoverable(hover)) {
        Canvas(Modifier.fillMaxSize()) {
            // The engine draws in dp (research D5); the canvas is pixels (B-47).
            inDp {
                val look = ToonSearchFieldLook(focus = focused, hv = hover.value, keyHint = parts.keyHint)
                drawScene(
                    toonSearchFieldScene(tokens, w, look) +
                        toonSearchFieldLabels(tokens, w, keyHint, count, parts, text),
                    text,
                )
                toonSearchFieldPlaceholder(tokens, placeholder, parts, text)?.let { label ->
                    val room = parts.inputWidth(w)
                    val words = ToonScene(emptyList(), listOf(label))
                    // The design lets a long placeholder run under the hint; the value stops at the
                    // input's end, so the placeholder does too. Clipped only when it overruns, since a
                    // clip moves letter edges.
                    if (text.measure(label.text, label.style).size.width > room) {
                        clipRect(label.x, 0f, label.x + room, H) { drawScene(words, text) }
                    } else {
                        drawScene(words, text)
                    }
                }
            }
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle =
                TextStyle(
                    fontFamily = Toon.fonts.body,
                    fontWeight = FontWeight(VALUE_STYLE.weight),
                    fontSize = VALUE_STYLE.size.sp,
                    color = tokens.colors.text,
                ),
            // The pointer page draws the focused field's caret in `primary`.
            cursorBrush = SolidColor(tokens.colors.primary),
            // Typed Cyrillic is drawn in the face that backs the body face (B-35), as every label is.
            visualTransformation = cyrillic,
            interactionSource = interaction,
            modifier =
                Modifier
                    .offset(INPUT_X.dp)
                    .size((parts.inputWidth(w) + 2 * INPUT_PAD).dp, H.dp)
                    .onPreviewKeyEvent { e ->
                        if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (e.key) {
                            Key.Escape -> {
                                onValueChange("")
                                focusManager.clearFocus()
                                true
                            }

                            Key.Enter, Key.NumPadEnter -> {
                                onNext(if (e.isShiftPressed) -1 else 1)
                                true
                            }

                            else -> {
                                false
                            }
                        }
                    }.semantics {
                        // The design's `aria-label`.
                        contentDescription = placeholder
                        if (parts.count) stateDescription = count
                    },
            decorationBox = { inner ->
                Box(
                    Modifier.fillMaxHeight().padding(horizontal = INPUT_PAD.dp),
                    contentAlignment = Alignment.CenterStart,
                ) { inner() }
            },
        )
    }
}

/** What the field shows besides its box, by the design's `renderVals`. */
internal data class ToonSearchFieldParts(
    val placeholder: Boolean,
    val keyHint: Boolean,
    val count: Boolean,
    /** A count is set, shown or not: the input keeps room for it, as `iw` does. */
    val countSet: Boolean,
) {
    /** The design's `iw`: the input from 38 dp to the hint's plate, the count, or 12 dp short of the end. */
    fun inputWidth(w: Float): Float =
        maxOf(
            0f,
            w - INPUT_X -
                if (keyHint) {
                    50f
                } else if (countSet) {
                    84f
                } else {
                    12f
                },
        )
}

/**
 * The placeholder while the field is empty, the key hint while it is empty and unfocused (and the
 * hint not empty), the count only beside a value.
 */
internal fun toonSearchFieldParts(
    value: String,
    focused: Boolean,
    keyHint: String,
    count: String,
): ToonSearchFieldParts =
    ToonSearchFieldParts(
        placeholder = value.isEmpty(),
        keyHint = keyHint.isNotEmpty() && !focused && value.isEmpty(),
        count = count.isNotEmpty() && value.isNotEmpty(),
        countSet = count.isNotEmpty(),
    )

/** What one frame of the box draws: `searchBox`'s `focus` and `ring`, `hv`, and `kbdW` 34 or 0. */
internal data class ToonSearchFieldLook(
    val focus: Boolean = false,
    val hv: Float = 0f,
    val keyHint: Boolean = !focus,
)

/** The extension's `searchBox`: the box [w] dp wide and 40 tall, from its top left. */
@OptIn(InternalToonApi::class)
internal fun toonSearchFieldScene(
    tokens: ToonTokens,
    w: Float,
    o: ToonSearchFieldLook,
): ToonScene {
    val c = tokens.colors
    val sh = tokens.shape
    val rs = sh.radiusScale
    val r =
        ToonRadii(
            BOX_RADII.topLeft * rs,
            BOX_RADII.topRight * rs,
            BOX_RADII.bottomRight * rs,
            BOX_RADII.bottomLeft * rs,
        )
    val f = if (o.focus) 1f else 0f
    val layers = mutableListOf<ToonLayer>()
    if (o.focus) {
        layers +=
            ringLayer(
                -6f,
                -6f,
                w + 12,
                H + 12,
                ToonRadii(r.topLeft + 6, r.topRight + 6, r.bottomRight + 6, r.bottomLeft + 6),
                c,
                sh.wobble,
                sh.seed + 3401,
            )
    }
    val k = 1 + .5f * f
    val ins = sh.insets.let { ToonInsets(it.top * k, it.right * k, it.bottom * k, it.left * k) }
    // Hover fills the box unless it is focused; the extension switches halfway through the ramp.
    val fill = if (o.hv > .5f && f < .5f) c.surfacePressed else c.field
    layers +=
        bodyLayers(
            shapePoints(0f, 0f, w, H, r, ins, sh.wobble, sh.seed + 3400),
            fill,
            c,
            shadow = 0f,
            rim =
                c.rim?.let {
                    c.fieldRim
                        ?: it
                },
        )
    layers += iconLayers(ToonIcons.Search, 11f, 9f, c.text, size = 22f)
    if (o.keyHint) {
        // The hint's plate: its radii and wobble are absolute, not the theme's.
        layers +=
            bodyLayers(
                shapePoints(w - KBD_W - KBD_END, KBD_TOP, KBD_W, KBD_H, KBD_RADII, KBD_INSETS, .4f, sh.seed + 3402, 3f),
                c.surfacePressed,
                c,
                shadow = 0f,
                rim = null,
            )
    }
    return ToonScene(layers, emptyList())
}

/** Where a line of [style] sits centred on a box [h] dp tall from [top], as a flex row centres it. */
private fun ToonTextRenderer.centred(
    s: String,
    style: ToonTextStyle,
    top: Float,
    h: Float,
): Float = measure(s, style).let { top + (h - it.size.height) / 2 + it.firstBaseline }

/**
 * The placeholder where the design's HTML puts it: 40 dp in, where the value starts, centred on the
 * box. Null while there is a value, or no placeholder.
 */
internal fun toonSearchFieldPlaceholder(
    tokens: ToonTokens,
    placeholder: String,
    parts: ToonSearchFieldParts,
    text: ToonTextRenderer,
): ToonLabel? =
    if (!parts.placeholder || placeholder.isEmpty()) {
        null
    } else {
        ToonLabel(
            placeholder,
            INPUT_X + INPUT_PAD,
            text.centred(placeholder, VALUE_STYLE, 0f, H),
            tokens.colors.textMuted,
            1f,
            VALUE_STYLE,
            ToonAnchor.Start,
        )
    }

/**
 * The count and the key hint where the design's HTML puts them, each centred as a flex row centres a
 * line: the count 12 dp from the end of the box, the hint in the middle of its 22 dp plate.
 */
internal fun toonSearchFieldLabels(
    tokens: ToonTokens,
    w: Float,
    keyHint: String,
    count: String,
    parts: ToonSearchFieldParts,
    text: ToonTextRenderer,
): ToonScene {
    val c = tokens.colors
    val labels = mutableListOf<ToonLabel>()
    if (parts.count) {
        labels +=
            ToonLabel(
                count,
                w - COUNT_END,
                text.centred(count, COUNT_STYLE, 0f, H),
                c.textMuted,
                1f,
                COUNT_STYLE,
                ToonAnchor.End,
            )
    }
    if (parts.keyHint) {
        labels +=
            ToonLabel(
                keyHint,
                w - KBD_END - KBD_W / 2,
                text.centred(keyHint, KBD_STYLE, KBD_TOP, KBD_H),
                c.text,
                1f,
                KBD_STYLE,
                ToonAnchor.Middle,
            )
    }
    return ToonScene(emptyList(), labels)
}

/** The box: 40 dp tall (the pointer page's control), its width editor's range. */
private const val H = 40f
private const val MIN_W = 160f
private const val MAX_W = 600f
private val BOX_RADII = ToonRadii(12f, 9f, 14f, 10f)

/** The input: 38 dp in, 2 dp of padding; the value and the placeholder at 15, medium. */
internal const val INPUT_X = 38f
internal const val INPUT_PAD = 2f
private val VALUE_STYLE = ToonTextStyle(500, 15f)

/** The count: 13, semibold, figures of one width, 12 dp from the end. */
private val COUNT_STYLE = ToonTextStyle(600, 13f, tabular = true)
private const val COUNT_END = 12f

/** The key hint's plate: 34 × 22, 9 dp from the top and the end; its text 12, bold. */
private const val KBD_W = 34f
private const val KBD_H = 22f
private const val KBD_TOP = 9f
private const val KBD_END = 9f
private val KBD_RADII = ToonRadii(7f, 5f, 8f, 6f)
private val KBD_INSETS = ToonInsets(1.1f, 1.3f, 2f, 1.2f)
private val KBD_STYLE = ToonTextStyle(700, 12f)
