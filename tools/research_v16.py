"""Freeze v15, retain lineage and collect actual anime frames for v16."""
from pathlib import Path
import hashlib,json,re,shutil,urllib.request
from urllib.parse import quote
from PIL import Image,ImageOps,ImageDraw
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v16';OUT.mkdir(exist_ok=True)
if not (OUT/'before').exists():
    shutil.copytree(ROOT/'prototype',OUT/'before/prototype');shutil.copy2(ROOT/'README.md',OUT/'before/README.md')
    hashes={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for group in ['prototype','assets'] for p in (ROOT/group).rglob('*') if p.is_file()}
    (OUT/'before-hashes.json').write_text(json.dumps(hashes,indent=2),encoding='utf8')
for folder in ['references','verification']:(OUT/folder).mkdir(exist_ok=True)
for folder in ['assets/generated/v16','assets/runtime/v16']:(ROOT/folder).mkdir(exist_ok=True)
rows=[]
pages={'sakura':['01_455'],'rasenshuriken':['01_532'],'gaara':['01_446','01_705'],'minato':['01_692'],'sai':['01_477','01_481']}
for tag,slugs in pages.items():
    count=0
    for slug in slugs:
        page='https://naruto-official.com/en/anime/naruto2/list/'+slug
        try:
            markup=urllib.request.urlopen(page,timeout=30).read().decode()
            paths=list(dict.fromkeys(re.findall(r'"(/naruto/import/images/[^"<>]+\.(?:jpg|png))"',markup)))
            for path in paths[:4]:
                count+=1;url='https://cmsapi-frontend.naruto-official.com/site/api/naruto/Image/get?path='+quote(path,safe='')
                target=OUT/f'references/{tag}-{count}.jpg';target.write_bytes(urllib.request.urlopen(url,timeout=30).read())
                rows.append(dict(tag=tag,file=target.relative_to(ROOT).as_posix(),page=page,image=url,sha256=hashlib.sha256(target.read_bytes()).hexdigest()))
        except Exception as e:print(tag,slug,type(e).__name__,str(e))
for tag,files in {'naruto':['revisions/v11/references/naruto-1.jpg','revisions/v11/references/naruto-2.jpg'],'rocklee':['revisions/v9/references/rocklee-1.jpg','revisions/v9/references/rocklee-2.jpg','revisions/v9/references/rocklee-3.jpg','revisions/v9/references/rocklee-4.jpg'],'jiraiya':['revisions/v12/references/jiraiya-1.jpg','revisions/v12/references/jiraiya-2.jpg','revisions/v12/references/jiraiya-3.jpg','revisions/v12/references/jiraiya-4.jpg']}.items():
    for i,src in enumerate(files,1):
        target=OUT/f'references/{tag}-{i}.jpg';shutil.copy2(ROOT/src,target);rows.append(dict(tag=tag,file=target.relative_to(ROOT).as_posix(),copied_from=src,sha256=hashlib.sha256(target.read_bytes()).hexdigest()))
for tag in pages.keys()|{'naruto','rocklee','jiraiya'}:
    group=[r for r in rows if r['tag']==tag];cols=min(4,len(group));nrows=(len(group)+cols-1)//cols
    board=Image.new('RGB',(500*cols,330*nrows),'#142b35');draw=ImageDraw.Draw(board)
    for i,row in enumerate(group):
        im=ImageOps.contain(Image.open(ROOT/row['file']).convert('RGB'),(490,295));x=i%cols*500;y=i//cols*330
        board.paste(im,(x+(500-im.width)//2,y+(295-im.height)//2));draw.text((x+8,y+306),f'{tag} / actual anime frame {i+1}',fill='white')
    board.save(OUT/f'references/{tag}-board.jpg',quality=95)
(OUT/'references/sources.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8')
print('v15 frozen; official anime frames and existing approved references saved:',len(rows))
