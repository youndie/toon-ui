---
id: B-97
title: "ToonDialog: a one-button form for a choice that applies at once"
status: open
priority: P1
size: S
stage: stage-4-product
epic: feature-components
blocked_by: [B-24]
---

# B-97 — ToonDialog: a one-button form for a choice that applies at once

Issue [#70](https://github.com/youndie/toon-ui/issues/70), from pyatiletka on toon-core `0.1.0.43`. `ToonDialog` always
draws `cancel` and `confirm`, and both strings are required. The board's appearance dialog applies each choice at once,
so it needs one button, «Готово»; today it shows «Закрыть» beside it, both closing the dialog, and an empty `cancel`
draws an empty button.

- **The decision** is the kit's. The issue's candidates: `cancel: String?`, where `null` leaves the cancel button out and
  the confirm button takes its place, Escape and a tap outside still calling `onCancel`; or a separate acknowledgement form.
- **Why before the release.** It is the shape of a public signature: on Central every version keeps the one it shipped
  with, so it is settled before 0.1.0 ([B-96](B-96-release-0-1-0-on-central.md)), and the ABI dump records it.

- AC: a dialog with one button draws one, in every theme, with its goldens; Escape and a tap outside still dismiss; the
  two-button form does not move; #70 is closed by the pull request.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonDialog.kt`
