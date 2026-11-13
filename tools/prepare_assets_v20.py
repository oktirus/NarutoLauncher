from pathlib import Path
from PIL import Image,ImageOps
import json,hashlib,shutil,re
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v20'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
rows=[]
for entry in json.loads((OUT/'generation-manifest.json').read_text(encoding='utf8')):
 src=ROOT/entry['source']
 if not src.exists():shutil.copy2(entry['source_path'],src)
 if not entry.get('selected'):continue
 original=Image.open(src);im=ImageOps.fit(original.convert('RGB'),(1920,1080),method=Image.Resampling.LANCZOS);target=ROOT/'assets/runtime/v20/scene-sai.jpg';im.save(target,quality=95,subsampling=0)
 rows.append(dict(id='sai',source=entry['source'],source_size=list(original.size),source_sha256=sha(src),file=target.relative_to(ROOT).as_posix(),size=list(im.size),sha256=sha(target)))
assert len(rows)==1
scenes=json.loads((OUT/'before/prototype/scenes.js').read_text(encoding='utf8').split('window.KonohaScenes=')[1].strip().rstrip(';'));s=next(s for s in scenes if s['id']=='sai')
s.update(image='v20/scene-sai.jpg?v='+rows[0]['sha256'][:12],caption='Малюнок здіймається в небо',mood='Політ на чорнильному птахові',description='Сай летить над лісом на птахові, створеному чорнильною технікою.',mobilePosition='80% center',cardPosition='77% 24%')
(ROOT/'prototype/scenes.js').write_text("'use strict';\nwindow.KonohaScenes="+json.dumps(scenes,ensure_ascii=False,indent=2)+';\n',encoding='utf8',newline='\r\n')
index=(OUT/'before/prototype/index.html').read_text(encoding='utf8');index,n=re.subn(r'scenes.js\?v=[a-zA-Z0-9]+','scenes.js?v='+sha(ROOT/'prototype/scenes.js')[:12],index);assert n==1
(ROOT/'prototype/index.html').write_text(index,encoding='utf8',newline='\r\n');(OUT/'asset-manifest.json').write_text(json.dumps(rows,indent=2),encoding='utf8');print('Only Sai wallpaper replaced; v19 UI preserved')
