---
id: B-86
title: "The textured stage draws the same pixels on every x86 machine, or can be drawn flat"
status: open
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
