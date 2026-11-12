---
id: B-71
title: "ToonSearchField: a search box with a count and a key hint"
status: open
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
