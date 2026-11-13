---
id: B-86
title: "The textured stage draws the same pixels on every x86 machine, or can be drawn flat"
status: done
priority: P2
size: M
stage: stage-4-product
epic: feature-components
---

# B-86 — The textured stage draws the same pixels on every x86 machine, or can be drawn flat

Issue [#35](https://github.com/youndie/toon-ui/issues/35). The same fixture, recorded on WSL2
Ubuntu and on `ubuntu-latest`, differs by 1–4 channel steps on the same 5,350 of 1280×800 pixels.
The differing pixels are only where `ToonStage` shows its texture, which is past viddik's 0.05 %.
A consumer's goldens with the stage then hold on one CPU only. Known elsewhere in the portfolio:
goldens split by runner CPU vendor (Intel against AMD), and Skiko's `ImageShader` sampling.

- **The decision and its reason.** Two steps, in order:
  1. Find what differs. Draw the tile with `FilterQuality.None`, or bake it at the target size,
     and compare one stage frame across two x86 machines (a CI matrix job printing `/proc/cpuinfo`
     and the frame's hash). Keep the change only if the hashes agree.
  2. Either way, add a switch to draw the stage flat in tests, through the theme like reduce motion.
     plakat-ui's `PlakatPrintSwitches(texture = false)` does the same, and it is the guarantee that
     needs no second machine.
- Rejected: a looser tolerance in the consumer. It would hide every real change on the stage too.
- Not covered: the paper textures inside components, unless step 1 finds the same cause there.

- AC: the switch draws the stage in its flat ground colour, by a test. Step 1's verdict is written
  in the findings with the two machines' CPU names and hashes. If it is "deterministic", a matrix
  job holds it.
- Anchors: `toon-core/src/commonMain/kotlin/io/github/youndie/toon/`, `.github/workflows/`.

## Findings

- **Step 1: the cause was not reproduced.** Hypothesis: Skiko samples the tile nearest, and at
  1 px a dp (scale 0.5) every pixel's centre lands on the line between two texels, so a CPU's
  rounding picks one. The stage now shifts the shader so each pixel samples a texel's centre at 1 and
  2 px a dp. `ToonStageSamplingTest` checks that every pixel is exactly its texel, in both gouache
  tiles at densities 1 and 2. It passed on:
  - the Mac (Apple silicon, the gate's runner);
  - `ubuntu-latest` on an AMD EPYC 9V45 with the shift;
  - `ubuntu-latest` on an AMD EPYC 7763 **without** the shift (the experiment branch
    `exp/b-86-no-shift`, run 43504092534).
  So on these three CPUs the tie already went the same way, and the shift changes no pixel there: no
  golden moved. The machine the issue names (WSL2 Ubuntu, the owner's box) was not tried, so whether
  the shift fixes the issue's 5,350 pixels is **not measured**. The issue's difference may come from
  elsewhere, for instance a density other than 1 or 2 in the consumer's fixture.
- **Kept anyway:** the shift removes the tie by construction and moves nothing on the measured
  machines. The new `stage-x86` job in `check.yaml` runs the sampling test on a Linux x86 runner on
  every push and prints its CPU, so a runner that draws the stage differently now turns CI red
  instead of a consumer's goldens.
- **Step 2, the guarantee:** `ToonTheme(stageTexture = false)` (read as `Toon.stageTexture`) draws
  `ToonStage` in the flat stage colour, as plakat-ui's `PlakatPrintSwitches(texture = false)` does.
  `stageTexture false draws the stage in its flat colour` holds it. A consumer's goldens with the flat
  stage are a claim about no texture at all.
- Mutants: the switch ignored is caught. The shift removed is **not** caught on any machine tried,
  for the reason above.
