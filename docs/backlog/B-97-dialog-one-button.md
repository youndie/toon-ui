---
id: B-97
title: "ToonDialog: a one-button form for a choice that applies at once"
status: done
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

## Findings (2026-11-25)

- **Built: `cancel: String?`.** `null` draws no cancel button; `confirm` stays where it stands beside one, the dialog's
  right, so a one-button dialog and a two-button one put their main button in one place. A tap outside and Escape still
  call `onCancel`; the 110 dp the cancel area held left of the confirm button is the card's and does nothing
  (`toonDialogHit(…, oneButton = true)`). Tab and the arrows keep the focus on the one button; with content, Tab goes
  from it to the slot and round. A reader is offered the one action. Chosen over a separate acknowledgement form: one
  dialog keeps one iris, one pop and one focus path, and a caller writing `cancel = null` says what it means.
- **Designed here (D13)**: the design draws no one-button dialog. No number is new: the confirm button keeps its 128 dp
  and its place, the card its 300 dp.
- **The ABI.** The JVM signature does not change (`String` either way); the klib's does (`kotlin.String?`), a rebuild
  for a klib compiled against 0.1.0.43. A call that passes a cancel word reads as before.
- **Goldens** `Dialog/OneButton_InkGouacheLight`, `…Night`, `…BrightCel`: the board's «Вид» — two radio groups in the
  slot, «Готово» alone — on each theme's stage; looked at. No other golden moved.
- **Mutants**, each caught: the cancel area kept with one button (the hit test, and the tap where cancel stood), the
  one-button keyboard branch removed (Tab moved the focus to a cancel that is not there, and the slot's walk), the
  reader offered an empty cancel action, the cancel never drawn even with a word (the card test). A cancel drawn with
  the word «Закрыть» when `null` turns the three `OneButton` goldens red; one drawn with an empty word draws nothing (a
  text button has no body), so the hit and semantics tests hold that case.
