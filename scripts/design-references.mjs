#!/usr/bin/env node
// B-06: render the pixel references from the vendored design engine in headless Chrome (research D3).
//
//   node scripts/design-references.mjs [--check] [--chrome <binary>]
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
import { join, resolve } from 'node:path';
import { setTimeout as sleep } from 'node:timers/promises';
import { pathToFileURL } from 'node:url';

const ROOT = resolve(import.meta.dirname, '..');
const ENGINE = join(ROOT, 'reference/design-system/toon-engine.js');
const FONTS = join(ROOT, 'toon-core/src/commonMain/composeResources/font');
const OUT = join(ROOT, 'toon-core/src/desktopTest/snapshots/design');

const THEMES = { il: 'InkGouacheLight', in: 'InkGouacheNight', bc: 'BrightCel' };

// The MVP page's Divider cells: `E.divider(T, { seed: i, ...o })` at 342 × 12 with 4 px padding.
const DIVIDERS = [['Full', { x1: 342, th: 3 }], ['Inset', { x0: 56, x1: 342, th: 3 }], ['Thick', { x1: 342, th: 4.6 }], ['Short', { x1: 120, th: 2.4 }]];
export const REFERENCES = Object.entries(THEMES).flatMap(([key, theme]) =>
  DIVIDERS.map(([state, o], i) => ({
    group: 'Divider', name: `${state}_${theme}`, theme: key, w: 342, h: 12, pad: [4, 4, 4, 4],
    scene: `E.divider(T, ${JSON.stringify({ seed: i, ...o })})`,
  })));

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
<script src="${pathToFileURL(ENGINE).href}"></script></head><body>
<svg id="s" width="${W}" height="${H}" viewBox="${-l} ${-t} ${W} ${H}" style="display:block"></svg>
<script>
  const E = new window.ToonEngine(), T = E.THEMES[${JSON.stringify(ref.theme)}];
  document.body.style.background = T.bg;
  document.getElementById('s').innerHTML = E.toSvg(${ref.scene});
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

async function render(cdp, ref, dir) {
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
    await cdp.send('Runtime.evaluate', { awaitPromise: true, expression: 'document.fonts.ready.then(() => new Promise((r) => requestAnimationFrame(() => requestAnimationFrame(r))))' }, sessionId);
    if (errors.length) throw new Error(`${ref.group}_${ref.name}: ${errors.join('; ')}`);
    const shot = await cdp.send('Page.captureScreenshot', { format: 'png', clip: { x: 0, y: 0, width: W, height: H, scale: 1 } }, sessionId);
    return Buffer.from(shot.data, 'base64');
  } finally {
    cdp.listeners.splice(cdp.listeners.indexOf(onError), 1);
    await cdp.send('Target.closeTarget', { targetId });
  }
}

const argv = process.argv.slice(2);
const check = argv.includes('--check');
const chrome = argv.includes('--chrome') ? argv[argv.indexOf('--chrome') + 1] : '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const work = mkdtempSync(join(tmpdir(), 'toon-refs-pages-'));
const { cdp, version, close } = await launch(chrome);
const stale = [];
try {
  if (!check) mkdirSync(OUT, { recursive: true });
  for (const ref of REFERENCES) {
    const png = await render(cdp, ref, work);
    const target = join(OUT, `${ref.group}_${ref.name}.png`);
    if (check) { if (!existsSync(target) || !readFileSync(target).equals(png)) stale.push(`${ref.group}_${ref.name}`); }
    else writeFileSync(target, png);
  }
} finally {
  await close();
  rmSync(work, { recursive: true, force: true });
}
const engineSha = createHash('sha256').update(readFileSync(ENGINE)).digest('hex');
const manifest = { browser: version, engine: engineSha, scale: 1, references: REFERENCES.map(({ group, name, theme, w, h, pad, scene }) => ({ file: `${group}_${name}.png`, theme, w, h, pad, scene })) };
if (check) {
  if (stale.length) { console.error(`design references differ from a fresh render in ${version}: ${stale.join(', ')}`); process.exit(1); }
  console.log(`${REFERENCES.length} design references match a fresh render in ${version}`);
} else {
  writeFileSync(join(OUT, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
  console.log(`${REFERENCES.length} design references written with ${version}`);
}
