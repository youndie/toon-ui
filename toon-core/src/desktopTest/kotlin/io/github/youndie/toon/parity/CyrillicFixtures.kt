package io.github.youndie.toon.parity

import androidx.compose.runtime.Composable
import io.github.youndie.toon.component.BUTTON_LABEL
import io.github.youndie.toon.component.CHECK_LABEL
import io.github.youndie.toon.component.CHIP_LABEL
import io.github.youndie.toon.component.FAB_LABEL
import io.github.youndie.toon.component.ToonBadgeLook
import io.github.youndie.toon.component.ToonButtonLook
import io.github.youndie.toon.component.ToonButtonVariant
import io.github.youndie.toon.component.ToonCardLook
import io.github.youndie.toon.component.ToonCheckboxLook
import io.github.youndie.toon.component.ToonChipLook
import io.github.youndie.toon.component.ToonChipVariant
import io.github.youndie.toon.component.ToonFabLook
import io.github.youndie.toon.component.ToonFieldLook
import io.github.youndie.toon.component.ToonListItemLook
import io.github.youndie.toon.component.ToonListTrailing
import io.github.youndie.toon.component.ToonNavItem
import io.github.youndie.toon.component.ToonNavLook
import io.github.youndie.toon.component.ToonRadioLook
import io.github.youndie.toon.component.ToonSnackLook
import io.github.youndie.toon.component.ToonSnackbarType
import io.github.youndie.toon.component.ToonSwitchLook
import io.github.youndie.toon.component.ToonTabsLook
import io.github.youndie.toon.component.ToonTopAppBarVariant
import io.github.youndie.toon.component.ToonTopBarLook
import io.github.youndie.toon.component.chipWidth
import io.github.youndie.toon.component.toonBadgeScene
import io.github.youndie.toon.component.toonButtonScene
import io.github.youndie.toon.component.toonCardScene
import io.github.youndie.toon.component.toonCheckboxScene
import io.github.youndie.toon.component.toonChipScene
import io.github.youndie.toon.component.toonDialogCardScene
import io.github.youndie.toon.component.toonFabScene
import io.github.youndie.toon.component.toonFieldScene
import io.github.youndie.toon.component.toonListItemScene
import io.github.youndie.toon.component.toonNavBarScene
import io.github.youndie.toon.component.toonRadioScene
import io.github.youndie.toon.component.toonSnackScene
import io.github.youndie.toon.component.toonSwitchRowScene
import io.github.youndie.toon.component.toonTabsScene
import io.github.youndie.toon.component.toonTopBarScene
import io.github.youndie.toon.draw.ToonScene
import io.github.youndie.toon.draw.ToonTextStyle
import io.github.youndie.toon.icon.ToonIcons
import io.github.youndie.toon.tokens.ToonTokens
import io.github.youndie.toon.tokens.brightCel
import io.github.youndie.toon.tokens.inkGouacheLight
import io.github.youndie.toon.tokens.inkGouacheNight
import io.github.youndie.viddik.annotations.ViddikScreenshot

// B-35: every component that carries text, with a Russian label, in each theme: the Cyrillic drawn
// in the face that backs the theme's role, the Latin beside it in the design's face. Goldens only —
// the Chrome references draw no Cyrillic in these faces.

@ViddikScreenshot(group = "Cyrillic", name = "Sheet_InkGouacheLight", width = 420, height = 1290)
@Composable
fun CyrillicSheetInkGouacheLight() = CyrillicSheet(inkGouacheLight)

@ViddikScreenshot(group = "Cyrillic", name = "Sheet_InkGouacheNight", width = 420, height = 1290)
@Composable
fun CyrillicSheetInkGouacheNight() = CyrillicSheet(inkGouacheNight)

@ViddikScreenshot(group = "Cyrillic", name = "Sheet_BrightCel", width = 420, height = 1290)
@Composable
fun CyrillicSheetBrightCel() = CyrillicSheet(brightCel)

@Composable
private fun CyrillicSheet(tokens: ToonTokens) =
    SceneCell(tokens, 420, 1290, 14, 0) {
        fun w(
            s: String,
            style: ToonTextStyle,
        ) = text.measure(s, style).multiParagraph.width
        var y = 0f
        var sheet = ToonScene.Empty

        fun row(
            h: Float,
            scene: ToonScene,
            x: Float = 0f,
        ) {
            sheet += scene.placed(x, y)
            y += h
        }
        row(
            80f,
            toonTopBarScene(
                tokens,
                390f,
                "Суббота",
                ToonTopAppBarVariant.Small,
                listOf(ToonIcons.Search, ToonIcons.More),
                true,
                ToonTopBarLook(),
            ),
            -14f,
        )
        row(64f, toonTabsScene(tokens, 342f, listOf("Сегодня", "Выходные", "Потом"), ToonTabsLook(sel = 1)), 10f)
        row(
            70f,
            toonButtonScene(
                tokens,
                ToonButtonVariant.Filled,
                200f,
                "Добавить план",
                w("Добавить план", BUTTON_LABEL),
                ToonButtonLook(),
            ) +
                toonButtonScene(
                    tokens,
                    ToonButtonVariant.Text,
                    150f,
                    "Отмена",
                    w("Отмена", BUTTON_LABEL),
                    ToonButtonLook(),
                ).placed(214f, 0f),
            10f,
        )
        val chip = "На природе"
        val cw = w(chip, CHIP_LABEL)
        row(
            52f,
            toonChipScene(tokens, ToonChipVariant.Filter, chip, cw, ToonIcons.Calendar, ToonChipLook(selected = true)) +
                toonChipScene(
                    tokens,
                    ToonChipVariant.Input,
                    "Мама",
                    w("Мама", CHIP_LABEL),
                    ToonIcons.Calendar,
                    ToonChipLook(),
                ).placed(chipWidth(ToonChipVariant.Filter, true, cw) + 12, 0f),
            10f,
        )
        row(
            44f,
            toonCheckboxScene(tokens, "Взять сироп", ToonCheckboxLook(on = true)) +
                toonRadioScene(tokens, "Утром", ToonRadioLook(selected = true)).placed(200f, 0f),
            10f,
        )
        row(72f, toonSwitchRowScene(tokens, "Напоминания", 342f, ToonSwitchLook(on = true)), 10f)
        row(
            110f,
            toonFieldScene(
                tokens,
                342f,
                ToonFieldLook(label = "Название плана", value = "Блины в субботу", helper = "Видно всем в группе"),
            ),
            10f,
        )
        row(
            100f,
            toonCardScene(tokens, 0, 342f, "Блинный завтрак", "Кухня, возьмите сироп", "9:00", ToonCardLook()),
            10f,
        )
        row(
            80f,
            toonListItemScene(
                tokens,
                342f,
                ToonIcons.Bell,
                "Напоминания",
                "В полдень",
                ToonListTrailing.Switch,
                ToonListItemLook(on = true),
            ),
            10f,
        )
        row(
            80f,
            toonFabScene(tokens, ToonIcons.Plus, "Новый план", w("Новый план", FAB_LABEL), ToonFabLook()) +
                toonBadgeScene(tokens, ToonBadgeLook("12")).placed(300f, 30f),
            10f,
        )
        y += 30f
        row(90f, toonSnackScene(tokens, 342f, "План добавлен", "Вернуть", ToonSnackbarType.Info, ToonSnackLook()), 10f)
        row(
            250f,
            toonDialogCardScene(
                tokens,
                "Удалить план?",
                listOf("«Покрасить забор» исчезнет", "навсегда."),
                "Удалить",
                "Отмена",
                true,
                null,
                w("Отмена", BUTTON_LABEL),
            ),
            31f,
        )
        val nav =
            listOf(
                ToonNavItem(ToonIcons.Home, "Главная"),
                ToonNavItem(ToonIcons.Search, "Поиск"),
                ToonNavItem(ToonIcons.Heart, "Избранное"),
                ToonNavItem(ToonIcons.Person, "Профиль"),
            )
        row(96f, toonNavBarScene(tokens, 390f, nav, ToonNavLook(sel = 2)), -14f)
        sheet
    }
