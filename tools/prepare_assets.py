"""Create runtime derivatives; preserve every generated original unchanged."""
import hashlib,json
from pathlib import Path
from PIL import Image,ImageOps,ImageDraw
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'assets/runtime';OUT.mkdir(parents=True,exist_ok=True)
rows=[];originals=ROOT/'assets/generated'
manifest_path=ROOT/'research/asset-manifest.json'
previous={row['source']:row for row in json.loads(manifest_path.read_text(encoding='utf-8'))} if manifest_path.exists() else {}
for path in sorted(originals.glob('*.png')):
 source_hash=hashlib.sha256(path.read_bytes()).hexdigest();old=previous.get(path.relative_to(ROOT).as_posix())
 if old and old['source_sha256']==source_hash and (ROOT/old['runtime']).is_file() and hashlib.sha256((ROOT/old['runtime']).read_bytes()).hexdigest()==old['runtime_sha256']:
  rows.append(old);continue
 image=Image.open(path)
 if path.name.startswith('konoha-'):
  dest=OUT/(path.stem+'.jpg');image.convert('RGB').save(dest,quality=94)
 else:
  if image.mode!='RGBA' or image.getchannel('A').getextrema()[0]!=0:raise RuntimeError(f'{path.name} has no transparent background')
  dest=OUT/(path.stem+'.webp');image.thumbnail((640,640),Image.Resampling.LANCZOS);image.save(dest,quality=94,method=6)
 rows.append({'source':path.relative_to(ROOT).as_posix(),'runtime':dest.relative_to(ROOT).as_posix(),'source_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'runtime_sha256':hashlib.sha256(dest.read_bytes()).hexdigest(),'width':image.width,'height':image.height,'alpha':image.mode=='RGBA'})
(ROOT/'research/asset-manifest.json').write_text(json.dumps(rows,indent=2),encoding='utf-8')
icons=[p for p in sorted(OUT.glob('*.webp'))];board=Image.new('RGB',(1200,2*310),'#102b38');draw=ImageDraw.Draw(board)
for i,path in enumerate(icons):
 image=Image.open(path).convert('RGBA');image.thumbnail((210,255));board.paste(image,(i%5*240+(240-image.width)//2,i//5*310),image);draw.text((i%5*240+20,i//5*310+275),path.stem,fill='#f5e3b6')
board.save(ROOT/'design/icon-board.jpg',quality=95)
print('Prepared',len(rows),'assets; all 10 icons have genuine alpha.')
