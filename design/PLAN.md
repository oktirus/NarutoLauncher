# Konoha — Naruto launcher

## Задум

Домашній екран — вечір на даху в Коносі. Naruto справа дивиться на село; зліва
короткий заголовок і запуск застосунків. Нижня стрічка — власноруч ілюстрований
набір ninja tools, а не набір прямокутних офіційних логотипів. Фокус з'являється
як chakra seal, летить між застосунками й стискається при OK.

Палітра: ink slate #102b38, evening teal #34565d, sunset orange #ed943f,
parchment #f5e3b6, chakra cyan #81dcf1, leaf moss #7c9873.
Типографіка: виразний serif/brush display з кирилицею та Nunito Sans для керування.
Один великий пейзаж, охайний dock, асиметрія та окремі ілюстративні емблеми.

```
 Leaf / KONOHA       Коноха   Сцени   Застосунки       clock   search   scroll

 Твоя воля                                      Naruto на даху
 вогню.                                        Коноха / Hokage Rock
 [Мої застосунки] [Мить у Коносі]                dynamic layers

 [Вечір у Коносі] [Нічні ліхтарі] [Ранковий вітер]     [Увімкнути атмосферу]

 Мої техніки                                    [Весь архів]
 [scroll▶] [lantern] [film tag] [ribbon N] [shuriken] [jade frog] [archive]
         animated chakra focus ring + wind trail
 ↑↓←→   OK   Menu                                  локальний макет
```

## Послідовність

1. Інвентаризація всіх елементів у ELEMENT-INVENTORY.md; baseline Yoru.
2. Офіційні Naruto reference pages, chakra/Rasengan, Konoha/stone/cloth motifs,
   локальні reference downloads з URL і хешами. Без виконання коду з джерел.
3. Генерація трьох панорам і десяти іконок у спільному стилі через built-in ImageGen.
   Промпти, оригінали та runtime derivatives зберігаються окремо.
4. Авторський sound kit: wind-swipe / chakra-confirm / scroll-back / scene swell,
   атмосферний loop. Звуки синтезуються локально; не видаємо їх за аудіо із серіалу.
5. Review candidate: готові assets, компоненти focus/dynamic scene, screenshot і
   коротка специфікація інтерактивної поведінки. Незалежне read-only ревю Claude
   та Antigravity через Orca orchestration, один і той самий пакет, без чужих висновків.
6. Тріаж рекомендацій: суттєві — виправити; косметичні — оцінити й записати рішення.
7. Фінальний інтерактивний макет: D-pad/keyboard, бібліотека, пошук, обране,
   scene selection, rotation, ambience, sound/motion controls, persistent preferences.
8. Desktop 1920×1080, TV 1280×720 / 960×540, mobile preview, browser errors,
   focus reachability, sound loading, reduced motion, source/Yoru preservation.

Ревю запуститься після готовності пакета. Orca запускає обох workers, далі керівний
агент завершує поточний хід і автоматично продовжує після їхніх відповідей.
При недоступності конкретного агента не підміняємо його іншою моделлю.

## Межі

Окрема тека NarutoLauncher, без змін AnimeLauncher, без commit/push/publication.
Мета цього проходу — макет, не live інсталяція. На реальному Android TV launch,
фізичний пульт, audio output і WebView потребують окремого приймального тесту.
Використані мотиви Naruto — локальний фанатський прототип; оригінальні generated
assets не означають права на бренд. Реєстр джерел збережений локально.

## Перевірка задуму

Прибрано універсальний multi-anime carousel. Сцени, шрифт, material, іконки,
motion і sound тепер працюють на одну історію. Не додаємо фіктивних «місій»,
рівнів користувача чи episode progress, які не мають реального джерела даних.
