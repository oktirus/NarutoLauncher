"""Encode two built-in generated v15 originals without altering prior assets."""
from pathlib import Path
from PIL import Image,ImageOps
import json,hashlib,shutil
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v15';BEFORE=OUT/'before/prototype'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
rows=[]
for entry in json.loads((OUT/'generation-manifest.json').read_text(encoding='utf8')):
    src=ROOT/entry['source']
    if not src.exists():shutil.copy2(entry['source_path'],src)
    original=Image.open(src)
    if entry['id']=='sakura':
        out=ROOT/'assets/runtime/v15/scene-sakura.jpg'
        im=ImageOps.fit(original.convert('RGB'),(1920,1080),method=Image.Resampling.LANCZOS)
        im.save(out,quality=95,subsampling=0)
    else:
        out=ROOT/'assets/effects/v15/chidori.webp'
        assert original.mode=='RGBA','Effect must have generated alpha'
        im=original.resize((768,768),Image.Resampling.LANCZOS)
        im.save(out,lossless=True,method=6)
    rows.append(dict(id=entry['id'],source=entry['source'],source_sha256=sha(src),source_size=list(original.size),file=out.relative_to(ROOT).as_posix(),sha256=sha(out),size=list(im.size)))
scenes=json.loads((BEFORE/'scenes.js').read_text(encoding='utf8').split('window.KonohaScenes=')[1].strip().rstrip(';'))
scene=next(s for s in scenes if s['id']=='sakura')
scene['image']='v15/scene-sakura.jpg?v='+rows[0]['sha256'][:12]
scene['mobilePosition']='78% center';scene['cardPosition']='77% 35%'
(ROOT/'prototype/scenes.js').write_text("'use strict';\nwindow.KonohaScenes="+json.dumps(scenes,ensure_ascii=False,indent=2)+';\n',encoding='utf8',newline='\r\n')
effects=(BEFORE/'effects.js').read_text(encoding='utf8').replace('../assets/effects/chidori.webp?v=3b0eed22c2ad','../assets/effects/v15/chidori.webp?v='+rows[1]['sha256'][:12])
(ROOT/'prototype/effects.js').write_text(effects,encoding='utf8',newline='\r\n')
s=(BEFORE/'index.html').read_text(encoding='utf8')
import re
for script in ('scenes.js','effects.js','technique-art.js'):
    s=re.sub(re.escape(script)+r'\?v=\d+',script+'?v=18',s)
s=s.replace('</head>','<link rel="stylesheet" href="revision15.css?v=18">\n</head>')
(ROOT/'prototype/index.html').write_text(s,encoding='utf8',newline='\r\n')
(OUT/'asset-manifest.json').write_text(json.dumps(rows,indent=2),encoding='utf8')
print('Encoded Sakura and Chidori v15; preserved 16 other scene records')
