# Поточний стан проєкту — 14.11.2026

Основний продукт: native-parity, applicationId `ua.konoha.tv.nativeparity`, Activity namespace `ua.konoha.tv.nativepreview`. Chromium `android/` + `prototype/` збережено альтернативою. Розробник у UI — Oktirus.

На 13.11 перевірено локальний кандидат з новими defaults і focus: 88 тестів, lint 0 Error/Fatal, 29 Warning, APK 270CA7B938B3314CEDE3E4B5DBA96DFA54FC8AD4E4AFD8D43B1FCFC9EC801A4B. Цей кандидат не встановлювався на TV у тій задачі.

Defaults: motion/audio/ambience=true; rotation=60; idle=900; rasengan, потім rasenshuriken. Explicit preferences зберігаються. HOME/cold open вибирає першу app card, звичайний resume — попередню. HOME setup пропонується один раз після першого видимого запуску і повторно доступний із Settings.

Поточна нативна 1.1.0/versionCode 2 додає GitHub Releases updater: локально 105 тестів, lint 0 Error/Fatal і 30 Warning. APK SHA-256 `463590560D2F1EE8849C8B5834EFD07E10BA1FBA79C70A57D017C4DC2A9869BC`. Публічний репозиторій `oktirus/NarutoLauncher` створено, історію перенесено. Installer просить згоду Android; private signing key не публікується. Реальний TV installer і новий RAM benchmark у цій задачі не перевірялися.

Перший GitHub CI виявив відсутній sdkmanager у PATH runner; workflow тепер явно встановлює SDK 34 через pinned setup-android. Контрольний запуск c8b7169 пройшов. Перенесення виконано оборотно: старі 4104 файли перевірено за хешами в `local-backups/Network-before-split`, у Network залишені pointers. Private archives/rollback data не входять до публічного Git. Нативна збірка зі свіжого public index має ті самі 91 compiled Java classes і runtime assets; debug multidex packaging не є побітово відтворюваним, тому канал оновлень звіряє конкретний випущений APK за SHA-256.

Lenovo зі старого контексту: 192.168.100.26:5555, LNVNB161216, Android 14. Це installation-specific історичний запис, перед доступом перевірити. Поточна міграція не дозволяє нове встановлення чи зміну HOME на TV.

Історичний matched Home RAM 13.11: Chromium 280.39 MiB PSS, native 53.22 MiB, audio off, motion on, dusk/rasengan, 10 samples кожної версії. Це не бюджет для updater/music/game і не новий замір; повний звіт збережено в native-parity/reviews/lenovo-memory-20261113.md.

Git history виділяється з Network без rewrite його source repo. Незакомічені HOME setup/defaults/updater зміни й попередні cleanup deletions мають бути збережені. Локальні private backups/архіви переносяться, але не публікуються.
