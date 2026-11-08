"""Encode two v10 drawings; preserve all older paintings and runtime images."""
from pathlib import Path
from PIL import Image, ImageOps
import hashlib,json,shutil
ROOT=Path(__file__).resolve().parents[1]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
rows=[]
for row in json.loads((ROOT/'revisions/v10/generation-manifest.json').read_text(encoding='utf8')):
    source=ROOT/row['source'];shutil.copy2(row['source_path'],source)
    im=Image.open(source);original=list(im.size)
    if row['id']=='smarttube':
        im=im.convert('RGBA');assert im.getchannel('A').getextrema()[0]==0
        im.thumbnail((640,640),Image.Resampling.LANCZOS)
        canvas=Image.new('RGBA',(640,640));canvas.alpha_composite(im,((640-im.width)//2,(640-im.height)//2));im=canvas
        dest=ROOT/'assets/runtime/v10/smarttube.webp';im.save(dest,quality=96,method=4)
    else:
        im=ImageOps.fit(im.convert('RGB'),(1920,1080),method=Image.Resampling.LANCZOS)
        dest=ROOT/'assets/runtime/v10/konoha-dusk.jpg';im.save(dest,quality=95,subsampling=0)
    rows.append({'id':row['id'],'source':row['source'],'source_sha256':sha(source),'source_size':original,'file':dest.relative_to(ROOT).as_posix(),'sha256':sha(dest),'size':list(im.size),'alpha':row['transparent']})
path=ROOT/'prototype/scenes.js';scenes=json.loads(path.read_text(encoding='utf8').split('window.KonohaScenes=')[1].strip().rstrip(';'));wall=next(r for r in rows if r['id']=='konoha-dusk');scenes[0]['image']='v10/konoha-dusk.jpg?v='+wall['sha256'][:12];scenes[0]['mobilePosition']='88% center';path.write_text("'use strict';\nwindow.KonohaScenes="+json.dumps(scenes,ensure_ascii=False,indent=2)+';\n',encoding='utf8')
icon=next(r for r in rows if r['id']=='smarttube');url='../'+icon['file']+'?v='+icon['sha256'][:12]
path=ROOT/'prototype/app.js';s=path.read_text(encoding='utf8');s=s.replace('const scenes=window.KonohaScenes;',"const scenes=window.KonohaScenes;\nconst smartTubeArt='"+url+"';")
old='src="../assets/runtime/${examples.findIndex(a=>a.kind===app.kind)>=9?\'v6/\':\'\'}${app.kind}.webp"'
new='src="${app.kind===\'smarttube\'?smartTubeArt:`../assets/runtime/${examples.findIndex(a=>a.kind===app.kind)>=9?\'v6/\':\'\'}${app.kind}.webp`}"'
assert old in s;s=s.replace(old,new);assert 'src="../assets/runtime/smarttube.webp"' in s;s=s.replace('src="../assets/runtime/smarttube.webp"','src="${smartTubeArt}"');path.write_text(s,encoding='utf8',newline='')
path=ROOT/'prototype/index.html';path.write_text(path.read_text(encoding='utf8').replace('?v=12','?v=13'),encoding='utf8')
(ROOT/'revisions/v10/asset-manifest.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8')
print(json.dumps(rows,ensure_ascii=False))
