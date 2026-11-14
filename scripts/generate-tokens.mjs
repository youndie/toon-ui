// B-07: write the token layer from the vendored design (research D4).
//
//   node scripts/generate-tokens.mjs           write ToonTokens.kt
//   node scripts/generate-tokens.mjs --check   fail if the committed file differs, or if a value the
//                                              Core page states is no longer stated there
//
// Two sources, and no typed value of its own. Colours and shape come from `THEMES` in
// toon-engine.js, read by running the engine. The readable token names and the values that are not
// in THEMES — the type scale, the control sizes, the motion durations and springs — are the Core
// spec page's token table (`toon-ui Core.dc.html`, `colorTokens`, `typeScale`, `shapeTokens`). Those
// are page code rather than data, so they are written below, and each is held to the exact text it
// was read from: a re-import that changes the page fails --check here instead of drifting quietly.
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const enginePath = join(root, 'reference/design-system/toon-engine.js');
const corePage = readFileSync(join(root, 'reference/design-system/toon-ui Core.dc.html'), 'utf8');
const out = join(root, 'toon-core/src/commonMain/kotlin/io/github/youndie/toon/tokens/ToonTokens.kt');

globalThis.window = {};
createRequire(import.meta.url)(enginePath);
const THEMES = new globalThis.window.ToonEngine().THEMES;

// The Core page's own text for every value taken from it.
const PAGE = [
  "['ink', T.ink], ['stage', T.bg], ['surface', T.paper], ['surface-pressed', T.surface2], ['text', T.text], ['text-muted', T.muted], ['primary', T.primary], ['on-primary', T.onPrimary]",
  "['danger', T.danger], ['on-danger', T.onDanger], ['danger-text', T.dangerText], ['success', T.success], ['success-light', T.successLight], ['warning', T.warn], ['focus', T.focus]",
  "['track-off', T.off], ['disabled', T.disabledFill], ['fx/dust', T.dust]].concat(T.rim ? [['rim', T.rim], ['field-rim', T.fieldRim]] : [])",
  "{ name: 'Display', spec: `40/44 · ${T.display}`, ff: T.display, fw: 400, fs: 40, lh: 44,",
  "{ name: 'Headline', spec: `28/32 · ${T.display}`, ff: T.display, fw: 400, fs: 28, lh: 32,",
  "{ name: 'Title', spec: `18/24 · ${T.body} 700`, ff: T.body, fw: 700, fs: 18, lh: 24,",
  "{ name: 'Body', spec: `15/22 · ${T.body} 400`, ff: T.body, fw: 400, fs: 15, lh: 22,",
  "{ name: 'Label', spec: `13/16 · ${T.body} 700`, ff: T.body, fw: 700, fs: 13, lh: 16,",
  "rw('toon/stroke/s', mul(.75)), rw('toon/stroke/m', mul(1)), rw('toon/stroke/l', mul(1.35)), rw('toon/stroke/squash-bottom', '×1.3')",
  "rw('toon/shadow/s · l', `${T.sh}×${T.sh} · ${T.sh + 2}×${T.sh + 2} dp, no blur`)",
  "rw('toon/size/button · field · chip', '52 · 56 · 36 dp'), rw('toon/size/target-min', '48 dp'), rw('toon/space/step', '4 dp')",
  "rw('toon/motion/duration', 'snap 80 · quick 160 · normal 260 · slow 420 · scene 600')",
  "rw('toon/motion/spring/bouncy', 'ζ 0.35 · ω 45'), rw('toon/motion/spring/wobbly', 'ζ 0.25 · ω 30'), rw('toon/motion/spring/stiff', 'ζ 0.7 · ω 60')",
  "rw('toon/motion/cascade', '40 ms step, ≤ 600 ms per screen')",
];

// Engine key → Kotlin name. The Core page's names first (camel-cased), then the keys its table does
// not list, which the renderers still use.
const COLORS = [
  ['ink', 'ink'], ['bg', 'stage'], ['paper', 'surface'], ['surface2', 'surfacePressed'], ['text', 'text'], ['muted', 'textMuted'],
  ['primary', 'primary'], ['onPrimary', 'onPrimary'], ['danger', 'danger'], ['onDanger', 'onDanger'], ['dangerText', 'dangerText'],
  ['success', 'success'], ['successLight', 'successLight'], ['warn', 'warning'], ['focus', 'focus'], ['off', 'trackOff'],
  ['disabledFill', 'disabled'], ['dust', 'fxDust'],
  // Not in the Core page's table; named after their engine key.
  ['btnHi', 'primaryHighlight'], ['chipSel', 'chipSelected'], ['field', 'field'], ['on', 'trackOn'], ['thumb', 'thumb'],
  ['sweat', 'fxSweat'], ['fxLine', 'fxLine'], ['holeEdge', 'holeEdge'], ['nail', 'nail'],
];
const NULLABLE = [['rim', 'rim'], ['fieldRim', 'fieldRim']];
const LISTS = [['cardColors', 'cardFills'], ['sColors', 'stickers']];
const THEME_NAMES = { il: 'inkGouacheLight', in: 'inkGouacheNight', bc: 'brightCel' };

const color = hex => `Color(0xFF${hex.slice(1).toUpperCase()})`;
const f = v => (Number.isInteger(v) ? `${v}f` : `${v}f`);
const list = (xs, map) => `listOf(${xs.map(map).join(', ')})`;

let theme = '';
for (const [key, name] of Object.entries(THEME_NAMES)) {
  const T = THEMES[key];
  const c = [...COLORS.map(([k, n]) => `${n} = ${color(T[k])}`), ...NULLABLE.map(([k, n]) => `${n} = ${T[k] ? color(T[k]) : 'null'}`), ...LISTS.map(([k, n]) => `${n} = ${list(T[k], color)}`)];
  theme += `
/** ${T.label}, ${T.sub}. */
internal val ${name}: ToonTokens =
    ToonTokens(
        name = "${name}",
        colors =
            ToonColors(
${c.map(l => `                ${l},`).join('\n')}
            ),
        shape =
            ToonShape(
                wobble = ${f(T.amp)},
                insets = ToonInsets(${T.ins.map(f).join(', ')}),
                buttonRadii = ToonRadii(${T.btnR.map(f).join(', ')}),
                radiusScale = ${f(T.rs)},
                shadow = ${f(T.sh)},
                tilts = ${list(T.rots, f)},
                seed = ${T.seed},
            ),
        displayFamily = "${T.display}",
        bodyFamily = "${T.body}",
    )
`;
}

const kt = `// GENERATED by scripts/generate-tokens.mjs from reference/design-system (B-07). Do not edit:
// change the design, re-import it (B-34) and run \`make tokens\`.
//
// The design keeps three sticker colours and single semantic tokens per theme, with no dark or light
// step per colour: the owner fixed that palette on 2026-11-02 (research §1.3).
package io.github.youndie.toon.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.github.youndie.toon.draw.ToonInsets
import io.github.youndie.toon.draw.ToonRadii

/** A theme's colours: the Core page's \`toon/color/…\` names, then the engine keys its table omits. */
@Immutable
public class ToonColors internal constructor(
${[...COLORS.map(([, n]) => `    public val ${n}: Color,`), ...NULLABLE.map(([, n]) => `    /** Night only. */\n    public val ${n}: Color?,`), ...LISTS.map(([, n]) => `    public val ${n}: List<Color>,`)].join('\n')}
)

/**
 * A theme's shape, in dp: the wobble amplitude, the fill insets that make the ink's weight, the
 * button's four radii, the radius scale every component's radii are multiplied by, the hard shadow's
 * offset, the tilt set, and the base seed.
 */
@Immutable
public class ToonShape internal constructor(
    public val wobble: Float,
    public val insets: ToonInsets,
    public val buttonRadii: ToonRadii,
    public val radiusScale: Float,
    public val shadow: Float,
    public val tilts: List<Float>,
    public val seed: Int,
)

/** One theme's tokens. The families are the design's names; \`type\` binds them to bundled fonts (B-02). */
@Immutable
public class ToonTokens internal constructor(
    public val name: String,
    public val colors: ToonColors,
    public val shape: ToonShape,
    public val displayFamily: String,
    public val bodyFamily: String,
)

/**
 * One step of the type scale: size and line height in sp, weight, and whether it is the display face.
 * The Core page's five are [ToonTypeScale]'s. A step of a consumer's own is made here, or by [copy] of
 * one of them (\`ToonTypeScale.Body.copy(weight = 500)\`, B-91), and \`ToonText\`, \`textStyle\` and
 * \`backed\` set it in the theme's faces as they set the scale's. The body faces are variable, Figtree
 * from 300 to 900 and Fredoka from 300 to 700, and a weight past a face's range draws at its end; the
 * display faces have one weight each.
 *
 * Two steps of the same values are equal. Throws [IllegalArgumentException] for a [size] or a
 * [lineHeight] that is not positive, or a [weight] outside 1 to 1000, \`FontWeight\`'s own range.
 */
@Immutable
public class ToonTypeSpec(
    public val size: Float,
    public val lineHeight: Float,
    public val weight: Int,
    public val display: Boolean,
) {
    init {
        require(size > 0f && lineHeight > 0f) { "a type step of $size / $lineHeight sp" }
        require(weight in 1..1000) { "a type step of weight $weight, outside 1..1000" }
    }

    /** This step with the values given changed. */
    public fun copy(
        size: Float = this.size,
        lineHeight: Float = this.lineHeight,
        weight: Int = this.weight,
        display: Boolean = this.display,
    ): ToonTypeSpec = ToonTypeSpec(size, lineHeight, weight, display)

    override fun equals(other: Any?): Boolean =
        other is ToonTypeSpec &&
            other.size == size &&
            other.lineHeight == lineHeight &&
            other.weight == weight &&
            other.display == display

    override fun hashCode(): Int =
        ((size.hashCode() * 31 + lineHeight.hashCode()) * 31 + weight) * 31 + display.hashCode()

    override fun toString(): String =
        "ToonTypeSpec($size / $lineHeight sp, $weight" + (if (display) ", display)" else ")")
}

/** The Core page's type scale. */
public object ToonTypeScale {
    public val Display: ToonTypeSpec = ToonTypeSpec(40f, 44f, 400, display = true)
    public val Headline: ToonTypeSpec = ToonTypeSpec(28f, 32f, 400, display = true)
    public val Title: ToonTypeSpec = ToonTypeSpec(18f, 24f, 700, display = false)
    public val Body: ToonTypeSpec = ToonTypeSpec(15f, 22f, 400, display = false)
    public val Label: ToonTypeSpec = ToonTypeSpec(13f, 16f, 700, display = false)
}

/** \`toon/stroke/…\`: multipliers of a theme's insets, and the bottom's growth under squash. */
public object ToonStroke {
    public const val S: Float = 0.75f
    public const val M: Float = 1f
    public const val L: Float = 1.35f
    public const val SQUASH_BOTTOM: Float = 1.3f

    /** \`toon/shadow/l\` is the theme's \`s\` plus this, in dp. */
    public const val SHADOW_L_EXTRA: Float = 2f
}

/** \`toon/size/…\` and \`toon/space/step\`, in dp. */
public object ToonSize {
    public const val BUTTON: Float = 52f
    public const val FIELD: Float = 56f
    public const val CHIP: Float = 36f
    public const val TARGET_MIN: Float = 48f
    public const val SPACE_STEP: Float = 4f
}

/** \`toon/motion/…\`: durations in ms, the cascade step, and springs as damping ratio ζ and angular frequency ω. */
public object ToonMotion {
    public const val SNAP: Int = 80
    public const val QUICK: Int = 160
    public const val NORMAL: Int = 260
    public const val SLOW: Int = 420
    public const val SCENE: Int = 600
    public const val CASCADE_STEP: Int = 40
    public const val CASCADE_MAX: Int = 600
    public val Bouncy: ToonSpring = ToonSpring(0.35f, 45f)
    public val Wobbly: ToonSpring = ToonSpring(0.25f, 30f)
    public val Stiff: ToonSpring = ToonSpring(0.7f, 60f)
}

/** A spring by damping ratio [zeta] and angular frequency [omega] (rad/s), as the engine evaluates it. */
@Immutable
public class ToonSpring internal constructor(
    public val zeta: Float,
    public val omega: Float,
)
${theme}`;

const check = process.argv.includes('--check');
const missing = PAGE.filter(s => !corePage.includes(s));
if (missing.length) {
  console.error(`the Core page no longer states:\n  ${missing.join('\n  ')}\nre-read its token table before regenerating`);
  process.exit(1);
}
if (check) {
  if (!existsSync(out) || readFileSync(out, 'utf8') !== kt) { console.error('ToonTokens.kt is stale; run make tokens'); process.exit(1); }
  console.log('ToonTokens.kt matches the design');
} else {
  writeFileSync(out, kt);
  console.log(`ToonTokens.kt written (${Object.keys(THEME_NAMES).length} themes)`);
}
