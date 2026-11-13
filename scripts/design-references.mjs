#!/usr/bin/env node
// B-06: render the pixel references from the vendored design engine in headless Chrome (research D3).
//
//   node scripts/design-references.mjs [--check] [--outlines] [--chrome <binary>]
//
// Each entry of REFERENCES is drawn by toon-engine.js itself, in a page that loads the engine
// untouched and the repository's bundled fonts (B-02), so the reference and the Compose fixture draw
// text with the same files. The page is the cell a spec page gives the component: the stage colour of
// the theme behind it, the cell's padding around it. Device scale 1, because viddik captures at
// density 1 (1 dp = 1 px). The PNGs go to toon-core/src/desktopTest/snapshots/design under the name
// viddik's golden would get, `<group>_<name>.png`.
//
// A component item adds its states here when it builds the component (its spec page names them);
// this file starts with the Divider, the one component whose numbers match exactly (B-04), so the
// floor measured on it is the rasterisers' difference alone.
//
// --check renders into a temporary directory and fails if any PNG differs from the committed one.
// Node 24 has WebSocket, so the DevTools protocol needs no dependency.

import { spawn } from 'node:child_process';
import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { setTimeout as sleep } from 'node:timers/promises';
import { pathToFileURL } from 'node:url';
import { ICONS, ICON_CELLS } from './icon-cells.mjs';
import { desktopIcons } from './desktop-icons.mjs';
import { FX_CELLS } from './fx-cells.mjs';
import { COMPONENT_CELLS } from './component-cells.mjs';

const ROOT = resolve(import.meta.dirname, '..');
const ENGINE = join(ROOT, 'reference/design-system/toon-engine.js');
const FONTS = join(ROOT, 'toon-core/src/commonMain/composeResources/font');
const OUT = join(ROOT, 'toon-core/src/desktopTest/snapshots/design');
const OUTLINES = join(ROOT, 'toon-core/src/desktopTest/resources/design-goldens/icon-outlines.json');

const THEMES = { il: 'InkGouacheLight', in: 'InkGouacheNight', bc: 'BrightCel' };

// The Divider's cells, the floor's ruler (B-06), are scripts/component-cells.mjs's since B-22.
export const REFERENCES = [];

// B-09: a plain body, the Principles page's `blk()` with the night rim left on, and the same body
// focused (`ring` at 6 px outset, as `btn` draws it).
const BODY = "E.body(T, 0, 0, 96, 52, [16, 11, 18, 12], { seed: T.seed + 1400, fill: T.primary, sh: T.sh })";
const RING = "E.ring(-6, -6, 108, 64, [22, 17, 24, 18], T, T.seed + 1403)";
REFERENCES.push(...Object.entries(THEMES).flatMap(([key, theme]) => [
  { group: 'Body', name: `Plain_${theme}`, theme: key, w: 96, h: 52, pad: [10, 12, 12, 10], scene: `{ L: ${BODY}, X: [] }` },
  { group: 'Body', name: `Focused_${theme}`, theme: key, w: 96, h: 52, pad: [10, 12, 12, 10], scene: `{ L: [${RING}, ...${BODY}], X: [] }` },
]));

// B-11: every icon cell of scripts/icon-cells.mjs, drawn by the engine's `icon` as the kit's
// `C.icon.scene` calls it: the theme's text colour, the `chipSel` fill when filled.
REFERENCES.push(...ICON_CELLS.map(c => ({
  group: c.group, name: c.name, theme: c.theme, w: c.w, h: c.h, pad: c.pad,
  scene: `{ L: E.icon(T, ${JSON.stringify(c.icon)}, 0, 0, { size: ${c.size}, bold: ${c.bold}, fill: ${c.filled ? 'T.chipSel' : 'null'} }), X: [] }`,
})));

// B-12: every frame of the FX page's strips (scripts/fx-cells.mjs), the effect alone on the stage.
REFERENCES.push(...FX_CELLS.map(({ group, name, theme, w, h, pad, scene }) => ({ group, name, theme, w, h, pad, scene })));

// B-14 on: each component's cells on the spec pages (scripts/component-cells.mjs).
REFERENCES.push(...Object.values(COMPONENT_CELLS).flat().map(({ group, name, theme, w, h, pad, scene }) => ({ group, name, theme, w, h, pad, scene })));

// B-11: the outlines themselves, which only a DOM can compute (`sampleSub` measures an SVG path), so
// the numeric half of the icon parity is read here rather than in scripts/engine-goldens.mjs.
// B-80: with the desktop extension's 12 after the kit's 25; only the outline page loads the extension
// (and the kit's runtime it waits for), since it replaces the kit's `sheet` for any page it is in.
const OUTLINE_ICONS = [...Object.keys(ICONS), ...Object.keys(desktopIcons(ICONS))];
const OUTLINE_EXPR = `JSON.stringify(Object.fromEntries(${JSON.stringify(OUTLINE_ICONS)}.map(n => [n, { regular: E.iconOutline(n, 2.1), bold: E.iconOutline(n, 2.7) }])))`;
const KIT = join(ROOT, 'reference/design-system/toon-kit.js');
const DESKTOP_ENGINE = join(ROOT, 'reference/desktop/desktop-engine.js');

const FACES = [
  ['Shrikhand', 'shrikhand_regular.ttf', '400'],
  ['Figtree', 'figtree_wght.ttf', '300 900'],
  ['Titan One', 'titan_one_regular.ttf', '400'],
  ['Fredoka', 'fredoka_wdth_wght.ttf', '300 700'],
];

function page(ref) {
  const faces = FACES.map(([f, file, w]) => `@font-face{font-family:"${f}";src:url("${pathToFileURL(join(FONTS, file)).href}");font-weight:${w}}`).join('');
  const [t, r, b, l] = ref.pad;
  const W = ref.w + l + r, H = ref.h + t + b;
  return `<!doctype html><html><head><meta charset="utf-8"><style>${faces}html,body{margin:0;padding:0}</style>
<script src="${pathToFileURL(ENGINE).href}"></script>${ref.desktop ? `
<script src="${pathToFileURL(KIT).href}"></script><script src="${pathToFileURL(DESKTOP_ENGINE).href}"></script>` : ''}</head><body>
<svg id="s" width="${W}" height="${H}" viewBox="${-l} ${-t} ${W} ${H}" style="display:block"></svg>
<script>
  // The engine measures text on a canvas (\`tw\`), so the faces load before the scene is built: a
  // width taken from the fallback face would size an extended FAB or a text button's underline wrong.
  const E = new window.ToonEngine(), T = E.THEMES[${JSON.stringify(ref.theme)}];
  document.body.style.background = T.bg;
  window.drawn = Promise.all(${JSON.stringify(FACES.map(([f]) => f))}.map(f => document.fonts.load('400 16px "' + f + '"'))).then(() => {
    document.getElementById('s').innerHTML = E.toSvg(${ref.scene});
  });
</script></body></html>`;
}

class Cdp {
  constructor(ws) {
    this.ws = ws; this.id = 0; this.pending = new Map(); this.listeners = [];
    ws.addEventListener('message', (event) => {
      const msg = JSON.parse(event.data);
      if (msg.id && this.pending.has(msg.id)) {
        const { ok, fail } = this.pending.get(msg.id); this.pending.delete(msg.id);
        msg.error ? fail(new Error(`${msg.error.message} ${msg.error.data ?? ''}`)) : ok(msg.result);
      } else for (const l of this.listeners) l(msg);
    });
  }
  send(method, params = {}, sessionId) {
    const id = ++this.id;
    this.ws.send(JSON.stringify({ id, method, params, sessionId }));
    return new Promise((ok, fail) => this.pending.set(id, { ok, fail }));
  }
  once(method, sessionId) {
    return new Promise((ok) => {
      const l = (msg) => { if (msg.method === method && msg.sessionId === sessionId) { this.listeners.splice(this.listeners.indexOf(l), 1); ok(msg.params); } };
      this.listeners.push(l);
    });
  }
}

async function launch(chrome) {
  const profile = mkdtempSync(join(tmpdir(), 'toon-refs-'));
  const proc = spawn(chrome, ['--headless=new', '--remote-debugging-port=0', `--user-data-dir=${profile}`, '--allow-file-access-from-files',
    '--force-device-scale-factor=1', '--hide-scrollbars', '--disable-gpu', '--no-first-run', '--no-default-browser-check', 'about:blank'], { stdio: 'ignore' });
  const portFile = join(profile, 'DevToolsActivePort');
  for (let i = 0; i < 100 && !existsSync(portFile); i++) await sleep(100);
  const [port, path] = readFileSync(portFile, 'utf8').trim().split('\n');
  const ws = new WebSocket(`ws://127.0.0.1:${port}${path}`);
  await new Promise((ok, fail) => { ws.onopen = ok; ws.onerror = fail; });
  const version = (await fetch(`http://127.0.0.1:${port}/json/version`).then((r) => r.json())).Browser;
  const exited = new Promise((ok) => proc.once('exit', ok));
  return { cdp: new Cdp(ws), version, close: async () => { ws.close(); proc.kill(); await exited; rmSync(profile, { recursive: true, force: true, maxRetries: 5 }); } };
}

async function render(cdp, ref, dir, capture) {
  const [t, r, b, l] = ref.pad;
  const W = ref.w + l + r, H = ref.h + t + b;
  const file = join(dir, `${ref.group}_${ref.name}.html`);
  writeFileSync(file, page(ref));
  const { targetId } = await cdp.send('Target.createTarget', { url: 'about:blank' });
  const { sessionId } = await cdp.send('Target.attachToTarget', { targetId, flatten: true });
  const errors = [];
  const onError = (msg) => { if (msg.sessionId === sessionId && msg.method === 'Runtime.exceptionThrown') errors.push(msg.params.exceptionDetails.exception?.description ?? msg.params.exceptionDetails.text); };
  cdp.listeners.push(onError);
  try {
    await cdp.send('Page.enable', {}, sessionId);
    await cdp.send('Runtime.enable', {}, sessionId);
    await cdp.send('Emulation.setDeviceMetricsOverride', { width: W, height: H, deviceScaleFactor: 1, mobile: false }, sessionId);
    const loaded = cdp.once('Page.loadEventFired', sessionId);
    await cdp.send('Page.navigate', { url: pathToFileURL(file).href }, sessionId);
    await loaded;
    await cdp.send('Runtime.evaluate', { awaitPromise: true, expression: 'window.drawn.then(() => document.fonts.ready).then(() => new Promise((r) => requestAnimationFrame(() => requestAnimationFrame(r))))' }, sessionId);
    if (errors.length) throw new Error(`${ref.group}_${ref.name}: ${errors.join('; ')}`);
    if (capture) return (await cdp.send('Runtime.evaluate', { expression: capture, returnByValue: true }, sessionId)).result.value;
    const shot = await cdp.send('Page.captureScreenshot', { format: 'png', clip: { x: 0, y: 0, width: W, height: H, scale: 1 } }, sessionId);
    return Buffer.from(shot.data, 'base64');
  } finally {
    cdp.listeners.splice(cdp.listeners.indexOf(onError), 1);
    await cdp.send('Target.closeTarget', { targetId });
  }
}

// B-29: the Screens page's phone screens and desktop layout, rendered by the design's own runtime (the
// page's `support.js` and the kit over the engine) rather than rebuilt here, so the reference is the
// page as the designer sees it. With reduced motion the kit puts every animation at its last frame
// and stops the idle loops, which makes the capture deterministic; each `[data-screen-label]` is cut
// out at its own box.
const SCREENS_PAGE = join(ROOT, 'reference/design-system/toon-ui Screens.dc.html');
const SCREEN_ROWS = ['InkGouacheLight', 'InkGouacheNight', 'BrightCel'];
const SCREEN_NAMES = { 'Sign in': 'SignIn', List: 'List', Settings: 'Settings' };
export const SCREENS = [
  ...SCREEN_ROWS.flatMap((theme) => Object.values(SCREEN_NAMES).map((n) => ({ group: 'Screens', name: `${n}_${theme}`, w: 390, h: 844 }))),
  { group: 'Screens', name: 'Desktop_InkGouacheLight', w: 1280, h: 800 },
];

async function renderScreens(cdp) {
  const { targetId } = await cdp.send('Target.createTarget', { url: 'about:blank' });
  const { sessionId } = await cdp.send('Target.attachToTarget', { targetId, flatten: true });
  try {
    await cdp.send('Page.enable', {}, sessionId);
    await cdp.send('Runtime.enable', {}, sessionId);
    await cdp.send('Emulation.setDeviceMetricsOverride', { width: 1500, height: 4200, deviceScaleFactor: 1, mobile: false }, sessionId);
    await cdp.send('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-reduced-motion', value: 'reduce' }] }, sessionId);
    const loaded = cdp.once('Page.loadEventFired', sessionId);
    await cdp.send('Page.navigate', { url: pathToFileURL(SCREENS_PAGE).href }, sessionId);
    await loaded;
    // The runtime mounts the components after load; wait for every one of them and the faces.
    await cdp.send('Runtime.evaluate', { awaitPromise: true, expression: `(async () => {
      for (let i = 0; i < 100 && document.querySelectorAll('[data-screen-label="Settings"]').length < 3; i++) await new Promise((r) => setTimeout(r, 100));
      await document.fonts.ready;
      await new Promise((r) => setTimeout(r, 1500));
      await new Promise((r) => requestAnimationFrame(() => requestAnimationFrame(r)));
    })()` }, sessionId);
    const boxes = (await cdp.send('Runtime.evaluate', { returnByValue: true, expression: `[...document.querySelectorAll('[data-screen-label]')]
      .filter((e) => e.dataset.screenLabel !== 'Demo screens')
      .map((e) => { const r = e.getBoundingClientRect(); return { label: e.dataset.screenLabel, x: r.x + scrollX, y: r.y + scrollY, w: r.width, h: r.height }; })` }, sessionId)).result.value;
    const named = [];
    let row = 0;
    for (const b of boxes) {
      if (b.label === 'Desktop') named.push({ name: 'Desktop_InkGouacheLight', ...b });
      else {
        named.push({ name: `${SCREEN_NAMES[b.label]}_${SCREEN_ROWS[Math.floor(row / 3)]}`, ...b });
        row++;
      }
    }
    const shots = {};
    for (const b of named) {
      const shot = await cdp.send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true, clip: { x: b.x, y: b.y, width: b.w, height: b.h, scale: 1 } }, sessionId);
      shots[`Screens_${b.name}`] = Buffer.from(shot.data, 'base64');
    }
    return shots;
  } finally {
    await cdp.send('Target.closeTarget', { targetId });
  }
}

const argv = process.argv.slice(2);
const check = argv.includes('--check');
// B-80: --outlines takes only icon-outlines.json, so a newer Chrome does not rewrite every reference.
const outlinesOnly = argv.includes('--outlines');
const chrome = argv.includes('--chrome') ? argv[argv.indexOf('--chrome') + 1] : '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const work = mkdtempSync(join(tmpdir(), 'toon-refs-pages-'));
const { cdp, version, close } = await launch(chrome);
const stale = [];
let outlines;
try {
  if (!check) mkdirSync(OUT, { recursive: true });
  const blank = { group: 'Outlines', name: 'all', theme: 'il', w: 1, h: 1, pad: [0, 0, 0, 0], scene: '{ L: [], X: [] }', desktop: true };
  const drawn = JSON.parse(await render(cdp, blank, work, OUTLINE_EXPR));
  const missing = OUTLINE_ICONS.filter((n) => !drawn[n]?.regular || !drawn[n]?.bold);
  if (missing.length) throw new Error(`the outline page has no outline for ${missing.join(', ')}`);
  outlines = JSON.stringify({ browser: version, outlines: drawn }, null, 2) + '\n';
  if (check) { if (!existsSync(OUTLINES) || readFileSync(OUTLINES, 'utf8') !== outlines) stale.push('icon-outlines.json'); }
  for (const ref of outlinesOnly ? [] : REFERENCES) {
    const png = await render(cdp, ref, work);
    const target = join(OUT, `${ref.group}_${ref.name}.png`);
    if (check) { if (!existsSync(target) || !readFileSync(target).equals(png)) stale.push(`${ref.group}_${ref.name}`); }
    else writeFileSync(target, png);
  }
  const screens = outlinesOnly ? {} : await renderScreens(cdp);
  for (const ref of outlinesOnly ? [] : SCREENS) {
    const key = `${ref.group}_${ref.name}`;
    if (!screens[key]) throw new Error(`the Screens page has no ${key}`);
    const target = join(OUT, `${key}.png`);
    if (check) { if (!existsSync(target) || !readFileSync(target).equals(screens[key])) stale.push(key); }
    else writeFileSync(target, screens[key]);
  }
} finally {
  await close();
  rmSync(work, { recursive: true, force: true });
}
const engineSha = createHash('sha256').update(readFileSync(ENGINE)).digest('hex');
const manifest = { browser: version, engine: engineSha, scale: 1, references: REFERENCES.map(({ group, name, theme, w, h, pad, scene }) => ({ file: `${group}_${name}.png`, theme, w, h, pad, scene })),
  screens: SCREENS.map(({ group, name, w, h }) => ({ file: `${group}_${name}.png`, page: 'toon-ui Screens.dc.html', w, h, reducedMotion: true })) };
if (check) {
  if (stale.length) { console.error(`design references differ from a fresh render in ${version}: ${stale.join(', ')}`); process.exit(1); }
  console.log(`${REFERENCES.length} design references match a fresh render in ${version}`);
} else if (outlinesOnly) {
  writeFileSync(OUTLINES, outlines);
  console.log(`${OUTLINE_ICONS.length} icon outlines written with ${version}`);
} else {
  writeFileSync(join(OUT, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
  mkdirSync(dirname(OUTLINES), { recursive: true });
  writeFileSync(OUTLINES, outlines);
  console.log(`${REFERENCES.length} design references and ${SCREENS.length} screens written with ${version}`);
}
