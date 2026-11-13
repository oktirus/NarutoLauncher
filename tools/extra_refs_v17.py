from pathlib import Path
from PIL import Image,ImageOps,ImageDraw
import json,urllib.request,hashlib
root=Path(__file__).resolve().parents[1];out=root/'revisions/v17/references'
items=[('formation-team','https://staticg.sportskeeda.com/editor/2022/05/50cca-16539864646521.png','https://wiki.sportskeeda.com/naruto/who-created-the-rasengan'),('formation-training','https://staticg.sportskeeda.com/editor/2022/07/46317-16572800650500.png','https://wiki.sportskeeda.com/naruto/wind-style-rasengan-in-naruto'),('rocklee-rope','https://myhero.com/images/guest/g266990/hero92607/naruto_lee0077.jpg','https://myhero.com/R_Lee_stbhs_US_2012_ul')]
rows=json.loads((out/'sources.json').read_text(encoding='utf8'))
for name,url,page in items:
 p=out/(name+'.jpg');p.write_bytes(urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'Mozilla/5.0'}),timeout=30).read());Image.open(p).verify();rows.append(dict(tag=name,file=p.relative_to(root).as_posix(),page=page,image=url,sha256=hashlib.sha256(p.read_bytes()).hexdigest()));print(name)
(out/'sources.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8')
