"""Encode v8 art without modifying any previous image or pose atlas."""
from pathlib import Path
from PIL import Image
import hashlib,json
ROOT=Path(__file__).resolve().parents[1];SRC=ROOT/'assets/generated/v8';rows=[]
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def record(src,dest,**extra):
    row={'source':src.relative_to(ROOT).as_posix(),'source_sha256':sha(src),'file':dest.relative_to(ROOT).as_posix(),'sha256':sha(dest),**extra};rows.append(row);return row
def read_js(path,name):return json.loads(path.read_text(encoding='utf-8').split('window.'+name+'=')[1].strip().rstrip(';'))
def write_js(path,name,data):path.write_text("'use strict';\nwindow."+name+'='+json.dumps(data,ensure_ascii=False,indent=2)+';\n',encoding='utf-8')
src=SRC/'sharingan.png';im=Image.open(src).convert('RGBA');assert im.getchannel('A').getextrema()[0]==0;box=im.getchannel('A').point(lambda a:255 if a>32 else 0).getbbox();art=im.crop(box);art.thumbnail((696,696),Image.Resampling.LANCZOS);im=Image.new('RGBA',(768,768));im.alpha_composite(art,((768-art.width)//2,(768-art.height)//2));dest=ROOT/'assets/effects/v8/sharingan.webp';im.save(dest,quality=96,method=4);row=record(src,dest,kind='effect',size=list(im.size),source_box=box)
path=ROOT/'prototype/effects.js';data=read_js(path,'KonohaEffectSources');data['sharingan']='../'+row['file']+'?v='+row['sha256'][:12];write_js(path,'KonohaEffectSources',data)
src=SRC/'naruto-slide.png';im=Image.open(src).convert('RGBA');assert im.getchannel('A').getextrema()[0]==0;cells=[]
for k in range(4):
    cell=im.crop((round(k%2*im.width/2),round(k//2*im.height/2),round((k%2+1)*im.width/2),round((k//2+1)*im.height/2)));box=cell.getchannel('A').point(lambda a:255 if a>32 else 0).getbbox();assert box;cells.append((cell,box))
scale=min(464/max(b[2]-b[0] for _,b in cells),464/max(b[3]-b[1] for _,b in cells));sheet=Image.new('RGBA',(1024,1024));frames=[]
for k,(cell,box)in enumerate(cells):
    art=cell.crop(box);art=art.resize((round(art.width*scale),round(art.height*scale)),Image.Resampling.LANCZOS);x=(512-art.width)//2;y=498-art.height;sheet.alpha_composite(art,(k%2*512+x,k//2*512+y));frames.append({'frame':k,'source_box':box,'x':x,'y':y,'width':art.width,'height':art.height})
draw_size=round(min(142,62*512/max(f['height'] for f in frames)),3);dest=ROOT/'assets/game/v8/naruto-slide.webp';sheet.save(dest,quality=96,method=4);row=record(src,dest,kind='slide',size=list(sheet.size),grid=[2,2],cell=512,baseline=498,uniform_scale=scale,draw_size=draw_size,frames=frames)
path=ROOT/'prototype/runner-art.js';data=read_js(path,'KonohaRunnerSources');data['naruto']['slide']={'url':'../'+row['file']+'?v='+row['sha256'][:12],'columns':2,'rows':2,'cell':512,'baseline':498,'drawSize':draw_size};write_js(path,'KonohaRunnerSources',data)
for name in ['hinata','shikamaru','rocklee','neji','choji']:
    src=SRC/(name+'.png');im=Image.open(src).convert('RGB');original_size=list(im.size);im=im.resize((1920,1080),Image.Resampling.LANCZOS);dest=ROOT/'assets/runtime/v8'/(name+'.jpg');im.save(dest,quality=95,subsampling=0);record(src,dest,kind='wallpaper',id=name,source_size=original_size,size=list(im.size))
path=ROOT/'prototype/scenes.js';scenes=read_js(path,'KonohaScenes');added=json.loads((ROOT/'revisions/v8/scene-content.json').read_text(encoding='utf-8'));ids={s['id'] for s in added};scenes=[s for s in scenes if s['id'] not in ids]
for scene in added:
    row=next(r for r in rows if r.get('id')==scene['id']);scene['image']='v8/'+scene['id']+'.jpg?v='+row['sha256'][:12];scenes.append(scene)
write_js(path,'KonohaScenes',scenes)
(ROOT/'revisions/v8/asset-manifest.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps([{'kind':r['kind'],'file':r['file'],'size':r['size']} for r in rows]))
