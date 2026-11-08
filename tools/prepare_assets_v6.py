"""Runtime encodings and sprite-cell baseline alignment; no generated painting altered."""
from pathlib import Path
from PIL import Image
import hashlib,json
ROOT=Path(__file__).resolve().parents[1];SRC=ROOT/'assets/generated/v6';OUT=ROOT/'assets/runtime/v6';OUT.mkdir(exist_ok=True)
icons=['youtube','tvbro','kyivstar','megogo','sweettv','primevideo','disneyplus','jellyfin','sendfiles','xplore'];rows=[]
def record(source,dest,**extra):rows.append({'source':source.relative_to(ROOT).as_posix(),'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'file':dest.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(dest.read_bytes()).hexdigest(),**extra})
for name in icons:
    source=SRC/(name+'.png');im=Image.open(source).convert('RGBA');im.thumbnail((640,640),Image.Resampling.LANCZOS);dest=OUT/(name+'.webp');im.save(dest,quality=94,method=6);record(source,dest,kind='icon',size=im.size)
source=SRC/'konoha-forest.png';im=Image.open(source).convert('RGB').resize((1280,540),Image.Resampling.LANCZOS);dest=ROOT/'assets/game/konoha-forest.webp';im.save(dest,quality=94,method=6);record(source,dest,kind='background',size=im.size)
source=SRC/'naruto-run.png';im=Image.open(source).convert('RGBA');cw,ch=im.width//3,im.height//2;sheet=Image.new('RGBA',im.size);bounds=[]
for k in range(6):
    cell=im.crop((k%3*cw,k//3*ch,(k%3+1)*cw,(k//3+1)*ch));box=cell.getchannel('A').point(lambda a:255 if a>16 else 0).getbbox();assert box
    art=cell.crop(box);x=(cw-art.width)//2;y=ch-14-art.height;sheet.alpha_composite(art,(k%3*cw+x,k//3*ch+y));bounds.append({'frame':k,'original_box':box,'destination':[x,y]})
dest=ROOT/'assets/game/naruto-run.webp';sheet.save(dest,quality=96,method=6);record(source,dest,kind='sprite-sheet',size=sheet.size,grid=[3,2],baseline=ch-14,frames=bounds)
(ROOT/'revisions/v6/asset-manifest.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8');print(f'{len(rows)} runtime assets prepared')
