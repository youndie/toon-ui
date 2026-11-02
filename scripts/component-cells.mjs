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

// ---- IconButton and Badge (B-15): `toon-ui MVP Components.dc.html`, `buildStatic` ----
const MVP = page('toon-ui MVP Components.dc.html');
const MVP_PAGE = [
  "out.ib = { cells: [['Standard / Default', { icon: 'heart' }], ['Standard / Pressed', { icon: 'heart', p: 1 }], ['Standard / Focused', { icon: 'heart', focus: true }], ['Standard / Selected', { icon: 'heart', sel: true }], ['Standard / Disabled', { icon: 'heart', disabled: true }], ['Filled / Default', { icon: 'edit', variant: 'filled' }], ['Filled / Selected', { icon: 'star', variant: 'filled', sel: true }]]",
  ".map(([n, o]) => C('IconButton / ' + n, E.iconBtn(T, o), 48, 48, [10, 12, 10, 10])),",
  "frames: [['0 ms', { p: 0 }], ['80 ms · held', { p: 1 }], ...[70, 140, 200, 300].map(t => ['+' + t + ' ms', { p: E.spr(t / 1000, .35, 45), iy: E.iconBounce(t) }])].map(([t, o]) => Fr(t, `sY ${n2(E.squashOf(o.p).sy)} · icon y ${n2(o.iy || 0)}`, E.iconBtn(T, { icon: 'heart', sel: t !== '0 ms' && t !== '80 ms · held', ...o }), 48, 48, [18, 10, 8, 10])) }] };",
  "out.badge = { cells: [['Dot', { dot: true }], ['1 digit', { n: 3 }], ['2 digits', { n: 12 }], ['Overflow', { n: '99+' }]].map(([n, o]) => C('Badge / ' + n, E.badge(T, o), 0, 0, [20, 30, 20, 30]))",
  ".concat([C('Badge / On IconButton', E.cat(E.iconBtn(T, { icon: 'bell' }), E.place(E.badge(T, { n: 3 }), 40, 6)), 48, 48, [12, 22, 10, 10])]),",
  "frames: [0, 42, 90, 180, 280].map(t => { const a = E.badgeAppear(t); return Fr(`${t} ms`, `scale ${n2(a.s)} · number ${a.numOp ? 'on' : 'off'}`, E.badge(T, { n: 3, ...a }), 0, 0, [22, 30, 22, 30]); }) },",
  "frames: [0, 60, 120, 180, 240].map(t => { const a = E.badgeCount(t); return Fr(`${t} ms`, `old y ${n0(a.oldDy)} · new y ${n0(a.numDy)} · x ${n2(a.dx)}`, E.badge(T, { n: 4, old: 3, ...a }), 0, 0, [22, 30, 22, 30]); }) },",
];
const pascal = s => s[0].toUpperCase() + s.slice(1);
// A Kotlin call as ktlint writes it: on one line when it fits in 120 columns at [indent], else one
// argument per line with a trailing comma. A node is a string or { f, a: [nodes] }.
const call = (f, ...a) => ({ f, a });
const fmt = (node, indent) => {
  if (typeof node === 'string') return node;
  const args = node.a.map(x => fmt(x, indent));
  const one = `${node.f}(${args.join(', ')})`;
  if (!args.some(x => x.includes('\n')) && indent + one.length + 1 <= 120) return one;
  const pad = ' '.repeat(indent + 4);
  return `${node.f}(\n${node.a.map(x => `${pad}${fmt(x, indent + 4)},\n`).join('')}${' '.repeat(indent)})`;
};
const rounded = o => Object.fromEntries(Object.entries(o).map(([k, x]) => [k, typeof x === 'number' ? num(x) : x]));
// A cell drawn by SceneCell: the page's padding, the frame's origin at its left and top.
const sceneCell = (group, theme, themeName, name, w, h, pad, opts, scene, kotlinScene) => ({
  group, name: `${name}_${themeName}`, theme, themeName, w, h, pad, opts: rounded(opts), scene,
  kotlin: `SceneCell(${TOKENS[theme]}, ${w + pad[1] + pad[3]}, ${h + pad[0] + pad[2]}, ${pad[3]}, ${pad[0]}) {\n        ${fmt(kotlinScene, 8)}\n    }`,
});
const ibLook = o => {
  const a = [];
  if (o.p) a.push(`p = ${kf(o.p)}`);
  if (o.iy) a.push(`iy = ${kf(o.iy)}`);
  if (o.sel) a.push('selected = true');
  if (o.disabled) a.push('disabled = true');
  if (o.focus) a.push('focus = true');
  return call('ToonIconButtonLook', ...a);
};
const ibScene = o => call('toonIconButtonScene', 'tokens', `ToonIcons.${pascal(o.icon)}`, `ToonIconButtonVariant.${o.variant === 'filled' ? 'Filled' : 'Standard'}`, ibLook(o));
const IB_CELLS = [['Standard / Default', { icon: 'heart' }], ['Standard / Pressed', { icon: 'heart', p: 1 }], ['Standard / Focused', { icon: 'heart', focus: true }], ['Standard / Selected', { icon: 'heart', sel: true }], ['Standard / Disabled', { icon: 'heart', disabled: true }], ['Filled / Default', { icon: 'edit', variant: 'filled' }], ['Filled / Selected', { icon: 'star', variant: 'filled', sel: true }]];
const IB_TAP = [['0ms', { p: 0 }], ['80msHeld', { p: 1 }], ...[70, 140, 200, 300].map(t => [`Plus${t}ms`, { p: E.spr(t / 1000, .35, 45), iy: E.iconBounce(t) }])]
  .map(([t, o]) => [t, { icon: 'heart', sel: t !== '0ms' && t !== '80msHeld', ...o }]);
export const ICON_BUTTON_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...IB_CELLS.map(([n, o]) => sceneCell('IconButton', theme, themeName, n.replace(' / ', '_'), 48, 48, [10, 12, 10, 10], o, `E.iconBtn(T, ${js(o)})`, ibScene(o))),
  ...IB_TAP.map(([t, o]) => sceneCell('IconButton', theme, themeName, `Tap_${t}`, 48, 48, [18, 10, 8, 10], o, `E.iconBtn(T, ${js(o)})`, ibScene(o))),
]);

const badgeLook = o => {
  const a = [o.dot ? '""' : JSON.stringify(String(o.n))];
  if (o.dot) a.push('dot = true');
  for (const k of ['s', 'numOp', 'numDy', 'oldOp', 'oldDy', 'dx']) if (o[k] != null) a.push(`${k} = ${kf(o[k])}`);
  if (o.old != null) a.push(`old = ${JSON.stringify(String(o.old))}`);
  return call('ToonBadgeLook', ...a);
};
const BADGE = [['Dot', { dot: true }], ['1 digit', { n: 3 }], ['2 digits', { n: 12 }], ['Overflow', { n: '99+' }]];
const BADGE_STRIPS = [
  ...[0, 42, 90, 180, 280].map(t => [`Appear_${t}ms`, { n: 3, ...E.badgeAppear(t) }]),
  ...[0, 60, 120, 180, 240].map(t => [`Count_${t}ms`, { n: 4, old: 3, ...E.badgeCount(t) }]),
];
export const BADGE_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...BADGE.map(([n, o]) => sceneCell('Badge', theme, themeName, n.replace(' ', ''), 0, 0, [20, 30, 20, 30], o, `E.badge(T, ${js(o)})`, call('toonBadgeScene', 'tokens', badgeLook(o)))),
  sceneCell('Badge', theme, themeName, 'OnIconButton', 48, 48, [12, 22, 10, 10], { onIconButton: true },
    "E.cat(E.iconBtn(T, { icon: 'bell' }), E.place(E.badge(T, { n: 3 }), 40, 6))",
    'toonIconButtonScene(tokens, ToonIcons.Bell, ToonIconButtonVariant.Standard, ToonIconButtonLook()) +\n            toonBadgeScene(tokens, ToonBadgeLook("3")).placed(40f, 6f)'),
  ...BADGE_STRIPS.map(([n, o]) => sceneCell('Badge', theme, themeName, n, 0, 0, [22, 30, 22, 30], o, `E.badge(T, ${js(o)})`, call('toonBadgeScene', 'tokens', badgeLook(o)))),
]);

// ---- FAB (B-16): `toon-ui MVP Components.dc.html`, `buildStatic` ----
const FAB_PAGE = [
  "out.fab = { cells: [['Default', {}], ['Pressed', { p: 1 }], ['Focused', { focus: true }], ['Disabled', { disabled: true }], ['Extended', { label: 'New plan' }]].map(([n, o]) => { const f = E.fab(T, o); return C('FAB / ' + n, f, f.w, 64, [14, 18, 14, 14]); }),",
  "reduce: 'fade 120', desc: 'Appears in the air, hangs for 90 ms",
  "frames: [0, 60, 160, 230, 300, 520].map(t => { const a = E.fabAppear(t); return Fr(`${t} ms`, `y ${n0(a.dy)} · sX ${n2(a.sx)} · sY ${n2(a.sy)}`, E.cat(E.place(E.fab(T, { sx: a.sx, sy: a.sy }), 0, a.dy, 0, 0, 0, a.op), E.dust(T, a.du, 32, 66, 40)), 64, 64, [80, 34, 14, 34], .75); }) }] };",
];
const fabLook = o => {
  const a = [];
  if (o.p) a.push(`p = ${kf(o.p)}`);
  if (o.sx != null && o.sx !== 1) a.push(`sx = ${kf(o.sx)}`);
  if (o.sy != null && o.sy !== 1) a.push(`sy = ${kf(o.sy)}`);
  if (o.disabled) a.push('disabled = true');
  if (o.focus) a.push('focus = true');
  return call('ToonFabLook', ...a);
};
const FAB_STATES = [['Default', {}], ['Pressed', { p: 1 }], ['Focused', { focus: true }], ['Disabled', { disabled: true }], ['Extended', { label: 'New plan' }]];
export const FAB_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...FAB_STATES.map(([n, o]) => {
    // The cell is the engine's FAB width as Node measures the label (length × 17 × 0.55); Chrome and
    // Compose measure it narrower, so the extended FAB sits a little inside its cell.
    const w = Math.ceil(E.fab(E.THEMES[theme], o).w);
    const width = o.label ? `text.measure(${JSON.stringify(o.label)}, FAB_LABEL).multiParagraph.width` : '0f';
    return sceneCell('Fab', theme, themeName, n, w, 64, [14, 18, 14, 14], o, `E.fab(T, ${js(o)})`,
      call('toonFabScene', 'tokens', 'ToonIcons.Plus', JSON.stringify(o.label ?? ''), width, fabLook(o)));
  }),
  ...[0, 60, 160, 230, 300, 520].map(t => {
    const a = E.fabAppear(t);
    return sceneCell('Fab', theme, themeName, `Land_${t}ms`, 64, 64, [80, 34, 14, 34], { land: true, ...a },
      `E.cat(E.place(E.fab(T, ${js({ sx: a.sx, sy: a.sy })}), 0, ${num(a.dy)}, 0, 0, 0, ${num(a.op)}), E.dust(T, ${num(a.du)}, 32, 66, 40))`,
      call('toonFabLandScene', 'tokens', 'ToonIcons.Plus', '""', '0f', fabLook({ sx: a.sx, sy: a.sy }), call('ToonFabAppear', kf(a.op), kf(a.dy), kf(a.sx), kf(a.sy), kf(a.du))));
  }),
]);

// ---- Checkbox (B-17): `toon-ui Core.dc.html`; RadioButton: `toon-ui MVP Components.dc.html` ----
const CHECK_PAGE = [
  "const kCells = [['Unchecked', {}], ['Checked', { on: true }], ['Pressed', { sx: .86, sy: .86 }], ['Focused', { on: true, focus: true }], ['Error', { error: true }], ['Disabled', { disabled: true }], ['Disabled / Checked', { disabled: true, on: true }]]",
  ".map(([S, o]) => C(`Checkbox / ${S}`, this.check(T, { label: 'Bring snacks', ...o }), 180, 48, [8, 8, 8, 8]));",
  "const kF = (t, a, v) => Fr(t, v, this.check(T, a), 48, 48, [6, 10, 6, 4]);",
  "frames: [0, 50, 110, 170, 240].map(t => { const a = this.chkAnim(t, true);",
  "frames: [0, 50, 100, 115, 160].map(t => { const a = this.chkAnim(t, false);",
];
const RADIO_PAGE = [
  "out.radio = { cells: [['Unselected', {}], ['Selected', { sel: true }], ['Pressed', { sel: true, sq: .86 }], ['Focused', { sel: true, focus: true }], ['Disabled', { disabled: true }], ['Disabled / Selected', { disabled: true, sel: true }]].map(([n, o]) => C('RadioButton / ' + n, E.radio(T, { label: 'Noon', ...o }), 110, 48, [8, 8, 8, 8])),",
  "frames: [0, 100, 150, 190, 240, 340].map(t => { const a = E.radioSelect(t); return Fr(`${t} ms`, `dot y ${n0(a.dotY)} · sX ${n2(a.dsx)} · sY ${n2(a.dsy)}`, E.radio(T, { sel: true, ...a }), 48, 48, [32, 8, 6, 8]); }) }] };",
];
const checkLook = o => {
  const a = [];
  if (o.on) a.push('on = true');
  if (o.trim != null) a.push(`trim = ${kf(o.trim)}`);
  for (const k of ['sx', 'sy', 'dx', 'sp']) if (o[k] != null && o[k] !== (k.startsWith('s') && k !== 'sp' ? 1 : 0)) a.push(`${k} = ${kf(o[k])}`);
  if (o.disabled) a.push('disabled = true');
  if (o.focus) a.push('focus = true');
  if (o.error) a.push('error = true');
  return call('ToonCheckboxLook', ...a);
};
const CHECK_CELLS = [['Unchecked', {}], ['Checked', { on: true }], ['Pressed', { sx: .86, sy: .86 }], ['Focused', { on: true, focus: true }], ['Error', { error: true }], ['Disabled', { disabled: true }], ['Disabled / Checked', { disabled: true, on: true }]];
export const CHECKBOX_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...CHECK_CELLS.map(([n, o]) => sceneCell('Checkbox', theme, themeName, n.replace(' / ', '_'), 180, 48, [8, 8, 8, 8], { label: 'Bring snacks', ...o },
    `E.check(T, ${js({ label: 'Bring snacks', ...o })})`, call('toonCheckboxScene', 'tokens', '"Bring snacks"', checkLook(o)))),
  ...[['Check', true, [0, 50, 110, 170, 240]], ['Uncheck', false, [0, 50, 100, 115, 160]]].flatMap(([name, on, ts]) => ts.map(t => {
    const a = E.chkAnim(t, on);
    return sceneCell('Checkbox', theme, themeName, `${name}_${t}ms`, 48, 48, [6, 10, 6, 4], a, `E.check(T, ${js(a)})`, call('toonCheckboxScene', 'tokens', '""', checkLook(a)));
  })),
]);
const radioLook = o => {
  const a = [];
  if (o.sel) a.push('selected = true');
  for (const [k, d] of [['ds', null], ['dotY', 0], ['dsx', 1], ['dsy', 1], ['sp', -1], ['sq', 1]]) if (o[k] != null && o[k] !== d) a.push(`${k} = ${kf(o[k])}`);
  if (o.disabled) a.push('disabled = true');
  if (o.focus) a.push('focus = true');
  return call('ToonRadioLook', ...a);
};
const RADIO_CELLS_SPEC = [['Unselected', {}], ['Selected', { sel: true }], ['Pressed', { sel: true, sq: .86 }], ['Focused', { sel: true, focus: true }], ['Disabled', { disabled: true }], ['Disabled / Selected', { disabled: true, sel: true }]];
export const RADIO_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...RADIO_CELLS_SPEC.map(([n, o]) => sceneCell('Radio', theme, themeName, n.replace(' / ', '_'), 110, 48, [8, 8, 8, 8], { label: 'Noon', ...o },
    `E.radio(T, ${js({ label: 'Noon', ...o })})`, call('toonRadioScene', 'tokens', '"Noon"', radioLook(o)))),
  ...[0, 100, 150, 190, 240, 340].map(t => {
    const a = { sel: true, ...E.radioSelect(t) };
    return sceneCell('Radio', theme, themeName, `Drop_${t}ms`, 48, 48, [32, 8, 6, 8], a, `E.radio(T, ${js(a)})`, call('toonRadioScene', 'tokens', '""', radioLook(a)));
  }),
]);

export const missingPageText = [
  ...CHECK_PAGE.filter(s => !CORE.includes(s)).map(s => `toon-ui Core.dc.html: ${s}`),
  ...RADIO_PAGE.filter(s => !MVP.includes(s)).map(s => `toon-ui MVP Components.dc.html: ${s}`),
  ...FAB_PAGE.filter(s => !MVP.includes(s)).map(s => `toon-ui MVP Components.dc.html: ${s}`),
  ...BUTTON_PAGE.filter(s => !CORE.includes(s)).map(s => `toon-ui Core.dc.html: ${s}`),
  ...MVP_PAGE.filter(s => !MVP.includes(s)).map(s => `toon-ui MVP Components.dc.html: ${s}`),
];

/** Every component's cells, by group: the fixture file each generates. */
export const COMPONENT_CELLS = { TextFloor: TEXT_FLOOR_CELLS, Button: BUTTON_CELLS, ButtonShape: BUTTON_SHAPE_CELLS, IconButton: ICON_BUTTON_CELLS, Badge: BADGE_CELLS, Fab: FAB_CELLS, Checkbox: CHECKBOX_CELLS, Radio: RADIO_CELLS };
