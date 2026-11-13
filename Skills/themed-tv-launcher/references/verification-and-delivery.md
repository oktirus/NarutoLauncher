# Перевірка, TV, RAM і передача

## Локальне приймання

Запускай фактичні scripts проєкту та meaningful targeted tests. Типова команда Gradle, коли wrapper/tasks наявні:

```powershell
.\gradlew.bat --no-daemon testDebugUnitTest lintDebug assembleDebug --console=plain
```

`--offline` лише якщо потрібні залежності вже є; offline failure не доводить дефект коду. Якщо AGP не переносить кириличний шлях, зроби ізольовану ASCII staging копію, зафіксуй byte/hash відповідність source та APK і поверни артефакт. Не збирай невідому застарілу копію. Lint errors не приховуй `abortOnError false`; warnings оцінюй за змістом. Test count залежить від версії, не копіюй старі 21/48/79 як критерій.

Перевір:

- Unit/control-flow: preferences/import atomicity, order, game physics/HUD hit-testing, cue muted/completion/error/timeout/cancel/double OK, lifecycle delayed callbacks, bounded player/cache counts.
- View integration: actual focus changes, target identity after rebuild, modal isolation, scroll visibility, static previews, close/reopen, trim while panel visible, resume with pending decode/music.
- Runtime/screenshots: компактний TV landscape з реальною density, 720p/1080p/4K за supported devices, empty/typical/overflow dock, довгі українські назви, різні сцени, touch-mode старт → D-pad focus, motion-off.
- Ambient: вхід по idle/вручну, один годинник, Home hidden, повернення Back/OK, не перекрити first-run dialog.
- HOME setup: default/already default, Later/Back, повтор із Settings, role unavailable, settings intents unavailable, refresh після повернення.
- Фінальний diff: немає unrelated змін, ключів, raw private prefs, test stubs у runtime, generated screenshots у source. Freeze exact candidate hashes **після** останньої правки, review проти нього. Manifest не хешує сам себе.

Для regression proof відтворюй конкретний дефект на попередньому коді, коли це практично. Тест має викликати production layout/logic, а не дублювати ту саму помилкову формулу у test helper. Android API doubles доводять керування callback, але не реальний декодер/динамік.

## Встановлення на фізичний TV

Потрібні явно заданий пристрій і дозвіл на write. Не вважай старий IP чи авторизацію для іншого APK/пристрою чинними для нової задачі. Не проси повторно вже однозначно дозволену дію. Готовий локальний кандидат можна створити до запиту на окремий live-крок.

1. Перевір serial, model, Android/ABI, current HOME, package/version/signature, screen/density, power/display і connectivity. Використовуй `adb -s <verified-serial>` для кожної дії, без неявного default target.
2. Backup installed APK та потрібні preferences/app data без відкритих секретів. Debug `run-as` працює лише коли це дозволяє installed build; не обіцяй повний backup для non-debug/OEM. DPAPI CurrentUser підходить для локальної захищеної копії Windows, але не для передачі на інший ПК. Перевір читання/відновлюваність доступного backup.
3. Перевір package/signing compatibility, виконай `install -r` без clear/uninstall. При signature mismatch зупинись і поясни варіанти, не знищуй дані для зручності.
4. Readback package/version і hash installed APK, якщо доступний; open, D-pad/OK/Back/Menu, launch app → return Home, settings/dropdown, scenes/effects, ambient; logcat crash/ANR. Фізичний пульт і injected ADB keys позначай окремо.
5. HOME змінюй лише якщо це дозволено: користувацький system chooser/role. Для явно авторизованого діагностичного ADB change перевір підтримку команди на цьому Android. Збережи попередній component і rollback. Не вимикай stock launcher або accessibility remap без окремої причини/доручення.
6. Перевір Settings → Home з іншої Activity, а не просто foreground після am start. Reboot acceptance виконуй лише коли дозволено; успішна Home key перевірка не означає, що reboot уже протестовано.
7. Віднови тимчасові settings, перевір exact prefs або обґрунтовану semantic equality. Запиши фінальний HOME, foreground, awake/display стан, hashes, rollback і межі тестів.

Журнал живе у проєкті: target, before/after, timestamps, команди, exit/result, backup, відкат. Build і install — різні результати. System Awake не завжди означає світіння екрана: окремий standby daemon/CEC/панель може вимкнути display. При wake проблемі спочатку розділи ці стани; не міняй timers/keymap навмання.

## Чесний вимір ОЗУ

RAM baseline береться на тому самому пристрої за однакового стану: scene/technique/focus, count/pins, motion, audio/music, rotation/idle, foreground, warmup. UI має бути реально видимим; відсутня аура або вимкнений дисплей роблять «економію» некоректною.

Один із розумних стартових протоколів: два незалежні запуски кожної збірки, 30s warmup, п'ять samples на запуск; для регресій після гри — кілька open/play/close циклів із часовою серією. Це початковий дизайн експерименту, не універсальна статистична гарантія. Прийняті/відкинуті runs і причини фіксуй до агрегування.

Основне порівняння — PSS пов'язаних процесів із `dumpsys meminfo`: для WebView врахуй renderer/service process, не додай чужі. KiB/1024 = MiB. RSS не підсумовуй як унікальну пам'ять через shared pages. Зберігай raw output, process association, build hashes, mean/range і кількість samples. Окремі сценарії Home, gallery, music, game, post-game/resume; дані Home без звуку не доводять економію active music.

`+23MiB після гри` не є доведеним leak: GPU/cache/allocator можуть тримати пам'ять. Потрібні ownership analysis, повторні цикли, plateau/growth, контрольована A/B зміна й повторні measurements. Raw bitmap/PCM bytes, APK size і PSS savings — різні метрики. Оцінки позначай як інженерні targets, без вигаданих 95% confidence.

Історичний Naruto case показав значну економію native проти WebView в одному домашньому сценарії; жоден його абсолютний RAM результат не є бюджетом для іншого твору/пристрою. Не знижуй якість непомітно заради красивої цифри.

## Переносний результат і чистота

Віддай source, Gradle wrapper/config, потрібні assets, toolchain prerequisites, build/run instructions, APK, version/package/hash manifest, style bible, prompt/reference provenance, acceptance report і unresolved TODO. Не клади caches, build trees, device credentials, private backups, private signing keys. Якщо потрібен подальший release update, опиши окреме безпечне передавання ключа, не генеруй новий підпис мовчки.

Розпакуй source bundle у новий шлях, перевір relative links і збірку за доступних prerequisites. Не ховай hardcoded local drive/IP. Не включай symlink, що веде назад у робочий checkout. Збережи кілька остаточних screenshots/ROI і raw metrics, потрібних для відтворення; відео й diagnostic crops обмежуй purpose/retention.

Cleanup існуючого проєкту — окремий scope: класифікуй output/original/reference/runtime/backup, список і approval перед видаленням, точні targets, duplicate hashes/keeper, безпечне оборотне видалення. Не залишай rollback scripts, що посилаються лише на прибраний дубль. Не записуй generic launcher lesson у пам'ять користувача без прямого запиту.
