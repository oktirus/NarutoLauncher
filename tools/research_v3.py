"""Download and catalogue 3 anime visual references per technique, with contact sheets."""
from pathlib import Path
import hashlib,json,requests,concurrent.futures
from PIL import Image,ImageOps,ImageDraw
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v3/references'
rows=json.loads((ROOT/'revisions/v3/reference-candidates.json').read_text(encoding='utf-8'))
def download(row):
 try:
  r=requests.get(row['image'],timeout=35,headers={'User-Agent':'Mozilla/5.0'});r.raise_for_status()
  p=OUT/(row['tag']+'-'+str(row['index'])+'.png');p.write_bytes(r.content)
  with Image.open(p) as im:im.verify()
  return dict(row,file=p.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(r.content).hexdigest())
 except Exception as e:return dict(row,error=str(e))
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:result=list(pool.map(download,rows))
(ROOT/'revisions/v3/reference-manifest.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
for tag in dict.fromkeys(r['tag'] for r in result):
 group=[r for r in result if r['tag']==tag and 'file' in r]
 board=Image.new('RGB',(1500,360),'#132d37');draw=ImageDraw.Draw(board)
 for i,row in enumerate(group):
  with Image.open(ROOT/row['file']) as source:
   image=ImageOps.contain(source.convert('RGB'),(490,310));board.paste(image,(i*500+(500-image.width)//2,(310-image.height)//2))
  draw.text((i*500+12,328),tag+' / ref '+str(row['index']),fill='white')
 board.save(OUT/(tag+'-board.jpg'),quality=94)
print('Downloaded',sum('file' in r for r in result),'/',len(rows));print('Errors:',[(r['tag'],r['index'],r.get('error')) for r in result if 'error' in r])
