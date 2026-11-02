// B-14: write each component's parity fixtures from its cells on the spec pages
// (scripts/component-cells.mjs), one file per component.
//
//   node scripts/generate-components.mjs           write <Group>ParityFixtures.kt
//   node scripts/generate-components.mjs --check   fail if one differs, or if a page no longer says
//                                                  what scripts/component-cells.mjs read from it
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { COMPONENT_CELLS, missingPageText } from './component-cells.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const dir = join(root, 'toon-core/src/desktopTest/kotlin/io/github/youndie/toon/parity');

// The groups drawn by SceneCell (toon-core/src/desktopTest/kotlin/io/github/youndie/toon/parity/SceneCell.kt).
const ICON_BUTTON_IMPORTS = [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.component.ToonIconButtonLook',
      'io.github.youndie.toon.component.ToonIconButtonVariant',
      'io.github.youndie.toon.component.toonIconButtonScene',
      'io.github.youndie.toon.icon.ToonIcons',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
    ];
const BADGE_IMPORTS = [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.component.ToonBadgeLook',
      'io.github.youndie.toon.component.ToonIconButtonLook',
      'io.github.youndie.toon.component.ToonIconButtonVariant',
      'io.github.youndie.toon.component.toonBadgeScene',
      'io.github.youndie.toon.component.toonIconButtonScene',
      'io.github.youndie.toon.icon.ToonIcons',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
    ];

// Per component: the imports and the cell composable its fixtures call.
const PRELUDE = {
  TextField: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.ToonFieldLook',
      'io.github.youndie.toon.component.engineCaretX',
      'io.github.youndie.toon.component.toonFieldScene',
      'io.github.youndie.toon.draw.ToonScene',
      'io.github.youndie.toon.fx.toonStarsScene',
    ],
    cell: `// The Core page's text field cells and its focus, error and caret strips against the engine's \`field\`
// (and \`stars\`) drawn in Chrome.

private fun plus(
    a: ToonScene,
    b: ToonScene,
) = a + b

private fun placedAt(
    scene: ToonScene,
    x: Float,
) = scene.placed(x, 0f)
`,
  },
  Switch: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.ToonSwitchLook',
      'io.github.youndie.toon.component.ToonSwitchOrigin',
      'io.github.youndie.toon.component.toonSwitchScene',
    ],
    cell: `// The Core page's switch cells and toggle strip against the engine's \`sw\` drawn in Chrome.
`,
  },
  Checkbox: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.ToonCheckboxLook',
      'io.github.youndie.toon.component.toonCheckboxScene',
    ],
    cell: `// The Core page's checkbox cells and strips against the engine's \`check\` drawn in Chrome.
`,
  },
  Radio: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.ToonRadioLook',
      'io.github.youndie.toon.component.toonRadioScene',
    ],
    cell: `// The MVP page's radio cells and drop strip against the engine's \`radio\` drawn in Chrome.
`,
  },
  Fab: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.component.FAB_LABEL',
      'io.github.youndie.toon.component.ToonFabAppear',
      'io.github.youndie.toon.component.ToonFabLook',
      'io.github.youndie.toon.component.toonFabLandScene',
      'io.github.youndie.toon.component.toonFabScene',
      'io.github.youndie.toon.icon.ToonIcons',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
    ],
    cell: `// The MVP page's FAB cells and landing strip against the engine's \`fab\` and \`dust\` drawn in Chrome.
`,
  },
  IconButton: {
    imports: ICON_BUTTON_IMPORTS,
    cell: `// The MVP page's icon button cells and tap strip against the engine's \`iconBtn\` drawn in Chrome.
`,
  },
  Badge: {
    imports: BADGE_IMPORTS,
    cell: `// The MVP page's badge cells and strips against the engine's \`badge\` drawn in Chrome.
`,
  },
  TextFloor: {
    imports: [
      'androidx.compose.foundation.Canvas',
      'androidx.compose.foundation.background',
      'androidx.compose.foundation.layout.Box',
      'androidx.compose.foundation.layout.padding',
      'androidx.compose.foundation.layout.size',
      'androidx.compose.runtime.Composable',
      'androidx.compose.ui.Modifier',
      'androidx.compose.runtime.remember',
      'androidx.compose.ui.geometry.Offset',
      'androidx.compose.ui.text.drawText',
      'androidx.compose.ui.unit.dp',
      'io.github.youndie.toon.component.BUTTON_LABEL',
      'io.github.youndie.toon.draw.rememberToonText',
      'io.github.youndie.toon.theme.ToonTheme',
      'io.github.youndie.toon.tokens.ToonTokens',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
    ],
    cell: `// The text floor: the button's label alone, in the button's place and style, against the same text
// drawn by Chrome. What these differ by is the two text rasterisers, the floor for any cell with text.

@Composable
private fun LabelCell(tokens: ToonTokens) {
    ToonTheme(tokens) {
        val text = rememberToonText()
        val label = remember(text) { text.measure("Add plan", BUTTON_LABEL) }
        Box(Modifier.size(180.dp, 82.dp).background(tokens.colors.stage).padding(start = 14.dp, top = 14.dp)) {
            Canvas(Modifier.size(150.dp, 52.dp)) {
                val baseline = 52f / 2 + 18f * .35f
                val x = 75f - label.multiParagraph.width / 2
                drawText(label, tokens.colors.text, Offset(x, baseline - label.firstBaseline))
            }
        }
    }
}
`,
  },
  Button: {
    imports: [
      'androidx.compose.foundation.background',
      'androidx.compose.foundation.layout.Box',
      'androidx.compose.foundation.layout.padding',
      'androidx.compose.foundation.layout.size',
      'androidx.compose.runtime.Composable',
      'androidx.compose.ui.Modifier',
      'androidx.compose.ui.unit.dp',
      'io.github.youndie.toon.component.ToonButtonFrame',
      'io.github.youndie.toon.component.ToonButtonLook',
      'io.github.youndie.toon.component.ToonButtonVariant',
      'io.github.youndie.toon.theme.ToonTheme',
      'io.github.youndie.toon.tokens.ToonTokens',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
    ],
    cell: `// The Core page's button cells against the engine's \`btn\` drawn in Chrome: 150 × 52 on the stage, 14 dp
// left and 16 right, with the state cells' 14 above and 16 below and the strips' 40 above and 14 below.

@Composable
private fun ButtonCell(
    tokens: ToonTokens,
    top: Int,
    bottom: Int,
    variant: ToonButtonVariant,
    look: ToonButtonLook,
) {
    ToonTheme(tokens) {
        Box(
            Modifier
                .size(180.dp, (52 + top + bottom).dp)
                .background(tokens.colors.stage)
                .padding(start = 14.dp, top = top.dp),
        ) {
            ToonButtonFrame("Add plan", variant, 150f, look)
        }
    }
}

@Composable
private fun StateCell(
    tokens: ToonTokens,
    variant: ToonButtonVariant,
    look: ToonButtonLook,
) = ButtonCell(tokens, 14, 16, variant, look)

@Composable
private fun StripCell(
    tokens: ToonTokens,
    look: ToonButtonLook,
) = ButtonCell(tokens, 40, 14, ToonButtonVariant.Filled, look)
`,
  },
  ButtonShape: {
    imports: [
      'androidx.compose.foundation.background',
      'androidx.compose.foundation.layout.Box',
      'androidx.compose.foundation.layout.padding',
      'androidx.compose.foundation.layout.size',
      'androidx.compose.runtime.Composable',
      'androidx.compose.ui.Modifier',
      'androidx.compose.ui.unit.dp',
      'io.github.youndie.toon.component.ToonButtonFrame',
      'io.github.youndie.toon.component.ToonButtonLook',
      'io.github.youndie.toon.component.ToonButtonVariant',
      'io.github.youndie.toon.theme.ToonTheme',
      'io.github.youndie.toon.tokens.ToonTokens',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
    ],
    cell: `// The Core page's button cells without their label, against the engine's \`btn\` drawn in Chrome: 150 × 52 on the stage, 14 dp
// left and 16 right, with the state cells' 14 above and 16 below and the strips' 40 above and 14 below.

@Composable
private fun ButtonCell(
    tokens: ToonTokens,
    top: Int,
    bottom: Int,
    variant: ToonButtonVariant,
    look: ToonButtonLook,
) {
    ToonTheme(tokens) {
        Box(
            Modifier
                .size(180.dp, (52 + top + bottom).dp)
                .background(tokens.colors.stage)
                .padding(start = 14.dp, top = top.dp),
        ) {
            ToonButtonFrame("", variant, 150f, look)
        }
    }
}

@Composable
private fun ShapeStateCell(
    tokens: ToonTokens,
    variant: ToonButtonVariant,
    look: ToonButtonLook,
) = ButtonCell(tokens, 14, 16, variant, look)

@Composable
private fun ShapeStripCell(
    tokens: ToonTokens,
    look: ToonButtonLook,
) = ButtonCell(tokens, 40, 14, ToonButtonVariant.Filled, look)
`,
  },
};

const files = Object.entries(COMPONENT_CELLS).map(([group, cells]) => {
  const { imports, cell } = PRELUDE[group];
  const kt = `// GENERATED by scripts/generate-components.mjs from scripts/component-cells.mjs. Do not edit: run
// \`make components\`, and \`make check\` fails while this file is stale.
package io.github.youndie.toon.parity

${[...imports].sort().map(i => `import ${i}`).join('\n')}

${cell}${cells.map(c => {
    const head = `fun ${c.group}${c.name.replaceAll('_', '')}() =`;
    return `
@ViddikScreenshot(group = "${c.group}", name = "${c.name}", width = ${c.w + c.pad[1] + c.pad[3]}, height = ${c.h + c.pad[0] + c.pad[2]})
@Composable
${!c.kotlin.includes('\n') && head.length + 1 + c.kotlin.length <= 120 ? `${head} ${c.kotlin}` : `${head}\n    ${c.kotlin}`}
`;
  }).join('')}`;
  return [join(dir, `${group}ParityFixtures.kt`), kt];
});

const check = process.argv.includes('--check');
if (missingPageText.length) { console.error(`a spec page no longer says:\n  ${missingPageText.join('\n  ')}`); process.exit(1); }
const stale = [];
for (const [path, kt] of files) {
  if (check) { if (!existsSync(path) || readFileSync(path, 'utf8') !== kt) stale.push(path.slice(root.length + 1)); }
  else writeFileSync(path, kt);
}
if (stale.length) { console.error(`stale: ${stale.join(', ')} - run make components`); process.exit(1); }
const n = Object.values(COMPONENT_CELLS).reduce((a, c) => a + c.length, 0);
console.log(check ? `${n} component fixtures match the spec pages` : `${n} component fixtures written`);
