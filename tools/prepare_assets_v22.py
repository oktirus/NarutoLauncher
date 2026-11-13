from pathlib import Path
from PIL import Image, ImageOps
import json, hashlib, shutil, re

ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v22'
GEN=Path(r'C:\Users\angel\.codex\generated_images\01a1b5d6-c1c9-7f20-9b97-1f0cd10f4a77')
files={'scene-night-candidate.png':'exec-f18bf005-829b-4286-aba5-0ad5d2597941.png','scene-sakura-candidate.png':'exec-3f4116f6-9a18-416b-8669-300a9d695ce0.png','scene-night.png':'exec-ea7a47be-527f-4964-9916-ed75572a58c4.png','scene-sakura.png':'exec-6725ce0f-cb14-4f85-8fab-55ea0567f6d7.png'}
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
for target,source in files.items():
    dst=ROOT/'assets/generated/v22'/target
    if not dst.exists():shutil.copy2(GEN/source,dst)
scenes=json.loads((OUT/'before/prototype/scenes.js').read_text(encoding='utf8').split('window.KonohaScenes=')[1].strip().rstrip(';'))
rows=[]
for id in ['night','sakura']:
    src=ROOT/f'assets/generated/v22/scene-{id}.png';original=Image.open(src)
    image=ImageOps.fit(original.convert('RGB'),(1920,1080),method=Image.Resampling.LANCZOS)
    target=ROOT/f'assets/runtime/v22/scene-{id}.jpg';image.save(target,quality=95,subsampling=0)
    row=dict(id=id,source=src.relative_to(ROOT).as_posix(),source_path=str(GEN/files[src.name]),source_size=list(original.size),source_sha256=sha(src),file=target.relative_to(ROOT).as_posix(),size=list(image.size),sha256=sha(target));rows.append(row)
    scene=next(s for s in scenes if s['id']==id)
    scene.update(image=f'v22/scene-{id}.jpg?v='+row['sha256'][:12])
    if id=='night':scene.update(mood='Спільне формування техніки',description='Наруто й два тіньові клони разом формують Расенсюрикен.',mobilePosition='72% center',cardPosition='72% 34%')
    else:scene.update(mood='Практика медичного ніндзюцу',description='Сакура відпрацьовує медичну техніку за столом.',mobilePosition='80% center',cardPosition='78% 30%')
(ROOT/'prototype/scenes.js').write_text("'use strict';\nwindow.KonohaScenes="+json.dumps(scenes,ensure_ascii=False,indent=2)+';\n',encoding='utf8',newline='\r\n')
index=(OUT/'before/prototype/index.html').read_text(encoding='utf8');index,n=re.subn(r'scenes.js\?v=[a-zA-Z0-9]+','scenes.js?v='+sha(ROOT/'prototype/scenes.js')[:12],index);assert n==1
(ROOT/'prototype/index.html').write_text(index,encoding='utf8',newline='\r\n')
(OUT/'asset-manifest.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8')
(OUT/'output-provenance.json').write_text(json.dumps({name:dict(source_path=str(GEN/source),file='assets/generated/v22/'+name,sha256=sha(ROOT/'assets/generated/v22'/name)) for name,source in files.items()},indent=2),encoding='utf8')
print('Two v22 wallpapers integrated; all other entries preserved')
