# Журнал врахування зауважень незалежного аудиту Native v1

**Дата:** 9 листопада 2026 року  
**Базова версія:** NarutoLauncher Native v1 (Android Views + Custom Canvas, Java 17)  
**Документ аудиту:** `NarutoLauncher/native/reviews/antigravity-native-review.md`  
**Статус верифікації baseline v26:** 780/780 PASS (`verify_v26.py`)

---

## 1. Резюме виправлень критичних зауважень (P1)

### [P1-1] Витік дескрипторів файлів (FD Leak) у `AudioController`
* **Файл:** `app/src/main/java/ua/konoha/tv/nativepreview/audio/AudioController.java`
* **Проблема:** Метод `loadSound` не закривав `AssetFileDescriptor`, що викликало поступове вичерпання дескрипторів процесу (`EMFILE`).
* **Виправлення:** Переведено відкриття дескриптора на конструкцію `try (AssetFileDescriptor afd = context.getAssets().openFd(assetPath))`. Дескриптор коректно закривається одразу після передачі в `SoundPool.load()`.
* **Статус:** ВИПРАВЛЕНО.

### [P1-2] Навантаження на GC (GC Churn) у циклі села `VillageBackgroundView`
* **Файл:** `app/src/main/java/ua/konoha/tv/nativepreview/views/VillageBackgroundView.java`
* **Проблема:** Щокадрове створення `LinearGradient`, масивів кольорів та 28 екземплярів `Path` для листя призводило до ~1800 об'єктів/с та частих пауз збирача сміття.
* **Виправлення:**
  1. Шейдери `leftScrim` та `bottomScrim` кешуються і створюються лише при зміні геометрії `w` / `h`.
  2. Константи кольорів розпарсено у `static final int`.
  3. Для листя застосовано єдиний багаторазовий `leafPath.reset()`.
  4. Для малювання бітмапів додано спільний `dstRect.set(...)`.
* **Статус:** ВИПРАВЛЕНО.

### [P1-3] Навантаження на GC у процедурному Чідорі `TechniqueRenderer`
* **Файл:** `app/src/main/java/ua/konoha/tv/nativepreview/views/TechniqueRenderer.java`
* **Проблема:** 20 нових `Path` щокадру в циклі блискавок Чідорі.
* **Виправлення:**
  1. Додано спільний екземпляр `chidoriPath` з очищенням `.reset()`.
  2. Кешовано константи кольорів (`COLOR_CHIDORI_BLUE`, `COLOR_CHIDORI_WHITE`, `COLOR_CHIDORI_SPARK`, `COLOR_DUST`).
  3. Додано метод `release()` для явного звільнення та `.recycle()` усіх кешованих бітмапів ефектів.
* **Статус:** ВИПРАВЛЕНО.

### [P1-4] Невидимий фокус D-pad на пульті TV у меню вибору шпалер і технік
* **Файли:** `panels/ScenesPanel.java`, `panels/TechniquesPanel.java`
* **Проблема:** Встановлення суцільного непрозорого кольору фону перекривало стандартний системний селектор фокусу Android TV.
* **Виправлення:**
  1. `ScenesPanel`: додано `card.setOnFocusChangeListener` з акцентним підсвічуванням кольором `#2a5568` при отриманні фокусу.
  2. `TechniquesPanel`: додано `card.setOnFocusChangeListener` з виразним золотаво-бронзовим підсвічуванням `#3a3520`.
* **Статус:** ВИПРАВЛЕНО.

### [P1-5] Відсутність гарячого застосування налаштувань
* **Файл:** `app/src/main/java/ua/konoha/tv/nativepreview/MainActivity.java`
* **Проблема:** Зміна параметрів «Живий світ», періоду ротації чи таймера спокою не набувала чинності до перезапуску активності.
* **Виправлення:** У методі `MainActivity.closeActivePanel()` додано миттєве оновлення:
  ```java
  backgroundView.setMotionEnabled(prefs.isMotionEnabled());
  chakraFocusView.setMotionEnabled(prefs.isMotionEnabled());
  setupTimers();
  resetIdleTimer();
  ```
* **Статус:** ВИПРАВЛЕНО.

### [P1-6] Витік таймера годинника у фоні (`onPause`)
* **Файл:** `app/src/main/java/ua/konoha/tv/nativepreview/MainActivity.java`
* **Проблема:** `clockRunnable` продовжував викликатися кожні 10 секунд при згортанні лаунчера у фон.
* **Виправлення:** У `onPause()` додано `mainHandler.removeCallbacks(clockRunnable)`.
* **Статус:** ВИПРАВЛЕНО.

---

## 2. Резюме виправлень зауважень P2 та P3

1. **[P2-3] Витік бітмапа при швидкій зміні шпалер:** У `VillageBackgroundView.setScene()` додано перевірку та `.recycle()` для попереднього `nextBitmap` при повторному виклику під час кросфейду.
2. **[P2-4] Втрата фокусу TV-пульта при перевпорядкуванні:** У `OrderPanel.java` реалізовано збереження `focusTargetPackage` та відновлення фокусу на кнопці переміщуваного елемента після рендеру списку.
3. **[P2-5] Авто-фокус текстового пошуку в архіві:** У `LibraryPanel.open()` початковий фокус переведено на `filterButton`, що запобігає небажаному відкриттю екранної клавіатури IME на ТВ.
4. **[P2-6] Алокації у раннері:** У `RunnerGameView.java` додано повторне використання `shurikenPath.reset()`.
5. **[P2-7] Очищення пам'яті:** Додано методи `release()` у `ScenesPanel` та `TechniqueRenderer`, які викликаються з `MainActivity.onDestroy()` та `MainActivity.onTrimMemory(TRIM_MEMORY_RUNNING_LOW)`.
6. **[P2-8] Таймер оверлею спокою:** Додано методи `pause()` та `resume()` в `AmbientOverlay`, які синхронізовано з життєвим циклом `MainActivity`.
7. **[P3-2] Оптимізація рамки фокусу:** У `ChakraFocusView.java` закешовано `COLOR_ORANGE` та перевикористовується `ringRect`.

---

## 3. Підсумковий стан
Усі 6 зауважень P1 та ключові зауваження P2/P3 повністю вирішено без компромісів щодо функціональності, автентичності та збереження 100% паритету з версією v26.
