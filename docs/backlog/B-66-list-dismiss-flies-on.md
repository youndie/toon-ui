---
id: B-66
title: "A card thrown from a list flies on from the finger and leaves no hole"
status: done
priority: P2
size: S
stage: stage-2-components
epic: feature-components
---

# B-66 — A card thrown from a list flies on from the finger and leaves no hole

The owner, in the running kit (2026-11-09): "the animation of an item disappearing from a list is
crooked". Drawn frame by frame, a swipe in the kit's card list and on the sample's plan screen showed
four faults:

- the swiped card came back from where the finger let it go to its slot over 70 ms, and only then
  was thrown — a long swipe left crossed the row back to the middle before it flew;
- from 170 ms its cracked silhouette, the hole it punches into the paper, stood in the slot as a
  black slab, and the card below slid up over it;
- on the plan screen the cards below jumped up at once, without the close-up, and took new colours:
  the screen was a column of `ToonCard`s coloured by their row, not a `ToonCardList`;
- the fifth plan popped in at the bottom.

- **The decision and its reason.**
  - A card thrown by a swipe flies on from where the finger let it go, toward the swipe, at the
    engine's fling from its first frame (`cardDismiss` from 70 ms) — the kit's 70 ms back to the slot
    and its 10 dp wind-up are dropped for it (`toonCardThrowDx`). Thrown with no drag, by a reader's
    action, it is the kit's throw, so every `Card` cell is unchanged.
  - A `ToonCardList` card punches no hole: the cards below close up over its slot at once and only
    covered it. A card on its own still does, and fires `card.gone` to a `ToonFxHost`.
  - The sample's plan screen is a `ToonCardList`, so its cards close up and keep their colours, and
    a plan that comes into view drops in.
- Rejected: shrinking or fading the hole faster in a list — any hole there is covered by the card
  closing up over it within 30 ms of its first frame.
- AC: a swiped card never moves back toward its slot and leaves the row; a list's card punches no
  hole and its cards below close up; the plan screen keeps its cards' colours.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonCard.kt`,
  `toon-core/src/commonMain/kotlin/io/github/youndie/toon/component/ToonCardList.kt`,
  `sample/src/commonMain/kotlin/io/github/youndie/toon/sample/SampleApp.kt`.

## Findings (2026-11-09)

- `ToonCardTest`: for swipes of −150, 120 and −85 dp the throw starts at the finger, never steps back
  toward the slot, moves under 15 dp a ms while drawn and is off the row by 200 ms; with no drag it is
  the kit's throw. A card in a list fires no `card.gone`; a card on its own does. Mutants: the kit's
  return to the slot — "back toward the slot at 1 ms (−150.0 then −147.86)"; the hole back in the
  list — "in a list: expected [] but was [card.gone]", and the list's frames over the floor at 176 ms.
- `ToonCardListTest` holds the live list's close-up to the page's `cardStack` frames drawn without
  the hole (`CardFrames.dismiss(…, hole = false)`); the `Card_Dismiss_*` cells, drawn from the
  page's stack with its hole, are unchanged, and so are their goldens. Nothing departs from a cell.
- `SampleAppTest` finds the plan by its title now that the screen is a list.
