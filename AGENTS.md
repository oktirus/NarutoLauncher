# Naruto · Коноха — правила розробки

## Проєкт і спілкування

- Спілкуйся українською, звертання — «Майстер Ілля». Команди/API/помилки лишай у точному написанні.
- Це самостійний Android TV launcher. Основний продукт — `native-parity/` (Android Views + Custom Canvas, Java 17). `android/` + `prototype/` — альтернативна Chromium/WebView версія; `native/` — історичний native preview, не візуальний еталон.
- Почни з README.md, Memory/MEMORY.md та KnowledgeBase/README.md. Перевір найближчі інструкції та `git status` до змін. Збережи unrelated user changes.
- Погоджені artwork і runtime визначають вигляд. Не замінюй їх generic UI або спрощеним набором ефектів. Для тематичних робіт: Skills/themed-tv-launcher; для anime art: Skills/anime-tv-cel-art; для порту Canvas: Skills/canvas-technique-porting. Їхні старі історичні команди/лічильники звіряй з актуальним кодом і цими правилами.

## Повноваження

- Аналіз/review/діагностика — read-only. Прямий запит виправити код дозволяє локальні оборотні edit/build/test.
- Commit/push/tag/release та публікація потребують прямого доручення. Фізичний TV, встановлення APK, зміна HOME, параметрів живлення чи мережі потребують підтверджених target і дії. Не проси повторного дозволу на вже однозначно авторизований крок.
- Перед масовим перенесенням/cleanup — інвентаризація, точний список, backup/rollback і підтвердження. Не використовуй reset --hard/force push/масове видалення для зручності.
- Не запускай окремих агентів без запиту користувача або явної чинної інструкції на делегування.
- Не зберігай токени, signing keystore, passwords або raw app-private data в Git, логах, screenshots чи source bundles. Захищені локальні backups не є матеріалами для публічного GitHub.

## Незмінні продуктові вимоги

- При чистому встановленні: motion/audio/ambience увімкнені, rotation 60s, ambient через 900s, техніка rasengan. Перші техніки: rasengan, rasenshuriken. Оновлення не перезаписує explicit saved settings.
- Cold open та системний HOME вибирають першу app card з ефектом одразу після готовності inventory/layout. Порожнє обране → Archive. Звичайний resume зберігає попередній package identity.
- Видимий D-pad focus відповідає OK target у Home/library/settings/dropdowns/dialogs/game. Reorder обраного доступний пультом. Сітки/док/підписи адаптивні до physical size **та density**.
- Галерея технік статична. Motion-off зберігає статичний focus; finite scene crossfade працює незалежно від декоративного frame loop.
- Audio off → негайний app launch. Audio on → cue completion, async prepare, watchdog усього prepare/play, cancel/Back, debounce та stale callback guards.
- Ambient приховує Home shell і показує один набір годинника/цитат. Жодних дублювань/накладань.
- Bitmap ownership, bounded lazy caches і close/pause/detach/trim/resume мають бути явними. clearCache — reusable, release/dispose — terminal. Не recycle живий ImageView bitmap.
- Canvas: reuse hot-path objects, relative monotonic double time, modulo rotation перед float, reset Paint alpha після shadows. Raster artwork і fallback не малюються водночас, якщо еталон вибирає if/else.
- GitHub updater: HTTPS, versionCode, package/hash/signing certificate checks, streamed bounded APK, cancel/error lifecycle, Android user confirmation. Жодного PAT у APK, silent install чи автоматичної зміни HOME.

## Реалізація й докази

- Читай сусідній код і caller lifecycle до правок. Мінімальні reviewable diffs, без dependency/framework bumps без потреби.
- Default build: `pwsh -NoProfile -File native-parity/Build.ps1`. Gradle gates: testDebugUnitTest, lintDebug, assembleDebug. Якщо кириличний шлях заважає AGP — ASCII staging зі звіркою hashes.
- Не послаблюй lint або падаючі тести. Regression proof відтворює поведінку production коду, не копіює його формулу у test helper.
- Для UI оглянь screenshots на compact TV та wide layout, довгі українські рядки, фактичні bounds і input. Robolectric/Skia не є доказом HW/GPU/audio/physical remote.
- Анімацію перевіряй у ROI саме потрібного елемента; рух фону, бітрейт відео або наявність onDraw не доводять руху аури. Еталонний renderer має бути реально виконаним, не другою JS-імітацією Android.
- RAM: matched state/warmup, PSS пов'язаних процесів (WebView renderer включно), KiB/1024; raw RGBA/PCM bytes та APK bytes не дорівнюють виміряній економії PSS. Одна дельта після гри не доводить leak.
- Device update: backup → same-signature install -r → package/APK hash readback → actual Home/input/display checks → prefs equality. Android Awake не завжди означає display on. Не заявляй перевірку keyboard/power wake або reboot, якщо її не було.
- Перед завершенням review diff, перевір current source/APK identity, назви результати й точні межі. Не оголошуй 100% parity чи успішне встановлення лише з build.

## Структура й артефакти

`History/` — датовані записи; `Memory/` — актуальний стан/уроки й історичний експорт; `KnowledgeBase/` — архітектура; `Skills/` — канонічні спеціалізовані скіли; `.agents/skills/` — project discovery; `migration/` — локальний журнал перенесення, не публікується.

Архіви `handoff/`, `revisions/`, локальні `verification/`, APK snapshots та rollback backups зберігаються локально. До Git включай код, потрібні runtime assets, корисні текстові review/knowledge docs; до GitHub Releases — перевірений APK та update.json. Приватний signing key залишається поза репозиторієм. Не змінюй центральну пам'ять Codex напряму: лише дозволена користувачем extension note.
