---
id: B-52
title: "The kit's theme and reduce motion reach the Screens app"
status: done
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

## Findings (2026-11-03)

- `SampleApp(look, onLook, calm, onCalm)` takes the theme and reduce motion from its caller; the
  no-argument `SampleApp()` keeps its own state for a phone on its own. In the kit both come from
  the header, and the settings' "Look" and "Calm motion" (now reduce motion, not a dead switch)
  report back to it, so the two places switch one theme.
- `SampleAppTest`: the phone under the header's Night is darker than under Day; with the screens
  given their own state again, the test fails.
