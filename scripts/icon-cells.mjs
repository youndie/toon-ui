// B-11: the icon set as the engine holds it, and the cells it is measured in.
//
// Imported by scripts/generate-icons.mjs (the Kotlin glyphs and their parity fixtures) and by
// scripts/design-references.mjs (the Chrome references), so the two lists cannot drift apart.
// Nothing here is typed from the design: the names and paths are `ICONS` read by running the
// engine, and the cells are the Icons spec page's — Regular, Bold and Filled at 48 dp, Regular and
// Bold at the real 24 dp underneath (`toon-ui Icons.dc.html`, `buildStatic`) — with the kit's
// 2 dp of overflow round the box (`toon-kit.js`, `C.icon.box`), doubled at 48 dp.
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
globalThis.window = globalThis.window || {};
createRequire(import.meta.url)(join(root, 'reference/design-system/toon-engine.js'));

/** `ICONS` in the engine's order: name → { d, fill? }. */
export const ICONS = new globalThis.window.ToonEngine().ICONS;

export const THEMES = { il: 'InkGouacheLight', in: 'InkGouacheNight', bc: 'BrightCel' };

export const pascal = name => name[0].toUpperCase() + name.slice(1);

const VARIANTS = [
  ['Regular48', { size: 48, bold: false, filled: false }],
  ['Bold48', { size: 48, bold: true, filled: false }],
  ['Filled48', { size: 48, bold: false, filled: true }],
  ['Regular24', { size: 24, bold: false, filled: false }],
  ['Bold24', { size: 24, bold: true, filled: false }],
];

/** One cell per theme × icon × variant; Filled only where the icon has a fill. */
export const ICON_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) =>
  Object.entries(ICONS).flatMap(([icon, ic]) =>
    VARIANTS.filter(([, v]) => !v.filled || ic.fill).map(([variant, v]) => {
      const pad = v.size / 12;
      return { group: 'Icon', name: `${pascal(icon)}_${variant}_${themeName}`, theme, themeName, icon, variant, ...v, w: v.size, h: v.size, pad: [pad, pad, pad, pad] };
    })));
