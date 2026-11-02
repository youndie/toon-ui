// B-12: the FX page's frame strips, as cells to measure the FX port against.
//
// Imported by scripts/generate-fx.mjs (the Kotlin fixtures) and scripts/design-references.mjs (the
// Chrome references). The strips are page code (`toon-ui FX.dc.html`, `buildStatic` and `scene`),
// not data the engine exports, so the timestamps and each effect's call are written here and held
// to the exact text they were read from: a re-import that changes the page fails here instead of
// drifting. A cell draws the effect alone on its theme's stage, with the arguments the page's scene
// gives it; the demo block the page animates under it is a component's choreography, not the FX.
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { THEMES } from './icon-cells.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const fxPage = readFileSync(join(root, 'reference/design-system/toon-ui FX.dc.html'), 'utf8').replaceAll('&quot;', '"');

// The page's own text for every frame list and every effect call taken from it.
const PAGE = [
  "frames: [[140, 20], [220, 100], [320, 200], [400, 280]].map(([t, f]) => F('dust'",
  "E.dust(T, (t - 120) / 300, 98, 112, 46), E.dust(T, (t - 120) / 300, 162, 112, 46)",
  "frames: [30, 120, 240, 330].map(t => F('stars'",
  "const dx = E.shake(t); return E.cat(G, this.blk(T, dx), E.stars(T, t / 350, 130 + dx, 56, 40))",
  "frames: [[70, 10], [120, 60], [230, 170]].map(([t, f]) => F('speed'",
  "else if (t < 160) { const u = (t - 60) / 100; x = -8 + 240 * u * u; sx = 1 + .5 * Math.sin(Math.PI * u);",
  "const lu = (t - 60) / 200, lx = Math.min(90 + x + (80 - 80 * sx) / 2, 262);",
  "E.speed(T, lu, lx, 62, 48)",
  "frames: [10, 40, 90, 150, 200].map(t =>",
  "E.flash(T, t / 220, 130, 84, 46)",
  "frames: [20, 60, 100, 180, 300].map(t => { const v = E.vibe(T, t / 320, 90, 62, 80, 48);",
  "frames: [100, 200, 260, 420, 600].map(t => { const a = E.irisAt(t);",
  "E.cell('', this.scene(k, t, T, from), 180, 320, [0, 0, 0, 0], .42)",
  "frames: [[50, 20], [110, 80], [190, 160], [280, 250]].map(([t, f]) =>",
  "E.hole(T, (t - 30) / 300, 90, 62, 80, 48, [12, 8, 13, 9], 5)",
  "frames: [40, 100, 180, 260, 330].map(t => F('sweat'",
  "E.sweat(T, t / 360, 90, 62, 80)",
];
// And the engine's, for the iris centre the page leaves to `irisScene`'s defaults.
const engine = readFileSync(join(root, 'reference/design-system/toon-engine.js'), 'utf8');
const ENGINE = ["const W = o.W || 180, H = o.H || 320, cx = o.cx ?? W / 2, cy = o.cy ?? H - 46"];
export const missingPageText = [...PAGE.filter(s => !fxPage.includes(s)), ...ENGINE.filter(s => !engine.includes(s))];

// The speed strip's element x at page time t (the page's `scene('speed')` between 60 and 160 ms,
// still 0 after 160 while it is hidden), from which the lines start.
const speedX = t => {
  let x = 0, sx = 1;
  if (t < 60) { x = -8 * t / 60; sx = .96; }
  else if (t < 160) { const u = (t - 60) / 100; x = -8 + 240 * u * u; sx = 1 + .5 * Math.sin(Math.PI * u); }
  return Math.min(90 + x + (80 - 80 * sx) / 2, 262);
};
const num = v => Number(v.toFixed(4));
const kf = v => `${num(v)}f`;

// Per effect: the page's frames as [page time, label ms], the cell, and the call on both sides at that
// time — the engine's JS for the reference, the port's Kotlin for the fixture.
const FX = [
  ['Dust', [[140, 20], [220, 100], [320, 200], [400, 280]], 260, 150, f =>
    [`E.cat(E.dust(T, ${f} / 300, 98, 112, 46), E.dust(T, ${f} / 300, 162, 112, 46))`,
      `drawToonDust(c, ${f}f / 300, 98f, 112f, 46f)\n        drawToonDust(c, ${f}f / 300, 162f, 112f, 46f)`]],
  ['Stars', [30, 120, 240, 330].map(t => [t, t]), 260, 150, t =>
    [`E.stars(T, ${t} / 350, 130 + E.shake(${t}), 56, 40)`, `drawToonStars(c, ${t}f / 350, 130 + toonShake(${t}f), 56f, 40f)`]],
  ['Speed', [[70, 10], [120, 60], [230, 170]], 260, 150, (f, t) =>
    [`E.speed(T, ${f} / 200, ${num(speedX(t))}, 62, 48)`, `drawToonSpeed(c, ${f}f / 200, ${kf(speedX(t))}, 62f, 48f)`]],
  ['Flash', [10, 40, 90, 150, 200].map(t => [t, t]), 260, 150, t =>
    [`E.flash(T, ${t} / 220, 130, 84, 46)`, `drawToonFlash(c, ${t}f / 220, 130f, 84f, 46f)`]],
  ['Shake', [20, 60, 100, 180, 300].map(t => [t, t]), 260, 150, t =>
    [`({ L: E.vibe(T, ${t} / 320, 90, 62, 80, 48).L, X: [] })`, `drawToonVibe(c, ${t}f / 320, 90f, 62f, 80f, 48f)`]],
  // The iris cell keeps irisScene's ink wipe and rings and drops the demo screens either side of it.
  ['Iris', [100, 200, 260, 420, 600].map(t => [t, t]), 180, 320, t =>
    [`({ L: E.irisScene(T, E.irisAt(${t})).L.filter(l => l.fr === 'evenodd' || l.fill === 'none'), X: [] })`,
      `drawToonIris(c, toonIrisAt(${t}f).k, 180f, 320f, 90f, 274f)`]],
  ['Hole', [[50, 20], [110, 80], [190, 160], [280, 250]], 260, 150, f =>
    [`E.hole(T, ${f} / 300, 90, 62, 80, 48, [12, 8, 13, 9], 5)`, `drawToonHole(c, ${f}f / 300, 90f, 62f, 80f, 48f, ToonRadii(12f, 8f, 13f, 9f), 5)`]],
  ['Sweat', [40, 100, 180, 260, 330].map(t => [t, t]), 260, 150, t =>
    [`E.sweat(T, ${t} / 360, 90, 62, 80)`, `drawToonSweat(c, ${t}f / 360, 90f, 62f, 80f)`]],
];

/** One cell per theme × effect × frame of the FX page's strips. */
export const FX_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) =>
  FX.flatMap(([fx, frames, w, h, call]) => frames.map(([t, label]) => {
    const [js, kotlin] = call(label, t);
    return { group: 'Fx', name: `${fx}_${label}ms_${themeName}`, theme, themeName, w, h, pad: [0, 0, 0, 0], scene: js, kotlin };
  })));
