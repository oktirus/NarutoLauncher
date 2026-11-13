from pathlib import Path
import json,re,urllib.request,hashlib
from urllib.parse import quote
from PIL import Image,ImageOps
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v16/references'
page='https://naruto-official.com/en/anime/naruto1/list/01_311'
markup=urllib.request.urlopen(page,timeout=30).read().decode()
paths=list(dict.fromkeys(re.findall(r'"(/naruto/import/images/[^"<>]+\.(?:jpg|png))"',markup)))[:4]
rows=[];board=Image.new('RGB',(1600,300),'#142b35')
for i,path in enumerate(paths,1):
    url='https://cmsapi-frontend.naruto-official.com/site/api/naruto/Image/get?path='+quote(path,safe='');p=OUT/f'young-naruto-{i}.jpg';p.write_bytes(urllib.request.urlopen(url,timeout=30).read())
    im=ImageOps.contain(Image.open(p),(390,290));board.paste(im,((i-1)*400+(400-im.width)//2,(300-im.height)//2));rows.append(dict(file=p.name,page=page,image=url,sha256=hashlib.sha256(p.read_bytes()).hexdigest()))
board.save(OUT/'young-naruto-board.jpg',quality=95)
im=Image.open(OUT/'minato-4.jpg');box=(1170,375,1280,610);im.crop(box).resize((330,705)).save(OUT/'minato-kunai-detail.png')
rows.append(dict(file='minato-kunai-detail.png',source='minato-4.jpg',box=box,role='actual anime prop detail, enlarged reference only'))
rows.append(dict(file='naruto-logo-reference.png',page='https://pngimg.com/image/109343',image='https://pngimg.com/uploads/naruto/naruto_PNG48.png',role='logo shape reference, not runtime'))
(OUT/'extra-sources.json').write_text(json.dumps(rows,indent=2),encoding='utf8');print('Young Naruto and actual kunai detail collected')
