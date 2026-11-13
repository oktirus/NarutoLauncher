"""Encode selected built-in generated v16 resources and update only intended scenes."""
from pathlib import Path
from PIL import Image,ImageOps
import json,hashlib,shutil
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v16'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
rows=[]
for entry in json.loads((OUT/'generation-manifest.json').read_text(encoding='utf8')):
    src=ROOT/entry['source']
    if not src.exists():shutil.copy2(entry['source_path'],src)
    original=Image.open(src)
    if entry['id']=='logo':
        assert original.mode=='RGBA'
        im=original.resize((768,round(original.height*768/original.width)),Image.Resampling.LANCZOS)
        target=ROOT/'assets/runtime/v16/naruto-logo.webp';im.save(target,lossless=True,method=6)
    else:
        im=ImageOps.fit(original.convert('RGB'),(1920,1080),method=Image.Resampling.LANCZOS)
        target=ROOT/f'assets/runtime/v16/scene-{entry["id"]}.jpg';im.save(target,quality=95,subsampling=0)
    rows.append(dict(id=entry['id'],source=entry['source'],source_size=list(original.size),source_sha256=sha(src),file=target.relative_to(ROOT).as_posix(),size=list(im.size),sha256=sha(target)))
scenes=json.loads((OUT/'before/prototype/scenes.js').read_text(encoding='utf8').split('window.KonohaScenes=')[1].strip().rstrip(';'))
positions={'dusk':'76% center','night':'80% center','sakura':'82% center','gaara':'76% center','rocklee':'85% center','jiraiya':'88% center','sai':'63% center','minato':'78% center'}
for row in rows:
    if row['id']=='logo':continue
    scene=next(s for s in scenes if s['id']==row['id']);scene['image']='v16/scene-'+row['id']+'.jpg?v='+row['sha256'][:12]
    scene['mobilePosition']=positions[row['id']];scene['cardPosition']='76% 36%'
    if row['id']=='night':scene.update(name='Тренування Расенсюрикена',caption='Нова стихія',mood='Навчання стихії вітру',title='Сила<br><em>вітру.</em>',description='Тренування Расенсюрикена на лісовій галявині.',particles='leaf',palette=['#aeceda','#88bfcf'])
    if row['id']=='gaara':scene.update(title='П’ятий<br><em>Казекаге.</em>',caption='Відповідальність за Суну',mood='Кабінет Казекаге')
    if row['id']=='sakura':scene.update(caption='Лікування чакрою',mood='Практика медичного ніндзюцу')
    if row['id']=='rocklee':scene.update(caption='Кожен удар — ще один крок',mood='Тайдзюцу біля бамбукового гаю')
    if row['id']=='jiraiya':scene.update(mood='Нотатки і три миски рамену')
(ROOT/'prototype/scenes.js').write_text("'use strict';\nwindow.KonohaScenes="+json.dumps(scenes,ensure_ascii=False,indent=2)+';\n',encoding='utf8',newline='\r\n')
(OUT/'asset-manifest.json').write_text(json.dumps(rows,indent=2),encoding='utf8')
print('Integrated eight wallpaper versions and one generated logo; old assets preserved')
