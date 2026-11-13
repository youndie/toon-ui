// B-80: the icons the desktop extension adds to the engine's `ICONS` table (research §1.8), read by
// running both files, never typed.
//
// The extension changes the engine for everything loaded with it (it replaces the kit's `sheet`), so
// it runs here in a context of its own: the engine and the extension evaluated in a fresh `vm` context
// whose `window` stands in for the kit's runtime, which the extension only waits for while it adds its
// icons. Nothing an importer holds is touched.
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import vm from 'node:vm';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');

/** `ICONS` as the engine holds it once the extension has loaded: the kit's, then the extension's. */
function extendedIcons() {
  const window = { ToonKit: {}, addEventListener() {}, dispatchEvent() {} };
  const context = vm.createContext({ window, Event: class {}, setTimeout, console });
  for (const file of ['reference/design-system/toon-engine.js', 'reference/desktop/desktop-engine.js']) {
    vm.runInContext(readFileSync(join(root, file), 'utf8'), context, { filename: file });
  }
  if (!window.DesktopKit?.ok) throw new Error('desktop-engine.js did not initialise');
  return new window.ToonEngine().ICONS;
}

/** The extension's icons, name → { d, fill? }, in the order it adds them. */
export function desktopIcons(kitIcons) {
  const all = extendedIcons();
  const added = Object.keys(all).filter((n) => !(n in kitIcons));
  const replaced = Object.keys(kitIcons).filter((n) => all[n].d !== kitIcons[n].d || all[n].fill !== kitIcons[n].fill);
  if (replaced.length) throw new Error(`the extension changes kit icons: ${replaced.join(', ')}`);
  return Object.fromEntries(added.map((n) => [n, all[n]]));
}
