---
id: B-52
title: "The kit's theme and reduce motion reach the Screens app"
status: open
priority: P2
size: S
stage: stage-4-product
epic: feature-components
---

# B-52 — The kit's theme and reduce motion reach the Screens app

The sample's header switches the theme and reduce motion for the kit, but the Screens tab keeps its
own: `SampleApp` wraps itself in `ToonTheme(THEMES[look])` with a `look` of its own, so the header's
Day / Night / Cel and Reduce motion do not reach it (owner, 2026-11-03).

- **The decision and its reason.** `SampleApp` takes the look and reduce motion as parameters from
  `ToonUiSample`, and its settings screen's "Look" calls back into the same state, so there is one
  theme in the app, set from either place.
- AC: switching the header's theme or reduce motion changes the Screens tab; the settings' "Look"
  changes the header's too.
- Anchors: `sample/src/commonMain/kotlin/io/github/youndie/toon/sample/SampleApp.kt`,
  `sample/src/commonMain/kotlin/io/github/youndie/toon/sample/ToonUiSample.kt`.
