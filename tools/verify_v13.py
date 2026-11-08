"""Verify the v13 art correction and preservation of everything outside its scope."""
from pathlib import Path
from PIL import Image
import json,hashlib
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v13';checks=[]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def read(p):return json.loads((ROOT/p).read_text(encoding='utf8'))
def js(p):return json.loads(p.read_text(encoding='utf8').split('window.KonohaScenes=')[1].strip().rstrip(';'))
def check(n,ok):
 checks.append({'name':n,'passed':bool(ok)})
 if not ok:raise RuntimeError(n)
for name,digest in read('verification/yoru-before.json').items():
 if name!='verification/preview-stderr.log':check('Yoru static '+name,sha(ROOT.parent/'AnimeLauncher'/name)==digest)
for name,digest in read('revisions/v12/verification/source-hashes.json').items():check('exact v12 backup '+name,sha(OUT/'before'/name)==digest)
for version in ['v6','v7','v8','v9','v10','v11','v12']:
 for r in read('revisions/'+version+'/asset-manifest.json'):
  check(version+' original '+r['source'],sha(ROOT/r['source'])==r['source_sha256']);check(version+' runtime '+r['file'],sha(ROOT/r['file'])==r['sha256'])
for r in read('research/asset-manifest.json'):
 check('initial original '+r['source'],sha(ROOT/r['source'])==r['source_sha256']);check('initial runtime '+r['runtime'],sha(ROOT/r['runtime'])==r['runtime_sha256'])
for r in read('revisions/v5/audio-manifest.json'):check('audio retained '+r['file'],sha(ROOT/r['file'])==r['sha256'])
rows=read('revisions/v13/asset-manifest.json');ids={'sai','jiraiya','minato'};check('three targeted edits',len(rows)==3 and {r['id'] for r in rows}==ids)
for r in rows:
 check('new original '+r['id'],sha(ROOT/r['source'])==r['source_sha256']);check('new runtime '+r['id'],sha(ROOT/r['file'])==r['sha256']);im=Image.open(ROOT/r['file']);im.load();check('Full HD decode '+r['id'],im.size==(1920,1080))
scenes=js(ROOT/'prototype/scenes.js');old=js(OUT/'before/prototype/scenes.js');check('same 17 scene IDs',[s['id'] for s in scenes]==[s['id'] for s in old])
for a,b in zip(old,scenes):
 if b['id'] not in ids:check('other scene exact '+b['id'],a==b)
 else:
  check('edited scene metadata retained '+b['id'],{k:v for k,v in a.items() if k not in ['image','mobilePosition']}=={k:v for k,v in b.items() if k not in ['image','mobilePosition']});r=next(r for r in rows if r['id']==b['id']);check('versioned path '+b['id'],b['image']=='v13/scene-'+b['id']+'.jpg?v='+r['sha256'][:12])
for p in (ROOT/'prototype').iterdir():
 if p.is_file() and p.name not in ['scenes.js','index.html','revision13.css']:check('unrelated code exact '+p.name,sha(p)==sha(OUT/'before/prototype'/p.name))
old_index=(OUT/'before/prototype/index.html').read_text(encoding='utf8');expected=old_index.replace('scenes.js?v=15','scenes.js?v=16').replace('<script src="icons.js?v=14"','<link rel="stylesheet" href="revision13.css?v=16"><script src="icons.js?v=14"')
check('only v13 style and scene cache keys in index',(ROOT/'prototype/index.html').read_text(encoding='utf8')==expected)
prompts=read('revisions/v13/generation-manifest.json');check('three separate built-in edits',len(prompts)==3 and all(p['tool']=='built-in image_gen' and p['skill']=='anime-tv-cel-art' and p['refs']==['assets/generated/v12/wallpaper-'+p['id']+'.png','revisions/v13/references/'+p['id']+'-board.jpg'] for p in prompts))
for r in read('revisions/v13/references/sources.json'):check('actual anime reference '+r['file'],sha(OUT/'references'/r['file'])==r['sha256'])
(OUT/'verification/offline.json').write_text(json.dumps({'passed':len(checks),'checks':checks},indent=2),encoding='utf8');(OUT/'verification/source-hashes.json').write_text(json.dumps({p.relative_to(ROOT).as_posix():sha(p) for p in sorted((ROOT/'prototype').iterdir()) if p.is_file()},indent=2),encoding='utf8');print(f'{len(checks)} v13 integrity checks passed')
