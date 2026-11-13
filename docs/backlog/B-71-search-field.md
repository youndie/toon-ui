---
id: B-71
title: "ToonSearchField: a search box with a count and a key hint"
status: done
priority: P1
size: M
stage: stage-5-desktop
epic: feature-components
blocked_by: [B-68, B-69]
---

# B-71 — ToonSearchField: a search box with a count and a key hint

`ToonSearchField.dc.html` (engine: `searchBox`). Props: `width` 160–600, `placeholder`, `value`,
`count`, `kbd` (default ⌘K), `state` rest | hover | focus; `onChange`, `onNext(±1)`.

- The key hint shows only while the field is empty and unfocused; the count only with a value.
  Escape clears and leaves; Enter and Shift+Enter step through matches.
- The match highlight inside text is not this component's: it belongs to the transcript turn.
- AC: rest, hover, focus, with a value and a count, match their renderer in three themes; Escape and
  Enter behave as above under a test.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonSearchField.kt`.

## Findings, 2026-11-13

- **The port** (`ToonSearchField`, `toonSearchFieldScene`): the extension's `searchBox` draws the box,
  the magnifier and the key hint's plate; the words are HTML over it in the design, and here a
  `BasicTextField` for the value and the kit's labels for the placeholder, the count and the hint,
  placed by that HTML's numbers (research §1.8): the input 38 dp in with 2 of padding, the count 12 dp
  from the end, the hint centred on its 34 × 22 plate, each line centred on its box. The input keeps
  the design's `iw`: room for the hint's plate, else for a count once one is set, else 12 dp.
- **Pointer and focus**: a mouse over the field fills it with `surface2` unless it is focused, the
  renderer switching halfway through the pointer rule's 80 ms ramp (B-69). Focus thickens the ink 1.5×
  and draws the ring, as the pointer page's rule 3 says and its focused cell draws, for focus taken by
  a click as well, since the design's page names a press on the field focus. The caret is the text
  field's own in `primary`, the colour the pointer page draws it in (its still is a 2.6 dp block; a
  drawn caret would not follow the field's own scroll).
- **Keys**: Enter calls `onNext(1)`, Shift+Enter `onNext(-1)` (the number pad's Enter too), and
  neither types; Escape clears the field through `onValueChange("")` and gives up focus. The key hint
  is a hint only: the shortcut it names is the caller's to bind, as in the design, where `kbd` is text.
- **The API**: `ToonSearchField(value, onValueChange, placeholder, modifier, count, keyHint = "⌘K",
  onNext, width = 276.dp)`. The placeholder has no default, since the design's is a sentence about the
  app it was drawn for. `width` is held to the design's 160–600 dp. The design's `state` prop forces a
  look for its canvas; here the look comes from the pointer and focus, so there is none. The count is
  read to a screen reader as the field's state, beside the placeholder as its name, the design's
  `aria-label`.
- **Beyond the design**: the count is set in figures of one width (`tabular-nums`, as its CSS asks),
  which needed `ToonTextStyle.tabular`; and a placeholder longer than the input is clipped where a
  value would stop, where the design's HTML lets it run under the hint's plate. Neither moves a frame.
- **Frames**: rest, hover, focus, focus under the pointer and a value with a count at the page's width,
  and the rest at 160 and 600 dp, in three themes — 21 cells in `desktop.json`, worst coordinate
  0.050 dp, within the engine's 0.1 dp rounding (`ToonSearchFieldTest`). Looked at on a contact sheet
  at density 2 in the three themes.
- **Not drawn**: no Chrome reference or golden holds the field's pixels, as for the sticker (B-70).

- [x] Rest, hover, focus, and a value with a count match their renderer in three themes.
- [x] Escape clears and leaves; Enter and Shift+Enter step through the matches, under a test.
- [x] The key hint shows only while the field is empty and unfocused, the count only with a value.
