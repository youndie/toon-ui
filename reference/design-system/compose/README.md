> FROZEN — not maintained. Numbers live only in `toon-engine.js`; this sketch already differs from it and is kept for the porting pattern only.

# toon-ui — Compose Multiplatform handoff

No `material` / `material3` dependency. Everything is `compose.foundation`, `compose.ui`, `compose.animation`, `compose.runtime`.

## Source of truth
`toon-engine.js` in this project. Every function here is a 1:1 port; when a number changes there, it changes here. The seeded RNG (`Mulberry32`) matches the JS bit-for-bit, so the same seed draws the same wobble on every platform and in the HTML spec.

## Files
- `toon/ui/theme/ToonTheme.kt` — tokens for the three themes (`inkAndGouacheLight`, `inkAndGouacheNight`, `brightCel`), `ToonMotion` durations and springs, `ToonTheme { }` provider, `LocalReduceMotion`, `LocalIdleBudget`.
- `toon/ui/draw/ToonGeometry.kt` — procedural shapes: rounded rect with 4 radii → seeded wobble → ink/fill pair with per-side insets (variable ink weight); `squashed()` rebuilds geometry on squash so stroke weight is kept and the bottom thickens 1.3×; `inkLine()` for dividers; `iconOutline()` turns a centerline into the variable-width icon fill with `PathMeasure`.
- `toon/ui/motion/ToonCurves.kt` — closed-form curves for every animation in the spec (press/release, switch, checkbox, radio drop, card appear, FAB land, snackbar, field, dialog, worm, sway, badge, idle breath/boil).
- `toon/ui/components/ToonButton.kt` — two reference components (Button, Switch) showing the pattern every other component follows.

## Pattern
1. One frame clock (`withFrameMillis`) per animating component; it stops when the animation is done (idle budget: max 2 always-on per screen).
2. Curves are functions of elapsed ms → draw params. No `Animatable` chains needed; springs are evaluated analytically.
3. Geometry is rebuilt from size + radii + insets + seed each frame the shape changes (≈130 points, cheap) and cached when static. Don't `graphicsLayer.scale` a shape that has a stroke.
4. Touch target is the layout box (≥ 48 dp); only the drawing moves. Actions fire on release, never at the end of an animation.
5. Text is `BasicText` in the layout, offset with the shape, never scaled or rotated.
6. `LocalReduceMotion` = system setting by default; every curve has a one-word fallback in the spec (instant / fade 120 / off).
7. FX (dust, stars, speed lines, flash, shake, iris, hole, sweat) are a separate draw layer above the component, triggered by named events (`card.land`, `field.error`, …) — the same names will carry sounds later.

## Fonts (SIL OFL, bundle via compose-resources)
Shrikhand, Figtree (base) · Titan One, Fredoka (Bright Cel).

## Textures
`assets/textures/*.png` — seamless 512 px tiles, draw with `ImageShader(…, TileMode.Repeated)` at 256 dp.

## Still to port
Remaining components follow `ToonButton`/`ToonSwitch`: map each renderer in `toon-engine.js` (`iconBtn`, `fab`, `radio`, `chip`, `card`, `field`, `check`, `snack`, `dialogScene`, `linear`, `circular`, `topBar`, `navBar`, `tabs`, `listItem`, `badge`) to a `Canvas` + the matching curve. FX functions (`dust`, `stars`, `speed`, `flash`, `vibe`, `irisScene`, `hole`, `sweat`) port the same way.
