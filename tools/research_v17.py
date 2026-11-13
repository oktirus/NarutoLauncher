"""Freeze v16 and gather actual anime frames for the three v17 scenes."""
from pathlib import Path
from PIL import Image,ImageOps,ImageDraw
from urllib.parse import quote
import hashlib,json,re,shutil,urllib.request
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v17';OUT.mkdir(exist_ok=True)
if not (OUT/'before').exists():
    shutil.copytree(ROOT/'prototype',OUT/'before/prototype');shutil.copy2(ROOT/'README.md',OUT/'before/README.md')
    hashes={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for group in ['prototype','assets'] for p in (ROOT/group).rglob('*') if p.is_file()}
    (OUT/'before-hashes.json').write_text(json.dumps(hashes,indent=2),encoding='utf8')
for folder in ['references','verification']:(OUT/folder).mkdir(exist_ok=True)
for folder in ['assets/generated/v17','assets/runtime/v17']:(ROOT/folder).mkdir(exist_ok=True)
rows=[]
for tag,slugs in {'formation':['01_520','01_531'],'sai':['01_477','01_481']}.items():
    n=0
    for slug in slugs:
        page='https://naruto-official.com/en/anime/naruto2/list/'+slug
        markup=urllib.request.urlopen(page,timeout=30).read().decode()
        paths=list(dict.fromkeys(re.findall(r'"(/naruto/import/images/[^"<>]+\.(?:jpg|png))"',markup)))[:4]
        for path in paths:
            n+=1;url='https://cmsapi-frontend.naruto-official.com/site/api/naruto/Image/get?path='+quote(path,safe='')
            p=OUT/f'references/{tag}-{n}.jpg';p.write_bytes(urllib.request.urlopen(url,timeout=30).read());rows.append(dict(tag=tag,file=p.relative_to(ROOT).as_posix(),page=page,image=url,sha256=hashlib.sha256(p.read_bytes()).hexdigest()))
for tag in ['rocklee','naruto']:
    for source in sorted((ROOT/'revisions/v16/references').glob(tag+'-[0-9].jpg')):
        p=OUT/'references'/source.name;shutil.copy2(source,p);rows.append(dict(tag=tag,file=p.relative_to(ROOT).as_posix(),copied_from=source.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(p.read_bytes()).hexdigest()))
for tag in ['formation','sai','rocklee','naruto']:
    group=[r for r in rows if r['tag']==tag];cols=min(4,len(group));board=Image.new('RGB',(500*cols,330*((len(group)+cols-1)//cols)),'#162d36');d=ImageDraw.Draw(board)
    for i,row in enumerate(group):
        im=ImageOps.contain(Image.open(ROOT/row['file']).convert('RGB'),(490,294));x=i%cols*500;y=i//cols*330;board.paste(im,(x+(500-im.width)//2,y+(294-im.height)//2));d.text((x+8,y+305),f'{tag} / actual anime frame {i+1}',fill='white')
    board.save(OUT/f'references/{tag}-board.jpg',quality=95)
rows.append(dict(tag='formation-mechanics',page='https://naruto.fandom.com/wiki/Wind_Release%3A_Rasenshuriken',finding='Original Naruto supplies chakra; two shadow clones contain/shape it and add Wind Release. Three Naruto figures total, not three extra clones.'))
(OUT/'references/sources.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8');print('v16 frozen; v17 anime references saved')
