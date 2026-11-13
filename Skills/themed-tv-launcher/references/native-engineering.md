# Нативна основа й інваріанти

## Архітектура

Використовуй Views для доступних контролів, focus і панелей; Custom Canvas для фону, ефектів і гри. Java/Kotlin обирай за кодом і toolchain. Не додавай WebView, Compose, Flutter чи game engine заради імітації готового HTML, якщо домовлено Views/Canvas. Відділи `ThemeSpec`/каталоги сцен, app mapping, ефектів і рядків від механіки AppRepository, Preferences, Audio, Focus, Panels. Це дозволить змінити твір без переписування lifecycle.

Мінімальний стан координатора: активний екран/панель, активний focus identity, foreground/resumed, ambient, motion, pending launch. Уникай розрізнених boolean, що дозволяють одночасні взаємовиключні екрани. Modal має забирати ввід у Home. Спочатку активний game/dropdown/dialog, потім panel, потім Home. Порівняння `activePanel == settingsPanel`, коли обидва null, не означає відкритих Settings.

Manifest: MAIN+HOME+DEFAULT для Home, окреме відкриття через LAUNCHER/LEANBACK_LAUNCHER за потреби, `exported` і launchMode відповідно до SDK. Перевір `onNewIntent` для повторного HOME при singleTask, а не лише cold start. TV не має вимагати touchscreen. Package visibility queries визначай конкретними intent/categories для inventory/default HOME; не копіюй надмірні permissions. applicationId й Activity namespace можуть різнитися — component бери з manifest/APK, не вгадуй.

## App inventory, порядок і фокус

- Сканування PackageManager й декодування іконок — bounded background work; результати застосовуються на main із generation/lifecycle guard. Старий callback не воскресає після close/reload/destroy.
- Реальні launchable packages, дедуплікація TV/phone intent, немає фіктивних Netflix/YouTube у першому запуску. Збережений невстановлений pin — явний missing state/repair, без crash.
- Focus identity — package/component або stable action ID, не індекс і не View зі старого layout. Після install/remove/filter/reorder поверни той самий ID або найближчого видимого сусіда. Прокрути ціль у viewport.
- Long press/Menu для керування pin/reorder не має водночас запускати застосунок. У reorder visible-neighbor order відображай на збережений порядок без втрати прихованих елементів. Перевір confirm/cancel/restart.
- Кожен focusable control має видимий focused/selected/pressed стан. У settings підсвічуй рядок і активний selector, повзунок керується пультом і touch. Popup cursor відокрем від committed selection: не показуй два однакові highlights через `activated`.
- Aura малюється позаду app art, але над потрібним panel background; бібліотека може потребувати власного focus layer. При переході на header/action clear app target. Координати оновлюй після layout/scroll.
- Один Bitmap cache може бути спільним; mutable animation/flight state має належати конкретному focus view. Два екрани не повинні рухати одного shared mascot state.

## Адаптивна геометрія

Рахуй available content width після padding/insets; враховуй density, а не прирівнюй 1366 physical px до 1366dp. Розподіляй ширину доку за реальною кількістю app cards + archive, gaps і readable minimum. Для великої кількості використай контрольований scroll/grid. `fillViewport` саме по собі не розтягує fixed-width cards.

Сітки scenes/effects/library отримують кількість колонок із ширини й мінімального розміру, а не фіксовані 4/5. Перевір 0,1,типовий набір і overflow. Довгі українські назви мають wrap/ellipsize за планом, а не нульову висоту.

Спочатку збережи висоту годинника/опису/кнопки шпалер, потім адаптуй великий декоративний заголовок. `maxLines` не допоможе label, якому parent залишив 4px висоти. Контроль — фактичні bounds і screenshot на компактному landscape, не тільки 1080p mockup.

Full-width game — повна погоджена область, без залишеного резерву під бокову шторку. HUD і touch hit-testing використовують одну geometry model, а не копії формул. Дозволь input лише у View bounds, для zero size/NaN — NONE. Меню паузи не перехоплюється Activity як Settings. Видимі pause/hero/exit controls, D-pad/OK/Back, lifecycle і score persistence мають збігатися.

## Анімація, ambient, Canvas

- Frame loop існує лише коли view attached, window/Activity видимі, екран активний і потрібен рух/перехід. На pause/detach/close прибери callbacks/Choreographer, listeners і stale focus. Resume не створює два паралельні loops.
- Motion-off зупиняє декоративні loops і залишає статичний focus. Одноразовий scene crossfade може мати окремий короткий loop, щоб не зависнути посеред переходу; не прив'язуй його виключно до motion=true.
- Галерея ефектів малює кожне preview один раз на фіксованому time/seed. Async load може одноразово оновити bitmap, але не запускати невидиму нескінченну анімацію для всіх карток.
- Animation time: monotonic relative elapsed, double math; rotation modulo 360 до float. Epoch milliseconds у float знищують дрібну міжкадрову різницю.
- Гарячий `onDraw`: перевикористовуй Paint/Path/RectF/arrays, заготов palette/shaders за потреби. Не декодуй assets і не скануй packages під час draw.
- Canvas save/restore не відновлює Paint. Після shadow alpha=40 обов'язково reset sprite alpha=255; окремі Paint знижують помилки. Контролюй blend/clip/transform стан.
- Hardware Canvas API залежать від Android/GPU. Для glow оцінюй cached art, багатопрохідний stroke або підтримуваний shader; не вимикай acceleration всього UI, щоб замаскувати один ефект.
- Clamp parallax і забезпеч overscan картинки, щоб краї не відкривалися. Перевір edge crop й motion-off.
- Ambient: залиш wallpaper, приховай shell Home (за потреби INVISIBLE для збереження layout), зупини його активні ефекти, покажи тільки ambient контент. На виході віднови shell/focus/timers. First-run/system prompt не перекривається idle режимом.

## Bitmap і кеші

Оціни decoded бюджет `width × height × bytesPerPixel`; RGBA8 ≈4 bytes/px. Це нижня модель pixels, не фактичний PSS/GPU allocation. Декодуй до розміру використання, thumbnail не потребує fullscreen original. RGB_565 — лише після перевірки banding/alpha; не застосовуй механічно.

Тримай активну сцену і тимчасову outgoing для переходу; активний ефект; потрібного героя/пози; bounded icon/thumbnail cache. Не preload усі шпалери, всі здібності та всі game atlases на Home. Memory budget у байтах корисніший за безрозмірний count.

Визнач `clearCache()` (повторно завантажуваний стан) та `release()/dispose()` (остаточне закриття executor/listeners). Після trim живий renderer має відновлюватися. Не викликай terminal shutdown на кожному UI_HIDDEN. Не recycle bitmap, на який ще посилається ImageView/Canvas/інший власник; спочатку detach/clear references або використай безпечну модель ownership без ручного recycle.

Async decode: request token/generation до дорогої роботи і перед commit, stale bitmap віддати/звільнити без публікації; destroy зупиняє подальші commits. Повторне відкриття панелі не використовує disposed executor. onTrimMemory політику звіряй з цільовим SDK і власним станом UI; не покладайся на очікування, що всі callbacks завжди надійдуть.

Close panel прибирає не лише parent reference: зупини ticks, clear drawable refs, listener/maps, великі canvas buffers за визначеним ownership. Тестуй repeated open/close і trim/resume, а не лише onDestroy.

## Аудіо й запуск

Короткі focus звуки — SoundPool, треки — streaming MediaPlayer або обґрунтований наявний механізм. Не накопичуй PCM усіх треків. Master/ambience/motion — різні налаштування; вимкнений motion не вимикає музику випадково. Зміна audio має одразу узгоджувати current playback.

Launch cue state machine: idle → preparing → playing → launch/cancel/fallback. Muted або cue absent: негайний запуск. Enabled: фактичний onCompletion, не вгадана fixed delay. Став bounded watchdog на весь prepare+play (наприклад, 8s для коротких cues; підбери до каталогу). Cleanup старого player не має прибрати watchdog нового запиту. Prepare async, Back/pause скасовують, повторний OK не дублює launch. Completion/error/timeout виконують запуск максимум один раз і лише для актуальної generation.

Застарілий onPrepared після pause/track switch/release не може start audio: перевір player identity, generation і lifecycle/state. Для crossfade обмеж максимум гравців, розклад loop/crossfade скасовуй на stop/error/pause. Тестуй virtual time, реальні тривалості fixture, overdue prepare, багато повторів і release counts. Sound preview у Settings не є pending app launch.

## Preferences і HOME setup

Схема має defaults, version, ID validation і finite/clamped volumes. Import допускає лише документовані типи й nullable defaults; при помилці жодних часткових записів. Зміна app ID не переносить приватні дані старого застосунку автоматично.

На першому **видимому запуску**, якщо не основний і ще не вирішено: діалог «Зробити домашнім екраном?» → Обрати / Пізніше. Пізніше/Back не викликає щоденних нагадувань. У Settings завжди є статус і повторна дія. Після повернення із системи перечитай фактичний default; не довіряй одному resultCode.

API29+: RoleManager, перевір `isRoleAvailable(ROLE_HOME)` і `isRoleHeld`, тоді foreground request через `createRequestRoleIntent`. Fallback: `Settings.ACTION_HOME_SETTINGS`, потім `ACTION_MANAGE_DEFAULT_APPS_SETTINGS`, потім зрозумілий manual шлях/Toast без crash. OEM може не підтримувати роль/екран. Немає гарантованого універсального способу тихо призначити HOME. APK installation без першого запуску не є надійним моментом показу chooser. Для відомого default перевір manifest visibility queries/resolve behavior.

Першоджерела, перевірені при створенні скілу; перед зміною API звір актуальність:

- [Android TV app setup](https://developer.android.com/training/tv/get-started/create)
- [RoleManager](https://developer.android.com/reference/android/app/role/RoleManager)
- [HOME settings action](https://developer.android.com/reference/android/provider/Settings#ACTION_HOME_SETTINGS)
- [Android memory](https://developer.android.com/topic/performance/memory)
