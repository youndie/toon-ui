---
id: B-51
title: "Check the two-effect clocks under a first composition"
status: done
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

## Findings (2026-11-02)

- **The hypothesis was wrong, the defect real and wider.** The effect's shape was not it: the frame
  loop in a second effect ticks through, every time. Each clock's `age()` returned 0 without reading
  `nowMs` while a play waited for its first frame (`at == PENDING_START`). Whatever read the clock at
  that moment — the transition host's composition, a component's draw lambda — took no subscription
  to the time, so the frames that followed never redrew it. One effect only hid it in B-44: the first
  tick could land before the composition read the clock.
- **What it broke, outside tests too, since it is a snapshot read:** a menu or a tooltip opened after
  it was shown was never drawn (0 px after 1.5 s), a sheet, drawer, dialog or snackbar stuck on its
  first frame, tabs, the segmented button and the navigation bar on the old selection. Components
  shown in their state from the start drew right, which is why no test had seen it: every frame and
  golden test builds the frame from a time it is given.
- **Fixed in all 24 clocks** of 22 files (21 components and the transition host): `age()` reads `nowMs` before anything else.
  `ToonClockSubscriptionTest` shows each of twelve components switched after it is shown and holds it
  to the pixels of the same component shown so from the start, 1.5 s on. It failed on the old clocks
  (the menu by 25 266 px, the tabs by 14 087) and passes on the new; reverting one clock (menu, tabs,
  drawer) fails it again. The checkbox, switch and chip were not affected — their draw reads other
  state that redraws them — and a revert of their clock passes, as it should.
- The transition host keeps its one effect (B-44); either shape works now.
