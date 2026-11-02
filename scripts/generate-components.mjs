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
  Divider: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.toonDividerScene',
    ],
    cell: `// B-06: the parity floor. The MVP page's Divider cells against the engine's \`divider\` drawn in Chrome:
// one filled inkLine whose numbers match the engine's within 0.05 dp, so what they differ by is the
// two rasterisers (research D3).
`,
  },
  ListItem: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.ToonListItemLook',
      'io.github.youndie.toon.component.ToonListTrailing',
      'io.github.youndie.toon.component.toonListItemScene',
      'io.github.youndie.toon.icon.ToonIcons',
    ],
    cell: `// The MVP page's list item cells and press strip against the engine's \`listItem\` drawn in Chrome.
`,
  },
  Tabs: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.ToonTabsLook',
      'io.github.youndie.toon.component.toonTabsScene',
    ],
    cell: `// The MVP page's tabs cells and its switch strip against the engine's \`tabs\` drawn in Chrome
// (TabsFrames).
`,
  },
  Slider: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.ToonSliderLook',
      'io.github.youndie.toon.component.toonSliderGrab',
      'io.github.youndie.toon.component.toonSliderScene',
    ],
    cell: `// The v1 page's slider cells and its grab strip against the engine's \`slider\` drawn in Chrome.
`,
  },
  BottomSheet: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.ToonSheetMotion',
    ],
    cell: `// The v1 page's bottom sheet cells and its open strip against the engine's \`sheet\` drawn in Chrome
// over the page's cards (SheetFrames).
`,
  },
  NavigationDrawer: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.ToonDrawerMotion',
    ],
    cell: `// The v1 page's navigation drawer cells and its open strip against the engine's \`drawer\` drawn in
// Chrome over the page's cards (DrawerFrames).
`,
  },
  Tooltip: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.TIP_TEXT',
    ],
    cell: `// The v1 page's tooltip cells and its inflate strip against the engine's \`tooltip\` drawn in Chrome,
// the balloon as wide as Compose measures its text (TipFrames).
`,
  },
  Segmented: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.ToonSegmentedLook',
      'io.github.youndie.toon.component.toonSegmentedScene',
    ],
    cell: `// The v1 page's segmented cells and its switch strip against the engine's \`segmented\` drawn in
// Chrome (SegFrames).
`,
  },
  PullToRefresh: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.ToonPullLook',
    ],
    cell: `// The v1 page's pull-to-refresh cells and its pull and release strips against the engine's \`pull\`
// drawn in Chrome over the page's cards (PullFrames).
`,
  },
  TitleCard: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.BUTTON_LABEL',
    ],
    cell: `// The v1 page's title card cells — the splash and the empty state — and its enter strip against the
// engine's \`titleCard\` drawn in Chrome (TitleFrames).
`,
  },
  Menu: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.ToonMenuLook',
      'io.github.youndie.toon.component.toonMenuScene',
    ],
    cell: `// The v1 page's menu cells and its unroll and roll-up strips against the engine's \`menu\` drawn in
// Chrome (MenuFrames).
`,
  },
  TabsShape: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.ToonTabsLook',
      'io.github.youndie.toon.component.toonTabsScene',
    ],
    cell: `// The MVP page's tabs cells without their labels: the bookmark, the shelf and the press, held to the
// Divider's ink floor.
`,
  },
  NavigationBar: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.ToonBadgeLook',
      'io.github.youndie.toon.component.ToonNavLook',
      'io.github.youndie.toon.component.toonNavBarScene',
      'io.github.youndie.toon.component.toonNavSelect',
    ],
    cell: `// The MVP page's navigation bar cells and its select strip against the engine's \`navBar\` drawn in
// Chrome, over the page's four destinations (NavFrames).
`,
  },
  TopAppBar: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.ToonTopAppBarVariant',
      'io.github.youndie.toon.component.ToonTopBarKey',
      'io.github.youndie.toon.component.ToonTopBarLook',
      'io.github.youndie.toon.component.toonSway',
      'io.github.youndie.toon.component.toonTopBarScene',
      'io.github.youndie.toon.icon.ToonIcons',
    ],
    cell: `// The MVP page's top app bar cells and its sway strip against the engine's \`topBar\` drawn in Chrome.
`,
  },
  Progress: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.toonCircularScene',
      'io.github.youndie.toon.component.toonLinearScene',
      'io.github.youndie.toon.component.toonWormScene',
    ],
    cell: `// The MVP page's progress cells and its worm and spin loops against the engine's \`linear\` and
// \`circular\` drawn in Chrome.
`,
  },
  Dialog: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.BUTTON_LABEL',
    ],
    cell: `// The MVP page's dialog cells and its open and close strips against the engine's \`dialogScene\`
// drawn in Chrome, over the page's demo screen (DialogFrames).
`,
  },
  Snackbar: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.ToonSnackLook',
      'io.github.youndie.toon.component.ToonSnackbarType',
      'io.github.youndie.toon.component.toonSnackScene',
    ],
    cell: `// The Core page's snackbar cells and its show and hide strips against the engine's \`snack\` drawn
// in Chrome; a strip frame is the kit's moment at its age (SnackFrames).
`,
  },
  Card: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.ToonCardLook',
    ],
    cell: `// The Core page's card cells and its appear, press and dismiss strips against the engine's \`card\`
// drawn in Chrome, composed as the page composes them (CardFrames).
`,
  },
  Chip: {
    imports: [
      'androidx.compose.runtime.Composable',
      'io.github.youndie.toon.tokens.brightCel',
      'io.github.youndie.toon.tokens.inkGouacheLight',
      'io.github.youndie.toon.tokens.inkGouacheNight',
      'io.github.youndie.viddik.annotations.ViddikScreenshot',
      'io.github.youndie.toon.component.CHIP_LABEL',
      'io.github.youndie.toon.component.ToonChipLook',
      'io.github.youndie.toon.component.ToonChipVariant',
      'io.github.youndie.toon.component.toonChipScene',
      'io.github.youndie.toon.icon.ToonIcons',
    ],
    cell: `// The MVP page's chip cells and select strip against the engine's \`chip\` drawn in Chrome.
`,
  },
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
