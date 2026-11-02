// B-03: run the vendored design engine in Node and write the golden JSON the Kotlin port is tested
// against (research D3). The engine is loaded untouched; every stub it needs lives here and says why.
//
//   node scripts/engine-goldens.mjs           write the goldens
//   node scripts/engine-goldens.mjs --check   fail if the committed goldens differ from a fresh run
//
// Geometry is captured by wrapping the engine's own functions while its renderers draw: whatever
// `wob`, `inkLine`, `star` and `burst` are called with by a component is what the port must
// reproduce, with the component's real radii, insets and seeds. Curves are sampled every
// millisecond, so every timestamp a spec page's frame strip names is in the file.
import { createHash } from 'node:crypto';
import { mkdirSync, readFileSync, writeFileSync, existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const enginePath = join(root, 'reference/design-system/toon-engine.js');
const outDir = join(root, 'toon-core/src/desktopTest/resources/engine-goldens');

// Stub: the engine registers itself on `window`. Without a `document`, `tw` measures text as
// length × size × 0.55 and `icon` draws nothing (research §1.2): boxes sized from text are the
// fallback's, which is fine for geometry, since a golden records the arguments it was drawn with.
globalThis.window = {};
createRequire(import.meta.url)(enginePath);
const E = new globalThis.window.ToonEngine();

// Six decimals: below anything visible and below Float's precision at these magnitudes, and coarse
// enough that a different libm's last bit does not make a run on another machine "stale".
const r6 = v => (typeof v === 'number' ? Math.round(v * 1e6) / 1e6 + 0 : v);
const round = x => (Array.isArray(x) ? x.map(round) : x && typeof x === 'object' ? Object.fromEntries(Object.entries(x).map(([k, v]) => [k, round(v)])) : r6(x));

// ---- geometry capture ----
// Stub: `wob` caches by key and only smooths on a miss, and `smooth` rounds to 0.1 px while writing
// the path string. Clearing the cache per call and catching the points `smooth` receives gives the
// unrounded outline the port computes.
let lastPts = null;
const smooth0 = E.smooth.bind(E);
E.smooth = pts => { lastPts = pts; return smooth0(pts); };
const wob0 = E.wob.bind(E), inkLine0 = E.inkLine.bind(E), star0 = E.star.bind(E), burst0 = E.burst.bind(E);
let rec = null;
const keep = (kind, args, out) => {
  if (!rec) return;
  const key = kind + JSON.stringify(round(args));
  if (!rec.seen.has(key)) { rec.seen.add(key); rec[kind].push({ args: round(args), ...out }); }
};
E.wob = (...a) => { E.wc.clear(); lastPts = null; const d = wob0(...a); keep('wob', a, { pts: round(lastPts) }); return d; };
// These three write their points straight into a path string at 0.1 px; the string is the golden,
// and B-04 compares against it within half that step.
E.inkLine = (...a) => { const d = inkLine0(...a); keep('inkLine', a, { d }); return d; };
E.star = (...a) => { const d = star0(...a); keep('star', a, { d }); return d; };
E.burst = (...a) => { const d = burst0(...a); keep('burst', a, { d }); return d; };

const themes = ['il', 'in', 'bc'];
const scenes = T => ({
  'button filled': () => E.btn(T, { v: 'filled', w: 200, label: 'Add plan' }),
  'button filled p=0.5': () => E.btn(T, { v: 'filled', w: 200, label: 'Add plan', p: 0.5 }),
  'button filled p=1': () => E.btn(T, { v: 'filled', w: 200, label: 'Add plan', p: 1 }),
  'button filled p=-0.31': () => E.btn(T, { v: 'filled', w: 200, label: 'Add plan', p: -0.31 }),
  'button outlined': () => E.btn(T, { v: 'outlined', w: 200, label: 'Add plan' }),
  'button text pressed': () => E.btn(T, { v: 'text', w: 200, label: 'Add plan', p: 1 }),
  'button focused': () => E.btn(T, { v: 'filled', w: 200, label: 'Add plan', focus: true }),
  'icon button': () => E.iconBtn(T, { icon: 'heart' }),
  'fab': () => E.fab(T, {}),
  'checkbox on': () => E.check(T, { on: true, label: 'Bring snacks' }),
  'radio selected': () => E.radio(T, { sel: true, label: 'Noon' }),
  'switch on': () => E.sw(T, { pos: 1, on: true, trackOn: true }),
  'chip selected': () => E.chip(T, { variant: 'filter', label: 'Outdoors', sel: true }),
  'card': () => E.card(T, { idx: 0, w: 342, title: 'Pancake breakfast', meta: 'Kitchen, bring syrup', time: '9:00' }),
  'text field': () => E.field(T, { w: 342, label: 'Plan name', lift: 0, thick: 0, bounce: 1 }),
  'snackbar': () => E.snack(T, { w: 342, type: 'info', text: 'Plan added', action: 'Undo' }),
  'dialog': () => E.dialog(T, { w: 300, title: 'Delete plan?', body: ['Gone for good.'], confirm: 'Delete', cancel: 'Cancel', destructive: true }),
  'progress linear': () => E.linear(T, { w: 280, a: 0, b: 0.4 }),
  'progress circular': () => E.circular(T, { value: 0.65 }),
  'top app bar': () => E.topBar(T, { title: 'Saturday' }),
  'navigation bar': () => E.navBar(T, { items: [{ icon: 'home', label: 'Home' }, { icon: 'heart', label: 'Saved' }], sel: 0 }),
  'tabs': () => E.tabs(T, { labels: ['Today', 'Weekend', 'Someday'], sel: 0 }),
  'list item': () => E.listItem(T, { w: 342, icon: 'star', title: 'Favourites', trailing: 'chevron' }),
  'divider': () => E.divider(T, { x1: 342, th: 3 }),
  'badge': () => E.badge(T, { n: 3 }),
  'slider': () => E.slider(T, { w: 300, value: 0.4 }),
  'segmented': () => E.segmented(T, { w: 300, labels: ['Day', 'Week', 'Month'], sel: 0 }),
  'tooltip': () => E.tooltip(T, { text: 'Tap to add a plan' }),
  'menu': () => E.menu(T, { w: 200, items: ['Edit', 'Share'], icons: ['edit', 'share'], u: 1 }),
});

const geometry = {};
for (const k of themes) {
  const T = E.THEMES[k];
  const perScene = {};
  for (const [name, draw] of Object.entries(scenes(T))) {
    rec = { seen: new Set(), wob: [], inkLine: [], star: [], burst: [] };
    draw();
    const { seen, ...captured } = rec;
    perScene[name] = Object.fromEntries(Object.entries(captured).filter(([, v]) => v.length));
    rec = null;
  }
  geometry[k] = perScene;
}

// ---- curves ----
const ms = (to, step = 1) => Array.from({ length: Math.floor(to / step) + 1 }, (_, i) => i * step);
const curves = {
  'squashOf(p)': { at: ms(200).map(i => -1 + i / 100), f: p => E.squashOf(p) },
  'spr(t s, 0.35, 45)': { at: ms(600), f: t => E.spr(t / 1000, 0.35, 45) },
  'osc(t s, 0.35, 40)': { at: ms(600), f: t => E.osc(t / 1000, 0.35, 40) },
  'chkAnim(t, true)': { at: ms(500), f: t => E.chkAnim(t, true) },
  'chkAnim(t, false)': { at: ms(500), f: t => E.chkAnim(t, false) },
  'swAnim(t, 0, 1)': { at: ms(520), f: t => E.swAnim(t, 0, 1) },
  'swAnim(t, 1, 0)': { at: ms(520), f: t => E.swAnim(t, 1, 0) },
  'iconBounce(t)': { at: ms(500), f: t => E.iconBounce(t) },
  'fabAppear(t)': { at: ms(700), f: t => E.fabAppear(t) },
  'radioSelect(t)': { at: ms(500), f: t => E.radioSelect(t) },
  'chipSelect(t)': { at: ms(700), f: t => E.chipSelect(t) },
  'dialogOpen(t)': { at: ms(700), f: t => E.dialogOpen(t) },
  'dialogClose(t)': { at: ms(400), f: t => E.dialogClose(t) },
  'worm(t)': { at: ms(1200), f: t => E.worm(t) },
  'navSelect(t, 0, 2, 97.5)': { at: ms(700), f: t => E.navSelect(t, 0, 2, 97.5) },
  'tabsSwitch(t, 0, 1, 114)': { at: ms(700), f: t => E.tabsSwitch(t, 0, 1, 114) },
  'listPress(t, true)': { at: ms(500), f: t => E.listPress(t, true) },
  'listPress(t, false)': { at: ms(500), f: t => E.listPress(t, false) },
  'badgeAppear(t)': { at: ms(400), f: t => E.badgeAppear(t) },
  'badgeCount(t)': { at: ms(400), f: t => E.badgeCount(t) },
  'sway(t, 28)': { at: ms(1200), f: t => E.sway(t, 28) },
  'cardAppear(t, 1.5)': { at: ms(700), f: t => E.cardAppear(t, 1.5) },
  'cardDismiss(t)': { at: ms(520), f: t => E.cardDismiss(t) },
  'snkShow(t)': { at: ms(700), f: t => E.snkShow(t) },
  'snkHide(t)': { at: ms(400), f: t => E.snkHide(t) },
  'fldFocus(t, true)': { at: ms(400), f: t => E.fldFocus(t, true) },
  'fldFocus(t, false)': { at: ms(400), f: t => E.fldFocus(t, false) },
  'irisAt(t)': { at: ms(700), f: t => E.irisAt(t) },
  'shake(t)': { at: ms(500), f: t => E.shake(t) },
  'sliderGrab(t, true)': { at: ms(500), f: t => E.sliderGrab(t, true) },
  'sliderGrab(t, false)': { at: ms(500), f: t => E.sliderGrab(t, false) },
  'menuUnroll(t, true)': { at: ms(500), f: t => E.menuUnroll(t, true) },
  'menuUnroll(t, false)': { at: ms(300), f: t => E.menuUnroll(t, false) },
  'sheetMove(t, true, 260)': { at: ms(600), f: t => E.sheetMove(t, true, 260) },
  'sheetMove(t, false, 260)': { at: ms(400), f: t => E.sheetMove(t, false, 260) },
  'drawerOpen(t)': { at: ms(600), f: t => E.drawerOpen(t) },
  'drawerClose(t)': { at: ms(400), f: t => E.drawerClose(t) },
  'tipInflate(t, true)': { at: ms(500), f: t => E.tipInflate(t, true) },
  'tipInflate(t, false)': { at: ms(500), f: t => E.tipInflate(t, false) },
  'segSwitch(t, 0, 2, 86.7)': { at: ms(700), f: t => E.segSwitch(t, 0, 2, 86.7) },
  'pullRelease(t, 110)': { at: ms(500), f: t => E.pullRelease(t, 110) },
  'splashAt(t)': { at: ms(900), f: t => E.splashAt(t) },
  'pushAt(t, 180)': { at: ms(600), f: t => E.pushAt(t, 180) },
};
// Columnar: one array per field, so a reader takes `pos` at index t without parsing objects.
const sampled = {};
for (const [name, { at, f }] of Object.entries(curves)) {
  const rows = at.map(f);
  const cols = { at: round(at) };
  if (typeof rows[0] === 'object' && rows[0] !== null) {
    for (const key of Object.keys(rows[0])) cols[key] = rows.map(r => round(r[key]));
  } else cols.value = rows.map(round);
  sampled[name] = cols;
}

const engineSha = createHash('sha256').update(readFileSync(enginePath)).digest('hex');
const files = {
  'manifest.json': { engine: 'reference/design-system/toon-engine.js', sha256: engineSha, decimals: 6, pathStep: 0.1, themes, scenes: Object.keys(scenes(E.THEMES.il)), curves: Object.keys(curves) },
  ...Object.fromEntries(themes.map(k => [`geometry-${k}.json`, geometry[k]])),
  'curves.json': sampled,
  // THEMES as the engine has it, for the token layer's test (B-07).
  'themes.json': round(E.THEMES),
};

const check = process.argv.includes('--check');
let stale = [];
for (const [name, body] of Object.entries(files)) {
  const text = JSON.stringify(body) + '\n';
  const path = join(outDir, name);
  if (check) { if (!existsSync(path) || readFileSync(path, 'utf8') !== text) stale.push(name); }
  else { mkdirSync(outDir, { recursive: true }); writeFileSync(path, text); }
}
if (check) {
  if (stale.length) { console.error(`engine goldens are stale: ${stale.join(', ')}; run make goldens`); process.exit(1); }
  console.log(`engine goldens match toon-engine.js (${engineSha.slice(0, 12)})`);
} else console.log(`engine goldens written for toon-engine.js (${engineSha.slice(0, 12)})`);
