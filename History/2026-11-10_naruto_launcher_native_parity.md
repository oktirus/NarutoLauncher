# 2026-11-10: Повне нативне переписування лаунчера Naruto TV (100% Native Parity)

## Контекст і мета
Майстер Ілля звернувся із завданням доопрацювати та довести до повного паритету нативну версію лаунчера для Smart TV (`NarutoLauncher/native-parity`) відносно еталонної версії Chromium Android TV лаунчера (`NarutoLauncher/android` та веб-прототипу `NarutoLauncher/prototype/index.html`).
Попередня нативна спроба мала суттєві недоліки: відсутні анімації, невідповідність кольорової гами, типографіки, структури карток та поведінки D-Pad.

Вимоги:
1. Виключно нативний Android стек: Java 17 + Android Views + кастомний Canvas 2D рендеринг. Повна заборона на WebView, Jetpack Compose або Flutter.
2. Суворе дотримання ТЗ (`TZ_CHROMIUM_V26_UK.md` та `PROMPT_FOR_AGENT_UK.md`): Caveat + Nunito шрифти, hero title з нахилом -4°, векторні гліфи, центрування `ChakraFocusView` на емблемі іконки, бічні шторки (680px та 1050px), кастомні віджети `KonohaSwitch` та `KonohaSeekBar`, статичні картки 320x210 для вибору технік, раннер 1280x540.
3. Проходження всіх 48 юніт- та UI-рендер тестів Robolectric, чистий Android Lint (0 помилок).
4. Порівняння скріншотів через `tools/image_diff.py` та підтвердження низького MAE.
5. Незалежне ревю двома субагентами Antigravity (Visual/UX та Architecture/Code Quality).

## Хід виконання робіт

1. **Аудит вихідного коду та побудова TODO:**
   - Проведено детальний аналіз еталонного Chromium CSS/JS та попередньої реалізації `native-parity`.
   - Зафіксовано 24 розбіжності та баги в артефакті `TODO_NATIVE_PARITY_ANALYSIS.md`.

2. **Реалізація теми, типографіки та векторних гліфів:**
   - У `KonohaTheme.java` імплементовано завантаження шрифтів `Caveat-Bold.ttf`, `Nunito-Regular.ttf`, `Nunito-SemiBold.ttf` та повну палітру кольорів.
   - Створено `HeroTitleView.java`: малювання рядка 1 "Разом" (`#f5e3b6`) та рядка 2 "до перемоги!" (`#ed943f`) з нахилом `canvas.rotate(-4f)`.
   - Створено `GlyphIconView.java`: векторні SVG-контури для Gamepad, Scroll, Wallpapers, Shuriken, Close, Seal 忍, Headband.
   - Створено `OutlinedGlyphButton.java` та `TechniqueShortcutButton.java` з напівпрозорим фоном, тонкою золотою рамкою та помаранчевим підсвічуванням фокусу.

3. **Фокус Чакри та док застосунків:**
   - У `ChakraFocusView.java` реалізовано динамічний розрахунок координат відносно внутрішньої емблеми `ImageView` картки додатка, а не всієї рамки картки.
   - Введено 10 режимів аури з плавною зміною масштабу та кольору.
   - У `DockView.java` реалізовано підтримку правила екранів TV `<= 800px`: блок `.app-meta` ховається для збереження вертикального простору без кліпінгу.

4. **Бічні шторки (Drawers) та віджети керування:**
   - У `SideSheetContainer.java` реалізовано праву шторку з підтримкою стандартної ширини 680px та розширеної 1050px для сітки технік 3x4. Додано темний scrim-оверлей та золоту праву рамку.
   - Створено `KonohaSwitch.java` (овальний тумблер з помаранчевим треком та бігунком кольору паперу).
   - Створено `KonohaSeekBar.java` (кастомний слайдер з індикатором фокусу).

5. **Статичні прев'ю технік:**
   - У `TechniqueRenderer.java` реалізовано метод `drawStaticPreview()` для генерації бітмапів 320x210 з аурою техніки позаду та іконкою SmartTube/Lampa спереду, без запуску анімаційних циклів.

6. **Міні-гра раннер:**
   - У `RunnerGameView.java` виправлено шляхи до спрайтів (`game/konoha-forest.webp`, `game/game-naruto-motion.webp`, `game/game-naruto-slide.webp`).
   - Додано підтримку Accessibility Actions (`runner_jump`, `runner_slide`, `runner_pause`, `runner_hero`, `runner_exit`).

7. **Вирішення проблем збірки Windows/Gradle:**
   - Виявлено збій ClassLoader Gradle через кириличні шляхи (`E:\файли\Робота\Network\...`).
   - Налаштовано збірку через скрипт `tools/build.py` зі стейджингом в ASCII-директорію `C:\KonohaBuild\parity-xxxx`.

8. **Тестування та ревю субагентів:**
   - Всі 48 юніт- та Robolectric-тестів завершилися успішно.
   - Android Lint пройшов без жодної помилки.
   - Проведено ревю субагентом 1 (Visual/UX): підтверджено паритет кольорів, шрифтів, геометрії та MAE 6.22 на сцені `team7`. Вердикт: **PASS**.
   - Проведено ревю субагентом 2 (Architecture/Code Quality): перевірено життєвий цикл, безпеку потоків, обробку D-Pad. Отримано вердикт **PASS** з рекомендацією винести залишковий `new Paint` з `onDraw`.
   - Рекомендацію субагента 2 виконано в `KonohaSwitch.java`, `KonohaSeekBar.java` та `TechniqueRenderer.java`.

## Фінальний результат
- Скомпільовано підписаний debug-APK: `NarutoLauncher/native-parity/releases/Konoha-parity-1.0.0-debug.apk`.
- Розмір: 15,573,378 байт.
- SHA256: `2643078d7b658d908227ed78ffedff5739db98bb0d87e06beb47cbdff81d252a`.
- Повна документація збережена в `Memory/project_naruto_launcher_native_parity.md`.
