// B-68: the cells the engine golden harness draws with the desktop design's renderers
// (reference/desktop/desktop-engine.js, research §1.8). That design has no state sheets for its nine
// components (its README says so under its known gaps), so a cell here is one of a component's own
// prop values from its `Toon*.dc.html`, at the page's defaults otherwise. Each component's item adds
// or widens its group; the frames land in engine-goldens/desktop.json, shaped as components.json is.
import { COMPONENT_CELLS } from './component-cells.mjs';

const THEMES = { il: 'InkGouacheLight', in: 'InkGouacheNight', bc: 'BrightCel' };

// ToonSpeakerSticker.dc.html: `size` 24 | 32 | 48 and `kind` named | unnamed | me, at `shape` 0 and
// `color` 0. The letter is the one its `paint()` falls back to for the kind: М, 2 and Я.
const STICKER_KINDS = [['Named', 'named', 'М'], ['Unnamed', 'unnamed', '2'], ['Me', 'me', 'Я']];
const STICKER_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) =>
  STICKER_KINDS.flatMap(([label, kind, letter]) => [24, 32, 48].map(size => {
    const opts = { size, kind, shape: 0, color: 0, letter };
    return { group: 'SpeakerSticker', name: `${label}${size}_${themeName}`, theme, themeName, opts, scene: `E.sticker(T, ${JSON.stringify(opts)})` };
  })));
// B-70: the seven other shapes, at the smallest size, each in the accent its index wraps to, so every
// radial function of `blobPts` and every accent has a frame.
const SHAPE_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) =>
  [1, 2, 3, 4, 5, 6, 7].map(shape => {
    const opts = { size: 24, kind: 'named', shape, color: shape % 3, letter: 'М' };
    return { group: 'SpeakerSticker', name: `Shape${shape}_${themeName}`, theme, themeName, opts, scene: `E.sticker(T, ${JSON.stringify(opts)})` };
  }));

// B-69: the pointer rule (`Desktop Pointer & Keyboard.dc.html`, section 01) on the kit's components the
// page draws: each hover cell is one of that component's own rest cells (component-cells.mjs), its
// scene wrapped the way the page wraps it, and its options are the rest cell's, so the component's
// frame test builds it as it builds that cell, hovered. A body with a hard shadow takes the extension's
// `hoverLift`; the text button gets a `plate` where its press blob goes; the slider lifts only its
// ball, by hand, as the page does; a menu item and a list row draw the selected shade. The page lifts
// the chip and the card inside a `place`, whose `translate(0 0)` makes `hoverLift` take every ink
// layer for a shadow, the outline included; the rule is the page's words, so here the bare renderer is
// lifted, and the card placed after, as the kit's own card cells place it.
const kitCell = (group, name, themeName) => {
  const c = COMPONENT_CELLS[group].find(x => x.name === `${name}_${themeName}`);
  if (!c) throw new Error(`no kit cell ${group}_${name}_${themeName}`);
  return c;
};
const hoverCell = (group, name, wrap, opts = {}) => Object.entries(THEMES).map(([theme, themeName]) => {
  const c = kitCell(group, name, themeName);
  return { group: 'Hover', name: `${group}_${name}_${themeName}`, theme, themeName, opts: { ...c.opts, ...opts }, scene: wrap(c.scene, c.opts) };
});
const lift = s => `E.hoverLift(${s}, T)`;
const liftCard = s => s.replace(/E\.card\(T, (\{[^}]*\})\)/, (_, o) => `E.hoverLift(E.card(T, ${o}), T)`);
const ringCard = s => s.replace(/E\.card\(T, (\{[^}]*\})\)/, (_, o) => `E.cat({ L: [E.ring(-6, -6, 354, 96, [22, 16, 24, 18], T, T.seed + 31)], X: [] }, E.card(T, ${o}))`);
// B-79: the rule on the kit's pressables the page does not draw, each look this repository's decision
// read off the page's two rules and the component's own renderer. A body with a hard shadow lifts: the
// checkbox's box and the radio's ring with what is on them, their label beside staying on the table;
// the switch whole, the track being the body with the shadow. A flat slot gets the plate its own press
// draws, at full strength, where the press puts it — the renderer's pressed frame's surface2 layer
// spliced into the rest frame, with nothing else of the press (no label drop, no icon squash); the
// selected slot, whose press the renderer leaves undrawn, gets none. A field fills with surface2, as the
// desktop design's own field (`searchBox`) does, unless focused. Each hovers a slot its page's pressed
// cell does not press.
const liftBeside = s => `(s => ({ ...E.hoverLift(s, T), X: s.X }))(${s})`;
const pressPlate = (pressedAs) => (s) => {
  const p = pressedAs(s);
  if (p === s) throw new Error(`no press in ${s}`);
  return `((r, p) => { const k = p.L.findIndex((l, i) => JSON.stringify(l) !== JSON.stringify(r.L[i])); `
    + `if (k < 0 || p.L[k].fill !== T.surface2 || p.L.length !== r.L.length + 1) throw new Error('no press plate'); `
    + `return { ...r, L: [...r.L.slice(0, k), p.L[k], ...r.L.slice(k)] }; })(${s}, ${p})`;
};
const once = (s, from, to) => { if (s.split(from).length !== 2) throw new Error(`${from} is not once in ${s}`); return s.replace(from, to); };
const fieldPlate = s => `(s => { const k = s.L.map(l => l.fill).lastIndexOf(T.field); return { ...s, L: s.L.map((l, i) => i === k ? { ...l, fill: T.surface2 } : l) }; })(${s})`;
const UNDRAWN_HOVER_CELLS = [
  ...hoverCell('Checkbox', 'Unchecked', liftBeside),
  ...hoverCell('Checkbox', 'Checked', liftBeside),
  ...hoverCell('Checkbox', 'Focused', liftBeside),
  ...hoverCell('Radio', 'Unselected', liftBeside),
  ...hoverCell('Radio', 'Selected', liftBeside),
  ...hoverCell('Radio', 'Focused', liftBeside),
  ...hoverCell('Switch', 'Off', lift),
  ...hoverCell('Switch', 'On', lift),
  ...hoverCell('Switch', 'Focused', lift),
  ...hoverCell('Segmented', 'Day', pressPlate(s => once(s, '{"sel":0}', '{"sel":0,"pressed":1}')), { hover: 1 }),
  ...hoverCell('Tabs', 'Today', pressPlate(s => once(s, '{"sel":0}', '{"sel":0,"pressed":1}')), { hover: 1 }),
  ...hoverCell('NavigationBar', 'HomeSelected', pressPlate(s => once(s, '{"sel":0}', '{"sel":0,"pressed":2}')), { hover: 2 }),
  ...hoverCell('NavigationDrawer', 'Open', pressPlate(s => once(s, '{"sx":1,"scrim":1,"textOp":1}', '{"sx":1,"scrim":1,"textOp":1,"pressed":1}')), { hover: 1 }),
  ...hoverCell('TextField', 'Default', fieldPlate),
  ...hoverCell('TextField', 'Filled', fieldPlate),
  ...hoverCell('TextField', 'Error', fieldPlate),
];
const HOVER_CELLS = [
  ...hoverCell('Button', 'Filled_Default', lift),
  ...hoverCell('Button', 'Outlined_Default', lift),
  ...hoverCell('Button', 'Filled_Focused', lift),
  ...hoverCell('Button', 'Text_Default', s => `E.cat({ L: [E.plate(T, 8, 5, 134, 42)], X: [] }, ${s})`),
  ...hoverCell('IconButton', 'Standard_Default', lift),
  ...hoverCell('IconButton', 'Filled_Default', lift),
  ...hoverCell('Fab', 'Default', lift),
  ...hoverCell('Chip', 'Filter_Default', lift),
  ...hoverCell('Chip', 'Filter_Selected', lift),
  ...hoverCell('Chip', 'Assist', lift),
  ...hoverCell('Card', 'Default', liftCard),
  ...hoverCell('Slider', 'Default', s => `(s => ({ ...s, L: [...s.L.slice(0, -2), ...s.L.slice(-2).map(l => ({ ...l, tf: ('translate(-2 -2) ' + l.tf).trim() }))] }))(${s})`),
  ...hoverCell('Menu', 'Open', s => s.replace(/, \{\}\)\)$/, ', { sel: 0 }))'), { hover: 0 }),
  ...hoverCell('ListItem', 'Oneline_Chevron', (s, o) => `E.listItem(T, ${JSON.stringify({ ...o, sel: true })})`),
  ...UNDRAWN_HOVER_CELLS,
].map(c => (c.scene.includes('hoverLift') || c.scene.includes('plate') || c.scene.includes('sel') || c.scene.includes('translate(-2 -2)') || c.scene.includes('surface2') ? c : (() => { throw new Error(`hover cell ${c.name} was not wrapped`); })()));
// The card's focus ring, which the kit never drew (the page's last column): 6 dp out, its own seed.
const FOCUS_CELLS = hoverCell('Card', 'Default', ringCard, { focus: true }).map(c => ({ ...c, group: 'Focus' }));

// B-71: ToonSearchField.dc.html's `searchBox`, as its `paint()` calls it: the key hint's plate only
// while the field is empty and unfocused (`kbdW` 34, else 0), focus thickening the ink with its ring,
// hover filling it with `surface2` unless focused. Its texts are HTML over the drawing, so a cell has
// none. A value with a count draws what an unfocused field with a value draws: no plate. At the
// page's default width, and at both ends of its `width` editor.
const SEARCH_LOOKS = [
  ['Rest', { kbdW: 34 }],
  ['Hover', { kbdW: 34, hv: 1 }],
  ['Focus', { focus: 1, ring: true, kbdW: 0 }],
  ['FocusHover', { focus: 1, ring: true, hv: 1, kbdW: 0 }],
  ['Value', { kbdW: 0 }],
];
const SEARCH_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) =>
  [...SEARCH_LOOKS.map(([label, o]) => [label, { w: 276, ...o }]), ['Narrow', { w: 160, kbdW: 34 }], ['Wide', { w: 600, kbdW: 34 }]]
    .map(([label, o]) => {
      const opts = { focus: 0, ring: false, hv: 0, ...o };
      return { group: 'SearchField', name: `${label}_${themeName}`, theme, themeName, opts, scene: `E.searchBox(T, ${JSON.stringify(opts)})` };
    }));

// B-72: ToonScrollbar.dc.html's `scrollThumb`: the pointer page's three states (rest, hover, drag)
// at its 120 dp track; the page's own defaults (220 dp, 0.2–0.45); a visible fraction so small the
// thumb stands at its 28 dp; and the end of a long list, where the thumb is held inside the track.
const SCROLL_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) =>
  [
    ['Rest', 120, { a: .2, b: .55, st: 0 }],
    ['Hover', 120, { a: .2, b: .55, st: 1 }],
    ['Drag', 120, { a: .2, b: .55, st: 2 }],
    ['Default', 220, { a: .2, b: .45, st: 0 }],
    ['Least', 220, { a: .5, b: .505, st: 0 }],
    ['End', 220, { a: .97, b: .995, st: 1 }],
  ].map(([label, h, o]) => {
    const opts = { h, ...o };
    return { group: 'Scrollbar', name: `${label}_${themeName}`, theme, themeName, opts, scene: `E.scrollThumb(T, ${h}, ${JSON.stringify(o)})` };
  }));

// B-75: ToonRecordButton.dc.html's `recBtn` as its `paint()` calls it: each state at a width its
// label gives (`36 + label + [10 + time] + 18`), recording at three points of `recPulse`, stopping at
// two turns of its spinner, and the pointer rule over it — hover, focus, a press half and all the way
// down. Its words are HTML over the drawing, so a cell has none.
const REC_LOOKS = [
  ['Idle', { w: 128, state: 'idle' }],
  ['Recording', { w: 186, state: 'recording', pulse: 0 }],
  ['RecordingMid', { w: 186, state: 'recording', pulse: .5 }],
  ['RecordingFull', { w: 186, state: 'recording', pulse: 1 }],
  ['Stopping', { w: 132, state: 'stopping', spin: 0 }],
  ['StoppingTurned', { w: 132, state: 'stopping', spin: 300 }],
  ['IdleHover', { w: 128, state: 'idle', hv: 1 }],
  ['RecordingHover', { w: 186, state: 'recording', hv: 1, pulse: 1 }],
  ['StoppingHover', { w: 132, state: 'stopping', hv: 1, spin: 0 }],
  ['IdleFocus', { w: 128, state: 'idle', focus: true }],
  ['RecordingFocus', { w: 186, state: 'recording', focus: true, pulse: 0 }],
  ['IdleHalfPressed', { w: 128, state: 'idle', hv: 1, p: .5 }],
  ['IdlePressed', { w: 128, state: 'idle', hv: 1, p: 1 }],
];
const REC_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) =>
  REC_LOOKS.map(([label, o]) => {
    const opts = { hv: 0, p: 0, focus: false, pulse: 0, spin: 0, ...o };
    return { group: 'RecordButton', name: `${label}_${themeName}`, theme, themeName, opts, scene: `(s => ({ L: s.L, X: s.X }))(E.recBtn(T, ${JSON.stringify(opts)}))` };
  }));

// B-74: ToonLevelMeter.dc.html's `levelMeter` as its `paint()` calls it: the strip `width` wide, or 24
// less when clipping (the burst stands in that room), live at its defaults and at both ends of
// `level`, silent and clipping, and the narrowest and widest strips of its `width` editor. The header
// above the strip is HTML, so a cell has no words.
const METER_LOOKS = [
  ['Live', { w: 320, state: 'live', level: .6 }],
  ['LiveEmpty', { w: 320, state: 'live', level: 0 }],
  ['LiveFull', { w: 320, state: 'live', level: 1 }],
  ['Silent', { w: 320, state: 'silent', level: .6 }],
  ['Clip', { w: 296, state: 'clip', level: 1 }],
  ['Narrow', { w: 160, state: 'live', level: .5 }],
  ['Wide', { w: 600, state: 'live', level: .3 }],
  ['NarrowSilent', { w: 160, state: 'silent', level: .5 }],
];
const METER_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) =>
  METER_LOOKS.map(([label, o]) => ({
    group: 'LevelMeter', name: `${label}_${themeName}`, theme, themeName, opts: o,
    scene: `E.levelMeter(T, ${o.w}, ${JSON.stringify({ state: o.state, level: o.level })})`,
  })));

// B-76: ToonTitleBar.dc.html's `trafficSlots`, the only thing its `paint()` draws: the three dashed
// places of the window's controls. The renderer takes no width, title or record state (those are HTML
// over it), so a theme is the whole of a cell; the bar's 1280 and 960 dp layouts are the words' test.
const TITLE_CELLS = Object.entries(THEMES).map(([theme, themeName]) => ({
  group: 'TitleBar', name: `Slots_${themeName}`, theme, themeName, opts: {}, scene: 'E.trafficSlots(T)',
}));

// B-73: ToonSplitHandle.dc.html's `splitGrip` as its `paint()` calls it: `st` 0 at rest, 1 under the
// mouse, 2 held, and focus the page's ring pushed after it, 40 dp above the middle. The pointer page's
// four states at its 120 dp; focus under the mouse; the component's default height, and both ends of
// its `height` editor.
const SPLIT_LOOKS = [
  ['Rest', { h: 120, st: 0 }],
  ['Hover', { h: 120, st: 1 }],
  ['Drag', { h: 120, st: 2 }],
  ['Focus', { h: 120, st: 0, focus: true }],
  ['FocusHover', { h: 120, st: 1, focus: true }],
  ['FocusDrag', { h: 120, st: 2, focus: true }],
  ['Default', { h: 200, st: 0 }],
  ['Short', { h: 80, st: 1, focus: true }],
  ['Tall', { h: 900, st: 2 }],
];
const SPLIT_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) =>
  SPLIT_LOOKS.map(([label, o]) => {
    const opts = { focus: false, ...o };
    const ring = opts.focus ? `, { L: [E.ring(-2, ${o.h / 2 - 40}, 20, 80, [10, 8, 10, 8], T, T.seed + 3620)], X: [] }` : '';
    return { group: 'SplitHandle', name: `${label}_${themeName}`, theme, themeName, opts, scene: `E.cat(E.splitGrip(T, ${o.h}, ${o.st})${ring})` };
  }));

// B-77: ToonSidebar.dc.html's own renderers as its `paint()` calls them, at the page's default width
// (300: rows 274 wide, the recording and the navigation rows 288). The panel is the extension's
// `sheet` — which overwrites the kit's bottom sheet in the design — with the rule over the two
// navigation rows, at the default and at both ends of the `width` and `height` editors. A list row is
// `navPlate`: nothing at rest, the hover's plate, a selected plate, the focus ring, a result's 80 dp
// row. The recording is `liftBody` in `danger` with its mark at three points of `recPulse`, its ink
// thicker when selected, and lifted under the pointer rule (B-69), which the page does not pass. A
// navigation row is its `navPlate` and its 22 dp icon. The words are HTML over the drawing, so a cell
// has none.
const SIDEBAR_PANEL = (w, h) =>
  `E.cat(E.sheet(T, ${w}, ${h}, { r: [18, 12, 22, 14], sh: T.sh, seed: 1 }), { L: [E.L(E.inkLine(14, ${w - 14}, ${h - 10 - 2 * 44 - 8}, 2.6, T.seed + 3950), T.ink)], X: [] })`;
const SIDEBAR_ROW = (w, h, o) => `({ L: E.navPlate(T, ${w}, ${h}, ${JSON.stringify(o)}), X: [] })`;
const SIDEBAR_REC = (w, o) => {
  const lift = 2 * o.hv;
  const mark = `(g => ({ L: [E.L(g.ink, T.ink), E.L(g.fill, T.paper)], X: [] }))(E.shape(20 - ${5 + 1.6 * o.pulse}, 30 - ${5 + 1.6 * o.pulse}, ${2 * (5 + 1.6 * o.pulse)}, ${2 * (5 + 1.6 * o.pulse)}, [${5 + 1.6 * o.pulse}, ${(5 + 1.6 * o.pulse) * .95}, ${5 + 1.6 * o.pulse}, ${(5 + 1.6 * o.pulse) * .9}], [1.4, 1.6, 2.2, 1.4], .35, T.seed + 3310, 2))`;
  const body = `{ L: E.liftBody(T, 2, 3, ${w - 4}, 54, [14, 10, 16, 11], { seed: T.seed + 3530, fill: T.danger, sh: 3, noRim: true, insMul: ${o.thick ? 1.4 : 1}, hv: ${o.hv} }), X: [] }`;
  return `E.cat(${body}, ${lift ? `E.place(${mark}, ${-lift}, ${-lift})` : mark})`;
};
const SIDEBAR_NAV = (w, icon, o) =>
  `E.cat({ L: E.navPlate(T, ${w}, 44, ${JSON.stringify(o)}), X: [] }, { L: E.icon(T, '${icon}', 12, 11, { size: 22, color: ${o.sel ? 'T.ink' : 'T.text'}, fill: ${o.sel ? 'T.paper' : 'null'} }), X: [] })`;
const SIDEBAR_LOOKS = [
  ['PanelDefault', { part: 'panel', w: 300, h: 700 }],
  ['PanelNarrow', { part: 'panel', w: 240, h: 400 }],
  ['PanelWide', { part: 'panel', w: 360, h: 1200 }],
  ['RowRest', { part: 'row', w: 274, h: 56, sel: false, hv: 0, focus: false, seed: 0 }],
  ['RowHover', { part: 'row', w: 274, h: 56, sel: false, hv: 1, focus: false, seed: 0 }],
  ['RowHoverHalf', { part: 'row', w: 274, h: 56, sel: false, hv: .5, focus: false, seed: 1 }],
  ['RowSelected', { part: 'row', w: 274, h: 56, sel: true, hv: 0, focus: false, seed: 0 }],
  ['RowSelectedSecond', { part: 'row', w: 274, h: 56, sel: true, hv: 1, focus: false, seed: 1 }],
  ['RowFocus', { part: 'row', w: 274, h: 56, sel: false, hv: 0, focus: true, seed: 0 }],
  ['RowHoverFocus', { part: 'row', w: 274, h: 56, sel: false, hv: 1, focus: true, seed: 2 }],
  ['RowSelectedFocus', { part: 'row', w: 274, h: 56, sel: true, hv: 0, focus: true, seed: 0 }],
  ['ResultSelected', { part: 'row', w: 274, h: 80, sel: true, hv: 0, focus: false, seed: 0 }],
  ['ResultHover', { part: 'row', w: 274, h: 80, sel: false, hv: 1, focus: false, seed: 1 }],
  ['RowNarrow', { part: 'row', w: 214, h: 56, sel: true, hv: 0, focus: true, seed: 0 }],
  ['Recording', { part: 'rec', w: 288, pulse: 0, thick: false, hv: 0 }],
  ['RecordingMid', { part: 'rec', w: 288, pulse: .5, thick: false, hv: 0 }],
  ['RecordingFull', { part: 'rec', w: 288, pulse: 1, thick: false, hv: 0 }],
  ['RecordingSelected', { part: 'rec', w: 288, pulse: 0, thick: true, hv: 0 }],
  ['RecordingHover', { part: 'rec', w: 288, pulse: 1, thick: false, hv: 1 }],
  ['NavRest', { part: 'nav', w: 288, icon: 'person', sel: false, hv: 0, focus: false, seed: 3 }],
  ['NavSelected', { part: 'nav', w: 288, icon: 'person', sel: true, hv: 0, focus: false, seed: 3 }],
  ['NavHover', { part: 'nav', w: 288, icon: 'settings', sel: false, hv: 1, focus: false, seed: 4 }],
  ['NavFocus', { part: 'nav', w: 288, icon: 'settings', sel: false, hv: 0, focus: true, seed: 4 }],
  ['NavSelectedFocus', { part: 'nav', w: 288, icon: 'settings', sel: true, hv: 0, focus: true, seed: 4 }],
];
const SIDEBAR_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) =>
  SIDEBAR_LOOKS.map(([label, opts]) => {
    const { part, w } = opts;
    const plate = { sel: opts.sel, hv: opts.hv, focus: opts.focus, seed: opts.seed };
    const scene =
      part === 'panel' ? SIDEBAR_PANEL(w, opts.h)
        : part === 'row' ? SIDEBAR_ROW(w, opts.h, plate)
          : part === 'rec' ? SIDEBAR_REC(w, opts)
            : SIDEBAR_NAV(w, opts.icon, plate);
    return { group: 'Sidebar', name: `${label}_${themeName}`, theme, themeName, opts, scene };
  }));

export const DESKTOP_CELLS = {
  LevelMeter: METER_CELLS,
  RecordButton: REC_CELLS,
  SpeakerSticker: [...STICKER_CELLS, ...SHAPE_CELLS],
  SearchField: SEARCH_CELLS,
  Sidebar: SIDEBAR_CELLS,
  Scrollbar: SCROLL_CELLS,
  TitleBar: TITLE_CELLS,
  SplitHandle: SPLIT_CELLS,
  Hover: HOVER_CELLS,
  Focus: FOCUS_CELLS,
};
