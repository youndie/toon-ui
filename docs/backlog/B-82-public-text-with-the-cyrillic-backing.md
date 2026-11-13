---
id: B-82
title: "A consumer's own text gets the Cyrillic backing faces"
status: done
priority: P1
size: M
stage: stage-4-product
epic: feature-components
---

# B-82 — A consumer's own text gets the Cyrillic backing faces

Issue [#31](https://github.com/youndie/toon-ui/issues/31). None of the kit's faces holds Cyrillic.
Inside the components, `withBacking` draws Cyrillic in the backing faces
(`ToonFontFamilies.displayCyrillic` / `bodyCyrillic`), but both are `internal`. So
`BasicText("Вид", style = ToonTypeScale.Headline.textStyle)` falls back to the platform font: a
thin system sans on Linux desktop, and nothing at all on wasm.

- **The decision and its reason.** A public `ToonText(text, spec, modifier, color, maxLines,
  overflow)` that runs `withBacking` over its text and draws it with the spec's style. It is the
  one path the components already use. A consumer's Russian headline then matches the kit's own
  headline. Expose the backing families on `ToonFontFamilies` as well, for a consumer that lays
  text out itself.
- Rejected: a `textStyle` whose family falls back by glyph. Compose picks a font inside a
  `FontFamily` by weight and style, not by coverage, so one family cannot hold both faces.
- Not covered: editable text. The text field already uses the backing.

- AC: `ToonText("Вид", ToonTypeScale.Headline)` renders the same pixels as the kit's own headline
  run with the same word, by a test comparing the two captures. The KDoc of `textStyle` points at
  `ToonText` for Cyrillic.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/type/`, `toon-core/api/`.

## Findings

- **Deviation from the item.** The item proposed exposing the backing families on `ToonFontFamilies`.
  The item ships `ToonTypeSpec.backed(text): AnnotatedString` instead. The families class and its
  `ToonBackingFace` are internal, with a weight mapping and a size scale per role. A consumer laying
  text out by hand needs the string those three produce, not the parts. One public function keeps
  the mapping in the kit.
- `ToonText` takes `BasicText`'s parameters (`maxLines`, `overflow`, `textAlign` and the rest), so a
  consumer's ellipsized line works too. Its colour defaults to the theme's text, through `textStyle`.
- Held by `ToonTextTest`, in all three themes:
  - Russian set with `ToonText` has the same ink as the word set in the backing face by hand, and
    differs from `textStyle` alone (the platform's fallback);
  - Latin is unchanged;
  - a mixed line backs only its Cyrillic run.
  The comparison crops each render to its ink, because a backing run inside a design-face paragraph
  can stand a pixel off a paragraph set in the backing face alone.
