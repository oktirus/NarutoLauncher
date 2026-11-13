from pathlib import Path
from PIL import Image, ImageOps, ImageDraw
import hashlib, json, shutil

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'revisions/v22'
OUT.mkdir(exist_ok=True)
if not (OUT/'before').exists():
    shutil.copytree(ROOT/'prototype',OUT/'before/prototype')
    shutil.copy2(ROOT/'README.md',OUT/'before/README.md')
    (OUT/'before-hashes.json').write_text(json.dumps({p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for group in ['prototype','assets'] for p in (ROOT/group).rglob('*') if p.is_file()},indent=2),encoding='utf8')
for folder in ['references','verification']:(OUT/folder).mkdir(exist_ok=True)
for folder in ['assets/generated/v22','assets/runtime/v22']:(ROOT/folder).mkdir(exist_ok=True)
rows=[]
for version,files in [('v18',['training-team.jpg','training-waterfall.jpg','training-blades.jpg']),('v16',['sakura-1.jpg','naruto-1.jpg','naruto-2.jpg']),('v15',['sakura-1.jpg','sakura-2.jpg','sakura-3.jpg','sakura-4.jpg'])]:
    registry=json.loads((ROOT/f'revisions/{version}/references/sources.json').read_text(encoding='utf8'))
    for file in files:
        source=ROOT/f'revisions/{version}/references/{file}'
        if not source.exists():continue
        target=OUT/f'references/{version}-{file}';shutil.copy2(source,target)
        provenance=next((r for r in registry if r.get('file','').endswith('/'+file)),{})
        rows.append(dict(file=target.relative_to(ROOT).as_posix(),copied_from=source.relative_to(ROOT).as_posix(),provenance=provenance,sha256=hashlib.sha256(target.read_bytes()).hexdigest()))
source=ROOT/'revisions/v21/references/cel-density-approved.jpg';target=OUT/'references/cel-density-approved.jpg';shutil.copy2(source,target)
rows.append(dict(file=target.relative_to(ROOT).as_posix(),copied_from=source.relative_to(ROOT).as_posix(),role='Approved generated drawing density only; not an anime character reference',sha256=hashlib.sha256(target.read_bytes()).hexdigest()))
for tag in ['sakura','training']:
    selected=[r for r in rows if tag in Path(r['file']).name]
    board=Image.new('RGB',(1500,320*((len(selected)+2)//3)),'#142d38');draw=ImageDraw.Draw(board)
    for i,row in enumerate(selected):
        im=ImageOps.contain(Image.open(ROOT/row['file']).convert('RGB'),(490,280));x=i%3*500;y=i//3*320
        board.paste(im,(x+(500-im.width)//2,y+(280-im.height)//2));draw.text((x+8,y+294),Path(row['file']).name,fill='white')
    board.save(OUT/f'references/{tag}-board.jpg',quality=95)
(OUT/'references/sources.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8')
print('v21 frozen; v22 reference provenance saved')
