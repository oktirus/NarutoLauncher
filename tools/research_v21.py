from pathlib import Path
from PIL import Image,ImageOps,ImageDraw
from urllib.parse import quote
import json,hashlib,urllib.request,re,shutil
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v21';OUT.mkdir(exist_ok=True)
if not (OUT/'before').exists():
 shutil.copytree(ROOT/'prototype',OUT/'before/prototype');shutil.copy2(ROOT/'README.md',OUT/'before/README.md')
 (OUT/'before-hashes.json').write_text(json.dumps({p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for group in ['prototype','assets'] for p in (ROOT/group).rglob('*') if p.is_file()},indent=2),encoding='utf8')
for name in ['references','verification']:(OUT/name).mkdir(exist_ok=True)
for name in ['assets/generated/v21','assets/runtime/v21']:(ROOT/name).mkdir(exist_ok=True)
rows=[]
for tag,slug in [('kushina','01_690'),('couple','01_691')]:
 page='https://naruto-official.com/en/anime/naruto2/list/'+slug;markup=urllib.request.urlopen(page,timeout=30).read().decode();paths=list(dict.fromkeys(re.findall(r'"(/naruto/import/images/[^"<>]+\.(?:jpg|png))"',markup)))[:4]
 for i,path in enumerate(paths):
  url='https://cmsapi-frontend.naruto-official.com/site/api/naruto/Image/get?path='+quote(path,safe='');p=OUT/f'references/{tag}-{i+1}.jpg';p.write_bytes(urllib.request.urlopen(url,timeout=30).read());rows.append(dict(file=p.relative_to(ROOT).as_posix(),page=page,image=url,sha256=hashlib.sha256(p.read_bytes()).hexdigest()));print(p.name,flush=True)
for source,name in [(ROOT/'revisions/v18/references/minato-board.jpg','minato-design-board.jpg'),(ROOT/'revisions/v20/references/cel-density-approved.jpg','cel-density-approved.jpg')]:
 p=OUT/'references'/name;shutil.copy2(source,p);rows.append(dict(file=p.relative_to(ROOT).as_posix(),copied_from=source.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(p.read_bytes()).hexdigest()))
board=Image.new('RGB',(2000,640),'#102b38');d=ImageDraw.Draw(board)
for i,row in enumerate(rows[:8]):
 im=ImageOps.contain(Image.open(ROOT/row['file']).convert('RGB'),(490,280));x=i%4*500;y=i//4*320;board.paste(im,(x+(500-im.width)//2,y+(280-im.height)//2));d.text((x+8,y+294),Path(row['file']).name,fill='white')
board.save(OUT/'references/kushina-board.jpg',quality=95)
(OUT/'references/sources.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8');print('v20 frozen; official Kushina references saved')
