# Самостійний NarutoLauncher і GitHub updater — 14.11.2026

Користувач доручив виділити launcher з Network, погодив публічний `oktirus/NarutoLauncher`, оновлення через GitHub Releases та точний план локального перенесення. Новий корінь: `E:\файли\Робота\NarutoLauncher`.

## Локальна міграція

Скопійовано й звірено SHA-256 для 4104 файлів (1176988065 байтів): весь наявний launcher і архіви, 18 окремих історичних/knowledge/memory документів, три спеціалізовані скіли й пакунок. Різниця з початковими 4099 — п'ять авторизованих source-файлів updater. Сім code commits і два пов'язані documentation commits виділено зі збереженням авторів/дат; SHA змінилися через перенесення шляхів і вилучення private archive paths із публічної історії. Історію Network не переписано.

Створено самостійні AGENTS.md, Memory/KnowledgeBase indexes, scoped export давніших Codex launcher groups, canonical Skills і project discovery copies. Поточні capture helpers більше не залежать від абсолютного Network/Gemini/Playwright browser path. Історичні manifests лишаються історичними. Старий handoff junction замінено поясненням внутрішнього розташування native-parity.

Backups/.dpapi/keystores/device journals, великі ZIP/revisions/handoff, build/cache та migration manifests не публікуються. Повна локальна історія/матеріали збережені; до public Git включаються код, runtime/original artwork, спеціалізовані навички й відібрана текстова документація. Guard `tools/audit_public_tree.py` перевіряє paths/blobs і впізнавані credentials; його self-test підтверджує блокування повторного додавання приватних fixtures. 51 наявний legacy whitespace case збережено без масового форматування; нових випадків не додано.

## Нативний випуск 1.1.0

versionCode=2, applicationId `ua.konoha.tv.nativeparity`. Settings → Оновлення Конохи показує встановлену версію й ручну перевірку. Фонова перевірка не частіше разу на добу, після відкриття Home і лише коли це не перериває modal/ambient/launch cue. Джерело — stable GitHub Release з update.json та APK.

Завантаження потокове з обмеженнями, timeout/cancel і cleanup часткового файлу. До встановлення перевіряються versionCode, package, size, SHA-256 і сертифікат установленого застосунку. PackageInstaller просить системне підтвердження, дані/HOME не скидаються. Ключ залишається локально; CI debug key не використовується для розповсюдження сумісних оновлень. Publisher спочатку готує локальний staging, потім за явним --publish створює draft, перевіряє завантажені назад assets і публікує stable release.

## Факти перевірки перед публікацією

- 105/105 unit/View/controller/stream/package tests, 0 skipped; lint 0 Error/Fatal, 30 Warning; assembleDebug успішний.
- 17 updater tests охоплюють metadata/repository/redirect/size/hash/cancel, package/version/certificate, abandonment до commit та UI lifecycle. Transport/package metadata test doubles не є перевіркою реального TV installer.
- Переглянуто Skia screenshots пропозиції й очікування оновлення на compact layout. Виправлено test-fixture race: transport блокується latch, щоб screenshot не малював уже закритий dialog.
- APK SHA-256 `463590560D2F1EE8849C8B5834EFD07E10BA1FBA79C70A57D017C4DC2A9869BC`, 15621808 байтів. Підпис збігається з попереднім локальним native APK.
- 158 файлів app/src відповідають повній тестовій staging збірці. Окремий export лише файлів Git index теж успішно збирає APK; залежність від старої Network теки не потрібна.
- Три canonical skills пройшли skill validator. Publisher dry-run перевірено; aapt metadata inspection працює через ASCII temp path, бо Windows aapt не відкриває кириличні шляхи надійно.

GitHub publication/CI readback та остаточний cutover фіксуються в локальному `migration/` журналі після виконання. Жодного встановлення 1.1.0, зміни HOME або нового RAM/wake виміру на Lenovo в цій задачі не виконувалося. Попередня версія без updater потребує одноразового звичайного встановлення 1.1.0 поверх сумісного APK.

## Підтвердження публікації

Cutover завершено: 24 оборотні переміщення, 4104 rollback-файли повторно перевірено за SHA-256, у Network лишилися pointers. Публічний репозиторій [oktirus/NarutoLauncher](https://github.com/oktirus/NarutoLauncher) працює. Після явного provisioning SDK workflow успішно пройшов для code/release commit `523df14930236c9c5fb655b2b833dd9bc68622a0`: [CI run](https://github.com/oktirus/NarutoLauncher/actions/runs/43526253018).

Опубліковано stable [v1.1.0](https://github.com/oktirus/NarutoLauncher/releases/tag/v1.1.0), assets — APK та update.json. Publisher перевірив downloaded readback до зняття draft. Додатковий JVM live-probe скомпілював незмінені production ReleaseUpdate/GitHubReleaseClient, отримав реальний GitHub latest release, завантажив 15621808 байтів і підтвердив SHA-256 конкретного APK. Це перевірка каналу/production transport, а не запуск Android installer на TV.

Chromium-альтернатива також збирається зі свіжого експорту public index. Для standalone clone виправлено створення output/log directories без залежності від ignored revisions, а bundler не включає приватний prefs XML. Public source guard перевіряє поточний index та історію; keys/.dpapi/device archives і великі локальні ZIP не публікуються. Центральна пам'ять Codex переадресовується дозволеною extension note, без прямого переписування базового реєстру.
