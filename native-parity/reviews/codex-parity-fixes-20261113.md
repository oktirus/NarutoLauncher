# Виправлення рев’ю native-parity — 13.11.2026

Виправлено всі 12 зауважень рев’ю. Зміни стосуються `native-parity`; Chromium,
попередній `native`, асети й залежності не змінювалися. Пристрій Lenovo не використовувався.

## Реалізація

| № | Зауваження | Результат |
|---|---|---|
| 1 | Фокус тікає за модальну панель | Home блокує фокус на час панелі/Ambient. Dock відновлює тільки власний попередній фокус. Відкладені Home callbacks перевіряють поточний режим. Архів зберігає фокус картки/закріплення при перебудові. |
| 2 | Пауза мишкою закриває гру | Малювання і hit testing використовують спільні RectF. Окремі області паузи, виходу, обох героїв, стрибка й підкату. |
| 3 | Menu не відкриває налаштування | Виправлено порівняння двох null. Menu також повертає з Ambient. |
| 4 | HOME-intent залишає відкриту гру | Додано onNewIntent для MAIN/HOME: скасування запуску/прев’ю, вихід з Ambient і закриття панелі. |
| 5 | Підставні початкові обрані | Для непорожнього реального inventory defaults формуються тільки з установлених застосунків. Раніше збережені відсутні preview.* прибираються. Реальні unavailable pins і явний порожній список збережено. Порожній тестовий inventory продовжує використовувати попередній demo-каталог. |
| 6 | Перестановка через прихований пакет | Сусід визначається за видимим списком; недоступні справжні package IDs залишаються у збережених pins. |
| 7 | Анімація при motion=false | Фіксований час рендеру, відсутність слідів/стрибків і постійного циклу в нерухомому режимі. |
| 8 | Перемикач звуків не оновлює OST | Зміна прапорця одразу викликає updateMusic; вимкнення зупиняє прев’ю репліки. |
| 9 | Слайдери не реагують на pointer | Оброблено DOWN/MOVE/UP/CANCEL, clamp і крок 5%, звільнення перехоплення батьківського ScrollView. |
| 10 | Зайві фонові кадри | Цикли прив’язані до Choreographer, видимості, активної цілі та motion/decor pause. Кросфейд має окремий скінченний цикл. |
| 11 | В архіві немає техніки | Додано ChakraFocusView, прив’язку до emblem вибраної картки/кнопки pin, оновлення при прокручуванні та pause/release. Звичайна чакра за картками, Гамабунта над ними. |
| 12 | Прев’ю програє UI-click | Обидві кнопки вмикають звуки. Повна confirm-репліка має окремий MediaPlayer без app launch guard/callback; cleanup на completion/error/timeout/pause/release/реальному запуску. |

Незалежне рев’ю нових змін виявило спільний mutable FrogState двох шарів.
Його ізольовано для кожного ChakraFocusView; кеш декодованих зображень лишився спільним.
Повторне рев’ю цього виправлення й очищення старих demo pins не виявило нових важливих проблем.
У Build.ps1 виправлено package/version metadata відповідно до app/build.gradle.

## Верифікація

Виконано в ізольованій ASCII-копії через JDK 17 / SDK 34:

```text
gradlew.bat --offline --no-daemon testDebugUnitTest lintDebug assembleDebug --console=plain
```

- **64/64 JUnit**, без failures/errors/skipped: попередні 48, 15 регресійних і одна нова візуальна перевірка.
- Lint: **0 error/fatal, 29 warnings**.
- Всі початкові 13 regression tests відтворювали помилки до зміни production-коду.
- Окремо перевірено старі збережені preview pins: тест падав до виправлення й проходить після.
- Для FrogState тест проходить з ізольованим станом і доведено падає після повторного внесення спільного стану лише у staging.
- Аудіопроба: **15 груп сценаріїв**, 16 створених/16 звільнених MediaPlayer,
  0 активних плеєрів і 0 незавершених Handler jobs наприкінці.
- Програмні Skia PNG: Home, панелі, гра; окремо оглянуто фокус архіву з Чідорі й Гамабунтою, налаштування та гру.
- Перевірено підпис APK через apksigner, package через aapt2, відповідність вихідних файлів staging-копії й SHA-256 скопійованого APK.
- `git diff --check` без зауважень. Немає нових залежностей, commit, push або deployment.

Докази: [results.json](../verification/review-fixes-20261113/results.json),
[JUnit](../verification/review-fixes-20261113/junit/),
[source manifest](../verification/review-fixes-20261113/source-manifest.json),
[аудіо](../verification/review-fixes-20261113/audio-regression-results.json),
[знімки](../verification/review-fixes-20261113/native-ui/).

## APK і межі приймання

APK: [KonohaNative-1.0.0-debug.apk](../releases/KonohaNative-1.0.0-debug.apk)

```text
package: ua.konoha.tv.nativeparity
versionCode: 1
versionName: 1.0.0-native-parity
bytes: 15656332
SHA-256: d044634cf0d565a5b1450fd540e96dd2d3a1570cf68520ff1e0cbad5430f1bf5
```

Це локальна перевірка на Robolectric API 28 зі software Skia, а не приймання на Android TV API 34.
Реальні GPU-шари/частота кадрів, декодування й звучання аудіо, системна кнопка HOME,
PSS/RSS та повний pixel parity на Lenovo потребують живої перевірки.

## Відкат

Ризик зміни — HIGH через lifecycle, аудіоресурси й асинхронний UI. Формат preferences не змінено.
На пристрої дані та HOME не змінювалися.

Точна початкова копія product sources: `C:\Users\angel\AppData\Local\Temp\KonohaFix-20261113-t_68zk9s\baseline`.
Початковий APK: `C:\Users\angel\AppData\Local\Temp\KonohaFix-20261113-t_68zk9s\before-debug.apk`.
Для локального відкату відновлювати лише змінені файли native-parity з цієї копії
та початковий APK; не скидати робочу теку цілком. Збережені справжні pins відновлення не потребують.
