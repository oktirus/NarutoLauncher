"""Freeze v14 and collect official anime design frames for two v15 redraws."""
from pathlib import Path
import hashlib, html, json, re, shutil, urllib.request
from urllib.parse import quote
from PIL import Image, ImageOps, ImageDraw
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'revisions/v15'
OUT.mkdir(exist_ok=True)
before=OUT/'before'
if not before.exists():
    shutil.copytree(ROOT/'prototype',before/'prototype')
    shutil.copy2(ROOT/'README.md',before/'README.md')
    files={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for folder in ('prototype','assets') for p in (ROOT/folder).rglob('*') if p.is_file()}
    (OUT/'before-hashes.json').write_text(json.dumps(files,indent=2),encoding='utf8')
refs=OUT/'references';refs.mkdir(exist_ok=True)
rows=[]
for tag,slug in [('sakura','01_715')]:
    page='https://naruto-official.com/en/anime/naruto2/list/'+slug
    markup=urllib.request.urlopen(page).read().decode()
    paths=list(dict.fromkeys(re.findall(r'"(/naruto/import/images/[^"<>]+\.(?:jpg|png))"',markup)))
    for i,path in enumerate(paths[:4],1):
        url='https://cmsapi-frontend.naruto-official.com/site/api/naruto/Image/get?path='+quote(path,safe='')
        target=refs/f'{tag}-{i}.jpg';target.write_bytes(urllib.request.urlopen(url).read())
        rows.append(dict(tag=tag,file=target.relative_to(ROOT).as_posix(),page=page,image=url,sha256=hashlib.sha256(target.read_bytes()).hexdigest()))
old=json.loads((ROOT/'revisions/v3/reference-manifest.json').read_text(encoding='utf8'))
for row in old:
    if row['tag']=='chidori':
        target=refs/Path(row['file']).name;shutil.copy2(ROOT/row['file'],target)
        rows.append({**row,'file':target.relative_to(ROOT).as_posix()})
for tag in ('sakura','chidori'):
    group=[r for r in rows if r['tag']==tag]
    board=Image.new('RGB',(640*len(group),400),'#142b35')
    draw=ImageDraw.Draw(board)
    for i,row in enumerate(group):
        im=ImageOps.contain(Image.open(ROOT/row['file']).convert('RGB'),(628,350))
        board.paste(im,(i*640+(640-im.width)//2,(350-im.height)//2))
        draw.text((i*640+12,370),f'{tag} / actual episode frame {i+1}',fill='white')
    board.save(refs/f'{tag}-board.jpg',quality=95)
(refs/'sources.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8')
print(f'Frozen v14; saved {len(rows)} anime frames and two boards')
