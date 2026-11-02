// B-14: the components' cells on the spec pages, to measure the Compose components against.
//
// Imported by scripts/generate-components.mjs (one Kotlin fixture file per component) and by
// scripts/design-references.mjs (the Chrome references). The spec pages lay their cells out in page
// code, not in data the engine exports, so each cell list and frame strip is written here and held to
// the exact text it was read from: a re-import that changes a page fails here instead of drifting.
// The cell is drawn by the engine's renderer — the ruler — with the page's options, size and padding.
// A component's entry adds itself below when its item builds it.
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';
import { THEMES } from './icon-cells.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const decode = s => s.replaceAll('&quot;', '"').replaceAll('&lt;', '<').replaceAll('&gt;', '>').replaceAll('&amp;', '&');
const page = name => decode(readFileSync(join(root, 'reference/design-system', name), 'utf8'));
globalThis.window = globalThis.window || {};
createRequire(import.meta.url)(join(root, 'reference/design-system/toon-engine.js'));
const E = new globalThis.window.ToonEngine();

const num = v => Number(v.toFixed(6));
const kf = v => `${num(v)}f`;

// ---- Button (B-14): `toon-ui Core.dc.html`, `buildStatic` ----
const CORE = page('toon-ui Core.dc.html');
const BUTTON_PAGE = [
  "[['filled', 'Filled'], ['outlined', 'Outlined'], ['text', 'Text']].forEach(([v, V]) => {",
  "[['Default', {}], ['Pressed', { p: 1 }], ['Focused', { focus: true }], ['Disabled', { disabled: true }], ['Loading', { lt: 400, spin: 600 }]].forEach(([S, o]) => {",
  "bCells.push(C(`Button / ${V} / ${S}`, this.btn(T, { v, w: 150, label: 'Add plan', ...o }), 150, 52, [14, 16, 16, 14]));",
  "const bF = (t, o, v) => Fr(t, v, this.btn(T, { v: 'filled', w: 150, label: 'Add plan', ...o }), 150, 52, [40, 16, 14, 14]);",
  "frames: [0, 40, 80].map(t => { const p = 1 - Math.pow(1 - t / 80, 3); return bF(",
  "frames: [0, 74, 149, 223, 320].map(t => { const p = this.spr(t / 1000, .35, 45); return bF(`${t} ms`, { p, pl: this.spr(Math.max(0, t - 40) / 1000, .35, 45) }, pv(p)); }) },",
  "frames: [[0, 'label y 0 · α 1'], [60, 'label y −5.5 · α 0.54'], [130, 'label gone · blob falls'], [200, 'blob lands · sX 1.22 sY 0.80'], [260, 'blob settles · spin starts']].map(([t, v]) => bF(`${t} ms`, { lt: t, spin: 0 }, v)) },",
  "frames: [[0, 1, 0], [1250, 1.015, 1], [2500, 1, 2]].map(([t, br, fr]) => bF(`${t} ms`, { br, seed: fr * 31 }, `scale ${br.toFixed(3)} · path v${fr + 1}`)) },",
];
const VARIANTS = [['filled', 'Filled'], ['outlined', 'Outlined'], ['text', 'Text']];
const STATES = [['Default', {}], ['Pressed', { p: 1 }], ['Focused', { focus: true }], ['Disabled', { disabled: true }], ['Loading', { lt: 400, spin: 600 }]];
const STRIPS = [
  ['Press', [0, 40, 80].map(t => { const p = 1 - Math.pow(1 - t / 80, 3); return [t, { p }]; })],
  ['Release', [0, 74, 149, 223, 320].map(t => [t, { p: E.spr(t / 1000, .35, 45), pl: E.spr(Math.max(0, t - 40) / 1000, .35, 45) }])],
  ['Loading', [0, 60, 130, 200, 260].map(t => [t, { lt: t, spin: 0 }])],
  ['Idle', [[0, 1, 0], [1250, 1.015, 1], [2500, 1, 2]].map(([t, br, fr]) => [t, { br, seed: fr * 31 }])],
];
// An option set as the Kotlin look: the engine's option names, mapped one to one.
const look = o => {
  const args = [];
  if (o.p != null) args.push(`p = ${kf(o.p)}`);
  if (o.pl != null) args.push(`pl = ${kf(o.pl)}`);
  if (o.br != null) args.push(`br = ${kf(o.br)}`);
  if (o.seed) args.push(`seed = ${o.seed}`);
  if (o.disabled) args.push('disabled = true');
  if (o.focus) args.push('focus = true');
  if (o.lt != null) args.push(`lt = ${kf(o.lt)}`);
  if (o.spin) args.push(`spin = ${kf(o.spin)}`);
  return `ToonButtonLook(${args.join(', ')})`;
};
const js = o => JSON.stringify(Object.fromEntries(Object.entries(o).map(([k, v]) => [k, typeof v === 'number' ? num(v) : v])));
const buttonCell = (theme, themeName, name, v, o, pad, strip, label = 'Add plan') => ({
  group: label ? 'Button' : 'ButtonShape', name: `${name}_${themeName}`, theme, themeName, w: 150, h: 52, pad,
  opts: { v, ...Object.fromEntries(Object.entries(o).map(([k, x]) => [k, typeof x === 'number' ? num(x) : x])) },
  scene: `E.btn(T, Object.assign({ v: ${JSON.stringify(v)}, w: 150, label: ${JSON.stringify(label)} }, ${js(o)}))`,
  kotlin: `${label ? '' : 'Shape'}${strip ? `StripCell(${TOKENS[theme]}, ${look(o)})` : `StateCell(${TOKENS[theme]}, ToonButtonVariant.${v[0].toUpperCase() + v.slice(1)}, ${look(o)})`}`,
});
const TOKENS = { il: 'inkGouacheLight', in: 'inkGouacheNight', bc: 'brightCel' };

const buttonCells = label => Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...VARIANTS.flatMap(([v, V]) => STATES.map(([S, o]) => buttonCell(theme, themeName, `${V}_${S}`, v, o, [14, 16, 16, 14], false, label))),
  ...STRIPS.flatMap(([strip, frames]) => frames.map(([t, o]) => buttonCell(theme, themeName, `${strip}_${t}ms`, 'filled', o, [40, 16, 14, 14], true, label))),
]);
export const BUTTON_CELLS = buttonCells('Add plan');
// The same cells without the label: the shape alone, read against the Divider's floor, since the
// text floor (below) is several times coarser and would hide a wrong shadow or squash.
export const BUTTON_SHAPE_CELLS = buttonCells('');

// ---- The text floor (B-14) ----
// The Divider's floor has no text in it. A label is drawn by Chrome through CoreText and by Compose
// through Skia, which ink a glyph differently (Chrome's "Add plan" has 12 % more dark pixels than
// Skia's, in the same box), so the label alone is measured in each theme, at the button's place and
// style, as the floor a component with text is read against.
export const TEXT_FLOOR_CELLS = Object.entries(THEMES).map(([theme, themeName]) => ({
  group: 'TextFloor', name: `Label_${themeName}`, theme, themeName, w: 150, h: 52, pad: [14, 16, 16, 14],
  scene: `({ L: [], X: [E.X('Add plan', 75, 52 / 2 + 18 * .35, { ff: T.body, fw: 700, fs: 18, fill: T.text, anchor: 'middle' })] })`,
  kotlin: `LabelCell(${TOKENS[theme]})`,
}));

export const missingPageText = BUTTON_PAGE.filter(s => !CORE.includes(s)).map(s => `toon-ui Core.dc.html: ${s}`);

/** Every component's cells, by group: the fixture file each generates. */
export const COMPONENT_CELLS = { TextFloor: TEXT_FLOOR_CELLS, Button: BUTTON_CELLS, ButtonShape: BUTTON_SHAPE_CELLS };
