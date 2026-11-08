"""Verify v11 originals, runtime assets, registry coverage and preservation."""
from pathlib import Path
from PIL import Image
import json,hashlib
ROOT=Path(__file__).resolve().parents[1];checks=[]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def read(p):return json.loads((ROOT/p).read_text(encoding='utf8'))
def js(p,name):return json.loads((ROOT/p).read_text(encoding='utf8').split('window.'+name+'=')[1].strip().rstrip(';'))
def check(name,ok):
    checks.append({'name':name,'passed':bool(ok)})
    if not ok:raise RuntimeError(name)
for name,digest in read('verification/yoru-before.json').items():
    if name!='verification/preview-stderr.log':check('Yoru static '+name,sha(ROOT.parent/'AnimeLauncher'/name)==digest)
for name,digest in read('revisions/v10/verification/source-hashes.json').items():check('exact v10 backup '+name,sha(ROOT/'revisions/v11/before'/name)==digest)
for row in read('research/asset-manifest.json'):
    check('original source '+row['source'],sha(ROOT/row['source'])==row['source_sha256']);check('original runtime '+row['runtime'],sha(ROOT/row['runtime'])==row['runtime_sha256'])
for version in ['v6','v7','v8','v9','v10']:
    for row in read('revisions/'+version+'/asset-manifest.json'):
        check(version+' source '+row['source'],sha(ROOT/row['source'])==row['source_sha256']);check(version+' runtime '+row['file'],sha(ROOT/row['file'])==row['sha256'])
for row in read('revisions/v5/audio-manifest.json'):check('audio retained '+row['file'],sha(ROOT/row['file'])==row['sha256'])
rows=read('revisions/v11/asset-manifest.json');check('6 wallpapers plus 20 icons',len(rows)==26 and sum(r['kind']=='icon' for r in rows)==20)
for row in rows:
    check('source '+row['kind']+' '+row['id'],sha(ROOT/row['source'])==row['source_sha256']);check('runtime '+row['id'],sha(ROOT/row['file'])==row['sha256']);im=Image.open(ROOT/row['file']);im.load();check('decodes '+row['id'],list(im.size)==row['size'])
    if row['kind']=='icon':
        check('640 square alpha '+row['id'],im.mode=='RGBA' and im.size==(640,640) and im.getchannel('A').getextrema()[0]==0);check('safe transparent corners '+row['id'],all(im.getpixel(p)[3]==0 for p in [(0,0),(639,0),(0,639),(639,639)]));box=im.getchannel('A').point(lambda a:255 if a>32 else 0).getbbox();check('safe content margins '+row['id'],box[0]>=38 and box[1]>=38 and box[2]<=602 and box[3]<=602)
icons=js('prototype/icons.js','KonohaIconSources');expected={r['id'] for r in rows if r['kind']=='icon'};check('registry covers 20 unique icons',set(icons)==expected and len(set(icons.values()))==20)
for row in rows:
    if row['kind']=='icon':check('icon versioned key '+row['id'],icons[row['id']]=='../'+row['file']+'?v='+row['sha256'][:12])
scenes=js('prototype/scenes.js','KonohaScenes');before=js('revisions/v11/before/prototype/scenes.js','KonohaScenes');changed={r['id'] for r in rows if r['kind']=='wallpaper'}
check('same twelve scene IDs',[s['id'] for s in scenes]==[s['id'] for s in before] and len(scenes)==12)
for old,new in zip(before,scenes):
    if new['id'] in changed:
        row=next(r for r in rows if r['kind']=='wallpaper' and r['id']==new['id']);check('new wallpaper key '+new['id'],new['image']=='v11/scene-'+new['id']+'.jpg?v='+row['sha256'][:12]);check('same hero role '+new['id'],new['hero']==old['hero'])
    else:check('other scene exact '+new['id'],new==old)
for p in (ROOT/'prototype').iterdir():
    if p.is_file() and p.name not in ['app.js','scenes.js','icons.js','index.html']:check('unrelated code exact '+p.name,sha(p)==sha(ROOT/'revisions/v11/before/prototype'/p.name))
prompts=read('revisions/v11/generation-manifest.json');check('26 complete separate built-in generations',len(prompts)==26 and all(p['prompt'] and p['skill']=='anime-tv-cel-art' and p['tool']=='built-in image_gen' for p in prompts));check('only actual anime refs used',all(set(p['refs'])<={'naruto-2.jpg','kakashi-board.jpg','sasuke-board.jpg','gaara-board.jpg'} for p in prompts))
out=ROOT/'revisions/v11/verification';(out/'offline.json').write_text(json.dumps({'passed':len(checks),'checks':checks},indent=2),encoding='utf8');(out/'source-hashes.json').write_text(json.dumps({p.relative_to(ROOT).as_posix():sha(p) for p in sorted((ROOT/'prototype').iterdir()) if p.is_file()},indent=2),encoding='utf8');print(f'{len(checks)} v11 integrity checks passed')
