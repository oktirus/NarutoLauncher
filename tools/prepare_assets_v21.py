from pathlib import Path
from PIL import Image,ImageOps
import json,hashlib,shutil,re
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v21'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
entry=json.loads((OUT/'generation-manifest.json').read_text(encoding='utf8'))[0];src=ROOT/entry['source']
if not src.exists():shutil.copy2(entry['source_path'],src)
original=Image.open(src);im=ImageOps.fit(original.convert('RGB'),(1920,1080),method=Image.Resampling.LANCZOS);target=ROOT/'assets/runtime/v21/scene-minato.jpg';im.save(target,quality=95,subsampling=0)
row=dict(id='minato',source=entry['source'],source_size=list(original.size),source_sha256=sha(src),file=target.relative_to(ROOT).as_posix(),size=list(im.size),sha256=sha(target))
scenes=json.loads((OUT/'before/prototype/scenes.js').read_text(encoding='utf8').split('window.KonohaScenes=')[1].strip().rstrip(';'));s=next(s for s in scenes if s['id']=='minato')
s.update(name='Мінато й Кушіна',image='v21/scene-minato.jpg?v='+row['sha256'][:12],caption='Тихі миті разом',mood='Разом на лісовій галявині',description='Мінато та Кушіна відпочивають після тренування.',mobilePosition='75% center',cardPosition='71% 28%')
(ROOT/'prototype/scenes.js').write_text("'use strict';\nwindow.KonohaScenes="+json.dumps(scenes,ensure_ascii=False,indent=2)+';\n',encoding='utf8',newline='\r\n')
index=(OUT/'before/prototype/index.html').read_text(encoding='utf8');index,n=re.subn(r'scenes.js\?v=[a-zA-Z0-9]+','scenes.js?v='+sha(ROOT/'prototype/scenes.js')[:12],index);assert n==1
index=index.replace('</head>','<link rel="stylesheet" href="revision21.css?v='+sha(ROOT/'prototype/revision21.css')[:12]+'">\n</head>')
(ROOT/'prototype/index.html').write_text(index,encoding='utf8',newline='\r\n');(OUT/'asset-manifest.json').write_text(json.dumps([row],indent=2),encoding='utf8');print('Minato and Kushina integrated; all other scenes preserved')
