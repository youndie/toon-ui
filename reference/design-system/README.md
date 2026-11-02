# toon-ui — design system (prototype library)

Golden-age theatrical cartoon UI kit for Compose Multiplatform. This project holds the spec pages and a working component library for building prototypes.

## Files
- `toon-engine.js` — the single source of truth: themes (tokens), shape generator (size + 4 corner radii + seed → wobbly outline with variable ink weight), 17 icons (centerline paths + width rule), every component renderer, FX and motion curves. The Kotlin port mirrors these functions 1:1.
- `toon-kit.js` — runtime for the library: shared rAF loop, theme switching, press/spring physics, one controller per component.
- `Toon*.dc.html` — 27 library components (see below).
- Spec pages: `toon-ui Directions`, `toon-ui Core` (tokens + 6 core components), `toon-ui MVP Components` (12 more), `toon-ui Icons`, `toon-ui FX`, `toon-ui Kit` (library index + demo), `toon-ui Screens` (sign in / list / settings, day + night, textures), `toon-ui Principles` (7 rules + recipe for a new component), `toon-ui v1 Components` (8 v1 components + screen transitions).
- `assets/textures/` — seamless 512 px paper and gouache tiles, light and night.
- `compose/` — Kotlin handoff without Material: tokens, geometry port, motion curves, reference Button + Switch. See `compose/README.md`.

## Using the library
```html
<dc-import name="ToonButton" label="Add plan" width="{{ 342 }}" onClick="{{ add }}" hint-size="342px,52px"></dc-import>
```
- Theme for the whole page: `window.ToonKit.setTheme('light' | 'night' | 'cel')`. Per instance: `theme="night"`.
- Reduce motion follows the OS setting; force with `ToonKit.setReduceMotion(true)`.
- Slow motion for review: `ToonKit.slow = 0.25`.
- Reduce motion: state changes the spec marks “fade 120” cross-fade over 120 ms; the rest switch instantly.
- `ToonDialog` fills its nearest positioned parent (put it inside a `position:relative` screen).
- List props are comma strings: `options="Morning,Noon,Evening"`, `items="home:Home,heart:Saved"`, `actions="search,more"`.

| Component | Key props |
|---|---|
| ToonButton | label, variant (filled/outlined/text), width, loading, idle, disabled, onClick |
| ToonIconButton | icon, variant (standard/filled), selected, toggle, onChange |
| ToonFab | icon, label, land, delay, onClick |
| ToonTextField | label, value, helper, error, width, onChange |
| ToonCheckbox | label, checked, error, onChange |
| ToonRadioGroup | options, value, onChange |
| ToonSwitch | label, checked, width, onChange |
| ToonChip | label, variant (filter/assist/input), icon, selected, tilt, onChange |
| ToonCard | title, meta, time, index (0–2), tilt, appear, delay, dismissible, onDismiss, selected, onClick |
| ToonListItem | icon, title, sub, trailing (chevron/switch/check/none), on, last, onChange |
| ToonDivider | width, inset, weight (s/m/l) |
| ToonDialog | open, title, body, confirm, cancel, destructive, onConfirm, onCancel |
| ToonSnackbar | text, action, type (info/success/error), shown, onAction |
| ToonProgress | kind (linear/circular), value (−1 = indeterminate), width |
| ToonBadge | count, dot |
| ToonTopAppBar | title, variant (small/center), actions, nav, scrolled, onAction |
| ToonNavBar | items, selected, badgeIndex, onChange |
| ToonTabs | labels, selected, onChange |
| ToonIcon | icon, size, weight (regular/bold), filled, color (token name) |
| ToonSlider | value, steps, width, onChange |
| ToonMenu | open, items (icon:Label), selected, onSelect |
| ToonBottomSheet | open, title, items, onSelect, onClose (fills parent) |
| ToonNavDrawer | open, title, items, selected, onSelect, onClose (fills parent) |
| ToonTooltip | text, show |
| ToonSegmented | labels, selected, onChange |
| ToonPullToRefresh | refreshing, onRefresh, onPull |
| ToonTitleCard | variant (splash/empty), title, sub, width, height |

## Decisions
- **Palette (fixed, 2026-11-02).** The kit ships 3 cel accents per theme (sticker / card colours) plus single semantic tokens (primary, success, warning, danger, focus, track, disabled). The brief's 8 cel colours with dark and light steps are not planned; pressed and disabled states use ink opacity, shadow removal and the `surface-pressed` / `disabled` tokens instead of per-colour steps.
- **Desktop.** Mobile-first; one 1280 × 800 reference layout in `toon-ui Screens`, no hover states.
- **compose/.** Frozen; numbers live only in `toon-engine.js`.

## Visual foundations (short)
- Thick ink outline with variable weight (bottom 1.3–1.8× top), flat cel fills, no gradients or blur.
- Uneven corner radii per shape, ±1–2° tilt on some cards/chips, hard offset ink shadow (4×4 / 6×6) that drops to 0 on press.
- Display: Shrikhand (Bright Cel: Titan One). Body: Figtree (Bright Cel: Fredoka). All SIL OFL.
- Motion: squash & stretch on press, overshoot 10–20 %, anticipation for big actions, FX as a separate layer fired by named events. Text never scales or tilts.
- Night theme: navy stage, muted cels, ink outline + pale rim on the top edge; FX lines switch to `fxLine`.
