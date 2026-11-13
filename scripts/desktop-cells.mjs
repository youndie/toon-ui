// B-68: the cells the engine golden harness draws with the desktop design's renderers
// (reference/desktop/desktop-engine.js, research §1.8). That design has no state sheets for its nine
// components (its README says so under its known gaps), so a cell here is one of a component's own
// prop values from its `Toon*.dc.html`, at the page's defaults otherwise. Each component's item adds
// or widens its group; the frames land in engine-goldens/desktop.json, shaped as components.json is.
const THEMES = { il: 'InkGouacheLight', in: 'InkGouacheNight', bc: 'BrightCel' };

// ToonSpeakerSticker.dc.html: `size` 24 | 32 | 48 and `kind` named | unnamed | me, at `shape` 0 and
// `color` 0. The letter is the one its `paint()` falls back to for the kind: М, 2 and Я.
const STICKER_KINDS = [['Named', 'named', 'М'], ['Unnamed', 'unnamed', '2'], ['Me', 'me', 'Я']];
const STICKER_CELLS = Object.entries(THEMES).flatMap(([theme, themeName]) =>
  STICKER_KINDS.flatMap(([label, kind, letter]) => [24, 32, 48].map(size => {
    const opts = { size, kind, shape: 0, color: 0, letter };
    return { group: 'SpeakerSticker', name: `${label}${size}_${themeName}`, theme, themeName, opts, scene: `E.sticker(T, ${JSON.stringify(opts)})` };
  })));

export const DESKTOP_CELLS = {
  SpeakerSticker: STICKER_CELLS,
};
