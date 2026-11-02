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
// A trailing [lambda] is passed as the call's last argument, outside its parentheses.
const sceneCell = (group, theme, themeName, name, w, h, pad, opts, scene, kotlinScene, lambda) => ({
  group, name: `${name}_${themeName}`, theme, themeName, w, h, pad, opts: rounded(opts), scene,
  kotlin: `SceneCell(${TOKENS[theme]}, ${w + pad[1] + pad[3]}, ${h + pad[0] + pad[2]}, ${pad[3]}, ${pad[0]}) {\n        ${fmt(kotlinScene, 8)}${lambda ? ` ${lambda}` : ''}\n    }`,
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

// ---- Switch (B-18): `toon-ui Core.dc.html` ----
const SWITCH_PAGE = [
  "const sCells = [['Off', { on: false }], ['On', { on: true }], ['Pressed', { on: true, pressed: true, sx: 1.18, sy: .84 }], ['Focused', { on: true, focus: true }], ['Disabled', { on: false, disabled: true }], ['Disabled / On', { on: true, disabled: true }]]",
  ".map(([S, o]) => C(`Switch / ${S}`, this.sw(T, o), 60, 48, [8, 14, 8, 10]));",
  "frames: [0, 40, 90, 140, 220, 320].map(t => { const a = this.swAnim(t, 0, 1); return Fr(`${t} ms`, `x ${(a.pos * 26).toFixed(0)} · sX ${a.sx.toFixed(2)} · sY ${a.sy.toFixed(2)}`, this.sw(T, { ...a, on: a.trackOn }), 60, 48, [6, 12, 6, 10]); }) },",
  "reduce: 'instant', desc: 'Thumb pulls back 40 ms",
];
const ORIGIN = { c: 'Centre', l: 'Left', r: 'Right' };
const switchLook = o => {
  const a = [];
  if (o.on) a.push('on = true');
  if (o.pos != null) a.push(`pos = ${kf(o.pos)}`);
  if (o.sx != null && o.sx !== 1) a.push(`sx = ${kf(o.sx)}`);
  if (o.sy != null && o.sy !== 1) a.push(`sy = ${kf(o.sy)}`);
  if (o.origin && o.origin !== 'c') a.push(`origin = ToonSwitchOrigin.${ORIGIN[o.origin]}`);
  if (o.trackOn != null && o.trackOn !== !!o.on) a.push(`trackOn = ${o.trackOn}`);
  if (o.echo) a.push(`echo = ${kf(o.echo)}`);
  if (o.wave != null) a.push(`wave = ${kf(o.wave)}`);
  if (o.pressed) a.push('pressed = true');
  if (o.disabled) a.push('disabled = true');
  if (o.focus) a.push('focus = true');
  return call('ToonSwitchLook', ...a);
};
const SWITCH_STATES = [['Off', { on: false }], ['On', { on: true }], ['Pressed', { on: true, pressed: true, sx: 1.18, sy: .84 }], ['Focused', { on: true, focus: true }], ['Disabled', { on: false, disabled: true }], ['Disabled / On', { on: true, disabled: true }]];
export const SWITCH_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...SWITCH_STATES.map(([n, o]) => sceneCell('Switch', theme, themeName, n.replace(' / ', '_'), 60, 48, [8, 14, 8, 10], o, `E.sw(T, ${js(o)})`, call('toonSwitchScene', 'tokens', switchLook(o)))),
  ...[0, 40, 90, 140, 220, 320].map(t => {
    const a = E.swAnim(t, 0, 1), o = { ...a, on: a.trackOn };
    return sceneCell('Switch', theme, themeName, `Toggle_${t}ms`, 60, 48, [6, 12, 6, 10], o, `E.sw(T, ${js(o)})`, call('toonSwitchScene', 'tokens', switchLook(o)));
  }),
]);

// ---- TextField (B-19): `toon-ui Core.dc.html` ----
const FIELD_PAGE = [
  "['Default', { label: 'Plan name' }], ['Focused', { label: 'Plan name', focus: true, value: 'Pancakes', caret: true }],",
  "['Filled', { label: 'Plan name', value: 'Pancakes' }], ['Error', { label: 'Plan name', value: 'Pan', error: true, helper: 'At least 4 letters' }],",
  "['Disabled', { label: 'Plan name', disabled: true }],",
  "].map(([S, o]) => C(`TextField / ${S}`, this.field(T, { w: 280, ...o }), 280, 56, [26, 14, 32, 14]));",
  "const fF = (t, o, v) => Fr(t, v, this.cat(this.field(T, { w: 240, label: 'Plan name', ...o }), o.stars ?? { L: [], X: [] }), 240, 56, [30, 14, 34, 14]);",
  "frames: [0, 90, 170, 260].map(t => { const a = this.fldFocus(t, true); return fF(`${t} ms`, { focus: true, ...a, caret: t > 200 }",
  "frames: [0, 30, 90, 150, 230, 400].map(t => { const x = this.shake(t); return Fr(`${t} ms`, `x ${x >= 0 ? '+' : ''}${x.toFixed(1)} · stars ${Math.round(this.c01(t / 350) * 100)} %`, this.place(this.cat(this.field(T, { w: 240, label: 'Plan name', value: 'Pan', error: true, focus: true }), this.stars(T, t / 350, 210, -4, 26)), x, 0), 240, 56, [40, 14, 14, 14]); }) },",
  "frames: [[0, true], [500, false], [800, true]].map(([t, c]) => fF(`${t} ms`, { focus: true, value: 'Pan', caret: c }, c ? 'caret on' : 'caret off')) },",
];
const fieldLook = o => {
  const a = [`label = ${JSON.stringify(o.label)}`];
  if (o.value) a.push(`value = ${JSON.stringify(o.value)}`);
  if (o.helper) a.push(`helper = ${JSON.stringify(o.helper)}`);
  if (o.focus) a.push('focus = true');
  for (const k of ['lift', 'thick', 'bounce']) if (o[k] != null) a.push(`${k} = ${kf(o[k])}`);
  if (o.error) a.push('error = true');
  if (o.disabled) a.push('disabled = true');
  if (o.caret) a.push(`caretX = engineCaretX(${JSON.stringify(o.value || '')})`);
  return call('ToonFieldLook', ...a);
};
const FIELD_STATES = [['Default', { label: 'Plan name' }], ['Focused', { label: 'Plan name', focus: true, value: 'Pancakes', caret: true }], ['Filled', { label: 'Plan name', value: 'Pancakes' }], ['Error', { label: 'Plan name', value: 'Pan', error: true, helper: 'At least 4 letters' }], ['Disabled', { label: 'Plan name', disabled: true }]];
export const FIELD_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...FIELD_STATES.map(([n, o]) => sceneCell('TextField', theme, themeName, n, 280, 56, [26, 14, 32, 14], o, `E.field(T, ${js({ w: 280, ...o })})`, call('toonFieldScene', 'tokens', '280f', fieldLook(o)))),
  ...[0, 90, 170, 260].map(t => {
    const o = { label: 'Plan name', focus: true, ...E.fldFocus(t, true), caret: t > 200 };
    return sceneCell('TextField', theme, themeName, `Focus_${t}ms`, 240, 56, [30, 14, 34, 14], o, `E.field(T, ${js({ w: 240, ...o })})`, call('toonFieldScene', 'tokens', '240f', fieldLook(o)));
  }),
  ...[0, 30, 90, 150, 230, 400].map(t => {
    const x = E.shake(t), o = { label: 'Plan name', value: 'Pan', error: true, focus: true };
    return sceneCell('TextField', theme, themeName, `Error_${t}ms`, 240, 56, [40, 14, 14, 14], { ...o, shake: x, stars: t / 350 },
      `E.place(E.cat(E.field(T, ${js({ w: 240, ...o })}), E.stars(T, ${num(t / 350)}, 210, -4, 26)), ${num(x)}, 0)`,
      call('placedAt', call('plus', call('toonFieldScene', 'tokens', '240f', fieldLook(o)), call('toonStarsScene', 'tokens.colors', kf(t / 350), '210f', '-4f', '26f')), kf(x)));
  }),
  ...[[0, true], [500, false], [800, true]].map(([t, c]) => {
    const o = { label: 'Plan name', focus: true, value: 'Pan', caret: c };
    return sceneCell('TextField', theme, themeName, `Caret_${t}ms`, 240, 56, [30, 14, 34, 14], o, `E.field(T, ${js({ w: 240, ...o })})`, call('toonFieldScene', 'tokens', '240f', fieldLook(o)));
  }),
]);

// ---- Chip (B-20): `toon-ui MVP Components.dc.html` ----
const CHIP_PAGE = [
  "out.chip = { cells: [['Filter / Default', { variant: 'filter', label: 'Outdoors' }], ['Filter / Selected', { variant: 'filter', label: 'Outdoors', sel: true }], ['Filter / Pressed', { variant: 'filter', label: 'Outdoors', p: 1 }], ['Assist', { variant: 'assist', label: 'Saturday', icon: 'calendar' }], ['Input', { variant: 'input', label: 'Pancakes' }], ['Focused', { variant: 'filter', label: 'Outdoors', focus: true }], ['Disabled', { variant: 'filter', label: 'Outdoors', disabled: true }]]",
  ".map(([n, o]) => { const c = E.chip(T, o); return C('Chip / ' + n, c, c.w, 36, [10, 12, 12, 10]); }),",
  "reduce: 'instant', desc: 'Inflates to 1.15",
  "frames: [0, 60, 90, 150, 260].map(t => { const a = E.chipSelect(t), c = E.chip(T, { variant: 'filter', label: 'Outdoors', sel: true, ...a }); return Fr(`${t} ms`, `scale ${n2(a.scale)} · paint ${n0(a.swipe * 100)} %`, c, c.w, 36, [12, 16, 12, 12]); }) }] };",
];
const chipLook = o => {
  const a = [];
  if (o.sel) a.push('selected = true');
  if (o.scale != null && o.scale !== 1) a.push(`scale = ${kf(o.scale)}`);
  if (o.swipe != null) a.push(`swipe = ${kf(o.swipe)}`);
  if (o.p) a.push(`p = ${kf(o.p)}`);
  if (o.focus) a.push('focus = true');
  if (o.disabled) a.push('disabled = true');
  return call('ToonChipLook', ...a);
};
const chipScene = o => call('toonChipScene', 'tokens', `ToonChipVariant.${o.variant[0].toUpperCase() + o.variant.slice(1)}`, JSON.stringify(o.label),
  `text.measure(${JSON.stringify(o.label)}, CHIP_LABEL).multiParagraph.width`, `ToonIcons.${pascal(o.icon || 'calendar')}`, chipLook(o));
const CHIP_STATES = [['Filter / Default', { variant: 'filter', label: 'Outdoors' }], ['Filter / Selected', { variant: 'filter', label: 'Outdoors', sel: true }], ['Filter / Pressed', { variant: 'filter', label: 'Outdoors', p: 1 }], ['Assist', { variant: 'assist', label: 'Saturday', icon: 'calendar' }], ['Input', { variant: 'input', label: 'Pancakes' }], ['Focused', { variant: 'filter', label: 'Outdoors', focus: true }], ['Disabled', { variant: 'filter', label: 'Outdoors', disabled: true }]];
// A cell is the engine's chip width as Node measures the label (length × 15 × 0.55), wider than
// Chrome's and Compose's own, so the chip sits a little inside it.
export const CHIP_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...CHIP_STATES.map(([n, o]) => sceneCell('Chip', theme, themeName, n.replace(' / ', '_'), Math.ceil(E.chip(E.THEMES[theme], o).w), 36, [10, 12, 12, 10], o, `E.chip(T, ${js(o)})`, chipScene(o))),
  ...[0, 60, 90, 150, 260].map(t => {
    const o = { variant: 'filter', label: 'Outdoors', sel: true, ...E.chipSelect(t) };
    return sceneCell('Chip', theme, themeName, `Select_${t}ms`, Math.ceil(E.chip(E.THEMES[theme], o).w), 36, [12, 16, 12, 12], o, `E.chip(T, ${js(o)})`, chipScene(o));
  }),
]);

// ---- Card (B-21): `toon-ui Core.dc.html` ----
const CARD_PAGE = [
  "{ title: 'Pancake breakfast', meta: 'Kitchen, bring syrup', time: '9:00' },",
  "{ title: 'Bike to the lake', meta: 'Check the tyre pump', time: '11:30' },",
  "{ title: 'Paint the fence', meta: 'Green this time', time: '14:00' },",
  "const cCells = [['Default', {}], ['Pressed', { press: 1 }], ['Selected', { sel: true }], ['Disabled', { dis: true }]]",
  ".map(([S, o], i) => C(`Card / ${S}`, this.place(this.card(T, { idx: i % 3, ...this.items[i % 3], ...o }), 0, 0, T.rots[i % 3] + (o.press ? -1 : 0), 171, 42), 342, 84, [18, 18, 18, 14]));",
  "frames: [0, 60, 120, 220, 380].map(t => { const a = this.cardAppear(t, T.rots[0]);",
  "this.cat(this.place(this.card(T, { idx: 0, ...this.items[0], sx: a.sx, sy: a.sy }), 0, a.dy, a.rot, 171, 42, a.op), this.dust(T, a.du, 34, 86, 36), this.dust(T, a.du, 308, 86, 36)), 342, 84, [40, 20, 30, 20], cS); }) },",
  "frames: [0, 50, 100].map(t => { const p = 1 - Math.pow(1 - t / 100, 3);",
  "this.place(this.card(T, { idx: 0, ...this.items[0], press: p }), 0, 0, T.rots[0] - p, 171, 42), 342, 84, [20, 20, 20, 20], cS); }) },",
  "frames: [0, 70, 140, 190, 300, 450].map(t => { const d = this.cardDismiss(t);",
  "this.cardStack(T, t, 'dismiss'), 342, 288, [24, 20, 24, 60], .36); }) },",
  "if (d && i === 1) { dx = d.dx; sx = d.sx; sy = d.sy; op = d.op; under.push(this.speed(T, d.lu, Math.min(dx, 342), base, 84)); const hu = (t - 170) / 300; if (hu >= 0 && hu < 1) under.push(this.place(this.hole(T, hu, 0, 0, 342, 84, [11, 17, 13, 19].map(v => v * T.rs), 5), 0, base, rot, 171, 42)); }",
  "if (d && i === 2) yy += d.n * 102;",
  "if (op > .01) parts.push(this.place(this.card(T, { idx: i, ...this.items[i], sx, sy, press: o_press(i) }), dx, yy, a.rot, 171, 42, op));",
];
const ITEMS = [{ title: 'Pancake breakfast', meta: 'Kitchen, bring syrup', time: '9:00' }, { title: 'Bike to the lake', meta: 'Check the tyre pump', time: '11:30' }, { title: 'Paint the fence', meta: 'Green this time', time: '14:00' }];
// The page's `cardStack(T, t, 'dismiss')` with the engine in place of the page's own copies.
const CARD_STACK = t => `(() => { const items = ${JSON.stringify(ITEMS)}, parts = [], under = [], d = E.cardDismiss(${t});
  for (let i = 0; i < 3; i++) { const base = i * 102, rot = T.rots[i]; let dx = 0, sx = 1, sy = 1, op = 1, yy = base;
    if (i === 1) { dx = d.dx; sx = d.sx; sy = d.sy; op = d.op; under.push(E.speed(T, d.lu, Math.min(dx, 342), base, 84)); const hu = (${t} - 170) / 300; if (hu >= 0 && hu < 1) under.push(E.place(E.hole(T, hu, 0, 0, 342, 84, [11, 17, 13, 19].map(v => v * T.rs), 5), 0, base, rot, 171, 42)); }
    if (i === 2) yy += d.n * 102;
    if (op > .01) parts.push(E.place(E.card(T, { idx: i, ...items[i], sx, sy, press: 0 }), dx, yy, rot, 171, 42, op)); }
  return E.cat(...under, ...parts); })()`;
const cardLook = o => {
  const a = [];
  if (o.press) a.push(`press = ${kf(o.press)}`);
  if (o.sel) a.push('selected = true');
  if (o.dis) a.push('disabled = true');
  return call('ToonCardLook', ...a);
};
const CARD_STATES = [['Default', {}], ['Pressed', { press: 1 }], ['Selected', { sel: true }], ['Disabled', { dis: true }]];
export const CARD_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...CARD_STATES.map(([n, o], i) => sceneCell('Card', theme, themeName, n, 342, 84, [18, 18, 18, 14], { kind: 'state', i, ...o },
    `E.place(E.card(T, ${js({ idx: i % 3, ...ITEMS[i % 3], ...o })}), 0, 0, T.rots[${i % 3}] + ${o.press ? -1 : 0}, 171, 42)`,
    call('CardFrames.state', 'tokens', String(i), cardLook(o)))),
  ...[0, 60, 120, 220, 380].map(t => sceneCell('Card', theme, themeName, `Appear_${t}ms`, 342, 84, [40, 20, 30, 20], { kind: 'appear', t },
    `(() => { const a = E.cardAppear(${t}, T.rots[0]); return E.cat(E.place(E.card(T, Object.assign({ idx: 0 }, ${JSON.stringify(ITEMS[0])}, { sx: a.sx, sy: a.sy })), 0, a.dy, a.rot, 171, 42, a.op), E.dust(T, a.du, 34, 86, 36), E.dust(T, a.du, 308, 86, 36)); })()`,
    call('CardFrames.appear', 'tokens', `${t}f`))),
  ...[0, 50, 100].map(t => { const p = 1 - Math.pow(1 - t / 100, 3); return sceneCell('Card', theme, themeName, `Press_${t}ms`, 342, 84, [20, 20, 20, 20], { kind: 'press', p: num(p) },
    `E.place(E.card(T, Object.assign({ idx: 0 }, ${JSON.stringify(ITEMS[0])}, { press: ${num(p)} })), 0, 0, T.rots[0] - ${num(p)}, 171, 42)`,
    call('CardFrames.press', 'tokens', kf(p))); }),
  ...[0, 70, 140, 190, 300, 450].map(t => sceneCell('Card', theme, themeName, `Dismiss_${t}ms`, 342, 288, [24, 20, 24, 60], { kind: 'dismiss', t },
    CARD_STACK(t), call('CardFrames.dismiss', 'tokens', `${t}f`))),
]);

// ---- Snackbar (B-23): `toon-ui Core.dc.html` ----
const SNACK_PAGE = [
  "const nCells = [['Info', { type: 'info', text: 'Plan added', action: 'Undo' }], ['Success', { type: 'success', text: 'Saved for Saturday' }], ['Error', { type: 'error', text: \"Couldn't save\", action: 'Retry' }]]",
  ".map(([S, o]) => C(`Snackbar / ${S}`, this.snack(T, o), 342, 56, [34, 18, 18, 14]));",
  "{ title: 'Snackbar · show', event: 'snackbar.show', dur: '450 ms', curve: 'spring ζ 0.48 · bouncy', reduce: 'fade 120',",
  "frames: [0, 100, 150, 240, 330, 450].map(t => { const a = this.snkShow(t); return Fr(`${t} ms`, `y ${a.dy.toFixed(0)} · rot ${a.rot.toFixed(1)}°`, this.place(this.snack(T, { type: 'info', text: 'Plan added', action: 'Undo' }), 0, 40 + a.dy, a.rot, 171, -26), 342, 120, [6, 16, 6, 16], .45); }) },",
  "{ title: 'Snackbar · hide', event: 'snackbar.hide', dur: '240 ms', curve: 'anticipate', reduce: 'fade 120',",
  "frames: [0, 70, 150, 240].map(t => { const a = this.snkHide(t); return Fr(`${t} ms`, `y ${a.dy.toFixed(0)} · sY ${a.sy.toFixed(2)}`, this.place(this.snack(T, { type: 'info', text: 'Plan added', action: 'Undo', sy: a.sy, speedU: t > 70 ? (t - 70) / 190 : -1 }), 0, 40 + a.dy, 0, 171, -26), 342, 120, [6, 16, 6, 16], .45); }) },",
];
const SNACK_STATES = [['Info', { type: 'info', text: 'Plan added', action: 'Undo' }], ['Success', { type: 'success', text: 'Saved for Saturday' }], ['Error', { type: 'error', text: "Couldn't save", action: 'Retry' }]];
const SNACK_TYPE = { info: 'Info', success: 'Success', error: 'Error' };
const snackScene = o => call('toonSnackScene', 'tokens', '342f', JSON.stringify(o.text), JSON.stringify(o.action || ''), `ToonSnackbarType.${SNACK_TYPE[o.type]}`, call('ToonSnackLook'));
// A strip frame is the kit's own moment at that age, so the cell holds the curve as well as the drawing.
const snackMoment = (shown, t) => call('SnackFrames.strip', 'tokens', String(shown), `${t}f`);
export const SNACK_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...SNACK_STATES.map(([n, o]) => sceneCell('Snackbar', theme, themeName, n, 342, 56, [34, 18, 18, 14], o, `E.snack(T, ${js(o)})`, snackScene(o))),
  ...[0, 100, 150, 240, 330, 450].map(t => sceneCell('Snackbar', theme, themeName, `Show_${t}ms`, 342, 120, [6, 16, 6, 16], { kind: 'show', t },
    `(() => { const a = E.snkShow(${t}); return E.place(E.snack(T, { type: 'info', text: 'Plan added', action: 'Undo' }), 0, 40 + a.dy, a.rot, 171, -26); })()`,
    snackMoment(true, t))),
  ...[0, 70, 150, 240].map(t => sceneCell('Snackbar', theme, themeName, `Hide_${t}ms`, 342, 120, [6, 16, 6, 16], { kind: 'hide', t },
    `(() => { const a = E.snkHide(${t}); return E.place(E.snack(T, { type: 'info', text: 'Plan added', action: 'Undo', sy: a.sy, speedU: ${t} > 70 ? (${t} - 70) / 190 : -1 }), 0, 40 + a.dy, 0, 171, -26); })()`,
    snackMoment(false, t))),
]);

// ---- Slider (B-36): `toon-ui v1 Components.dc.html` ----
const V1 = page('toon-ui v1 Components.dc.html');
const SLIDER_PAGE = [
  "out.slider = { cells: [['Default', { value: .4 }], ['Dragging', { value: .6, grab: 1 }], ['Focused', { value: .4, focus: true }], ['Full', { value: 1 }], ['Disabled', { value: .4, disabled: true }]].map(([n, o]) => C('Slider / ' + n, E.slider(T, { w: 260, ...o }), 260, 48, [10, 14, 10, 14])),",
  "frames: [['0 ms', 0], ['40 ms', .875], ['80 ms · held', 1], ...[60, 130, 220].map(t => ['+' + t + ' ms', E.sliderGrab(t, false)])].map(([lb, q]) => Fr(lb, sq(q), E.slider(T, { w: 200, value: .5, grab: q }), 200, 48, [10, 14, 10, 14])) }] };",
];
const SLIDER_STATES = [['Default', { value: .4 }], ['Dragging', { value: .6, grab: 1 }], ['Focused', { value: .4, focus: true }], ['Full', { value: 1 }], ['Disabled', { value: .4, disabled: true }]];
const sliderLook = (o, grab) => {
  const a = [];
  if (grab) a.push(`grab = ${grab}`);
  else if (o.grab) a.push(`grab = ${kf(o.grab)}`);
  if (o.focus) a.push('focus = true');
  if (o.disabled) a.push('disabled = true');
  return call('ToonSliderLook', ...a);
};
// The page's strip: held at 0, 40 and 80 ms (the press curve gives 0, .875 and 1, the page's own
// numbers), then let go 60, 130 and 220 ms — each the engine's `sliderGrab` at its age, so the cell
// holds the curve as well as the drawing.
const SLIDER_FRAMES = [['0ms', 0, true], ['40ms', 40, true], ['80msHeld', 80, true], ['Plus60ms', 60, false], ['Plus130ms', 130, false], ['Plus220ms', 220, false]];
export const SLIDER_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...SLIDER_STATES.map(([n, o]) => sceneCell('Slider', theme, themeName, n, 260, 48, [10, 14, 10, 14], { kind: 'state', ...o },
    `E.slider(T, ${js({ w: 260, ...o })})`, call('toonSliderScene', 'tokens', '260f', kf(o.value), sliderLook(o)))),
  ...SLIDER_FRAMES.map(([n, t, down]) => sceneCell('Slider', theme, themeName, `Grab_${n}`, 200, 48, [10, 14, 10, 14], { kind: 'grab', t, down },
    `E.slider(T, { w: 200, value: .5, grab: E.sliderGrab(${t}, ${down}) })`, call('toonSliderScene', 'tokens', '200f', '0.5f', sliderLook({}, `toonSliderGrab(${t}f, ${down})`)))),
]);

// ---- BottomSheet (B-38): `toon-ui v1 Components.dc.html` ----
const SHEET_PAGE = [
  "out.sheet = { cells: [['Open', { dy: 0, scrim: 1 }], ['Dragged down', { dy: 60, scrim: .7 }]].map(([n, a]) => C('BottomSheet / ' + n, this.sheetScene(T, a), 360, 520, [0, 0, 0, 0], .55)),",
  "frames: [0, 90, 180, 300, 450].map(t => { const a = E.sheetMove(t, true, 260); return Fr(`${t} ms`, `y ${n0(a.dy)} · scrim ${n2(a.scrim)}`, this.sheetScene(T, a), 360, 520, [0, 0, 0, 0], .34); }) }] };",
  "bgCards(T, W) { const E = this.E, it = [['Pancake breakfast', 'Kitchen, bring syrup', '9:00'], ['Bike to the lake', 'Check the tyre pump', '11:30'], ['Paint the fence', 'Green this time', '14:00']]; return E.cat(...it.map((v, i) => E.place(E.card(T, { idx: i, w: W - 36, title: v[0], meta: v[1], time: v[2] }), 18, 24 + i * 102, T.rots[i], (W - 36) / 2, 42))); }",
  "sheetScene(T, a, Hs = 520) { const E = this.E, sh = E.sheet(T, { W: 360, Hs, h: 260, dy: a.dy, scrim: a.scrim, title: 'Share plan' }); const li = E.cat(E.place(E.listItem(T, { w: 312, icon: 'person', title: 'Family', sub: '4 people', trailing: 'check', on: true }), 24, sh.y + 70), E.place(E.listItem(T, { w: 312, icon: 'share', title: 'Copy link', trailing: 'chevron', last: true, seed: 1 }), 24, sh.y + 142)); const bg = this.bgCards(T, 360); bg.X = bg.X.filter(x => x.y < sh.y - 20); return E.cat(bg, sh, li); }",
];
// The page's `bgCards` and `sheetScene`, verbatim but for `this`.
const SHEET_SCENE = a => `(() => { const W = 360, it = [['Pancake breakfast', 'Kitchen, bring syrup', '9:00'], ['Bike to the lake', 'Check the tyre pump', '11:30'], ['Paint the fence', 'Green this time', '14:00']]; const bg = E.cat(...it.map((v, i) => E.place(E.card(T, { idx: i, w: W - 36, title: v[0], meta: v[1], time: v[2] }), 18, 24 + i * 102, T.rots[i], (W - 36) / 2, 42))); const a = ${a}, sh = E.sheet(T, { W: 360, Hs: 520, h: 260, dy: a.dy, scrim: a.scrim, title: 'Share plan' }); const li = E.cat(E.place(E.listItem(T, { w: 312, icon: 'person', title: 'Family', sub: '4 people', trailing: 'check', on: true }), 24, sh.y + 70), E.place(E.listItem(T, { w: 312, icon: 'share', title: 'Copy link', trailing: 'chevron', last: true, seed: 1 }), 24, sh.y + 142)); bg.X = bg.X.filter(x => x.y < sh.y - 20); return E.cat(bg, sh, li); })()`;
const SHEET_STATES = [['Open', { dy: 0, scrim: 1 }], ['DraggedDown', { dy: 60, scrim: .7 }]];
// A strip frame is the engine's `sheetMove` at its age, so the cell holds the curve as well as the drawing.
export const SHEET_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...SHEET_STATES.map(([n, a]) => sceneCell('BottomSheet', theme, themeName, n, 360, 520, [0, 0, 0, 0], { kind: 'state', ...a },
    SHEET_SCENE(js(a)), call('SheetFrames.scene', 'tokens', call('ToonSheetMotion', kf(a.dy), kf(a.scrim))))),
  ...[0, 90, 180, 300, 450].map(t => sceneCell('BottomSheet', theme, themeName, `Open_${t}ms`, 360, 520, [0, 0, 0, 0], { kind: 'open', t },
    SHEET_SCENE(`E.sheetMove(${t}, true, 260)`), call('SheetFrames.open', 'tokens', `${t}f`))),
]);

// ---- Tooltip (B-40): `toon-ui v1 Components.dc.html` ----
const TIP_PAGE = [
  "out.tip = { cells: [['Default', { text: 'Tap to add a plan' }], ['Long', { text: 'Long-press a card to move it' }], ['Inflating', { text: 'Tap to add a plan', s: .6 }]].map(([n, o]) => { const tp = E.tooltip(T, o); return C('Tooltip / ' + n, E.place(tp, tp.w / 2, 46), tp.w, 46, [6, 8, 6, 8]); }),",
  "frames: [0, 60, 120, 200, 320].map(t => { const s = E.tipInflate(t, true), tp = E.tooltip(T, { s }); return Fr(`${t} ms`, `scale ${n2(s)}`, E.place(tp, 100, 52), 200, 56, [4, 4, 4, 4]); }) }] };",
];
const TIP_STATES = [['Default', { text: 'Tap to add a plan' }], ['Long', { text: 'Long-press a card to move it' }], ['Inflating', { text: 'Tap to add a plan', s: .6 }]];
const tipWidth = text => `text.measure(${JSON.stringify(text)}, TIP_TEXT).multiParagraph.width`;
// A cell is the engine's balloon width as Node measures the text (length × 14 × 0.55), wider than
// Chrome's and Compose's own; the page places the tip at half the measured width, so in Chrome and in
// Compose the balloon sits left of the cell's middle. A strip frame is `tipInflate` at its age.
export const TIP_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...TIP_STATES.map(([n, o]) => sceneCell('Tooltip', theme, themeName, n, Math.ceil(E.tooltip(E.THEMES[theme], o).w), 46, [6, 8, 6, 8], { kind: 'state', ...o },
    `(() => { const tp = E.tooltip(T, ${js(o)}); return E.place(tp, tp.w / 2, 46); })()`,
    call('TipFrames.cell', 'tokens', JSON.stringify(o.text), kf(o.s ?? 1), tipWidth(o.text)))),
  ...[0, 60, 120, 200, 320].map(t => sceneCell('Tooltip', theme, themeName, `Inflate_${t}ms`, 200, 56, [4, 4, 4, 4], { kind: 'inflate', t },
    `(() => { const s = E.tipInflate(${t}, true), tp = E.tooltip(T, { s }); return E.place(tp, 100, 52); })()`,
    call('TipFrames.inflate', 'tokens', `${t}f`, tipWidth('Tap to add a plan')))),
]);

// ---- NavigationDrawer (B-39): `toon-ui v1 Components.dc.html` ----
const DRAWER_PAGE = [
  "out.drawer = { cells: [['Open', {}], ['Item pressed', { pressed: 2 }], ['Half open', { sx: .5, scrim: .5, textOp: 0 }]].map(([n, a]) => C('NavigationDrawer / ' + n, this.drawerScene(T, { sx: 1, scrim: 1, textOp: 1, ...a }), 360, 520, [0, 0, 0, 0], .5)),",
  "frames: [0, 80, 160, 240, 420].map(t => { const a = E.drawerOpen(t); return Fr(`${t} ms`, `open ${n0(a.sx * 100)} % · rot ${n2(a.rot)}°`, this.drawerScene(T, a), 360, 520, [0, 0, 0, 0], .34); }) }] };",
  "drawerScene(T, a, H = 520) { const bg = this.bgCards(T, 360), edge = 280 * Math.max(0, Math.min(1, a.sx ?? 1)); bg.X = bg.X.filter(x => { const right = x.anchor === 'middle' ? x.x + 30 : x.x; return right > edge + 6 && x.x > edge + 6; }); return this.E.cat(bg, this.E.drawer(T, { W: 280, H, SW: 360, ...a })); }",
];
// The page's `bgCards` and `drawerScene`, verbatim but for `this`.
const DRAWER_SCENE = a => `(() => { const W = 360, it = [['Pancake breakfast', 'Kitchen, bring syrup', '9:00'], ['Bike to the lake', 'Check the tyre pump', '11:30'], ['Paint the fence', 'Green this time', '14:00']]; const bg = E.cat(...it.map((v, i) => E.place(E.card(T, { idx: i, w: W - 36, title: v[0], meta: v[1], time: v[2] }), 18, 24 + i * 102, T.rots[i], (W - 36) / 2, 42))); const a = ${a}, H = 520, edge = 280 * Math.max(0, Math.min(1, a.sx ?? 1)); bg.X = bg.X.filter(x => { const right = x.anchor === 'middle' ? x.x + 30 : x.x; return right > edge + 6 && x.x > edge + 6; }); return E.cat(bg, E.drawer(T, { W: 280, H, SW: 360, ...a })); })()`;
const DRAWER_STATES = [['Open', {}], ['ItemPressed', { pressed: 2 }], ['HalfOpen', { sx: .5, scrim: .5, textOp: 0 }]];
// A strip frame is the engine's `drawerOpen` at its age, so the cell holds the curve as well as the drawing.
export const DRAWER_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...DRAWER_STATES.map(([n, o]) => {
    const a = { sx: 1, scrim: 1, textOp: 1, ...o };
    const m = call('ToonDrawerMotion', kf(a.sx), '0f', kf(a.scrim), kf(a.textOp));
    return sceneCell('NavigationDrawer', theme, themeName, n, 360, 520, [0, 0, 0, 0], { kind: 'state', ...a },
      DRAWER_SCENE(js(a)), a.pressed == null ? call('DrawerFrames.scene', 'tokens', m) : call('DrawerFrames.scene', 'tokens', m, String(a.pressed)));
  }),
  ...[0, 80, 160, 240, 420].map(t => sceneCell('NavigationDrawer', theme, themeName, `Open_${t}ms`, 360, 520, [0, 0, 0, 0], { kind: 'open', t },
    DRAWER_SCENE(`E.drawerOpen(${t})`), call('DrawerFrames.open', 'tokens', `${t}f`))),
]);

// ---- Menu (B-37): `toon-ui v1 Components.dc.html` ----
const MENU_PAGE = [
  "out.menu = { cells: [['Open', {}], ['Item pressed', { pressed: 1 }], ['Item focused', { focus: 0 }], ['Item disabled', { disabled: 1 }], ['Unrolling', { u: .55 }]].map(([n, o]) => C('Menu / ' + n, E.menu(T, { w: 200, items: ['Edit', 'Share', 'Delete'], icons: ['edit', 'share', 'delete'], ...o }), 200, 172, [6, 10, 10, 6])),",
  "frames: [0, 60, 120, 200, 360].map(t => { const u = E.menuUnroll(t, true); return Fr(`${t} ms`, `paper ${n0(u * 100)} %`, this.menuScene(T, u), 260, 240, [0, 0, 0, 0], .5); }) },",
  "frames: [0, 60, 120, 160].map(t => { const u = E.menuUnroll(t, false); return Fr(`${t} ms`, `paper ${n0(u * 100)} %`, this.menuScene(T, u), 260, 240, [0, 0, 0, 0], .5); }) }] };",
  "menuScene(T, u, o = {}) { const E = this.E, m = E.menu(T, { w: 200, items: ['Edit', 'Share', 'Delete'], icons: ['edit', 'share', 'delete'], u, ...o }); return E.cat(E.place(E.iconBtn(T, { icon: 'more', p: 0 }), 186, 4), u > .01 ? E.place(m, 34, 58) : { L: [], X: [] }); }",
];
const MENU_STATES = [['Open', {}], ['ItemPressed', { pressed: 1 }], ['ItemFocused', { focus: 0 }], ['ItemDisabled', { disabled: 1 }], ['Unrolling', { u: .55 }]];
const MENU_JS = "{ w: 200, items: ['Edit', 'Share', 'Delete'], icons: ['edit', 'share', 'delete'] }";
const menuLook = o => {
  const a = [];
  if (o.u != null) a.push(`u = ${kf(o.u)}`);
  if (o.pressed != null) a.push(`pressed = ${o.pressed}`);
  if (o.focus != null) a.push(`focus = ${o.focus}`);
  if (o.disabled != null) a.push(`disabled = ${o.disabled}`);
  return call('ToonMenuLook', ...a);
};
// A strip frame is the page's `menuScene` at the engine's `menuUnroll` for its age, so the cell holds
// the curve as well as the drawing.
export const MENU_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...MENU_STATES.map(([n, o]) => sceneCell('Menu', theme, themeName, n, 200, 172, [6, 10, 10, 6], { kind: 'state', ...o },
    `E.menu(T, Object.assign(${MENU_JS}, ${js(o)}))`, call('toonMenuScene', 'tokens', '200f', 'MenuFrames.items', menuLook(o)))),
  ...[[true, [0, 60, 120, 200, 360]], [false, [0, 60, 120, 160]]].flatMap(([open, ts]) => ts.map(t =>
    sceneCell('Menu', theme, themeName, `${open ? 'Unroll' : 'RollUp'}_${t}ms`, 260, 240, [0, 0, 0, 0], { kind: open ? 'unroll' : 'rollUp', t },
      `(() => { const u = E.menuUnroll(${t}, ${open}), m = E.menu(T, Object.assign(${MENU_JS}, { u })); return E.cat(E.place(E.iconBtn(T, { icon: 'more', p: 0 }), 186, 4), u > .01 ? E.place(m, 34, 58) : { L: [], X: [] }); })()`,
      call('MenuFrames.strip', 'tokens', String(open), `${t}f`)))),
]);

// ---- Tabs (B-28): `toon-ui MVP Components.dc.html` ----
const TABS_PAGE = [
  "TABS = ['Today', 'Weekend', 'Someday'];",
  "out.tabs = { cells: [['Today', { sel: 0 }], ['Weekend', { sel: 1 }], ['Someday pressed', { sel: 0, pressed: 2 }], ['Weekend focused', { sel: 0, focus: 1 }], ['Someday disabled', { sel: 0, disabled: 2 }]].map(([n, o]) => C('Tabs / ' + n, E.tabs(T, { labels: this.TABS, ...o }), 342, 52, [6, 8, 8, 8])),",
  "frames: [0, 80, 160, 240, 320].map(t => { const a = E.tabsSwitch(t, 0, 1, 114); return Fr(`${t} ms`, `bookmark ${n0(a.ia)}–${n0(a.ib)} · content ${n0(a.cx)}`, E.tabs(T, { labels: this.TABS, sel: a.sel, ia: a.ia, ib: a.ib }), 342, 52, [6, 8, 8, 8], .5); }) }] };",
];
const TABS = ['Today', 'Weekend', 'Someday'];
const TABS_STATES = [['Today', { sel: 0 }], ['Weekend', { sel: 1 }], ['SomedayPressed', { sel: 0, pressed: 2 }], ['WeekendFocused', { sel: 0, focus: 1 }], ['SomedayDisabled', { sel: 0, disabled: 2 }]];
const tabsLook = o => {
  const a = [`sel = ${o.sel}`];
  if (o.pressed != null) a.push(`pressed = ${o.pressed}`);
  if (o.focus != null) a.push(`focus = ${o.focus}`);
  if (o.disabled != null) a.push(`disabled = ${o.disabled}`);
  return call('ToonTabsLook', ...a);
};
const TABS_KT = 'listOf("Today", "Weekend", "Someday")';
export const TABS_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...TABS_STATES.map(([n, o]) => sceneCell('Tabs', theme, themeName, n, 342, 52, [6, 8, 8, 8], { kind: 'state', ...o },
    `E.tabs(T, Object.assign({ labels: ${JSON.stringify(TABS)} }, ${JSON.stringify(o)}))`, call('toonTabsScene', 'tokens', '342f', TABS_KT, tabsLook(o)))),
  // A switch frame is the engine's `tabsSwitch` at its age, so the cell holds the curve as well as the drawing.
  ...[0, 80, 160, 240, 320].map(t => sceneCell('Tabs', theme, themeName, `Switch_${t}ms`, 342, 52, [6, 8, 8, 8], { kind: 'switch', t },
    `(() => { const a = E.tabsSwitch(${t}, 0, 1, 114); return E.tabs(T, { labels: ${JSON.stringify(TABS)}, sel: a.sel, ia: a.ia, ib: a.ib }); })()`,
    call('toonTabsSwitchScene', 'tokens', `${t}f`))),
]);
// The same cells without their labels: the shape alone, held to the Divider's floor, since the labels'
// ink takes the group past it (18 % in Bright Cel) and the text floor would hide a wrong bookmark.
export const TABS_SHAPE_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => TABS_STATES.map(([n, o]) =>
  sceneCell('TabsShape', theme, themeName, n, 342, 52, [6, 8, 8, 8], { kind: 'state', ...o },
    `E.tabs(T, Object.assign({ labels: ['', '', ''] }, ${JSON.stringify(o)}))`, call('toonTabsScene', 'tokens', '342f', 'listOf("", "", "")', tabsLook(o)))));

// ---- NavigationBar (B-27): `toon-ui MVP Components.dc.html` ----
const NAV_PAGE = [
  "NAV = [{ icon: 'home', label: 'Home' }, { icon: 'search', label: 'Explore' }, { icon: 'heart', label: 'Saved' }, { icon: 'person', label: 'Profile' }];",
  "out.nav = { cells: [['Home selected', { sel: 0 }], ['Saved + badge', { sel: 2, badge: { i: 3, n: 3 } }], ['Explore pressed', { sel: 0, pressed: 1 }], ['Profile focused', { sel: 0, focus: 3 }], ['Saved disabled', { sel: 0, disabled: 2 }]].map(([n, o]) => C('NavigationBar / ' + n, E.navBar(T, { items: this.NAV, ...o }), 390, 80, [8, 8, 16, 8], .8)),",
  "frames: [0, 90, 180, 240, 360].map(t => { const a = E.navSelect(t, 0, 2, 97.5); return Fr(`${t} ms`, `icon y ${n0(a.iy)} · paint ${n0(a.ia)}–${n0(a.ib)}`, E.navBar(T, { items: this.NAV, sel: 2, ...a }), 390, 80, [24, 0, 0, 0], .42); }) }] };",
];
const NAV_ITEMS = [{ icon: 'home', label: 'Home' }, { icon: 'search', label: 'Explore' }, { icon: 'heart', label: 'Saved' }, { icon: 'person', label: 'Profile' }];
const NAV_STATES = [['HomeSelected', { sel: 0 }], ['SavedBadge', { sel: 2, badge: { i: 3, n: 3 } }], ['ExplorePressed', { sel: 0, pressed: 1 }], ['ProfileFocused', { sel: 0, focus: 3 }], ['SavedDisabled', { sel: 0, disabled: 2 }]];
const navLook = (o, motion) => {
  const a = [`sel = ${o.sel}`];
  if (motion) a.push(`motion = ${motion}`);
  if (o.pressed != null) a.push(`pressed = ${o.pressed}`);
  if (o.focus != null) a.push(`focus = ${o.focus}`);
  if (o.disabled != null) a.push(`disabled = ${o.disabled}`);
  if (o.badge) a.push(`badge = ${o.badge.i} to ToonBadgeLook("${o.badge.n}")`);
  return call('ToonNavLook', ...a);
};
const navScene = (o, motion) => call('toonNavBarScene', 'tokens', '390f', 'NavFrames.ITEMS', navLook(o, motion));
export const NAV_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...NAV_STATES.map(([n, o]) => sceneCell('NavigationBar', theme, themeName, n, 390, 80, [8, 8, 16, 8], { kind: 'state', ...o, badge: o.badge ? o.badge.i : null, n: o.badge ? o.badge.n : null },
    `E.navBar(T, Object.assign({ items: ${JSON.stringify(NAV_ITEMS)} }, ${JSON.stringify(o)}))`, navScene(o))),
  // A select frame is the engine's `navSelect` at its age, so the cell holds the curve as well as the drawing.
  ...[0, 90, 180, 240, 360].map(t => sceneCell('NavigationBar', theme, themeName, `Select_${t}ms`, 390, 80, [24, 0, 0, 0], { kind: 'select', t },
    `E.navBar(T, Object.assign({ items: ${JSON.stringify(NAV_ITEMS)}, sel: 2 }, E.navSelect(${t}, 0, 2, 97.5)))`, navScene({ sel: 2 }, `toonNavSelect(${t}f, 0, 2, 97.5f)`))),
]);

// ---- TopAppBar (B-26): `toon-ui MVP Components.dc.html` ----
const TOP_PAGE = [
  "out.top = { cells: [['Small', {}, 0], ['Center', { variant: 'center', title: 'Plans' }, 0], ['Scrolled', { scrolled: true }, -1.2], ['Nav / Pressed', { pressed: 'nav' }, 0], ['Nav / Focused', { focus: 'nav' }, 0], ['Action / Pressed', { pressed: 0 }, 0], ['Action / Focused', { focus: 1 }, 0]].map(([n, o, rot]) => C('TopAppBar / ' + n, E.place(E.topBar(T, o), 0, 0, rot, 195, -40), 390, 76, [8, 8, 12, 8], .8)),",
  "frames: [0, 90, 180, 280, 450, 700].map(t => { const r = E.sway(t, 28); return Fr(`${t} ms`, `rot ${r >= 0 ? '+' : ''}${n2(r)}°`, E.place(E.topBar(T, { scrolled: true }), 0, 0, r, 195, -40), 390, 76, [8, 8, 12, 8], .4); }) }] };",
];
const TOP_STATES = [['Small', {}, 0], ['Center', { variant: 'center', title: 'Plans' }, 0], ['Scrolled', { scrolled: true }, -1.2], ['Nav_Pressed', { pressed: 'nav' }, 0], ['Nav_Focused', { focus: 'nav' }, 0], ['Action_Pressed', { pressed: 0 }, 0], ['Action_Focused', { focus: 1 }, 0]];
const topKey = k => k === 'nav' ? 'ToonTopBarKey.Nav' : `ToonTopBarKey.Action(${k})`;
const topLook = o => {
  const a = [];
  if (o.scrolled) a.push('scrolled = true');
  if (o.pressed != null) a.push(`pressed = ${topKey(o.pressed)}`);
  if (o.focus != null) a.push(`focus = ${topKey(o.focus)}`);
  return call('ToonTopBarLook', ...a);
};
// The engine's defaults: "Saturday", the small variant, search and more, the back arrow.
const topScene = (o, rot) => call(`${fmt(call('toonTopBarScene', 'tokens', '390f', JSON.stringify(o.title || 'Saturday'), `ToonTopAppBarVariant.${o.variant === 'center' ? 'Center' : 'Small'}`, 'listOf(ToonIcons.Search, ToonIcons.More)', 'true', topLook(o)), 8)}.placed`, '0f', '0f', '1f', rot, '195f', '-40f');
export const TOP_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...TOP_STATES.map(([n, o, rot]) => sceneCell('TopAppBar', theme, themeName, n, 390, 76, [8, 8, 12, 8], { kind: 'state', rot, ...o, pressed: o.pressed ?? null, focus: o.focus ?? null },
    `E.place(E.topBar(T, ${JSON.stringify(o)}), 0, 0, ${rot}, 195, -40)`, topScene(o, kf(rot)))),
  // A sway frame is the engine's `sway` at its age, so the cell holds the curve as well as the drawing.
  ...[0, 90, 180, 280, 450, 700].map(t => sceneCell('TopAppBar', theme, themeName, `Sway_${t}ms`, 390, 76, [8, 8, 12, 8], { kind: 'sway', t, scrolled: true },
    `E.place(E.topBar(T, { scrolled: true }), 0, 0, E.sway(${t}, 28), 195, -40)`, topScene({ scrolled: true }, `toonSway(${t}f, 28f)`))),
]);

// ---- Progress (B-25): `toon-ui MVP Components.dc.html` ----
const PROGRESS_PAGE = [
  "out.prog = { cells: [['Linear / Indeterminate', E.linear(T, { w: 240, ...E.worm(420) }), 240, 14], ['Linear / 40 %', E.linear(T, { w: 240, a: 0, b: .4 }), 240, 14], ['Linear / Complete', E.linear(T, { w: 240, a: 0, b: 1 }), 240, 14], ['Circular / Indeterminate', E.circular(T, { ph: 300 }), 48, 48], ['Circular / 65 %', E.circular(T, { value: .65 }), 48, 48]]",
  ".map(([n, sc, w, h]) => C('Progress / ' + n, sc, w, h, [10, 10, 10, 10])),",
  "frames: [0, 300, 600, 900].map(t => { const w = E.worm(t); return Fr(`${t} ms`, `tail ${n0(E.c01(w.a) * 100)} % · head ${n0(E.c01(w.b) * 100)} %`, E.linear(T, { w: 200, ...w }), 200, 14, [16, 8, 8, 8]); }) },",
  "frames: [0, 250, 500, 750].map(t => Fr(`${t} ms`, `angle ${n0(((t / 1000 * 2 * Math.PI) + .7 * Math.sin(t / 1000 * 2 * Math.PI)) * 180 / Math.PI)}°`, E.circular(T, { ph: t }), 48, 48, [8, 8, 8, 8])) },",
];
// A worm cell is the engine's `worm` at its age, so the cell holds the curve as well as the drawing.
export const PROGRESS_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  sceneCell('Progress', theme, themeName, 'Linear_Indeterminate', 240, 14, [10, 10, 10, 10], { kind: 'worm', w: 240, t: 420 }, 'E.linear(T, { w: 240, ...E.worm(420) })', call('toonWormScene', 'tokens', '240f', '420f')),
  ...[['Linear_40', .4], ['Linear_Complete', 1]].map(([n, b]) => sceneCell('Progress', theme, themeName, n, 240, 14, [10, 10, 10, 10], { kind: 'linear', w: 240, b },
    `E.linear(T, { w: 240, a: 0, b: ${b} })`, call('toonLinearScene', 'tokens', '240f', '0f', kf(b), 'worm = false'))),
  sceneCell('Progress', theme, themeName, 'Circular_Indeterminate', 48, 48, [10, 10, 10, 10], { kind: 'circular', ph: 300 }, 'E.circular(T, { ph: 300 })', call('toonCircularScene', 'tokens', 'null', '300f')),
  sceneCell('Progress', theme, themeName, 'Circular_65', 48, 48, [10, 10, 10, 10], { kind: 'circular', value: .65 }, 'E.circular(T, { value: .65 })', call('toonCircularScene', 'tokens', '0.65f', '0f')),
  ...[0, 300, 600, 900].map(t => sceneCell('Progress', theme, themeName, `Worm_${t}ms`, 200, 14, [16, 8, 8, 8], { kind: 'worm', w: 200, t },
    `E.linear(T, { w: 200, ...E.worm(${t}) })`, call('toonWormScene', 'tokens', '200f', `${t}f`))),
  ...[0, 250, 500, 750].map(t => sceneCell('Progress', theme, themeName, `Spin_${t}ms`, 48, 48, [8, 8, 8, 8], { kind: 'circular', ph: t },
    `E.circular(T, { ph: ${t} })`, call('toonCircularScene', 'tokens', 'null', `${t}f`))),
]);

// ---- Dialog (B-24): `toon-ui MVP Components.dc.html` ----
const DIALOG_PAGE = [
  "out.dialog = { cells: [['Destructive', {}], ['Confirm focused', { focus: 'confirm' }], ['Cancel focused', { focus: 'cancel' }], ['Basic', { title: 'Leave the plan?', body: ['Changes you made will be', 'kept as a draft.'], confirm: 'Leave', destructive: false }]].map(([n, o]) => C('Dialog / ' + n, E.dialogScene(T, { R: 190, s: 1, op: 1, textOp: 1, stars: 0 }, o), 360, 460, [0, 0, 0, 0], .62)),",
  "frames: [0, 60, 120, 200, 400].map(t => { const a = E.dialogOpen(t); return Fr(`${t} ms`, `iris r ${n0(a.R)} · scale ${n2(a.s)}`, E.dialogScene(T, a), 360, 460, [0, 0, 0, 0], .36); }) },",
  "frames: [0, 80, 140, 180, 300].map(t => { const a = E.dialogClose(t); return Fr(`${t} ms`, `scale ${n2(a.s)} · iris r ${n0(a.R)}`, E.dialogScene(T, a), 360, 460, [0, 0, 0, 0], .36); }) },",
];
const DIALOG_STATES = [['Destructive', {}], ['ConfirmFocused', { focus: 'confirm' }], ['CancelFocused', { focus: 'cancel' }], ['Basic', { title: 'Leave the plan?', body: ['Changes you made will be', 'kept as a draft.'], confirm: 'Leave', destructive: false }]];
// A strip frame is the engine's curve at that age, so the cell holds the curve as well as the drawing;
// the label widths are measured by the cell's renderer, which only the text button's underline reads.
const WIDTH = '{ text.measure(it, BUTTON_LABEL).multiParagraph.width }';
export const DIALOG_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...DIALOG_STATES.map(([n, o], i) => sceneCell('Dialog', theme, themeName, n, 360, 460, [0, 0, 0, 0], { kind: 'state', i },
    `E.dialogScene(T, { R: 190, s: 1, op: 1, textOp: 1, stars: 0 }, ${JSON.stringify(o)})`, call('DialogFrames.cell', 'tokens', String(i)), WIDTH)),
  ...[0, 60, 120, 200, 400].map(t => sceneCell('Dialog', theme, themeName, `Open_${t}ms`, 360, 460, [0, 0, 0, 0], { kind: 'open', t },
    `E.dialogScene(T, E.dialogOpen(${t}))`, call('DialogFrames.open', 'tokens', `${t}f`), WIDTH)),
  ...[0, 80, 140, 180, 300].map(t => sceneCell('Dialog', theme, themeName, `Close_${t}ms`, 360, 460, [0, 0, 0, 0], { kind: 'close', t },
    `E.dialogScene(T, E.dialogClose(${t}))`, call('DialogFrames.close', 'tokens', `${t}f`), WIDTH)),
]);

// ---- Divider and ListItem (B-22): `toon-ui MVP Components.dc.html` ----
const LIST_PAGE = [
  "out.div = { cells: [['Full', { x1: 342, th: 3 }], ['Inset', { x0: 56, x1: 342, th: 3 }], ['Thick', { x1: 342, th: 4.6 }], ['Short', { x1: 120, th: 2.4 }]].map(([n, o], i) => C('Divider / ' + n, E.divider(T, { seed: i, ...o }), 342, 12, [4, 4, 4, 4])), specs: [] };",
  "const li = [['One-line / Chevron', { icon: 'star', title: 'Favourites', trailing: 'chevron' }], ['Two-line / Switch', { icon: 'bell', title: 'Reminders', sub: 'At noon', trailing: 'switch', on: true }], ['Pressed', { icon: 'calendar', title: 'Weekend plans', sub: '3 plans', trailing: 'chevron', sag: 1 }], ['Selected', { icon: 'calendar', title: 'Weekend plans', sub: '3 plans', trailing: 'check', on: true, sel: true }], ['Focused', { icon: 'bell', title: 'Reminders', sub: 'At noon', trailing: 'switch', on: true, focus: true }], ['Disabled', { icon: 'person', title: 'Shared with', sub: 'Nobody yet', trailing: 'chevron', disabled: true }]];",
  "const liF = [['0 ms', 0, true], ['50 ms', 50, true], ['100 ms · held', 100, true], ['+60 ms', 60, false], ['+130 ms', 130, false], ['+220 ms', 220, false]];",
  "out.list = { cells: li.map(([n, o], i) => C('ListItem / ' + n, E.listItem(T, { seed: i, ...o }), 342, 72, [8, 10, 10, 10])),",
  "frames: liF.map(([lb, t, d]) => { const s = E.listPress(t, d); return Fr(lb, `sag ${n2(s)} · rule ${n2(6 * s)} dp`, E.listItem(T, { icon: 'calendar', title: 'Weekend plans', sub: '3 plans', trailing: 'chevron', sag: s }), 342, 72, [6, 10, 14, 10], .45); }) }] };",
];
const DIVIDERS = [['Full', { x1: 342, th: 3 }], ['Inset', { x0: 56, x1: 342, th: 3 }], ['Thick', { x1: 342, th: 4.6 }], ['Short', { x1: 120, th: 2.4 }]];
// The Divider's cells are the floor's ruler (research D3): one filled inkLine, whose numbers match the
// engine's within 0.05 dp, so what they differ by in pixels is the rasterisers.
export const DIVIDER_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => DIVIDERS.map(([n, o], i) =>
  sceneCell('Divider', theme, themeName, n, 342, 12, [4, 4, 4, 4], { seed: i, ...o }, `E.divider(T, ${js({ seed: i, ...o })})`,
    call('toonDividerScene', 'tokens', kf(o.x0 || 0), kf(o.x1), kf(o.th), String(i)))));
const TRAIL = { chevron: 'Chevron', switch: 'Switch', check: 'Check' };
const listLook = (o, i) => {
  const a = [];
  if (o.on) a.push('on = true');
  if (o.sag) a.push(`sag = ${kf(o.sag)}`);
  if (o.sel) a.push('selected = true');
  if (i) a.push(`seed = ${i}`);
  if (o.disabled) a.push('disabled = true');
  if (o.focus) a.push('focus = true');
  return call('ToonListItemLook', ...a);
};
const listScene = (o, i) => call('toonListItemScene', 'tokens', '342f', `ToonIcons.${pascal(o.icon)}`, JSON.stringify(o.title), JSON.stringify(o.sub || ''), `ToonListTrailing.${TRAIL[o.trailing]}`, listLook(o, i));
const LIST_STATES = [['One-line / Chevron', { icon: 'star', title: 'Favourites', trailing: 'chevron' }], ['Two-line / Switch', { icon: 'bell', title: 'Reminders', sub: 'At noon', trailing: 'switch', on: true }], ['Pressed', { icon: 'calendar', title: 'Weekend plans', sub: '3 plans', trailing: 'chevron', sag: 1 }], ['Selected', { icon: 'calendar', title: 'Weekend plans', sub: '3 plans', trailing: 'check', on: true, sel: true }], ['Focused', { icon: 'bell', title: 'Reminders', sub: 'At noon', trailing: 'switch', on: true, focus: true }], ['Disabled', { icon: 'person', title: 'Shared with', sub: 'Nobody yet', trailing: 'chevron', disabled: true }]];
const LIST_FRAMES = [['0ms', 0, true], ['50ms', 50, true], ['100msHeld', 100, true], ['Plus60ms', 60, false], ['Plus130ms', 130, false], ['Plus220ms', 220, false]];
export const LIST_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) => [
  ...LIST_STATES.map(([n, o], i) => sceneCell('ListItem', theme, themeName, n.replace(' / ', '_').replace('-', ''), 342, 72, [8, 10, 10, 10], { seed: i, ...o },
    `E.listItem(T, ${js({ seed: i, ...o })})`, listScene(o, i))),
  ...LIST_FRAMES.map(([lb, t, d]) => {
    const o = { icon: 'calendar', title: 'Weekend plans', sub: '3 plans', trailing: 'chevron', sag: E.listPress(t, d) };
    return sceneCell('ListItem', theme, themeName, `Press_${lb}`, 342, 72, [6, 10, 14, 10], o, `E.listItem(T, ${js(o)})`, listScene(o, 0));
  }),
]);

// Frames the engine draws at moments no spec cell shows, recorded for the frame tests only (no
// reference, no fixture): a pressed row passes through these sags in its first 30 ms, where the press
// tint's opacity, c01(sag × 1.4), is neither 0 nor 1 — every page frame clamps it.
export const FRAME_PROBES = {
  ListItemProbe: Object.entries(THEMES).flatMap(([theme, themeName]) => [.2, .4, .6].map(sag => {
    const o = { icon: 'calendar', title: 'Weekend plans', sub: '3 plans', trailing: 'chevron', sag };
    return sceneCell('ListItemProbe', theme, themeName, `Sag${sag * 10}`, 342, 72, [0, 0, 0, 0], o, `E.listItem(T, ${js(o)})`, '');
  })),
  // B-39: a press on the selected row, which no page cell shows: the engine draws the selection, not
  // the press, under it.
  NavigationDrawerProbe: Object.entries(THEMES).map(([theme, themeName]) => {
    const a = { sx: 1, scrim: 1, textOp: 1, pressed: 0 };
    return sceneCell('NavigationDrawerProbe', theme, themeName, 'PressedSelected', 360, 520, [0, 0, 0, 0], { kind: 'state', ...a }, DRAWER_SCENE(js(a)), '');
  }),
  // B-38: the kit's `C.sheet` scene, which the page does not draw (its rows are its own): the
  // default two items and four, at rest and 90 ms into an open, on a 360 × 520 screen.
  BottomSheetKit: Object.entries(THEMES).flatMap(([theme, themeName]) => [2, 4].flatMap(n => [null, 90].map(t => {
    const items = [['person', 'Family'], ['share', 'Copy link'], ['star', 'Saved'], ['calendar', 'Plans']].slice(0, n);
    const h = Math.min(520 - 80, 110 + n * 72);
    return sceneCell('BottomSheetKit', theme, themeName, `Rows${n}_${t == null ? 'Rest' : `${t}ms`}`, 360, 520, [0, 0, 0, 0], t == null ? { n } : { n, t },
      `(() => { const it = ${JSON.stringify(items)}, h = ${h}, a = ${t == null ? '{ dy: 0, scrim: 1 }' : `E.sheetMove(${t}, true, h)`}, s = E.sheet(T, { W: 360, Hs: 520, h, dy: a.dy, scrim: a.scrim, title: 'Share plan' }); return E.cat(s, ...it.map(([ic, lb], i) => E.place(E.listItem(T, { w: 360 - 48, icon: ic || 'star', title: lb, trailing: 'chevron', last: i === it.length - 1, seed: i }), 24, s.y + 70 + i * 72))); })()`, '');
  }))),
  // B-37: the paper's first 20 dp, which no page frame lands in — the strips jump from none to most —
  // where the body is drawn from 2 dp and only the rolls show below it.
  MenuProbe: Object.entries(THEMES).flatMap(([theme, themeName]) => [.01, .05, .1].map(u =>
    sceneCell('MenuProbe', theme, themeName, `U${u * 100}`, 200, 172, [0, 0, 0, 0], { kind: 'state', u },
      `E.menu(T, Object.assign(${MENU_JS}, { u: ${u} }))`, ''))),
};

export const missingPageText = [
  ...SLIDER_PAGE.filter(s => !V1.includes(s)).map(s => `toon-ui v1 Components.dc.html: ${s}`),
  ...MENU_PAGE.filter(s => !V1.includes(s)).map(s => `toon-ui v1 Components.dc.html: ${s}`),
  ...SHEET_PAGE.filter(s => !V1.includes(s)).map(s => `toon-ui v1 Components.dc.html: ${s}`),
  ...DRAWER_PAGE.filter(s => !V1.includes(s)).map(s => `toon-ui v1 Components.dc.html: ${s}`),
  ...TIP_PAGE.filter(s => !V1.includes(s)).map(s => `toon-ui v1 Components.dc.html: ${s}`),
  ...TABS_PAGE.filter(s => !MVP.includes(s)).map(s => `toon-ui MVP Components.dc.html: ${s}`),
  ...NAV_PAGE.filter(s => !MVP.includes(s)).map(s => `toon-ui MVP Components.dc.html: ${s}`),
  ...TOP_PAGE.filter(s => !MVP.includes(s)).map(s => `toon-ui MVP Components.dc.html: ${s}`),
  ...PROGRESS_PAGE.filter(s => !MVP.includes(s)).map(s => `toon-ui MVP Components.dc.html: ${s}`),
  ...DIALOG_PAGE.filter(s => !MVP.includes(s)).map(s => `toon-ui MVP Components.dc.html: ${s}`),
  ...SNACK_PAGE.filter(s => !CORE.includes(s)).map(s => `toon-ui Core.dc.html: ${s}`),
  ...LIST_PAGE.filter(s => !MVP.includes(s)).map(s => `toon-ui MVP Components.dc.html: ${s}`),
  ...CARD_PAGE.filter(s => !CORE.includes(s)).map(s => `toon-ui Core.dc.html: ${s}`),
  ...CHIP_PAGE.filter(s => !MVP.includes(s)).map(s => `toon-ui MVP Components.dc.html: ${s}`),
  ...FIELD_PAGE.filter(s => !CORE.includes(s)).map(s => `toon-ui Core.dc.html: ${s}`),
  ...SWITCH_PAGE.filter(s => !CORE.includes(s)).map(s => `toon-ui Core.dc.html: ${s}`),
  ...CHECK_PAGE.filter(s => !CORE.includes(s)).map(s => `toon-ui Core.dc.html: ${s}`),
  ...RADIO_PAGE.filter(s => !MVP.includes(s)).map(s => `toon-ui MVP Components.dc.html: ${s}`),
  ...FAB_PAGE.filter(s => !MVP.includes(s)).map(s => `toon-ui MVP Components.dc.html: ${s}`),
  ...BUTTON_PAGE.filter(s => !CORE.includes(s)).map(s => `toon-ui Core.dc.html: ${s}`),
  ...MVP_PAGE.filter(s => !MVP.includes(s)).map(s => `toon-ui MVP Components.dc.html: ${s}`),
];

/** Every component's cells, by group: the fixture file each generates. */
export const COMPONENT_CELLS = { TextFloor: TEXT_FLOOR_CELLS, Button: BUTTON_CELLS, ButtonShape: BUTTON_SHAPE_CELLS, IconButton: ICON_BUTTON_CELLS, Badge: BADGE_CELLS, Fab: FAB_CELLS, Checkbox: CHECKBOX_CELLS, Radio: RADIO_CELLS, Switch: SWITCH_CELLS, TextField: FIELD_CELLS, Chip: CHIP_CELLS, Card: CARD_CELLS, Divider: DIVIDER_CELLS, ListItem: LIST_CELLS, Snackbar: SNACK_CELLS, Dialog: DIALOG_CELLS, Progress: PROGRESS_CELLS, TopAppBar: TOP_CELLS, NavigationBar: NAV_CELLS, Tabs: TABS_CELLS, TabsShape: TABS_SHAPE_CELLS, Slider: SLIDER_CELLS, Menu: MENU_CELLS, BottomSheet: SHEET_CELLS, NavigationDrawer: DRAWER_CELLS, Tooltip: TIP_CELLS };
