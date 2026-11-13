# NarutoLauncher v25 — незалежне RAM/performance-рев'ю (Claude)

Дата: 2026-11-09 · Режим: **read-only** (продукт, APK, prefs, Git, AnimeLauncher не змінювались; ADB і пристрій не чіпав; чужі рев'ю та звіт координатора не читав).
Об'єкт: поточні незакомічені `prototype/` та `android/`, заморожені `revisions/v25/review-source-hashes.json`.
Докази: `reviews/claude-v25-memory-evidence/` (опис — `README.md` там).

## 0. Головне

1. **Спільна першопричина утримання — `closePanel()` не очищує `#panel-content`** (`prototype/app.js:141`). Остання відкрита панель лишається в прихованому DOM: canvas гри 1280×540, 10 canvas-прев'ю технік або 18 повнорозмірних JPEG-мініатюр сцен. На desktop-A/B це видно (GPU-процес: після закриття панелі сцен +140…150 MiB, після технік +190…200 MiB понад Home; панель «Архів», що перезаписує `innerHTML`, повертає GPU до ~265 MiB). На Lenovo виміряно лише цикл гри: **+23.5 MiB PSS** (300 → 323), що не повернулось за 2–8 с після закриття. Це **дельта після одного циклу, а не доведений витік**: я не перевіряв, чи її забере purge під тиском або час.
2. **Аудіо** (на ТВ `prefs.audio=true`, `ambience=false`; AudioContext ще не створено, кеш 0): декодовані буфери ніколи не виходять із кешу (`audio.js:24-26`). Desktop-A/B підтверджує модель: кожен 30-с трек ≈ **+9…+14 MiB renderer** (PCM 10.99 MiB @48 kHz), видалення двох старих треків із кешу — **−23…−25 MiB renderer**. Наразі на ТВ це 0, бо музика вимкнена; стає актуальним при `ambience=true`.
3. **Фон у паузі:** повноекранні canvas (2 × 4.0 MiB raw) тримаються, поки лаунчер за іншим застосунком. Desktop: обнулення canvas у паузі дало **−15…−62 MiB у GPU-процесі** (розкид великий). На Lenovo `Graphics: 0` — ефект у PSS невідомий.
4. Ейгер-декод 18 сцен (142 MiB raw) і 11 плит ефектів (24.75 MiB raw): desktop A/B не показав стабільної різниці renderer на 10-й секунді (lazy-scenes 222 vs baseline 221 MiB, розкид ±5). Це аргумент **не обіцяти** від них steady-state економії; ризик — пік і CPU холодного старту (не виміряно на Lenovo).
5. Перформанс: home-цикл `draw()` дає **60 recalc style/с** (desktop: 300 за 5 с, 0.27 с CPU) і продовжує працювати під модальними панелями, включно з грою, під повноекранним `backdrop-filter: blur(8px)`.

**Сценарні оцінки (не емпіричний прогноз Lenovo; без optimized A/B на ТВ не підтверджені):**

| Сценарій | Lenovo baseline (виміряно) | Оцінка після F1–F4, F7 | Впевненість |
|---|---|---|---|
| Home, щойно після старту, музика вимкнена | 299.9 MiB PSS | ~0…−10 MiB | низька (основне — GPU/кеші, які `meminfo` не показує) |
| Home після одного циклу гри | 323.4 MiB PSS | −10…−20 MiB відносно цього стану | середня (є виміряна дельта + відомий механізм) |
| Музика ввімкнена, трек змінювався | не виміряно на ТВ | −11 MiB за кожен зайвий трек; −11 MiB у фоні | висока для PCM, середня для PSS (desktop ~1:1) |
| Лаунчер у фоні | не виміряно | raw 8 MiB canvas + до 7.9 MiB схованої сцени | низька для PSS (GPU) |

Я **не** встановлюю нижню межу процесу: один `meminfo` на стан не дозволяє стверджувати, що file-backed/clean PSS незмінний — він залежить від resident/shared pages, кешів і тиску.

## 1. Freeze-хеші

| Коли | Результат |
|---|---|
| До рев'ю — `evidence/hashes-before.txt` | `checked=29 mismatches=0` |
| Після рев'ю — `evidence/hashes-after.txt` | `checked=29 mismatches=0` (див. файл) |

## 2. Baseline-спостереження Lenovo (дані координатора, read-only парсинг)

Джерело: `revisions/v25/device/memory/*-4043.txt` (app) і `*-4077.txt` (WebView sandboxed renderer), по 3 семпли; розбір — `evidence/device-meminfo-deltas.txt`, `device-meminfo-categories.json`.

| Стан | PSS разом | app PSS | renderer PSS |
|---|---|---|---|
| home-before | 299.9 (299.1–301.0) | 145.0 | 154.9 |
| game-open | 321.1 | 149.0 | 172.2 |
| home-after-game | 323.4 | 149.1 | 174.3 |

Спостереження (не межі):
- home-before: app private dirty 48.8 / private clean 76.0 MiB; renderer private dirty 32.5 / private clean 105.3 MiB. File-mapped категорії (`.apk/.so/.oat/.art/.jar/.dex/.ttf mmap`) ≈ 199 MiB PSS сумарно; `.apk mmap` 66 + 73 MiB — це, найімовірніше, APK провайдера WebView, не наш APK. `.ttf mmap` renderer 8.8 MiB — системні шрифти.
- `Graphics: 0` в обох процесах: GPU/dmabuf-пам'ять у цьому звіті `meminfo` не відображена. Мала Ashmem на Home (0.95 MiB renderer) **не доводить** відсутність bitmap-пам'яті в інших allocator/GPU-категоріях.
- Дельта home-after-game − home-before: renderer Ashmem +13.3 PSS (+11.8 private dirty), Unknown +3.9; app Native Heap +3.3, Ashmem +1.5. Тобто це dirty-пам'ять, не код.
- Стан UI на ТВ (`ui-before.json`, `audit-ui-final.json`): 6 карток доку (5 pins + «Весь архів»), сцена `team7`, техніка **`gamabunta`** (на Home малюються дві пози — idle/jump), `audio=true`, `ambience=false`, `rotation=0`, `motion=true`. `runtime-baseline.json`: 2 canvas 1366×767 (4.0 MiB RGBA кожен), 11 ефектів завантажено, `scenePreloads: 18`, audio cache 0, AudioContext не створено (звук розблоковується лише першою дією).

## 3. Знахідки за пріоритетом

Легенда: **raw** = пікселі×4 або PCM Float32 (теоретична верхня межа, не PSS). **PSS** = що можна чекати в `dumpsys meminfo` Lenovo. Ризики: V — візуал; S — повний звук техніки на OK; M — mute → миттєвий запуск; G — гра; H — повернення на Home / resume.

### F1 · P1 · `closePanel()` залишає вміст панелі (гра, техніки, сцени) у прихованому DOM

- **Де:** `prototype/app.js:141` (`closePanel` ховає `#modal`, але не очищує `#panel-content`); гра — `prototype/runner.js:38-41` (на кожен mount новий `<canvas 1280×540>` і `decode()` усіх 5 аркушів), `runner.js:101-104` (`dispose` скасовує rAF/listeners, але не звільняє canvas/зображення); техніки — `app.js:207` (10 `<canvas 320×210>` + `<img>`); сцени — `app.js:148` (18 `<img>` з повнорозмірних 1920×1080 JPEG як мініатюри).
- **Механізм:** вміст живе, доки не відкриється інша панель, що перезапише `innerHTML`.
  - Гра: canvas 2.64 MiB raw; аркуші naruto-motion 8 + gamabunta-motion 8 + 2 slide × 4 + forest 2.64 = **26.6 MiB raw** декодуються одразу, хоча малюється лише обраний герой. Гіпотеза: renderer Ashmem +11.8 MiB ≈ naruto-motion + forest (10.6 MiB) як decoded discardable, а app Native Heap +3.3 ≈ backing прискореного canvas у GPU-потоці процесу застосунку. Збіг порядку величини, не траса `memory-infra`.
  - Сцени: 18 × 7.91 = **142 MiB raw** для мініатюр ~280 CSS-px.
  - Техніки: 10 × 0.26 MiB raw canvas + анімація всіх прев'ю (і displacement F5).
- **Рекомендація:** у `closePanel` — `$('#panel-content').replaceChildren()` після `unmount`; у `runner.unmount` — `canvas.width=canvas.height=0` і скинути `images`; декодувати аркуші лише поточного героя; для панелі сцен — окремі мініатюри (~480×270, 0.49 MiB raw кожна → 8.9 MiB замість 142).
- **Економія:** Lenovo — виміряна дельта гри **23.5 MiB PSS** як верхній орієнтир; оцінка **−10…−20 MiB**. Desktop (`ab-summary.txt`): після закриття гри GPU-процес −19…−31 MiB відносно game-open, ще −7…−9 MiB від імітації `width=0 + replaceChildren`; панель сцен +262 MiB GPU відкрита / +140…150 MiB після закриття; техніки +237…251 / +191…204 після закриття; панель «Архів» (перезаписує вміст) повертає GPU до 263…277 MiB. На Lenovo ефект панелей сцен/технік **не виміряно** (GPU поза PSS).
- **Ризик:** V — нуль для очищення; мініатюри — низький. G — повторне відкриття гри заново декодує спрайти (~100–300 мс; `render()` має fallback без спрайта). S/M/H — не зачіпає.

### F2 · P1 при `ambience=true` · Декодовані аудіо-буфери не звільняються; AudioContext не призупиняється у фоні

- **Де:** `prototype/audio.js:24-26` (кеш `Map<url, Promise<AudioBuffer>>` без евікції), `audio.js:60-67,77` (`stopMusic` обнуляє `this.buffer`, але не кеш), `app.js:330` (`YoruPause` → `stopMusic`, `context.suspend()` немає).
- **Механізм:** трек 30.02 с × 2 канали × 48 kHz × 4 B = **10.99 MiB PCM**. Зміна треку лишає попередній у кеші (3 треки = 33 MiB PCM). Репліки: 21 досяжний WAV ≈ 7.5 MiB PCM, якщо прослухати всі; типово 2 (виділення `jutsu` + `gamabunta-confirm`) ≈ 0.5 MiB.
- **Desktop A/B (sampleRate 48000):** трек 2 → +8.9/+9.7 MiB renderer, трек 3 → +13.9/+10.9; евікція двох старих треків → **−24.9/−23.0 MiB**; у паузі видалення музики + `suspend()` → ще −14.6/−16.1 MiB renderer і −35 MiB GPU-процесу.
- **Рекомендація:** при зміні треку видаляти попередній `.m4a` з кешу; у `YoruPause` видаляти буфер музики і `context.suspend()`, у `YoruRefresh` — `resume()` через наявний `unlock()`. Репліки: pinned лише `focus` обраного звуку виділення і `confirm` обраної техніки (те, що вже preload'ить `app.js:71`), решта — LRU 2–4.
- **Економія:** PCM-модель підтверджена desktop ~1:1; на Lenovo **не виміряно** (музика вимкнена). Сценарно −11 MiB за кожен зайвий трек, −11 MiB у фоні.
- **Ризик:** S — **не евіктити** pinned `confirm` обраної техніки, інакше OK чекатиме декоду (8-с fallback `app.js:130` збережеться). M — шлях `if(!prefs.audio){open();return;}` (`app.js:124`) не зачіпається. H — музика після resume стартує на ~0.2–0.5 с пізніше (fade-in 3 с).

### F3 · P2 · Ейгер-декод усіх 18 сцен на старті

- **Де:** `prototype/app.js:335` + `app.js:74-78`.
- **Механізм:** 18 декодів по 7.91 MiB raw паралельно. `decodedScenes` тримає Promise, не `Image`, тож JS-посилань на bitmap немає.
- **Desktop A/B (3 прогони):** renderer на 10-й с: baseline 220.5/221.4/222.5, lazy-scenes 222.2/219.7/226.0 — різниці немає; пік старту «total» 608 vs 600 MiB (−8, в межах шуму). Тобто steady-state ефекту не видно; на Lenovo пік старту не вимірювався.
- **Рекомендація:** декодувати поточну сцену (і наступну, якщо `rotation>0`), решту — на вимогу в `setScene` (там уже `await decode` перед swap).
- **Економія:** PSS steady-state — не очікую; вигода — CPU/пік холодного старту HOME. Потрібен A/B із частими семплами.
- **Ризик:** V — затримка ~100–250 мс при виборі нової сцени з панелі; ротація — без змін при префетчі.

### F4 · P2 · Схований backdrop тримає попередню сцену

- **Де:** `prototype/app.js:82` (у прихованому шарі `backgroundImage` не скидається), `style.css:4` (`.backdrop` з `translate3d` + `transition:opacity 1.6s`).
- **Механізм:** після першої зміни сцени обидва шари мають зображення 1920×1080; невидимий шар лишається композитним із посиланням на decoded image. Raw до +7.9 MiB + тайли.
- **Рекомендація:** після `transitionend` — `layers[old].style.backgroundImage='none'`.
- **Економія:** GPU, на Lenovo в PSS не видно; desktop-A/B цього окремо не ізолював (у тесті сцена не мінялась). Невідомо.
- **Ризик:** V — нуль; повернення на ту саму сцену декодує її знову.

### F5 · P2 · Плити ефектів 768² — усі декодовані й утримувані; displacement 512 рядків

- **Де:** `prototype/technique-art.js:11-14` (11 `Image` в `images`, `decode()` на старті), `technique-art.js:50-66` (`displaced` — 512² offscreen-canvas для fireball/amaterasu/kurama, 512 `drawImage` на тік 30 Гц).
- **Механізм:** 24.75 MiB raw + до 3 MiB поверхонь; на Home (Lenovo — Гамабунта) малюються лише `gamabunta-idle/jump` (2 × 2.25 MiB raw) розміром ~150–215 device-px.
- **Desktop A/B:** lazy-effects (блок 9 з 11 плит) — renderer 224.2/221.4/223.0 проти baseline 221.4 медіани: різниці немає.
- **Рекомендація:** плити 384² (0.56 MiB raw), displacement 256²; ейгер — лише обрана техніка, решта — при відкритті панелі технік.
- **Економія:** raw −18.6 MiB плит, −2.25 MiB поверхонь; PSS на Home — не очікую суттєвої. Основна вигода — CPU панелі технік (3 × 512 `drawImage` на тік).
- **Ризик:** V — середній для fireball/amaterasu/kurama (грубіша хвиля), перевіряти очима; інші — низький.

### F6 · P2 (perf) · Home-цикл працює під модальними панелями і щокадру інвалідує стиль усього документа

- **Де:** `prototype/app.js:316-325` (rAF при `!paused && canMove()` незалежно від `modal`), `app.js:319-320` (`--px/--py` на `documentElement`), `style.css:4` (`.modal-dim{backdrop-filter:blur(8px)}`).
- **Desktop perf (Performance.getMetrics, 5 с):** Home — 301 RecalcStyle / 0.27 с; гра + home-цикл — 300 / 0.31 с, Layout 258, Task 0.71 с; після імітації `cancelAnimationFrame(frame)` — 2 / 0.0002 с. Застереження: у третьому вікні гра вже дійшла до game-over, тож порівняння «гра з циклом vs без» змішане; однозначно виміряно лише, що home-цикл = 60 recalc/с.
- **Рекомендація:** зупиняти `draw()` при `modal && modal!=='techniques'` і відновлювати в `closePanel`; писати `--px/--py` на `.village`, а не на `:root`.
- **Економія:** RAM ≈ 0; CPU/GPU на ARM-ТВ — суттєво (оцінка без Lenovo-виміру).
- **Ризик:** V — фон за 80% затемненням + blur «замерзає»; G — лише краще.

### F7 · P3 · Повноекранні canvas у фоні; немає trim-хука

- **Де:** `prototype/app.js:285-288` (2 × 1366×767 = 8.0 MiB raw), `app.js:330` (`YoruPause` лише чистить technique-canvas), `android/app/src/main/java/ua/konoha/tv/MainActivity.java:158-163` (нема `onTrimMemory`).
- **Desktop A/B:** у паузі `width=height=0` обом canvas (+ `backgroundImage='none'` схованому шару, який у тесті був порожній) → GPU-процес −62.5 / −15.0 MiB, renderer ≈ 0.
- **Рекомендація:** у `YoruPause` (або в `YoruTrim`, що викликається з `onTrimMemory(TRIM_MEMORY_UI_HIDDEN+)`) обнуляти canvas + F2 + F4; у `YoruRefresh` — наявний `resize()`. WebView сам реагує на trim (тайли), але JS-ресурси не звільнить.
- **Економія:** PSS Lenovo — невідомо (GPU поза `meminfo`).
- **Ризик:** H — перший кадр після resume після `resize()`, мілісекунди.

### F8 · P3 · Іконки 640² при показі ≤181 CSS-px

- **Де:** `prototype/icons.js:2-23`, `style.css:4` (`.app-emblem{height:clamp(95px,10.6vw,181px)}`).
- **Механізм:** на 963-px viewport емблема ~102–116 CSS-px ≈ 145–170 device-px. Док Lenovo: 5 тематичних іконок + архів = 6 × 1.56 = **9.4 MiB raw**; панель «Архів» — до 20 × 1.56 = 31 MiB raw.
- **Рекомендація:** WebP 256² (0.25 MiB raw).
- **Економія:** raw −7.8 MiB (док) / −26 MiB (архів). PSS — невідомо: GPU-кеш Chromium може брати зменшений mip, але повний декод WebP відбувається. Desktop: відкрита панель «Архів» +17…+27 MiB GPU-процесу.
- **Ризик:** V — низький на DPR 1.42; для 4K/вищого DPR — 320–384².

### F9 · P3 · Сцени більші за поверхню WebView

WebView рендерить 963×541 CSS × 1.41875 = **1366×767 фізичних px**; `cover` + `scale(1.035…1.08)` потребує ≈1475×830. 1600×900 замість 1920×1080 — −2.4 MiB raw на кожну декодовану сцену. Регенерація ассетів, V — низький ризик.

### F10 · P3 · Android: подвійний refresh на resume

`MainActivity.java:151-156` викликає `YoruRefresh`, і `visibilitychange` (`app.js:332`) викликає його вдруге → 2 × `queryIntentActivities` і 2 × перебудова доку на кожне повернення на Home (CPU і тимчасові алокації на шляху H, не утримання). `appIcon()` (`MainActivity.java:86-97`) робить bitmap 128² + PNG q100 і `recycle()` — витоку немає. Рекомендація: дедуплікація refresh у межах ~500 мс.

### F11 · Інформативно · Bundle/APK

- `releases/bundled-assets.json`: 145 файлів, 30.2 MB; аудіо 62 файли 13.76 MB, з них **38 недосяжних, 6.72 MB** за `audio-reachable.json` (`v4-*`, `full-*`, `clone-*`, `v5-sharingan-confirm.wav` тощо).
- Їх видалення зменшить **лише APK**. PSS — 0: ассети відкриваються тільки на запит (`MainActivity.java:73`).
- `debuggable: true`: Java heap 2–6 MiB, release дасть щонайбільше одиниці MiB — лише A/B.
- `.ttf mmap` 8.8 MiB у renderer: гіпотеза — CJK-гліфи (`忍`, `木`, `砂隠れの里`) підтягують системний Noto CJK. Низький пріоритет, лише A/B.

## 4. Локальний A/B (desktop headless Chromium 1243 — НЕ Lenovo PSS)

Метод: `evidence/memory-ab.mjs`, окремий профіль на кожен прогін у scratchpad (видалено), viewport 963×541 DPR 1.41875, private bytes процесів саме цього профілю. Варіанти лише блокують запити (CDP `Fetch.failRequest`) або виконують runtime-JS в ізольованому браузері. Усі 21 збережений прогін (+1 smoke) — exit 0. Сирі дані — `ab-results*.jsonl`, зведення — `ab-summary.txt`.

| Сценарій | n | renderer Home (медіана, MiB) | GPU-процес Home | Пік старту (total) |
|---|---|---|---|---|
| baseline | 3 | 221.4 | 254.0 | 608.4 |
| lazy-scenes | 3 | 222.2 | 247.9 | 599.7 |
| lazy-effects | 3 | 223.0 | 246.0 | 607.0 |
| lazy-scenes + lazy-effects | 3 | 212.6 (210.6/227.0/212.6) | 249.0 | 593.0 |

Застереження:
- renderer «Home» на 10-й секунді завищений стартовим транзієнтом: у всіх сценаріях він сам падає на ~50–60 MiB протягом наступних ~30 с без дій користувача. Тому дельти між послідовними кроками (game/audio/pause/panels) частково включають цей дрейф; надійні лише великі й узгоджені між двома прогонами зміни (аудіо-евікція, панелі, canvas у паузі).
- GPU-процес desktop (окремий, SwiftShader/ANGLE) не має аналогу в `meminfo` Lenovo (там GPU-потік у процесі застосунку, `Graphics: 0`).
- `Memory.simulatePressureNotification(critical)` додатково знімає ~15–26 MiB renderer і ~13–49 MiB GPU: частину утримуваного Chromium віддає сам під тиском — ще одна причина не називати дельту після гри витоком.

## 5. Що вже добре

- **Аудіо:** дедуплікація завантажень (Promise-кеш, видалення при помилці); дедуплікація голосів і pending (немає «хору»); `onended` від'єднує вузли; журнал обмежено 160; `lifecycle`-токен гасить застарілі cue; горизонт музики ~1.12 тривалості (2–3 вузли); `stopMusic` зупиняє все й чистить таймер. Mute-запуск (`app.js:124`) не створює аудіо-роботи; 8-с fallback (`app.js:130`) не дає застрягнути.
- **Гра:** fixed-step фізика з обмеженням кроку; rAF лише в `running`; `AbortController` для listeners; перешкоди відфільтровуються; пауза з `YoruPause`.
- **Home:** rAF скасовується в паузі/при вимкненому русі; таймери ротації/idle чистяться; trails/bursts обмежені `life`; 28 частинок; DPR canvas ≤ 2; `decodedScenes` тримає Promise, не bitmap; swap фону після `decode()` без спалаху; кеш displacement — `WeakMap` (≤3).
- **Android:** один WebView; `removeJavascriptInterface` + `destroy()` в `onDestroy`; потокова віддача ассетів з APK без буферизації в Java; `Bitmap.recycle()` для іконок; `singleTask`; whitelist шляхів.

## 6. Потрібний A/B на Lenovo (для перетворення оцінок у факти)

1. F1: v25 vs build із `replaceChildren()` у `closePanel` + звільненням canvas гри. Сценарій: Home → гра 30 с → Back → 10 с / 60 с → `meminfo` ×3, плюс те саме для панелей сцен і технік.
2. F2: `ambience=true`, 3 зміни треку, `meminfo`; потім Home → інший застосунок → `meminfo` лаунчера.
3. F3: холодний старт HOME із `meminfo` кожні 250 мс протягом 10 с (пік, не steady-state).
4. F4/F7/F8: окрім `meminfo` — `dumpsys gfxinfo ua.konoha.tv` і, якщо доступно, dmabuf-статистика, бо `Graphics: 0` ховає саме ці зміни.
5. Кожен стан — щонайменше 3 семпли через 10 с і 60 с, щоб відділити утримання від повільного звільнення.

## 7. Обмеження

- Lenovo-дані — лише зібрані координатором (по 3 семпли через 2–8 с після дії); довготривалий спад і поведінку під тиском не вимірював.
- Атрибуція Ashmem/Native Heap до спрайтів і canvas гри — гіпотеза за порядком величини, без трасування.
- Desktop A/B: інший GPU-шлях, стартовий дрейф renderer, `perf`-прогін один і частково змішаний game-over; придатний лише для знаку й порядку величини.
- Жодної оптимізації не реалізовано; нових залежностей немає.
