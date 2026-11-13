# Точне перенесення існуючого launcher

Читай, якщо користувач просить ідентичність із веб/Chromium або іншим runtime. Для нового дизайну використовуй погоджений vertical slice як візуальний авторитет. «Той самий список функцій» і «той самий вигляд» — різні критерії.

## Freeze і mapping

Зафіксуй версію еталону, asset hashes, viewport, density/DPR, fonts, focus identity, scene, motion, time/seed і settings. Не покращуй дизайн під виглядом порту. Старий native preview може дати physics/cache/audio logic, але не замінює візуальний еталон.

Для кожного елемента: `source call path → asset → render mode → native owner → acceptance capture`. Читай реально підключені JS/CSS revision overrides і draw calls; наявність старої функції у файлі не означає її виклик у фінальній версії.

Відокрем:

- raster complete artwork, якому потрібні точні transform/deformation/glints;
- procedural effect із власною геометрією/шумом;
- fallback, що викликається тільки до завантаження/помилки asset;
- спільні accents, які еталон справді додає до artwork.

Не малюй fallback поверх завантаженого artwork, якщо еталон обирає `if/else`. Подвійні леза/кола/filled core спотворюють ефект. Водночас не прибирай підтверджені core/accent layers тільки через існування растрового asset — перевір конкретну branch.

## Геометрія й час

Перенеси anchor, radius, crop, alpha, blend, z-order, текстові метрики, speed/easing, rotation direction, noise floor і flight path. JS radians не дорівнюють Android degrees. Для `fract(x)` використовуй `x-floor(x)`, а не remainder для від'ємних значень. Seed/time freeze має бути спільним для порівняння.

Використовуй relative monotonic double time та modulo до float, перевір frames після тривалого uptime. Для sprite зберігай pose-specific baseline, flip і розмір; напрямок idle і jump artwork може бути протилежним. Shared Paint alpha після shadow не має робити героя напівпрозорим.

Web shadowBlur не завжди переноситься одним Canvas call. Підібрати native glow/mesh/shader можна, але результат оцінюй поряд із еталоном; не обіцяй pixel identity лише тому, що формула схожа. Не пиши другу JS-імітацію native і не називай screenshot цієї імітації знімком Android.

## Матриця кадрів

Знімай однакові стани: Home cold/focused, кожна сцена і ефект, library, settings/dropdown, reorder, modal transitions, ambient, game за наявності, motion-off і повернення після launched app. Для ефекту: кілька фіксованих фаз + коротке відео реального native renderer. Спочатку переконайся, що ефект узагалі присутній і target не null.

Статичні прев'ю порівнюй на одному time/seed. Для живого руху аналізуй **ROI цільової картки/аури**: рух листя/фону/годинника на всьому екрані не доводить, що обертається потрібний ефект. Перевір форму/характерні кольори й target coords, потім локальний рух; врахуй фон усередині ROI та H.264 noise. Відеорозмір/бітрейт не є доказом анімації, а мала дельта сама не доводить відсутність повільного/симетричного ефекту.

Метрики: окремі crops, image diff/overlay, geometry/text bounds, changed pixels із документованим threshold. Різні rasterizers/fonts/HW вимагають обґрунтованих tolerances і ручного огляду. Не встановлюй універсальні «<0.05%» або «60FPS» без вимірювань. Clock/random regions маскуй явно, не ховай проблемні ROI. Для static preview перевір відсутність scheduled loops, а не лише однаковість двох скриншотів.

Якщо screenshot від Robolectric/Skia — так і познач. Він перевіряє layout/стан, але не сертифікує апаратне GPU rendering, акустичний crossfade, фізичний пульт чи OEM HOME. Окремий перелік fidelity gaps кращий за непідтверджені «100% parity».
