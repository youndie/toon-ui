# desktop — дизайн десктопного приложения на toon-ui

Состояния по брифу и дополнению от 12 ноября: 41 состояние в трёх темах, Library, Recording и Meeting ещё и в 960 × 600. Всего 189 артбордов.

## Страницы
- `Library`, `Recording`, `Meeting`, `Speakers`, `Settings`, `Onboarding`, `MenuBar` (`.dc.html`) — по странице на экран. Сверху список состояний, дальше по разделу на состояние, в каждом артборды трёх тем. Артборды живые и монтируются по мере прокрутки.
- `Desktop Window Layout.dc.html` — раскладка окна в двух размерах.
- `Desktop Pointer & Keyboard.dc.html` — правило hover, фокус, размер цели, выделение текста, действия без hover.

## Таблица покрытия
Все 41 состояний из полной таблицы дополнения. Без суффикса — 1280 × 800.

| Экран | Состояние | 1280 × 800 | 960 × 600 |
| --- | --- | --- | --- |
| Library | Empty | `Library_Empty_InkGouacheLight`<br>`Library_Empty_InkGouacheNight`<br>`Library_Empty_BrightCel` | `Library_Empty_InkGouacheLight_960x600`<br>`Library_Empty_InkGouacheNight_960x600`<br>`Library_Empty_BrightCel_960x600` |
| Library | Content | `Library_Content_InkGouacheLight`<br>`Library_Content_InkGouacheNight`<br>`Library_Content_BrightCel` | `Library_Content_InkGouacheLight_960x600`<br>`Library_Content_InkGouacheNight_960x600`<br>`Library_Content_BrightCel_960x600` |
| Library | Recording | `Library_Recording_InkGouacheLight`<br>`Library_Recording_InkGouacheNight`<br>`Library_Recording_BrightCel` | `Library_Recording_InkGouacheLight_960x600`<br>`Library_Recording_InkGouacheNight_960x600`<br>`Library_Recording_BrightCel_960x600` |
| Library | SearchResults | `Library_SearchResults_InkGouacheLight`<br>`Library_SearchResults_InkGouacheNight`<br>`Library_SearchResults_BrightCel` | `Library_SearchResults_InkGouacheLight_960x600`<br>`Library_SearchResults_InkGouacheNight_960x600`<br>`Library_SearchResults_BrightCel_960x600` |
| Library | NoResults | `Library_NoResults_InkGouacheLight`<br>`Library_NoResults_InkGouacheNight`<br>`Library_NoResults_BrightCel` | `Library_NoResults_InkGouacheLight_960x600`<br>`Library_NoResults_InkGouacheNight_960x600`<br>`Library_NoResults_BrightCel_960x600` |
| Recording | Setup | `Recording_Setup_InkGouacheLight`<br>`Recording_Setup_InkGouacheNight`<br>`Recording_Setup_BrightCel` | `Recording_Setup_InkGouacheLight_960x600`<br>`Recording_Setup_InkGouacheNight_960x600`<br>`Recording_Setup_BrightCel_960x600` |
| Recording | SetupNoSource | `Recording_SetupNoSource_InkGouacheLight`<br>`Recording_SetupNoSource_InkGouacheNight`<br>`Recording_SetupNoSource_BrightCel` | `Recording_SetupNoSource_InkGouacheLight_960x600`<br>`Recording_SetupNoSource_InkGouacheNight_960x600`<br>`Recording_SetupNoSource_BrightCel_960x600` |
| Recording | Starting | `Recording_Starting_InkGouacheLight`<br>`Recording_Starting_InkGouacheNight`<br>`Recording_Starting_BrightCel` | `Recording_Starting_InkGouacheLight_960x600`<br>`Recording_Starting_InkGouacheNight_960x600`<br>`Recording_Starting_BrightCel_960x600` |
| Recording | StartFailed | `Recording_StartFailed_InkGouacheLight`<br>`Recording_StartFailed_InkGouacheNight`<br>`Recording_StartFailed_BrightCel` | `Recording_StartFailed_InkGouacheLight_960x600`<br>`Recording_StartFailed_InkGouacheNight_960x600`<br>`Recording_StartFailed_BrightCel_960x600` |
| Recording | Recording | `Recording_Recording_InkGouacheLight`<br>`Recording_Recording_InkGouacheNight`<br>`Recording_Recording_BrightCel` | `Recording_Recording_InkGouacheLight_960x600`<br>`Recording_Recording_InkGouacheNight_960x600`<br>`Recording_Recording_BrightCel_960x600` |
| Recording | Silent | `Recording_Silent_InkGouacheLight`<br>`Recording_Silent_InkGouacheNight`<br>`Recording_Silent_BrightCel` | `Recording_Silent_InkGouacheLight_960x600`<br>`Recording_Silent_InkGouacheNight_960x600`<br>`Recording_Silent_BrightCel_960x600` |
| Recording | Stopping | `Recording_Stopping_InkGouacheLight`<br>`Recording_Stopping_InkGouacheNight`<br>`Recording_Stopping_BrightCel` | `Recording_Stopping_InkGouacheLight_960x600`<br>`Recording_Stopping_InkGouacheNight_960x600`<br>`Recording_Stopping_BrightCel_960x600` |
| Meeting | Processing | `Meeting_Processing_InkGouacheLight`<br>`Meeting_Processing_InkGouacheNight`<br>`Meeting_Processing_BrightCel` | `Meeting_Processing_InkGouacheLight_960x600`<br>`Meeting_Processing_InkGouacheNight_960x600`<br>`Meeting_Processing_BrightCel_960x600` |
| Meeting | Failed | `Meeting_Failed_InkGouacheLight`<br>`Meeting_Failed_InkGouacheNight`<br>`Meeting_Failed_BrightCel` | `Meeting_Failed_InkGouacheLight_960x600`<br>`Meeting_Failed_InkGouacheNight_960x600`<br>`Meeting_Failed_BrightCel_960x600` |
| Meeting | Content | `Meeting_Content_InkGouacheLight`<br>`Meeting_Content_InkGouacheNight`<br>`Meeting_Content_BrightCel` | `Meeting_Content_InkGouacheLight_960x600`<br>`Meeting_Content_InkGouacheNight_960x600`<br>`Meeting_Content_BrightCel_960x600` |
| Meeting | AutoNamed | `Meeting_AutoNamed_InkGouacheLight`<br>`Meeting_AutoNamed_InkGouacheNight`<br>`Meeting_AutoNamed_BrightCel` | `Meeting_AutoNamed_InkGouacheLight_960x600`<br>`Meeting_AutoNamed_InkGouacheNight_960x600`<br>`Meeting_AutoNamed_BrightCel_960x600` |
| Meeting | UnnamedSpeakers | `Meeting_UnnamedSpeakers_InkGouacheLight`<br>`Meeting_UnnamedSpeakers_InkGouacheNight`<br>`Meeting_UnnamedSpeakers_BrightCel` | `Meeting_UnnamedSpeakers_InkGouacheLight_960x600`<br>`Meeting_UnnamedSpeakers_InkGouacheNight_960x600`<br>`Meeting_UnnamedSpeakers_BrightCel_960x600` |
| Meeting | AddParticipant | `Meeting_AddParticipant_InkGouacheLight`<br>`Meeting_AddParticipant_InkGouacheNight`<br>`Meeting_AddParticipant_BrightCel` | `Meeting_AddParticipant_InkGouacheLight_960x600`<br>`Meeting_AddParticipant_InkGouacheNight_960x600`<br>`Meeting_AddParticipant_BrightCel_960x600` |
| Meeting | ReassignTurn | `Meeting_ReassignTurn_InkGouacheLight`<br>`Meeting_ReassignTurn_InkGouacheNight`<br>`Meeting_ReassignTurn_BrightCel` | `Meeting_ReassignTurn_InkGouacheLight_960x600`<br>`Meeting_ReassignTurn_InkGouacheNight_960x600`<br>`Meeting_ReassignTurn_BrightCel_960x600` |
| Meeting | Playing | `Meeting_Playing_InkGouacheLight`<br>`Meeting_Playing_InkGouacheNight`<br>`Meeting_Playing_BrightCel` | `Meeting_Playing_InkGouacheLight_960x600`<br>`Meeting_Playing_InkGouacheNight_960x600`<br>`Meeting_Playing_BrightCel_960x600` |
| Meeting | Find | `Meeting_Find_InkGouacheLight`<br>`Meeting_Find_InkGouacheNight`<br>`Meeting_Find_BrightCel` | `Meeting_Find_InkGouacheLight_960x600`<br>`Meeting_Find_InkGouacheNight_960x600`<br>`Meeting_Find_BrightCel_960x600` |
| Meeting | AudioDeleted | `Meeting_AudioDeleted_InkGouacheLight`<br>`Meeting_AudioDeleted_InkGouacheNight`<br>`Meeting_AudioDeleted_BrightCel` | `Meeting_AudioDeleted_InkGouacheLight_960x600`<br>`Meeting_AudioDeleted_InkGouacheNight_960x600`<br>`Meeting_AudioDeleted_BrightCel_960x600` |
| Speakers | Empty | `Speakers_Empty_InkGouacheLight`<br>`Speakers_Empty_InkGouacheNight`<br>`Speakers_Empty_BrightCel` | — |
| Speakers | Content | `Speakers_Content_InkGouacheLight`<br>`Speakers_Content_InkGouacheNight`<br>`Speakers_Content_BrightCel` | — |
| Speakers | Naming | `Speakers_Naming_InkGouacheLight`<br>`Speakers_Naming_InkGouacheNight`<br>`Speakers_Naming_BrightCel` | — |
| Speakers | Merge | `Speakers_Merge_InkGouacheLight`<br>`Speakers_Merge_InkGouacheNight`<br>`Speakers_Merge_BrightCel` | — |
| Speakers | DeleteProfile | `Speakers_DeleteProfile_InkGouacheLight`<br>`Speakers_DeleteProfile_InkGouacheNight`<br>`Speakers_DeleteProfile_BrightCel` | — |
| Settings | Default | `Settings_Default_InkGouacheLight`<br>`Settings_Default_InkGouacheNight`<br>`Settings_Default_BrightCel` | — |
| Settings | ModelDownloading | `Settings_ModelDownloading_InkGouacheLight`<br>`Settings_ModelDownloading_InkGouacheNight`<br>`Settings_ModelDownloading_BrightCel` | — |
| Settings | McpOff | `Settings_McpOff_InkGouacheLight`<br>`Settings_McpOff_InkGouacheNight`<br>`Settings_McpOff_BrightCel` | — |
| Settings | McpOn | `Settings_McpOn_InkGouacheLight`<br>`Settings_McpOn_InkGouacheNight`<br>`Settings_McpOn_BrightCel` | — |
| Settings | PermissionMissing | `Settings_PermissionMissing_InkGouacheLight`<br>`Settings_PermissionMissing_InkGouacheNight`<br>`Settings_PermissionMissing_BrightCel` | — |
| Onboarding | Welcome | `Onboarding_Welcome_InkGouacheLight`<br>`Onboarding_Welcome_InkGouacheNight`<br>`Onboarding_Welcome_BrightCel` | — |
| Onboarding | Microphone | `Onboarding_Microphone_InkGouacheLight`<br>`Onboarding_Microphone_InkGouacheNight`<br>`Onboarding_Microphone_BrightCel` | — |
| Onboarding | SystemAudio | `Onboarding_SystemAudio_InkGouacheLight`<br>`Onboarding_SystemAudio_InkGouacheNight`<br>`Onboarding_SystemAudio_BrightCel` | — |
| Onboarding | ModelDownload | `Onboarding_ModelDownload_InkGouacheLight`<br>`Onboarding_ModelDownload_InkGouacheNight`<br>`Onboarding_ModelDownload_BrightCel` | — |
| Onboarding | TestCapture | `Onboarding_TestCapture_InkGouacheLight`<br>`Onboarding_TestCapture_InkGouacheNight`<br>`Onboarding_TestCapture_BrightCel` | — |
| Onboarding | Denied | `Onboarding_Denied_InkGouacheLight`<br>`Onboarding_Denied_InkGouacheNight`<br>`Onboarding_Denied_BrightCel` | — |
| MenuBar | Idle | `MenuBar_Idle_InkGouacheLight`<br>`MenuBar_Idle_InkGouacheNight`<br>`MenuBar_Idle_BrightCel` | — |
| MenuBar | Recording | `MenuBar_Recording_InkGouacheLight`<br>`MenuBar_Recording_InkGouacheNight`<br>`MenuBar_Recording_BrightCel` | — |
| MenuBar | Processing | `MenuBar_Processing_InkGouacheLight`<br>`MenuBar_Processing_InkGouacheNight`<br>`MenuBar_Processing_BrightCel` | — |

## Что добавлено
- Библиотека toon-ui без правок: `toon-engine.js`, `toon-kit.js`, 27 `Toon*.dc.html`, `assets/textures/`.
- `desktop-engine.js` — 12 новых иконок, токены раскладки, указателя и движения (`DESKTOP`), рендереры новых компонентов, кривые `hoverRamp`, `recPulse`, `livelevel` и контроллер на общем rAF-цикле кита. Движок кита не изменён: всё добавляется в `ToonEngine.prototype` и общую таблицу `ICONS` из этого файла.
- `desktop-base.css` — кириллица для каждой роли шрифта и цвет выделения текста.
- `desktop-sample.js` — вымышленные созвоны, участники, поиск. `desktop-pages.js` — список состояний и артбордов, из него же собрана эта таблица.
- Девять новых компонентов: `ToonTitleBar`, `ToonSidebar`, `ToonSplitHandle`, `ToonScrollbar`, `ToonTranscriptTurn`, `ToonSpeakerSticker`, `ToonLevelMeter`, `ToonRecordButton`, `ToonSearchField`.
- Сборки экранов, не компоненты кита: `DesktopWindow`, `DesktopMeetingView`, `DesktopRecordingView`, `DesktopRecordSetup`, `DesktopNamingSheet`, `DesktopSpeakersView`, `DesktopSettingsView`, `DesktopOnboardingView`, `DesktopMenuBarView`, `DesktopPlaceholderView`, `DesktopParticipantsField`, `DesktopSheet`, `DesktopBoard`, `DesktopArtboard`.

## Решения
- **Hover.** Тело с тенью поднимается на −2, −2, тень остаётся на месте (4 → 6), нажатие роняет её в 0. У плоских элементов вместо подъёма появляется подложка `surface2`. Вход 80 мс на кривой нажатия кита, выход 120 мс, reduce motion — мгновенно. В зоне чтения подложка всегда мгновенная.
- **Фокус.** Пунктирное кольцо кита (`ring`, токен `focus`), у полей ещё и утолщение туши ×1.5.
- **Цели.** Минимум 28 dp, иконка-кнопка 32, контрол 40, строка списка 56, строка навигации 44.
- **Спикеры.** 8 форм стикера × 3 акцента × буква. «Я» — капсула в `primary` со значком микрофона у имени. Голос без имени и ожидаемый, но молчавший участник — пунктирный контур на бумаге.
- **Участники в шапке созвона** заменили ряд фильтров-чипов: клик по участнику фильтрует реплики, «+» открывает поле с подсказками.
- **Поле участников** собрано из `ToonTextField`, `ToonChip` в варианте `input` и `ToonMenu` (`DesktopParticipantsField`). Десятый компонент не понадобился.
- **Лист старта записи** — две колонки: слева источник и микрофон, справа название и участники. Так он помещается в 960 × 600.
- **Поиск.** Совпадение — акцентная метка, текущее — `primary` с обводкой тушью. Выделение текста — `chipSel`, поэтому метки поиска с ним не путаются.
- **Транскрипт** — HTML-текст 16/1.6, строка до 640 px. Монтируются только реплики около видимой области, поэтому 1500 реплик не тормозят.

## Отступления от брифа и допущения
- **Кириллица.** Доступа к репозиторию не было, поэтому пары шрифтов выбраны свои, все под OFL: Figtree → Golos Text, Shrikhand → Lobster, Fredoka → Nunito, Titan One → Rubik 800. Они подключены под именами семейств кита через `unicode-range`. Если в коде пары другие, меняется только `desktop-base.css`.
- **Источник библиотеки.** Файлы скопированы из исходного проекта Claude Design. Сверить их байт в байт с `reference/design-system/` и с демо не получилось.
- **Hover у компонентов кита** показан на странице правила их же рендерерами. В живых артбордах кит hover не рисует: код кита не редактируется.
- **ToonDialog** измеряет себя через `getBoundingClientRect`, поэтому на масштабированном холсте съезжает. `DesktopWindow` делит его рамку на масштаб холста. В приложении масштаба нет, это касается только макетов.
- **Literal-цвета** остались только в хроме страниц-витрин и в заглушках чужих значков в menu bar. На экранах цвета, размеры и длительности идут из токенов темы и `DESKTOP`.
- **Speakers, Settings, Onboarding, MenuBar** нарисованы только в 1280 × 800, как в брифе.
- **Ещё не сделано** (этапы 2 и 4): листы состояний девяти компонентов, кадры анимаций восьми событий, страница иконок.
