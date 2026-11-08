"""Encode complete painted effects and register coherent 4x2 character pose sheets."""
from pathlib import Path
from PIL import Image
import hashlib,json
ROOT=Path(__file__).resolve().parents[1];SRC=ROOT/'assets/generated/v7';rows=[]
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def record(src,dest,**extra):
    row={'source':src.relative_to(ROOT).as_posix(),'source_sha256':sha(src),'file':dest.relative_to(ROOT).as_posix(),'sha256':sha(dest),**extra};rows.append(row);return row
path=ROOT/'prototype/effects.js';sources=json.loads(path.read_text(encoding='utf-8').split('window.KonohaEffectSources=')[1].strip().rstrip(';'))
for name in ['rasengan','sharingan','kurama','fireball','amaterasu']:
    src=SRC/(name+'.png');im=Image.open(src).convert('RGBA');assert im.getchannel('A').getextrema()[0]==0
    im.thumbnail((768,768),Image.Resampling.LANCZOS);dest=ROOT/'assets/effects/v7'/(name+'.webp');im.save(dest,quality=96,method=4)
    row=record(src,dest,kind='complete-effect',size=list(im.size),references=[f'revisions/v3/references/{name}-{k}.png' for k in range(1,4)])
    sources[name]='../'+row['file']+'?v='+row['sha256'][:12]
path.write_text("'use strict';\nwindow.KonohaEffectSources="+json.dumps(sources,indent=2)+';\n',encoding='utf-8')
runner={}
for hero in ['naruto','gamabunta']:
    src=SRC/(hero+'-motion.png');im=Image.open(src).convert('RGBA');assert im.getchannel('A').getextrema()[0]==0, f'{hero} needs actual transparency'
    cells=[]
    for k in range(8):
        cell=im.crop((round(k%4*im.width/4),round(k//4*im.height/2),round((k%4+1)*im.width/4),round((k//4+1)*im.height/2)))
        box=cell.getchannel('A').point(lambda a:255 if a>32 else 0).getbbox();assert box
        cells.append((cell,box))
    scale=min(464/max(b[2]-b[0] for _,b in cells),464/max(b[3]-b[1] for _,b in cells))
    sheet=Image.new('RGBA',(2048,1024));frames=[]
    for k,(cell,box)in enumerate(cells):
        art=cell.crop(box);art=art.resize((round(art.width*scale),round(art.height*scale)),Image.Resampling.LANCZOS)
        x=(512-art.width)//2;y=498-art.height;sheet.alpha_composite(art,(k%4*512+x,k//4*512+y));frames.append({'frame':k,'source_box':box,'x':x,'y':y,'width':art.width,'height':art.height})
    dest=ROOT/'assets/game/v7'/(hero+'-motion.webp');sheet.save(dest,quality=96,method=4)
    row=record(src,dest,kind='motion-sheet',hero=hero,size=list(sheet.size),grid=[4,2],cell=512,baseline=498,uniform_scale=scale,frames=frames)
    runner[hero]={'url':'../'+row['file']+'?v='+row['sha256'][:12],'columns':4,'rows':2,'cell':512,'baseline':498}
(ROOT/'prototype/runner-art.js').write_text("'use strict';\nwindow.KonohaRunnerSources="+json.dumps(runner,indent=2)+';\n',encoding='utf-8')
(ROOT/'revisions/v7/asset-manifest.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8')
print('Prepared five complete VFX and two eight-pose character sheets')
