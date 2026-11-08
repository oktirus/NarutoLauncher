"""Encode new artwork and integrate one versioned icon registry; preserve old assets."""
from pathlib import Path
from PIL import Image,ImageOps
import json,hashlib
ROOT=Path(__file__).resolve().parents[1];BEFORE=ROOT/'revisions/v11/before/prototype'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def load_js(p,name):return json.loads(p.read_text(encoding='utf8').split('window.'+name+'=')[1].strip().rstrip(';'))
def write_js(p,name,data):p.write_text("'use strict';\nwindow."+name+'='+json.dumps(data,ensure_ascii=False,indent=2)+';\n',encoding='utf8',newline='\r\n')
sources=json.loads((ROOT/'revisions/v11/generation-manifest.json').read_text(encoding='utf8'))
assert len(sources)==26 and sum(s['kind']=='icon' for s in sources)==20
rows=[];icons={}
for source in sources:
    src=ROOT/source['source'];im=Image.open(src);native=list(im.size);extra={}
    if source['kind']=='icon':
        im=im.convert('RGBA');assert im.getchannel('A').getextrema()[0]==0,source['id']
        box=im.getchannel('A').point(lambda a:255 if a>16 else 0).getbbox();assert box
        box=(max(0,box[0]-3),max(0,box[1]-3),min(im.width,box[2]+3),min(im.height,box[3]+3))
        art=im.crop(box);art.thumbnail((560,560),Image.Resampling.LANCZOS)
        im=Image.new('RGBA',(640,640));x,y=(640-art.width)//2,(640-art.height)//2;im.alpha_composite(art,(x,y))
        out=ROOT/'assets/runtime/v11'/(source['id']+'.webp');im.save(out,quality=96,method=4)
        extra={'source_box':box,'content_box':[x,y,x+art.width,y+art.height]}
        icons[source['id']]='../assets/runtime/v11/'+source['id']+'.webp?v='+sha(out)[:12]
    else:
        im=ImageOps.fit(im.convert('RGB'),(1920,1080),method=Image.Resampling.LANCZOS)
        out=ROOT/'assets/runtime/v11'/('scene-'+source['id']+'.jpg');im.save(out,quality=95,subsampling=0)
    rows.append({'id':source['id'],'kind':source['kind'],'source':source['source'],'source_sha256':sha(src),'source_size':native,'file':out.relative_to(ROOT).as_posix(),'sha256':sha(out),'size':list(im.size),**extra})
write_js(ROOT/'prototype/icons.js','KonohaIconSources',icons)
scenes=load_js(BEFORE/'scenes.js','KonohaScenes');content=json.loads((ROOT/'revisions/v11/scene-content.json').read_text(encoding='utf8'))
for info in content:
    scene=next(s for s in scenes if s['id']==info['id']);scene.update(info)
    row=next(r for r in rows if r['kind']=='wallpaper' and r['id']==scene['id'])
    scene['image']='v11/scene-'+scene['id']+'.jpg?v='+row['sha256'][:12]
write_js(ROOT/'prototype/scenes.js','KonohaScenes',scenes)
s=(BEFORE/'app.js').read_text(encoding='utf8');old=next(line for line in s.splitlines() if line.startswith('const smartTubeArt='));s=s.replace(old,"const iconArt=window.KonohaIconSources;\nconst smartTubeArt=iconArt.smarttube;")
old=next(line for line in s.splitlines() if line.startswith(' const art=app.kind?'))
s=s.replace(old," const art=app.kind?`<img src=\"${iconArt[app.kind]}\" alt=\"\" draggable=\"false\">`:`<span class=\"app-fallback\">${escapeHTML(app.name.slice(0,2))}</span>`;")
assert 'src="../assets/runtime/archive.webp"' in s and 'src="../assets/runtime/lampa.webp"' in s
s=s.replace('src="../assets/runtime/archive.webp"','src="\'+iconArt.archive+\'"').replace('src="../assets/runtime/lampa.webp"','src="\'+iconArt.lampa+\'"')
(ROOT/'prototype/app.js').write_text(s,encoding='utf8',newline='')
s=(BEFORE/'index.html').read_text(encoding='utf8').replace('?v=13','?v=14');s=s.replace('<script src="scenes.js?v=14"','<script src="icons.js?v=14" defer></script><script src="scenes.js?v=14"')
(ROOT/'prototype/index.html').write_text(s,encoding='utf8',newline='\r\n')
(ROOT/'revisions/v11/asset-manifest.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8')
print(f'Encoded {len(rows)} images; registered {len(icons)} icons and {len(content)} new scenes.')
