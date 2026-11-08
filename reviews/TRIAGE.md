# Рішення після двох незалежних ревю

2026-11-07. Claude (`ctx_ef5788a2c8e3`) і Antigravity (`ctx_66a41cf52057`)
завершили окремі ревю одного candidate. Оригінальні звіти збережено без змін.
Severity нижче відображає оцінку авторів; дублікати зведено до одного виправлення.
Після виправлень виконано локальну регресійну перевірку, не повторне схвалення агентами.

| Findings | Рішення | Реалізація / доказ |
|---|---|---|
| Claude P1-1: Select захоплює ↑↓, змінює prefs; пропускається volume | Виправлено | Рядки налаштувань проходяться ↑↓ у DOM-порядку; select/range змінюються ←→. Перевірка повного проходу ↓ та незмінності prefs |
| Claude P1-2: saved audio=true вимикається кнопкою активації | Виправлено | Активація без інверсії збереженого true; глобальні handlers не розблоковують audio-button перед action; aria-pressed та однозначний label |
| Claude P2-1 / Antigravity P1-02: hover ≠ activeElement | Виправлено | Pointer hover синхронізує DOM focus; OK запускає картку, на якій chakra-печатка |
| Регресія при panel reflow під нерухомим cursor | Виправлено | Hover змінює focus лише після фактичної зміни координат; бібліотека утримує focus на пошуку |
| Claude P3-1 / Antigravity P1-01: stale trail coordinates | Виправлено | hideFocus очищує center/trails/bursts; панелі й resize скидають координати |
| Claude P2-2: confirm/back + focus одночасно | Виправлено | focus sound лише на навігаційну дію; програмний focus тихий. Інструментовано exact порядок cue-викликів |
| Claude P2-3 / Antigravity P2-01: текст на обличчі Naruto | Виправлено | Scene caption перенесено в центральну вільну зону ліворуч від персонажа; перевірено скриншотами та геометрією |
| Claude P2-4: hero-kicker перекриває brand | Виправлено | Перераховано compact hero; на 960×540 декоративний kicker прихований; copy.top >= header.bottom |
| Claude P2-5 / Antigravity P2-04: мікроскопічна TV-типографіка | Виправлено | Функціональні назви/CTA/scenes ≥12 CSS px; footer guidance і дата 12px; app-meta 11px або прихований у compact mode. Фізична TV-відстань ще не тестувалась |
| Claude P2-6 / Antigravity P2-03: parallax лише від миші | Виправлено | Автономний часовий drift плюс внесок фокуса та курсора; рух є без pointer events, reduced motion його зупиняє |
| Claude P2-7: ambience fade до тиші кожні 24 s | Виправлено | Overlap/cosine crossfade при синтезі; seam step 171/32767, RMS країв близький до загального. V1 WAV збережено в research/audio-variants |
| Antigravity P2-02: unpin у filtered library втрачає focus | Виправлено | Сусідній pin з тим самим індексом; якщо список спорожнів — кнопка фільтра |
| Claude P3-2/3: initial focus / library focus | Прийнято | Перший app у dock на старті; library/search відкривають поле пошуку |
| Claude P3-4: сьомий pin не дає feedback | Прийнято | Toast пояснює кількість обраного та ліміт видимих шести |
| Claude P3-5: pause не очищує таймери | Прийнято | clearInterval(rotationTimer), clearTimeout(idleTimer); resume створює їх знову |
| Claude P3-6: панель із панелі губить home focus | Прийнято | Зберігається початковий home-origin до заміни панелі |
| Claude P3-7: crossfade без preload | Прийнято | Image.decode перед перемиканням шару, cached preloads та version guard для швидких повторних виборів |
| Claude P3-8: boolean preferences | Прийнято | Строга валідація audio/ambience/motion; pins — унікальні рядки |

Художні покращення: додано світіння ліхтарів, нічне chakra-світло Lampa,
світлячки в нічній сцені та окремі палітри листя для ранку/вечора/ночі.
Хмари рухаються alternate easing без циклічного стрибка назад.

Не прийнято як обов'язкове:

- Перегенерація Spotify: обидва ревю підтверджують загальну цілісність набору;
  жаба й музичні дуги збережені, функціональна назва видима. Альтернативний силует
  можна зробити окремим варіантом, це не блокує прототип.
- Деформація стрічок прямо в плоскому raster: без окремої маски/шару ризикує
  спотворити голову й дах. Жива панорама досягнута окремими шарами та motion.
- Збільшення parallax до 1.5–2 px на кожну одиницю: надто великий стрибок для
  спокійної сцени. Обрано плавний drift із достатнім запасом країв і внеском фокуса.

Підсумкові докази: `verification/final-checks-*.json`,
`verification/review-regressions-*.json`, `verification/audio-loop.json`,
`verification/offline-checks.json` і фінальні скриншоти.
