"""Archive built-in originals and integrate only three selected scenes."""
from pathlib import Path
from PIL import Image,ImageOps
import json,hashlib,shutil
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v17'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
rows=[]
for entry in json.loads((OUT/'generation-manifest.json').read_text(encoding='utf8')):
 src=ROOT/entry['source']
 if not src.exists():shutil.copy2(entry['source_path'],src)
 if not entry.get('selected'):continue
 original=Image.open(src);im=ImageOps.fit(original.convert('RGB'),(1920,1080),method=Image.Resampling.LANCZOS)
 target=ROOT/f'assets/runtime/v17/scene-{entry["id"]}.jpg';im.save(target,quality=95,subsampling=0)
 rows.append(dict(id=entry['id'],source=entry['source'],source_size=list(original.size),source_sha256=sha(src),file=target.relative_to(ROOT).as_posix(),size=list(im.size),sha256=sha(target)))
scenes=json.loads((OUT/'before/prototype/scenes.js').read_text(encoding='utf8').split('window.KonohaScenes=')[1].strip().rstrip(';'))
positions={'night':'73% center','rocklee':'81% center','sai':'100% center'}
for row in rows:
 scene=next(s for s in scenes if s['id']==row['id']);scene['image']='v17/scene-'+row['id']+'.jpg?v='+row['sha256'][:12];scene['mobilePosition']=positions[row['id']];scene['cardPosition']='77% 32%'
 if row['id']=='night':scene.update(caption='Разом опанувати вітер',mood='Оригінал і два тіньові клони',description='Формування Расенсюрикена: чакра, форма та стихія вітру.')
 if row['id']=='rocklee':scene.update(caption='Ще один стрибок',mood='Ранкове тренування зі скакалкою',description='Ритм, витривалість і щоденна праця.')
 if row['id']=='sai':scene.update(caption='Чорнильний тигр оживає',mood='Малюнок, що робить перший крок',description='Практика надзвіриних малюнків на даху Конохи.')
assert {r['id'] for r in rows}=={'night','rocklee','sai'}
(ROOT/'prototype/scenes.js').write_text("'use strict';\nwindow.KonohaScenes="+json.dumps(scenes,ensure_ascii=False,indent=2)+';\n',encoding='utf8',newline='\r\n')
(OUT/'asset-manifest.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8');print('Three v17 scenes integrated; all old assets preserved')
