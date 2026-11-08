"""Preserve generated alpha; convert only size/format and build an inspection board."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import hashlib,json
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'assets/effects';rows=[]
for source in sorted((OUT/'generated').glob('*.png')):
 image=Image.open(source);image.load()
 if image.mode!='RGBA' or image.getchannel('A').getextrema()[0]!=0:raise RuntimeError('No genuine transparency: '+source.name)
 dest=OUT/(source.stem+'.webp');image.thumbnail((768,768),Image.Resampling.LANCZOS);image.save(dest,quality=95,method=4)
 rows.append({'id':source.stem,'source':source.relative_to(ROOT).as_posix(),'runtime':dest.relative_to(ROOT).as_posix(),'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'runtime_sha256':hashlib.sha256(dest.read_bytes()).hexdigest(),'width':image.width,'height':image.height,'alpha':True})
(ROOT/'revisions/v3/effect-manifest.json').write_text(json.dumps(rows,indent=2),encoding='utf-8')
sources={row['id']:'../'+row['runtime']+'?v='+row['runtime_sha256'][:12] for row in rows}
(ROOT/'prototype/effects.js').write_text("'use strict';\nwindow.KonohaEffectSources="+json.dumps(sources,indent=2)+";\n",encoding='utf-8')
effects=[r for r in rows if not r['id'].startswith('gamabunta-')];board=Image.new('RGB',(1200,1260),'#102b38');draw=ImageDraw.Draw(board)
for i,row in enumerate(effects):
 image=Image.open(ROOT/row['runtime']);image.thumbnail((370,370));board.paste(image,(i%3*400+(400-image.width)//2,i//3*420),image)
 draw.text((i%3*400+24,i//3*420+390),row['id'],fill='#efd8ae')
board.save(ROOT/'revisions/v3/effect-board.jpg',quality=95)
print('Prepared',len(rows),'genuine-alpha effect/sprite assets.')
