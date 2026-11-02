---
id: B-30
title: "Accessibility: semantics, state descriptions, contrast, targets"
status: done
priority: P2
size: M
stage: stage-4-product
blocked_by: [B-14, B-15, B-16, B-17, B-18, B-19, B-20, B-21, B-22, B-23, B-24, B-25, B-26, B-27, B-28]
---

# B-30 — Accessibility: semantics, state descriptions, contrast, targets

The brief's hard limits (contrast ≥ 4.5, targets ≥ 48 dp, reduce motion, nothing flashing faster
than 3 per second) and the semantics a screen reader needs.

- **The decision and its reason.** One test per component asserts role, state description and
  target size. One test over the generated tokens computes every text/background pair a component
  uses and fails below 4.5, except the one exception the owner accepted: `onDanger` on `danger` in Ink & Gouache light, 4.38:1 (B-24).
- Not covered: localisation of state descriptions beyond English, which waits on B-02.

- AC: the semantics and contrast tests pass for all 19 components.
- Anchors: `reference/design-system/uploads/brief.md`, `docs/research/research-architecture.md`.

## Findings (2026-11-02)

- **Contrast** (`ToonContrastTest`): 33 text-on-background pairs, read off the engine's renderers,
  in three themes. All hold 4.5:1 but `onDanger` on `danger` in Ink & Gouache light, 4.38, the
  owner's accepted exception (B-24) — which the same pair also draws as the badge's number and the
  error snackbar's text, not only as the destructive button.
- **Semantics and targets** (`ToonAccessibilityTest`): one test per component, 19, each asserting
  its role, its state (toggle, selection, state description, progress) and its text, and, where a
  finger presses it, a target of at least 48 dp each way. The navigation bar and the tabs are one
  node with the selected item as their state and each item as an action; the dialog is a pane
  titled by its title with its two buttons as actions; the divider and a decorative icon say
  nothing.
- **Fixed on the way:** a dismissible card had no way to be dismissed without a swipe; it now
  offers `Dismiss`. The top bar's action targets were 40 dp: the outer ones now reach the sign's
  edges, so one or two actions are 48 dp; three or more is B-49, a question for the owner.
- **Flashing:** nothing in the library flashes more than three times a second — the caret is on 500
  ms of every 800, 1.25 times a second, and the effects play once.
- **Mutants:** removing the card's action, either widening, a switch's role, a chip's selection or
  the snackbar's polite live region each fail a test.
