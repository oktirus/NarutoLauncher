from pathlib import Path
from PIL import Image,ImageOps
import json,hashlib,shutil
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v18'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def registry(p,name):return json.loads(p.read_text(encoding='utf8').split('window.'+name+'=')[1].strip().rstrip(';'))
rows=[]
for entry in json.loads((OUT/'generation-manifest.json').read_text(encoding='utf8')):
 src=ROOT/entry['source']
 if not src.exists():shutil.copy2(entry['source_path'],src)
 if not entry.get('selected'):continue
 original=Image.open(src);im=ImageOps.fit(original.convert('RGB'),(1920,1080),method=Image.Resampling.LANCZOS);target=ROOT/f'assets/runtime/v18/scene-{entry["id"]}.jpg';im.save(target,quality=95,subsampling=0)
 rows.append(dict(id=entry['id'],source=entry['source'],source_size=list(original.size),source_sha256=sha(src),file=target.relative_to(ROOT).as_posix(),size=list(im.size),sha256=sha(target)))
assert {r['id'] for r in rows}=={'night','minato','ramen'}
scenes=registry(OUT/'before/prototype/scenes.js','KonohaScenes')
ramen=dict(id='ramen',name='Рамен в Ічіраку',caption='Смак дому',mood='Рамен після довгого дня',color='#eda653',hero=False,title='Смак<br><em>дому.</em>',description='Наруто їсть рамен біля стійки Ічіраку.',particles='dust',palette=['#f0c88e','#e6b775'])
scenes.insert(3,ramen)
for row in rows:
 s=next(s for s in scenes if s['id']==row['id']);s['image']='v18/scene-'+row['id']+'.jpg?v='+row['sha256'][:12];s['mobilePosition']={'night':'85% center','minato':'70% center','ramen':'83% center'}[row['id']];s['cardPosition']={'night':'78% 30%','minato':'77% 30%','ramen':'80% 35%'}[row['id']]
 if row['id']=='night':s.update(mood='Техніка сформована: Наруто і два клони',caption='Крок до власної техніки',description='Після спільного формування Наруто утримує Расенсюрикен над долонею.')
quotes=registry(OUT/'before/prototype/quotes.js','KonohaQuotes');quotes['ramen']=dict(quotes['dusk'])
for name,data in [('scenes',scenes),('quotes',quotes)]:
 (ROOT/f'prototype/{name}.js').write_text("'use strict';\nwindow."+('KonohaScenes' if name=='scenes' else 'KonohaQuotes')+'='+json.dumps(data,ensure_ascii=False,indent=2)+';\n',encoding='utf8',newline='\r\n')
(OUT/'asset-manifest.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8');print('18 scenes, three v18 assets, added ramen quote')
