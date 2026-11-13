# Каталог помилок попередньої роботи

Це узагальнення NarutoLauncher з цього чату, а не вимоги копіювати його фічі. Застосовуй рядок лише якщо відповідна можливість є в scope. Історичні review claims могли бути уточнені; використовуй причину й перевірку, не старі кількісні прогнози.

| Спостереження | Що робити і чим довести |
|---|---|
| Багато перемальовувань героя без влучання у зовнішність | Переглянути actual frames, зафіксувати версію обличчя/костюма, порівняти збоку; style-only imitation не виправляє likeness |
| Неприродна поза, руки, канон тренування/техніки | Окремий референс дії/контактів, перевірка складу й фаз, спрощення постановки за brief; не повторювати невдалий prompt без нового висновку |
| Користувач просить прибрати сцену після повторних невдач | Видалити її з усіх runtime selectors/rotation, repair selected ID; не повертати іншою назвою |
| Тематичні назви не пояснюють функцію | Семантика app library/settings/reorder має бути очевидною; не називати застосунки «техніками» автоматично |
| Зайві tagline, зовнішні стрілки/пошук, плашка годинника | Додавати лише функціонально потрібне й погоджене; чиста композиція, не набір тематичних прикрас |
| Улюблене можна додати, але не переставити | Явний D-pad reorder, confirm/cancel, persistent stable order, tests filtered neighbors |
| Док не розтягується, гра лишається вузькою | Measure actual width/count/insets/density; game full agreed width; screenshot compact+wide |
| Settings перемикаються без видимого курсора | Реальний focused state у кожного рядка/control, scroll into view; injected D-pad + screenshot |
| Dropdown підсвічує і поточне значення, і курсор однаково | Відділити committed value від navigation cursor, не зловживати activated selector |
| Підпис шпалер отримав кілька px висоти | Bounds inspection nested layouts, shrink hero title, reserve label height; усі довгі назви на compact TV |
| Ambient містить Home заголовок/док і два годинники | Hide home shell while keep wallpaper; focus/lifecycle restore; actual runtime screenshot |
| OK запускає app раніше за cue / muted отримує delay | Enabled waits completion; muted immediate; cancellation/debounce/error/watchdog tests |
| Watchdog знято cleanup попереднього player | Окреме ownership і generation, перевір watchdog весь prepare+play, stalled callbacks |
| Після pause раптово стартує музика | Prepared callbacks fence identity+generation+lifecycle; bounded players/fades, real speaker acceptance окремо |
| Closed panel тримає mini-game canvas, scene thumbnails, effect previews | Close/dispose/reconstruct policy, clear references/listeners, repeated cycles, A/B measurement |
| Навантаження й RAM від усіх неактивних scenes/effects/PCM | Lazy decode/active assets, bounded byte cache, streamed tracks; не прибирати якість навмання |
| Trim recycle залишив bitmap у живому ImageView, ефект не повернувся | Ownership, drawable detach, reusable clearCache vs final release, trim/resume тест для всіх активних панелей |
| Уся галерея ефектів анімується | Одноразові статичні previews, fixed time/seed, lifecycle без scheduled continuous ticks |
| Shader/анімація є в коді, але target=null і ефект відсутній | Actual focus restore з non-touch start, координати/колір/shape ROI; відеорозмір і глобальна pixel delta не доказ |
| Ефект накрив іконку або сховався під opaque library | Окремі шари в кожному контейнері, behind-icon order, screenshot Home+library |
| Рух завис після uptime або втрати float precision | Relative monotonic double, angle wrap до float, довгий uptime тест |
| Raster і fallback одночасно дають подвоєння деталей | Читати фінальну render branch і complete artwork contract; не нашаровувати заглушки |
| Тіні зробили героя напівпрозорим; jump дивиться неправильно | Paint alpha reset, pose-specific flip/baseline, кадри обох напрямків/поз |
| Motion-off зависає під час scene switch | Окремий finite transition loop; декоративний loop off, фінальний commit scene |
| Game/Menu і lifecycle не узгоджені з Activity | Пріоритет active game input, paused physics до явного resume, один tick chain; touch/render shared bounds |
| Повторна інвентаризація й decoding блокують main | Async bounded work, cancellation/generation, stable focus за package після update |
| Заявлено 100% parity після unit/catalog тестів | Runtime comparison матриці, actual native screenshots, відокремити layout/model/HW/audio/physical remote |
| «Витік 12MiB» заявлено з однієї PSS дельти | Позначити cache/allocator hypothesis, repeated cycles та A/B, без неправдивої confidence |
| PSS пораховано без renderer або порівняно різні focus/audio стани | Associated-process sum і однаковий видимий scenario, KiB/1024, прозорі excluded runs |
| «Встановлено» означало лише локальний APK | Separate install/readback hash/open/key tests; не змінювати HOME випадково під час update |
| Lenovo Awake, але екран не прокидається | Розділити Android power і зовнішній standby/display state; keyboard/power wake не підтверджується ADB wake |
| Користувач мусить шукати default HOME вручну | First-visible-launch suggestion + Settings/status, RoleManager/fallback з consent, без promise auto-popup при install |
| Погодження кожної звичайної команди витрачає роботу | Звичайні дозволені локальні read/edit/build робити послідовно; дотримуватися поточної approval policy, не обходити відмови і не розширювати live scope |
| Review стосується попереднього APK, новий оголошено approved | Freeze source+APK manifest після material edits, перевіряти review against candidate, не хешувати manifest самим собою |
| Пакунок прив'язаний до цього ПК або захаращений гігабайтами | Fresh extraction/build, relative paths, лише needed source/assets/evidence, no secrets/symlinks to checkout |
| Масове cleanup прибрало корисні backups/джерела | Класифікація й точний allowlist до write, keeper hash для duplicates, approval/reversible cleanup і post-check |

## Самоперевірка застосування скілу

- Для нового sci-fi фільму: зміняться medium, props, type, palette й назви, але збережуться інваріанти focus/audio/HOME/lifecycle. Немає автоматичних сувоїв/чакри.
- Для нового anime: реальні кадри потрібної арки, один approved slice перед серією, static catalog і runtime crop check; не фотореалізм через слово «фото».
- Для exact web port: freeze актуального еталону, render branch mapping, native frames і ROI, ніяких «поліпшень» стилю без запиту.
- Без фізичного TV: віддати локально перевірений APK з чіткою межею emulator/HW; не вигадувати install, RAM, wake чи reboot результат.
- Без image generator: не маскувати нестачу generic CSS art; явно позначити assets gap і продовжити незалежні дозволені кроки.

Це desk-check сценарії для оцінки інструкцій, не свідчення створення нового лаунчера чи проходження його runtime тестів.
