"""Integrate Gaara replacement and five new original scene assets."""
from pathlib import Path
from PIL import Image,ImageOps
import json,hashlib,shutil
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v12';BEFORE=OUT/'before/prototype'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
sources=json.loads((OUT/'generation-manifest.json').read_text(encoding='utf8'));assert len(sources)==6
(ROOT/'assets/generated/v12').mkdir(parents=True,exist_ok=True);(ROOT/'assets/runtime/v12').mkdir(parents=True,exist_ok=True)
rows=[]
for s in sources:
 src=ROOT/s['source']
 if not src.exists():shutil.copy2(s['source_path'],src)
 original=Image.open(src);out=ROOT/'assets/runtime/v12'/('scene-'+s['id']+'.jpg')
 im=ImageOps.fit(original.convert('RGB'),(1920,1080),method=Image.Resampling.LANCZOS);im.save(out,quality=95,subsampling=0)
 rows.append({'id':s['id'],'kind':'wallpaper','source':s['source'],'source_sha256':sha(src),'source_size':list(original.size),'file':out.relative_to(ROOT).as_posix(),'sha256':sha(out),'size':list(im.size)})
scenes=json.loads((BEFORE/'scenes.js').read_text(encoding='utf8').split('window.KonohaScenes=')[1].strip().rstrip(';'))
for info in json.loads((OUT/'scene-content.json').read_text(encoding='utf8')):
 row=next(r for r in rows if r['id']==info['id']);info['image']='v12/scene-'+info['id']+'.jpg?v='+row['sha256'][:12]
 existing=next((s for s in scenes if s['id']==info['id']),None)
 if existing:existing.update(info)
 else:scenes.append(info)
assert len(scenes)==17 and len({s['id'] for s in scenes})==17
(ROOT/'prototype/scenes.js').write_text("'use strict';\nwindow.KonohaScenes="+json.dumps(scenes,ensure_ascii=False,indent=2)+';\n',encoding='utf8',newline='\r\n')
s=(BEFORE/'app.js').read_text(encoding='utf8');old='Три настрої села й дев’ять шпалер героїв.';assert old in s
s=s.replace(old,"Три настрої Наруто й '+scenes.filter(s=>s.hero).length+' шпалер героїв.")
(ROOT/'prototype/app.js').write_text(s,encoding='utf8',newline='')
s=(BEFORE/'index.html').read_text(encoding='utf8').replace('scenes.js?v=14','scenes.js?v=15').replace('app.js?v=14','app.js?v=15')
(ROOT/'prototype/index.html').write_text(s,encoding='utf8',newline='\r\n')
(OUT/'asset-manifest.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8')
print('Prepared six wallpaper originals and derivatives; 17 total scenes')
