"""Encode one targeted anatomy repair; retain all prior resources and 17 scenes."""
from pathlib import Path
from PIL import Image,ImageOps
import json,hashlib,shutil
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v14';BEFORE=OUT/'before/prototype'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
source=json.loads((OUT/'generation-manifest.json').read_text(encoding='utf8'))[0];assert source['id']=='sai'
(ROOT/'assets/generated/v14').mkdir(parents=True,exist_ok=True);(ROOT/'assets/runtime/v14').mkdir(parents=True,exist_ok=True)
src=ROOT/source['source']
if not src.exists():shutil.copy2(source['source_path'],src)
original=Image.open(src);out=ROOT/'assets/runtime/v14/scene-sai.jpg';im=ImageOps.fit(original.convert('RGB'),(1920,1080),method=Image.Resampling.LANCZOS);im.save(out,quality=95,subsampling=0)
row={'id':'sai','kind':'wallpaper','source':source['source'],'source_sha256':sha(src),'source_size':list(original.size),'file':out.relative_to(ROOT).as_posix(),'sha256':sha(out),'size':list(im.size)}
scenes=json.loads((BEFORE/'scenes.js').read_text(encoding='utf8').split('window.KonohaScenes=')[1].strip().rstrip(';'));next(s for s in scenes if s['id']=='sai')['image']='v14/scene-sai.jpg?v='+row['sha256'][:12]
(ROOT/'prototype/scenes.js').write_text("'use strict';\nwindow.KonohaScenes="+json.dumps(scenes,ensure_ascii=False,indent=2)+';\n',encoding='utf8',newline='\r\n')
s=(BEFORE/'index.html').read_text(encoding='utf8').replace('scenes.js?v=16','scenes.js?v=17').replace('app.js?v=15','app.js?v=17').replace('revision13.css?v=16','revision13.css?v=17')
(ROOT/'prototype/index.html').write_text(s,encoding='utf8',newline='\r\n');(OUT/'asset-manifest.json').write_text(json.dumps([row],ensure_ascii=False,indent=2),encoding='utf8')
print('Encoded Sai v14 anatomy repair; retained all other 16 scene entries')
