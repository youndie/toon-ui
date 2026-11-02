package io.github.youndie.toon.parity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.component.ToonButton
import io.github.youndie.toon.component.ToonListItem
import io.github.youndie.toon.component.ToonTransition
import io.github.youndie.toon.component.ToonTransitionFrame
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.theme.ToonTheme
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import io.github.youndie.viddik.annotations.ViddikScreenshot

// B-44: the v1 page's three transitions between two real screens — a list, then a screen with a
// button — at the page's timestamps, 180 × 320 as the page draws them. The engine's references are
// its `miniScreen` stand-ins, so these are goldens only (viddikVerify): the motion is held to the
// engine numerically by ToonTransitionHostTest, not by pixels.

/** The old screen: three plans. */
@Composable
private fun ListScreen() {
    Box(Modifier.fillMaxSize().background(Toon.tokens.colors.stage)) {
        Column(Modifier.padding(top = 24.dp, start = 4.dp)) {
            ToonListItem("Pancakes", icon = ToonIcons.Calendar, sub = "9:00", width = 172.dp)
            ToonListItem("Bike ride", icon = ToonIcons.Star, sub = "11:30", index = 1, width = 172.dp)
            ToonListItem("Paint", icon = ToonIcons.Heart, sub = "14:00", index = 2, last = true, width = 172.dp)
        }
    }
}

/** The new screen: one plan's page, with its button. */
@Composable
private fun PlanScreen() {
    Box(Modifier.fillMaxSize().background(Toon.tokens.colors.surfacePressed)) {
        Column(Modifier.padding(top = 24.dp, start = 4.dp)) {
            ToonListItem("Saturday", icon = ToonIcons.Calendar, sub = "3 plans", last = true, width = 172.dp)
        }
        Box(Modifier.padding(start = 14.dp, top = 250.dp)) { ToonButton("Add plan", {}, width = 152.dp) }
    }
}

@Composable
private fun TransitionCell(
    tokens: ToonTokens,
    kind: ToonTransition,
    t: Float,
) = ToonTheme(tokens) {
    ToonTransitionFrame(
        kind,
        t,
        false,
        null,
        Modifier.size(180.dp, 320.dp),
        from = { ListScreen() },
        to = { PlanScreen() },
    )
}

@ViddikScreenshot(group = "Transition", name = "Push_0ms_InkGouacheLight", width = 180, height = 320)
@Composable
fun TransitionPush0msInkGouacheLight() = TransitionCell(inkGouacheLight, ToonTransition.Push, 0f)

@ViddikScreenshot(group = "Transition", name = "Push_60ms_InkGouacheLight", width = 180, height = 320)
@Composable
fun TransitionPush60msInkGouacheLight() = TransitionCell(inkGouacheLight, ToonTransition.Push, 60f)

@ViddikScreenshot(group = "Transition", name = "Push_150ms_InkGouacheLight", width = 180, height = 320)
@Composable
fun TransitionPush150msInkGouacheLight() = TransitionCell(inkGouacheLight, ToonTransition.Push, 150f)

@ViddikScreenshot(group = "Transition", name = "Push_240ms_InkGouacheLight", width = 180, height = 320)
@Composable
fun TransitionPush240msInkGouacheLight() = TransitionCell(inkGouacheLight, ToonTransition.Push, 240f)

@ViddikScreenshot(group = "Transition", name = "Push_330ms_InkGouacheLight", width = 180, height = 320)
@Composable
fun TransitionPush330msInkGouacheLight() = TransitionCell(inkGouacheLight, ToonTransition.Push, 330f)

@ViddikScreenshot(group = "Transition", name = "Push_450ms_InkGouacheLight", width = 180, height = 320)
@Composable
fun TransitionPush450msInkGouacheLight() = TransitionCell(inkGouacheLight, ToonTransition.Push, 450f)

@ViddikScreenshot(group = "Transition", name = "Scene_100ms_InkGouacheLight", width = 180, height = 320)
@Composable
fun TransitionScene100msInkGouacheLight() = TransitionCell(inkGouacheLight, ToonTransition.Scene, 100f)

@ViddikScreenshot(group = "Transition", name = "Scene_200ms_InkGouacheLight", width = 180, height = 320)
@Composable
fun TransitionScene200msInkGouacheLight() = TransitionCell(inkGouacheLight, ToonTransition.Scene, 200f)

@ViddikScreenshot(group = "Transition", name = "Scene_260ms_InkGouacheLight", width = 180, height = 320)
@Composable
fun TransitionScene260msInkGouacheLight() = TransitionCell(inkGouacheLight, ToonTransition.Scene, 260f)

@ViddikScreenshot(group = "Transition", name = "Scene_420ms_InkGouacheLight", width = 180, height = 320)
@Composable
fun TransitionScene420msInkGouacheLight() = TransitionCell(inkGouacheLight, ToonTransition.Scene, 420f)

@ViddikScreenshot(group = "Transition", name = "Scene_600ms_InkGouacheLight", width = 180, height = 320)
@Composable
fun TransitionScene600msInkGouacheLight() = TransitionCell(inkGouacheLight, ToonTransition.Scene, 600f)

@ViddikScreenshot(group = "Transition", name = "Hole_0ms_InkGouacheLight", width = 180, height = 320)
@Composable
fun TransitionHole0msInkGouacheLight() = TransitionCell(inkGouacheLight, ToonTransition.Hole, 0f)

@ViddikScreenshot(group = "Transition", name = "Hole_80ms_InkGouacheLight", width = 180, height = 320)
@Composable
fun TransitionHole80msInkGouacheLight() = TransitionCell(inkGouacheLight, ToonTransition.Hole, 80f)

@ViddikScreenshot(group = "Transition", name = "Hole_200ms_InkGouacheLight", width = 180, height = 320)
@Composable
fun TransitionHole200msInkGouacheLight() = TransitionCell(inkGouacheLight, ToonTransition.Hole, 200f)

@ViddikScreenshot(group = "Transition", name = "Hole_330ms_InkGouacheLight", width = 180, height = 320)
@Composable
fun TransitionHole330msInkGouacheLight() = TransitionCell(inkGouacheLight, ToonTransition.Hole, 330f)

@ViddikScreenshot(group = "Transition", name = "Hole_460ms_InkGouacheLight", width = 180, height = 320)
@Composable
fun TransitionHole460msInkGouacheLight() = TransitionCell(inkGouacheLight, ToonTransition.Hole, 460f)

@ViddikScreenshot(group = "Transition", name = "Push_200ms_InkGouacheNight", width = 180, height = 320)
@Composable
fun TransitionPush200msInkGouacheNight() = TransitionCell(inkGouacheNight, ToonTransition.Push, 200f)

@ViddikScreenshot(group = "Transition", name = "Push_200ms_BrightCel", width = 180, height = 320)
@Composable
fun TransitionPush200msBrightCel() = TransitionCell(brightCel, ToonTransition.Push, 200f)

@ViddikScreenshot(group = "Transition", name = "Scene_180ms_InkGouacheNight", width = 180, height = 320)
@Composable
fun TransitionScene180msInkGouacheNight() = TransitionCell(inkGouacheNight, ToonTransition.Scene, 180f)

@ViddikScreenshot(group = "Transition", name = "Scene_180ms_BrightCel", width = 180, height = 320)
@Composable
fun TransitionScene180msBrightCel() = TransitionCell(brightCel, ToonTransition.Scene, 180f)

@ViddikScreenshot(group = "Transition", name = "Hole_260ms_InkGouacheNight", width = 180, height = 320)
@Composable
fun TransitionHole260msInkGouacheNight() = TransitionCell(inkGouacheNight, ToonTransition.Hole, 260f)

@ViddikScreenshot(group = "Transition", name = "Hole_260ms_BrightCel", width = 180, height = 320)
@Composable
fun TransitionHole260msBrightCel() = TransitionCell(brightCel, ToonTransition.Hole, 260f)
