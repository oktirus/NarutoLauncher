---
name: canvas-technique-porting
description: Port HTML5 Canvas procedural animations and art effects 1:1 to Android hardware-accelerated Canvas with zero-allocation, exact mathematical parity, and physical TV verification. Use whenever porting or refining chakra techniques, aura animations, particles, procedural noise, or effects from JavaScript/web prototypes into Android native views (such as NarutoLauncher).
---

# Canvas Technique Porting (HTML5 Canvas to Android Native 1:1 Parity)

Цей скіл визначає регламент, математичні патерни та процес верифікації для перенесення процедурних технік, аури чакри та Canvas 2D анімацій із веб-прототипу (`prototype/technique-art.js` та `prototype/techniques.js`) на нативний Android Canvas (`TechniqueRenderer.java`).

---

## 1. Коли активувати скіл
- Запити на кшталт: "перенеси техніку один в один", "скопіюй расенган / шарінган / кураму з еталону", "зроби чідорі як у прототипі", "виправ анімацію техніки".
- Робота над будь-яким процедурним Canvas 2D ефектом у `NarutoLauncher/native-parity`.

---

## 2. Фаза 1: Аналіз вихідного коду еталону (Code Dissection)

ОБОВ'ЯЗКОВО перевірити два файли еталону:
1. `prototype/technique-art.js` (пріоритетний художній рушій):
   - Перевірити, чи є ID техніки у списку `api.proceduralEffects` (наприклад, `chidori`). Якщо так — техніка малюється процедурною функцією (`electricity()`).
   - Перевірити, чи є ID техніки у множині `completePaintings` (`rasengan`, `sharingan`, `kurama`, `fireball`, `amaterasu`).
     - Якщо ТАК: коли текстура завантажена, малюється **виключно** текстура з точними параметрами розміру, обертання та спекулярних ефектів! Не нашаровувати векторні заглушки поверх художньої картини!
   - Перевірити обробку енергетичної хвилі (`energy`/`burstLife`).
2. `prototype/techniques.js` (векторний fallback):
   - Використовується **виключно як запасний варіант (fallback)**, коли растровий ассет ще не завантажений (`if (!loaded[id]) baseDraw(...)`).
   - ⚠️ **КРИТИЧНЕ ПРАВИЛО РОЗДІЛЕННЯ (HARD SEPARATION GATE):**
     **Категорично заборонено** малювати векторні заглушки з `techniques.js` (наприклад, 4 векторні леза чи спіраль Расенсюрикена) поверх завантаженої растрової платівки з `technique-art.js`!
     У веб-еталоні `baseDraw` **НІКОЛИ** не викликається одночасно з растровим диском. Якщо накласти векторні леза поверх растрових, вони обертаються з різними швидкостями ($1.6t$ у fallback vs $2.05t$ у растрі), що створює брудне двоїння, стробінг та руйнує анімацію.
     Векторні леза та спіралі належать суворо блоку `else` (`draw*Fallback`).

---

## 3. Фаза 2: Математичні формули та паритет

### 3.1. Квантизація часу та шум (Noise)
- JS: `const tick = Math.floor(t * 24);`
- Java: `long tick = (long) Math.floor(t * 24.0);`
- Формула процедурного псевдовипадкового шуму:
  ```java
  private double noise(double n, long tick) {
      double v = Math.sin(n * 127.1 + (double) tick * 311.7) * 43758.5453;
      return v - Math.floor(v);
  }
  ```

### 3.2. Трипрохідний неоновий Glow (Заміна `shadowBlur`)
HTML5 Canvas використовує `shadowBlur` та подвійний `stroke`. На апаратному Android Canvas це емулюється без `setShadowLayer` (щоб не ламати HW acceleration):
```java
private void drawNeonStroke(Canvas canvas, Path path, int glowColor, int bodyColor, int coreColor, float width, float r, float alpha) {
    float dScale = density / 1.5f;
    // 1. Зовнішній розмитий ореол
    strokePaint.setColor(glowColor);
    strokePaint.setStrokeWidth((width * 3.0f + r * 0.032f) * dScale);
    strokePaint.setAlpha((int) (alpha * 0.38f * 255));
    canvas.drawPath(path, strokePaint);

    // 2. Внутрішній насичений шар
    strokePaint.setColor(bodyColor);
    strokePaint.setStrokeWidth(width * 2.2f * dScale);
    strokePaint.setAlpha((int) (alpha * 0.85f * 255));
    canvas.drawPath(path, strokePaint);

    // 3. Розпечене ядро
    strokePaint.setColor(coreColor);
    strokePaint.setStrokeWidth(Math.max(0.6f * dScale, width * dScale));
    strokePaint.setAlpha((int) (alpha * 1.15f * 255));
    canvas.drawPath(path, strokePaint);
}
```

### 3.3. Zero-Allocation у 60 FPS
- **ЖОДНОГО `new Path()`, `new RectF()`, `new float[]` всередині `draw*` методів!**
- Усі буфери виділяються заздалегідь у полях класу:
  - `tempOval = new RectF()`
  - `dstRect = new RectF()`
  - `pathBuffer = new Path()`
  - `pointsX = new float[N]`
  - `pointsY = new float[N]`

### 3.4. Пастка точності Float32 та час анімації (КРИТИЧНО!)
- **Ніколи не використовувати `System.currentTimeMillis()` для розрахунку кутів обертання:**
  Epoch-час ($1.76 \cdot 10^{12}$ мс) дає кути в десятки мільярдів градусів. При величині $8 \cdot 10^{10}$ тип `float32` має крок дискретизації **8192°**. Міжкадрова дельта у $0.75^\circ$ округлюється в **`0.000000`**, і зображення на GPU повністю застигає на місці!
- **Обов'язкове правило:**
  1. Час `now` відлічувати від старту View: `long now = android.os.SystemClock.uptimeMillis() - animationStartTime;`
  2. Будь-який кут загортати за модулем у `double` ДО приведення до `float`:
     ```java
     float rot = (float) ((Math.toDegrees(t * speed)) % 360.0);
     ```

---

## 4. Фаза 3: Реалізація технік

### Расенґан (`rasengan`)
- Швидкість обертання: `rot = (float) Math.toDegrees(t * 0.82)`
- Розмір: `size = r * 2.46f * (1.0f + (isPreview ? 0f : (float) (Math.sin(t * 1.5) * 0.016 + burstLife * 0.035)))`
- Прозорість: `214` (0.84f)
- Сплеск енергії (`burstLife > 0`): коло радіусом `r * (1.04f + (1.0f - burstLife) * 0.14f)`, колір `#85e5ff`, товщина `dp(1.2f)`, альфа `(int)(burstLife * 0.5f * 255)`.

### Шарінґан (`sharingan`)
- Швидкість обертання: `rot = (float) Math.toDegrees(t * 0.38)`
- Розмір: `size = r * 2.46f * (1.0f + (isPreview ? 0f : (float) (Math.sin(t * 1.5) * 0.016 + burstLife * 0.035)))`
- Прозорість: `214` (0.84f)
- Спекулярний відблиск (Glint): біло-кремовий овал `#fff5e4`, альфа `217` (0.85f), центр `(-r * 0.68f, -r * 0.35f)`, поворот `0.6` рад (`34.38°`), півосі `rx = r * 0.065f`, `ry = r * 0.12f`.
- Сплеск енергії (`burstLife > 0`): коло радіусом `r * (1.04f + (1.0f - burstLife) * 0.14f)`, колір `#ff6265`, товщина `dp(1.2f)`, альфа `(int)(burstLife * 0.5f * 255)`.

---

## 5. Фаза 4: Верифікація та Definition of Done

1. **Збірка:**
   ```powershell
   pwsh -NoProfile -File native-parity/Build.ps1
   ```
   Усі фактичні тести кандидата мають пройти, 0 Error/Fatal у `lintDebug`; не копіюй старий лічильник тестів.
2. **Реальний Android TV — лише коли конкретні target та встановлення прямо дозволені користувачем.** Перевір serial/model/current HOME, збережи доступний backup, звір підпис і встановлюй через `install -r` без clear/uninstall. Не використовуй старий IP або назву APK з історії. Без дозволу заверши локальні перевірки й явно назви live gap.
3. **Запис фактичного рендеру та витяг кадрів:**
   - Короткий `screenrecord` із зафіксованими параметрами; не називай його фізичним 60 FPS відео без перевірки cadence.
   - Витяг кадрів 0.8с, 1.4с (Idle різниця 600мс) та 2.6с (перехід D-pad).
4. **Математичний розрахунок ROI саме цільової картки:**
   - Виміряй актуальні bounds; розмір 42 750 px та поріг $\Delta > 10$ були історичними параметрами, не універсальним стандартом.
   - Обчислення `Idle Mean Delta`, кількості змінених пікселів у % від ROI, наявності цільових кольорів.
5. **Оновлення документації:**
   - Створити запис у `History/YYYY-MM-DD_*.md`.
   - Проєктні notes оновлювати за scope задачі. Центральну пам'ять Codex — лише за прямим запитом користувача і через дозволену extension note. Не переписуй історичні звіти так, ніби вони приймають новий APK.
