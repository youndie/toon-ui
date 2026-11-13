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
].map(c => (c.scene.includes('hoverLift') || c.scene.includes('plate') || c.scene.includes('sel') || c.scene.includes('translate(-2 -2)') ? c : (() => { throw new Error(`hover cell ${c.name} was not wrapped`); })()));
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

export const DESKTOP_CELLS = {
  SpeakerSticker: [...STICKER_CELLS, ...SHAPE_CELLS],
  SearchField: SEARCH_CELLS,
  Hover: HOVER_CELLS,
  Focus: FOCUS_CELLS,
};
