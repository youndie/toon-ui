---
id: B-08
title: "ToonTheme provider, system reduce motion, and a counted idle budget"
status: done
priority: P1
size: M
stage: stage-1-materials
blocked_by: [B-07]
---

# B-08 — ToonTheme provider, system reduce motion, and a counted idle budget

The sketch provides the theme and `LocalReduceMotion = false` by default, and `LocalIdleBudget = 2`
counts nothing (research R1).

- **The decision and its reason.** `ToonTheme(theme)` reads the platform's reduce-motion setting
  (Android animator scale, iOS `UIAccessibilityIsReduceMotionEnabled`, desktop and wasm: off unless
  set). An explicit parameter overrides it. Idle animation is a resource: a component asks for an
  idle slot, at most two are granted per theme scope, and a third stays static.
- Reduce motion follows each animation's word in the spec (D8). This item provides the switch; each
  component item implements its word.
- Rejected: the kit's "everything instant" (research §1.6).

- AC: a test shows the third idle request denied; a test flips reduce motion and the Button's press
  pose is instant.
- Anchors: `reference/design-system/toon-kit.js`, `reference/design-system/compose/toon/ui/theme/ToonTheme.kt`.

## Done (2026-11-02)

- `ToonTheme(tokens, reduceMotion, idleBudget)` provides the tokens, the bundled fonts for the theme's
  families (B-02), whether motion is reduced, and a `ToonIdleBudget`. `Toon.*` reads them, and
  `ToonThemes` names the three themes.
- `reduceMotion = null` follows the platform: Android's animator scale at 0, iOS's Reduce Motion,
  and, beyond what the item said, the browser's `prefers-reduced-motion` on wasm, as oldge-ui
  reads it. On the desktop JVM it is off.
- `rememberToonIdleSlot()` takes a slot when it is remembered and gives it back when forgotten.
- `ToonThemeTest`, 5 tests: a third request is refused; a freed slot is not handed to the refused
  element; reduced motion grants none; the desktop JVM defaults to motion on; the theme provides
  what it was given. Mutant `used < capacity` → `<=` failed the first two. `compileKotlinIosArm64`
  and `compileAndroidMain` are green too.
- **Moved to B-10:** "the Button's press pose is instant" needs the press machine. This item
  provides the switch it reads.
