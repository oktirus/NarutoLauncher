from pathlib import Path
from PIL import Image,ImageOps,ImageDraw
from urllib.parse import quote
import json,hashlib,shutil,re,urllib.request
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v23';OUT.mkdir(exist_ok=True)
if not (OUT/'before').exists():
 shutil.copytree(ROOT/'prototype',OUT/'before/prototype');shutil.copy2(ROOT/'README.md',OUT/'before/README.md')
 (OUT/'before-hashes.json').write_text(json.dumps({p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for group in ['prototype','assets'] for p in (ROOT/group).rglob('*') if p.is_file()},indent=2),encoding='utf8')
for folder in ['references','verification','device']:(OUT/folder).mkdir(exist_ok=True)
for folder in ['assets/generated/v23','assets/runtime/v23','releases']:(ROOT/folder).mkdir(exist_ok=True)
rows=[]
def get(name,url,page,role):
 p=OUT/'references'/name
 p.write_bytes(urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'Mozilla/5.0'}),timeout=35).read());Image.open(p).verify()
 rows.append(dict(file=p.relative_to(ROOT).as_posix(),image=url,page=page,role=role,sha256=hashlib.sha256(p.read_bytes()).hexdigest()));print(name,flush=True)
get('team-photo.jpg','https://vignette2.wikia.nocookie.net/naruto/images/5/50/Team_Kakashi.png/revision/20161118024320','https://naruto.fandom.com/wiki/Team_7_(Kakashi)','Actual anime team photograph; main pose/order/costume reference')
get('team-photo-detail.jpg','https://news.nateimg.co.kr/orgImg/is/2023/12/09/isp20231208000122.800x.0.jpg','https://news.nate.com/view/20231209n04549','Anime photo detail, compare with primary reference')
page='https://naruto-official.com/en/anime/naruto1/list/01_228';markup=urllib.request.urlopen(page,timeout=35).read().decode()
paths=list(dict.fromkeys(re.findall(r'"(/naruto/import/images/[^"<>]+\.(?:jpg|png))"',markup)))[:4]
for i,path in enumerate(paths):get(f'episode4-{i+1}.jpg','https://cmsapi-frontend.naruto-official.com/site/api/naruto/Image/get?path='+quote(path,safe=''),page,'Official actual Part I episode frame; character models')
board=Image.new('RGB',(1500,640),'#152d36');d=ImageDraw.Draw(board)
for i,row in enumerate(rows):
 im=ImageOps.contain(Image.open(ROOT/row['file']).convert('RGB'),(490,280));x=i%3*500;y=i//3*320;board.paste(im,(x+(500-im.width)//2,y+(280-im.height)//2));d.text((x+8,y+294),Path(row['file']).name,fill='white')
board.save(OUT/'references/team7-board.jpg',quality=95)
(OUT/'references/sources.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8')
