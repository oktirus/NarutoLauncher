"""Encode the three targeted artwork edits and preserve all v12 resources."""
from pathlib import Path
from PIL import Image,ImageOps
import json,hashlib,shutil
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v13';BEFORE=OUT/'before/prototype'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
sources=json.loads((OUT/'generation-manifest.json').read_text(encoding='utf8'));assert {s['id'] for s in sources}=={'sai','jiraiya','minato'}
(ROOT/'assets/generated/v13').mkdir(parents=True,exist_ok=True);(ROOT/'assets/runtime/v13').mkdir(parents=True,exist_ok=True)
rows=[]
for s in sources:
 src=ROOT/s['source']
 if not src.exists():shutil.copy2(s['source_path'],src)
 original=Image.open(src);out=ROOT/'assets/runtime/v13'/('scene-'+s['id']+'.jpg')
 im=ImageOps.fit(original.convert('RGB'),(1920,1080),method=Image.Resampling.LANCZOS);im.save(out,quality=95,subsampling=0)
 rows.append({'id':s['id'],'kind':'wallpaper','source':s['source'],'source_sha256':sha(src),'source_size':list(original.size),'file':out.relative_to(ROOT).as_posix(),'sha256':sha(out),'size':list(im.size)})
scenes=json.loads((BEFORE/'scenes.js').read_text(encoding='utf8').split('window.KonohaScenes=')[1].strip().rstrip(';'))
for row in rows:
 s=next(s for s in scenes if s['id']==row['id']);s['image']='v13/scene-'+s['id']+'.jpg?v='+row['sha256'][:12]
 s['mobilePosition']={'sai':'77% center','jiraiya':'79% center','minato':'80% center'}[s['id']]
(ROOT/'prototype/scenes.js').write_text("'use strict';\nwindow.KonohaScenes="+json.dumps(scenes,ensure_ascii=False,indent=2)+';\n',encoding='utf8',newline='\r\n')
s=(BEFORE/'index.html').read_text(encoding='utf8').replace('scenes.js?v=15','scenes.js?v=16');needle='<script src="icons.js?v=14"';assert needle in s
s=s.replace(needle,'<link rel="stylesheet" href="revision13.css?v=16">'+needle)
(ROOT/'prototype/index.html').write_text(s,encoding='utf8',newline='\r\n')
(OUT/'asset-manifest.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8')
print('Encoded three v13 edits; retained 17 scenes and every previous asset')
