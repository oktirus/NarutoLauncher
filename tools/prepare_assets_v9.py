"""Encode v9 images and normalize slide cells, preserving previous painted assets."""
from pathlib import Path
from PIL import Image, ImageOps
import hashlib, json

ROOT = Path(__file__).resolve().parents[1]
rows = []

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def js(path, name):
    return json.loads(path.read_text(encoding='utf-8').split('window.'+name+'=')[1].strip().rstrip(';'))

def write_js(path, name, data):
    path.write_text("'use strict';\nwindow."+name+'='+json.dumps(data, ensure_ascii=False, indent=2)+';\n', encoding='utf-8')

for name in ['hinata', 'shikamaru', 'rocklee', 'neji', 'choji']:
    src = ROOT/'assets/generated/v9'/(name+'.png')
    im = Image.open(src).convert('RGB')
    original = list(im.size)
    im = ImageOps.fit(im, (1920, 1080), method=Image.Resampling.LANCZOS)
    dest = ROOT/'assets/runtime/v9'/(name+'.jpg')
    im.save(dest, quality=95, subsampling=0)
    rows.append({'id':name, 'kind':'wallpaper', 'source':src.relative_to(ROOT).as_posix(), 'source_sha256':sha(src), 'source_size':original, 'file':dest.relative_to(ROOT).as_posix(), 'sha256':sha(dest), 'size':list(im.size)})

src = ROOT/'assets/generated/v9/gamabunta-slide.png'
im = Image.open(src).convert('RGBA')
assert im.getchannel('A').getextrema()[0] == 0
cells = []
for k in range(4):
    cell = im.crop((round(k%2*im.width/2), round(k//2*im.height/2), round((k%2+1)*im.width/2), round((k//2+1)*im.height/2)))
    box = cell.getchannel('A').point(lambda a:255 if a>32 else 0).getbbox()
    assert box
    cells.append((cell, box))
scale = min(464/max(b[2]-b[0] for _, b in cells), 464/max(b[3]-b[1] for _, b in cells))
sheet = Image.new('RGBA', (1024, 1024))
frames = []
for k, (cell, box) in enumerate(cells):
    art = cell.crop(box)
    art = art.resize((round(art.width*scale), round(art.height*scale)), Image.Resampling.LANCZOS)
    x, y = (512-art.width)//2, 498-art.height
    sheet.alpha_composite(art, (k%2*512+x, k//2*512+y))
    frames.append({'frame':k, 'source_box':box, 'x':x, 'y':y, 'width':art.width, 'height':art.height})
draw_size = round(min(160, 62*512/max(f['height'] for f in frames)), 3)
dest = ROOT/'assets/game/v9/gamabunta-slide.webp'
sheet.save(dest, quality=96, method=4)
row = {'kind':'slide', 'source':src.relative_to(ROOT).as_posix(), 'source_sha256':sha(src), 'file':dest.relative_to(ROOT).as_posix(), 'sha256':sha(dest), 'source_size':list(im.size), 'size':list(sheet.size), 'grid':[2,2], 'cell':512, 'baseline':498, 'draw_size':draw_size, 'uniform_scale':scale, 'frames':frames}
rows.append(row)
path = ROOT/'prototype/runner-art.js'
data = js(path, 'KonohaRunnerSources')
data['gamabunta']['slide'] = {'url':'../'+row['file']+'?v='+row['sha256'][:12], 'columns':2, 'rows':2, 'cell':512, 'baseline':498, 'drawSize':draw_size}
write_js(path, 'KonohaRunnerSources', data)

path = ROOT/'prototype/scenes.js'
scenes = js(path, 'KonohaScenes')
for scene in scenes:
    row = next((r for r in rows if r.get('id') == scene['id']), None)
    if row:
        scene['image'] = 'v9/'+scene['id']+'.jpg?v='+row['sha256'][:12]
write_js(path, 'KonohaScenes', scenes)
path = ROOT/'prototype/index.html'
path.write_text(path.read_text(encoding='utf-8').replace('?v=11', '?v=12'), encoding='utf-8')
(ROOT/'revisions/v9/asset-manifest.json').write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps([{'file':r['file'], 'size':r['size'], 'draw_size':r.get('draw_size')} for r in rows]))
