# Konoha — повний перелік елементів

Окремий Naruto-only launcher. `AnimeLauncher/` (Yoru) збережено; baseline SHA-256
усіх його файлів: `verification/yoru-before.json`. Перший deliverable — інтерактивний
локальний макет. Встановлення на пристрій і заміна HOME у цю задачу не входять.

| ID | Елемент попереднього лаунчера / новий елемент | Naruto-стилізація й ресурс | Поведінка / критерій |
|---|---|---|---|
| BG01 | Головний фон | Згенерована панорама Конохи, Hokage Rock, Naruto на даху, захід сонця | Власний raster, не офіційний poster із накладеним UI |
| BG02 | Другий фон | Згенерована нічна Коноха, ліхтарі, місячний синій | Плавний crossfade між сценами |
| BG03 | Третій фон | Згенерована ранкова Коноха, туман та золоті хмари | Окремий настрій в одному аніме |
| BG04 | Динаміка пейзажу | Багатошаровий parallax, хмари, мерехтіння ліхтарів, листя на Canvas | Рухається не лише backdrop scale; pause/reduced motion |
| BG05 | Частинки | SVG-листя Конохи, легкі chakra sparks і wind streaks | Фізика швидкості/вітру, без випадкового flashing |
| BG06 | Підкладка читабельності | Slate/ink gradient і прозорий темний папір | Контраст тексту на всіх сценах |
| ID01 | Назва / знак | KONOHA / 木ノ葉, авторський leaf/headband crest | Локальний SVG, кнопка «додому» |
| ID02 | Avatar | Shinobi forehead protector / leaf seal | Не фальшивий account status |
| TX01 | Hero heading | «Твоя воля вогню.»; brush-like Cyrillic typography | Шрифт з кирилицею, без растрованого основного тексту |
| TX02 | Hero description | Короткий український текст атмосфери вечора в Коносі | Текст не перекриває обличчя Naruto |
| TX03 | Scene label / Japanese caption | Назва місця, період доби, 木ノ葉隠れの里 | Семантичний підпис, без вигаданих live даних |
| TX04 | Clock/date | Системний час і дата, стиль ink ledger | Оновлення без перезавантаження |
| NV01 | Home tab | Коноха, orange brush underline | Keyboard/D-pad focus та selected окремі |
| NV02 | Worlds tab | «Сцени Конохи» замість різних аніме | Три локації/настрої одного світу |
| NV03 | Apps tab | «Застосунки» / архів технік | Повна бібліотека |
| NV04 | Search button | Kunai/lens SVG mark | Пошук працює за видимою назвою |
| NV05 | Settings button | Scroll/seal SVG mark | Панель налаштувань |
| NV06 | Hero primary CTA | Світлий пергаментний сувій | Відкрити бібліотеку |
| NV07 | Hero ambient CTA | Місячна leaf-емблема | Режим спокою |
| FC01 | Фокус застосунку | Рухомий chakra/summoning seal; Rasengan blue whirl, orange corner strokes | Геометричний focus ring рухається між картками |
| FC02 | Focus trail | Chakra dash / wind trail між попереднім і новим фокусом | Анімоване перелистування пультом |
| FC03 | Confirm effect | Короткий chakra burst / ink stamp | Не блокує повторний ввід, без стробу |
| FC04 | Hover | Та сама семантика фокуса для миші | Без втрати keyboard selection |
| AP01 | SmartTube / YouTube | Згенерований red-orange shinobi scroll із play seal | Повністю нова ілюстрація, назва поруч лишається читабельною |
| AP02 | Lampa | Згенерований chakra paper lantern | Самостійна впізнавана іконка |
| AP03 | UAKino | Згенерований film-scroll / blue-yellow ninja tag | Не просто старий логотип на фоні |
| AP04 | Netflix | Згенерована червона ninja ribbon N | Візуальний зв'язок із призначенням |
| AP05 | Kodi | Згенерований blue four-point shuriken K | Контрастний силует |
| AP06 | Spotify | Згенерований jade frog / три sound-wave arcs | Музичний зміст, єдиний матеріал і стиль |
| AP07 | Crunchyroll | Згенероване orange fox-eye chakra ring | Anime application tile |
| AP08 | VLC | Згенерований orange-white shinobi cone hat | Media-player tile |
| AP09 | Plex | Згенерована пара golden kunai-chevron | Додатковий media tile |
| AP10 | All apps / система | Згенерований archive scroll із nine sealing marks | Відкрити повний список |
| AP11 | Unknown app fallback | Авторський scroll monogram з назвою | Не зникає, якщо пакета немає в тематичному наборі |
| AP12 | Labels / pinned indicator | Пергаментні підписи, leaf-star pin | Не раструвати функціональні назви |
| SC01 | Scene thumbnails | Похідні мініатюри BG01–03 | Вибір, selected seal, aria-pressed |
| SC02 | Rotation control | Маленький круговий wind seal | Manual / 1 / 3 / 10 хв |
| LB01 | Library modal | Ninja archive, темний папір, ink rules | Focus trap, повернення фокуса |
| LB02 | Search field | Поле сувою з brush border | Ввід, clear, empty result |
| LB03 | App grid | Перемальовані AP01–10 | Responsive columns, назви не обрізаються |
| LB04 | Favorite filter / buttons | Leaf-shaped sealing stamp | Pin/unpin, перші шість у dock |
| LB05 | Empty / error state | Маленький ink leaf, конкретна підказка | Без порожнього екрану |
| ST01 | Settings panel | Сувій з правого краю, parchment/ink contrast | Закриття Back/Escape |
| ST02 | Sound toggle / volume | Chakra bead toggle, 0–100% | Реально вимикає SFX і ambience |
| ST03 | Motion toggle | Leaf toggle | Canvas та CSS animations зупиняються |
| ST04 | Scene / idle selectors | Компактні paper choices | Значення зберігаються |
| ST05 | System settings / Wi-Fi | Themed SVG shortcuts | Preview чесно пояснює native-only дію |
| ST06 | Refresh / about / credits | Scroll / leaf glyphs | Реєстр джерел і походження звуків |
| AM01 | Ambient screen | BG01–03 без dock, clock, breathing chakra halo | Вихід OK / Back, не Android DreamService |
| AM02 | Ambient audio | Авторський village air / wind / low tonal chime | Старт після жесту, окремий контроль |
| AU01 | Focus / перегортання | Авторський короткий wind/cloth chakra swipe | WAV + WebAudio/runtime, тихіший за confirm |
| AU02 | Confirm / OK | Авторський chakra compression + ink impact | Без вирізаних діалогів / музики серіалу |
| AU03 | Back / cancel | Авторський soft scroll release | Впізнаваний інший тембр |
| AU04 | Scene change | Авторський layered wind + bell/chakra swell | Короткий, не перериває навігацію |
| AU05 | Sound activation | Явна кнопка «Увімкнути атмосферу» | Browser autoplay не обходиться |
| FB01 | Toast / launch feedback | Ink stamp на папері | Preview не видає себе за реальний Android launch |
| FT01 | Footer / remote hints | Kunai arrows, seal OK, scroll Menu | Видимий D-pad hint |
| FT02 | Scene provenance / runtime status | Короткий підпис локального макета | Не вигадувати Wi-Fi/погоду/дані пристрою |
| PK01 | App icon / banner (майбутній APK) | Leaf crest / BG01 + wordmark | У макеті підготовлено assets, APK не обіцяється |

Raster-генерація потрібна для BG01–03 та AP01–10. Ефекти фокуса, control icons,
пергаментні рамки та функціональна типографіка створюються кодом/SVG: це дозволяє
масштабування, анімацію, доступність і читабельність. Це свідоме розділення ресурсів.
