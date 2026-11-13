# Стиль і промпти

## Затверджені приклади та їхні ролі

Переглядай зображення перед передаванням їх генератору:

| Файл | Роль |
|---|---|
| [images/neji-approved.jpg](images/neji-approved.jpg) | Згенерований і схвалений v9 результат: чистий силует, прості тіні, матовий зимовий фон |
| [images/choji-approved.jpg](images/choji-approved.jpg) | Згенерований і схвалений v9 результат: просте обличчя й костюм, тепла стримана сцена |
| [images/neji-anime-board.jpg](images/neji-anime-board.jpg) | Справжні кадри аніме: лінія й зовнішність Неджи; ранній період, не точний костюм Shippuden |
| [images/choji-anime-board.jpg](images/choji-anime-board.jpg) | Справжні кадри аніме: зовнішність Чоджі та cel-тіні; інших людей із дошки не копіювати |

Схвалені результати допомагають відтворити рівень деталізації, але не є
офіційними аніме-кадрами й не замінюють референсів нового персонажа.
[sample-sources.json](sample-sources.json) містить походження, хеші й URL кадрів.
Два точні промпти, що дали ці результати, збережено без переписування у
[approved-prompts.json](approved-prompts.json). Вони запускалися built-in image_gen.

## Чому цей підхід спрацював у v9

У v8 настрій і композиції були виразними, але матеріали мали забагато дрібних
відблисків, фактур, декорацій та складного освітлення. У v9 кожному виклику
передали дошку реальних кадрів відповідного героя; промпт задавав просту
телевізійну постановку, рівні кольори, один-два тони тіні та скромніший фон.
Кількість предметів зменшили: у Чоджі — один шампур із трьома данґо,
чашка й миска; у Неджи — один птах і невелика клітка. Це спостереження за
схваленими результатами, а не гарантія для кожної майбутньої генерації.

## Шаблон нової сцени

Заповни фігурні дужки реальними даними й прибери непотрібні рядки.
Англійську мову промпту збережено як у перевірених викликах; це не вимога моделі.

```text
Use case: illustration-story.
Asset: {wallpaper / illustration / background}, {aspect ratio}.
Primary request: create ONE new scene in the TV anime drawing style of {series, era}.
Input roles: image 1 = actual anime character-design/style reference;
image 2 = approved drawing-density reference only, not a character to copy.
If a board contains multiple people or ages, draw ONLY {character and exact version}.

Style: crisp dark contour lines of steady weight, flat local colors,
only one or two hard-edged cel shadow tones, spare highlights on hair,
modest hand-painted gouache background built from broad simple shapes.
Faithful anime character design, grounded TV-animation staging,
readable silhouette, restrained palette, simple lighting appropriate to this scene.

Subject: {distinctive face, eyes, hair, build, exact clothing and accessories}.
Action: {one clear physical action, natural pose, understandable hand positions}.
Backdrop: {location and a few meaningful objects, not a list of decorations}.
Framing: {camera angle, shot size, intended crop, UI space if needed}.
Mood and palette: {specific mood, limited color family, time of day}.

Avoid: 3D rendering, glossy skin, photorealistic materials, painterly faces,
ornate microdetail, excessive fabric/stone textures, volumetric light shafts,
bloom, lens flare, bokeh, hyper-sharpened textures, unintended magical ornaments.
Do not copy incidental scratches, injuries or unrelated characters from the references.
No extra limbs, merged fingers, accidental text, watermark, collage or split panels.
```

У природному небі й мальованому фоні допускаються м'які переходи кольору.
«Flat colors» не означає, що треба зробити все зображення векторним або
прибрати атмосферу. Найважливіше — простий дизайн персонажа та керовані тіні.

## Preset для шпалер Конохи

Додай лише для цього типу UI:

```text
16:9 full-frame launcher wallpaper. One complete scene, not a promotional poster.
Frame the hero in the right third, face around x=77%, y=30%.
Keep the left 45% lower-contrast for the launcher title; no typography in the image.
Keep the face clear of the top status bar and leave room for lower app icons.
```

Оглянь у реальному лаунчері: CSS cover/parallax може змістити й обрізати
краї. Положення 77% — робоча початкова точка, не правило для кожного героя.
Для серії не повторюй один і той самий сад: відпочинок на даху, тренування,
сніг і ресторан можуть мати одну мову малюнка при різних композиціях.

## Короткі виправлення

Глянцевість або забагато деталей:

```text
Keep the character identity, pose and composition. Redraw the rendering as a restrained
TV anime episode still: flat local colors, one hard-edged shadow tone on the face,
simple clothing shadow blocks, sparse hair highlights. Simplify background texture
into broad matte painted shapes. Remove bloom, glossy reflections and decorative
microdetail. Preserve the scene's actual requested effects and meaningful objects.
```

Неправильне обличчя:

```text
Change only the facial design to match the actual anime reference: {specific differences}.
Keep the pose, framing, clothing, palette, simple cel shading and background unchanged.
Do not introduce makeup, shiny skin, extra eye highlights, scratches or new accessories.
```

Помилка руки/предмета:

```text
Fix only {specific hand/object and its location}: {desired natural grasp or anatomy}.
Keep the character identity, composition, lighting and simple TV anime cel shading.
Do not add props or redraw the rest of the scene.
```

Не накопичуй багато невдалих правок у довгий промпт. Якщо базова постановка
розпадається, спростити дію та згенерувати заново за правильним референсом
часто доцільніше, ніж намагатися врятувати кожну деталь.
