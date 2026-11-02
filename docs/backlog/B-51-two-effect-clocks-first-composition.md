---
id: B-51
title: "Check the two-effect clocks under a first composition"
status: wip
priority: P3
size: S
stage: stage-4-product
epic: feature-components
blocked_by: []
---

# B-51 — Check the two-effect clocks under a first composition

Found in B-44. Most animated components keep their clock in two effects: one keyed on the prop that
starts a play (`LaunchedEffect(open) { clock.changed(open) }`), and one keyed on the clock's start
count that asks for frames (`LaunchedEffect(clock.starts) { while (clock.running()) … }`). The
transition host was first written that way, and in the first `runComposeUiTest` of its test class,
with the test clock driven by hand, the second effect was never relaunched: the transition stayed at
0 ms and both screens stayed composed. Every later run in the same JVM played through. Moving the
frame loop into the first effect fixed it there.

Whether the other components (menu, sheet, drawer, tooltip, segmented, title card, dialog, …) stall
the same way in a real first composition, or only under a hand-driven test clock, was not checked.

- **The decision and its reason.** Reproduce it on one component first, with its first test of the
  class and the clock driven by hand, and in the sample app on a cold start. If it is real, move each
  component's frame loop into the effect that starts the play, as B-44 did; if it is the test clock
  alone, write that down in the service quirks and close this.
- AC: a test that fails on the two-effect clock and passes on the one-effect one, or a written
  finding that it does not reproduce outside the hand-driven clock.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonMenu.kt`,
  `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonTransitionHost.kt`.
