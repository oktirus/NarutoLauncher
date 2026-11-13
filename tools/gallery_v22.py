from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v22'
old=json.loads((OUT/'before/prototype/scenes.js').read_text(encoding='utf8').split('window.KonohaScenes=')[1].strip().rstrip(';'))
html=['<!doctype html><html lang="uk"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>v22 — Расенсюрикен і Сакура</title><style>body{margin:0;background:#102a35;color:#efe3bd;font:16px system-ui;padding:24px}main{max-width:1440px;margin:auto}a{color:#8bd3e8}h1{font-size:28px}h2{margin-top:36px}.pair{display:grid;grid-template-columns:1fr 1fr;gap:16px}figure{margin:0}img{width:100%;display:block;object-fit:contain}figcaption{padding:8px 0}.shots{display:grid;grid-template-columns:2fr 2fr 1fr;gap:16px}.shots img{max-height:560px}@media(max-width:700px){.pair,.shots{grid-template-columns:1fr}}</style><main><h1>v22 — нові тренування Расенсюрикена та Сакура</h1><p><a href="../../../prototype/">Лаунчер</a> · <a href="../REPORT.md">Звіт</a> · <a href="../generation-manifest.json">Початкові промпти</a> · <a href="../correction-prompts.json">Фінальні промпти</a></p>']
for id,title in [('night','Спільне формування Расенсюрикена'),('sakura','Медична практика Сакури')]:
    image=next(s['image'].split('?')[0] for s in old if s['id']==id)
    html.append(f'<h2>{title}</h2><div class="pair"><figure><img src="../../../assets/runtime/{image}"><figcaption>До v22</figcaption></figure><figure><img src="../../../assets/runtime/v22/scene-{id}.jpg"><figcaption>v22 · <a href="../../../assets/generated/v22/scene-{id}.png">Збережений оригінал PNG</a></figcaption></figure></div><h3>У лаунчері</h3><div class="shots">')
    for width in [1920,960,390]:html.append(f'<figure><img src="{id}-{width}.png"><figcaption>{width}px</figcaption></figure>')
    html.append('</div>')
html.append('<p>Широкі сцени для TV; у портретному кадруванні другорядні персонажі та предмети частково поза екраном.</p></main></html>')
(OUT/'verification/visual-review.html').write_text(''.join(html),encoding='utf8')
print('v22 comparison gallery saved')
