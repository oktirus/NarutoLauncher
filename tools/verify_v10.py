"""Verify two new images and preservation of previous launcher resources."""
from pathlib import Path
from PIL import Image
import hashlib,json
ROOT=Path(__file__).resolve().parents[1];checks=[]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def read(p):return json.loads((ROOT/p).read_text(encoding='utf8'))
def js(p,name):return json.loads((ROOT/p).read_text(encoding='utf8').split('window.'+name+'=')[1].strip().rstrip(';'))
def check(name,ok):
    checks.append({'name':name,'passed':bool(ok)})
    if not ok:raise RuntimeError(name)
for name,digest in read('verification/yoru-before.json').items():
    if name!='verification/preview-stderr.log':check('Yoru static '+name,sha(ROOT.parent/'AnimeLauncher'/name)==digest)
for name,digest in read('revisions/v9/verification/source-hashes.json').items():check('exact v9 backup '+name,sha(ROOT/'revisions/v10/before'/name)==digest)
for row in read('research/asset-manifest.json'):
    check('original source '+row['source'],sha(ROOT/row['source'])==row['source_sha256']);check('original runtime '+row['runtime'],sha(ROOT/row['runtime'])==row['runtime_sha256'])
for version in ['v6','v7','v8','v9']:
    for row in read('revisions/'+version+'/asset-manifest.json'):
        check(version+' source preserved '+row['source'],sha(ROOT/row['source'])==row['source_sha256']);check(version+' runtime preserved '+row['file'],sha(ROOT/row['file'])==row['sha256'])
rows=read('revisions/v10/asset-manifest.json');check('exactly two new paintings',len(rows)==2)
for row in rows:
    check('new source '+row['id'],sha(ROOT/row['source'])==row['source_sha256']);check('new runtime '+row['id'],sha(ROOT/row['file'])==row['sha256']);im=Image.open(ROOT/row['file']);im.load();check('runtime size '+row['id'],list(im.size)==row['size'])
    if row['alpha']:
        check('real alpha exterior',im.mode=='RGBA' and im.getchannel('A').getextrema()[0]==0);check('icon has solid center',im.getpixel((320,320))[3]>245)
old=js('revisions/v10/before/prototype/scenes.js','KonohaScenes');new=js('prototype/scenes.js','KonohaScenes');check('other eleven scenes exact',new[1:]==old[1:]);check('dusk copy unchanged',{k:v for k,v in new[0].items() if k not in ['image','mobilePosition']}=={k:v for k,v in old[0].items() if k not in ['image','mobilePosition']});check('dusk versioned URL',new[0]['image'].endswith(rows[0]['sha256'][:12]))
for p in (ROOT/'prototype').iterdir():
    if p.is_file() and p.name not in ['app.js','scenes.js','index.html']:check('unrelated code exact '+p.name,sha(p)==sha(ROOT/'revisions/v10/before/prototype'/p.name))
prompts=read('revisions/v10/generation-manifest.json');check('two skill-guided built-in prompts',len(prompts)==2 and all(r['skill']=='anime-tv-cel-art' and r['prompt'] and r['tool']=='built-in image_gen' for r in prompts))
out=ROOT/'revisions/v10/verification';(out/'offline.json').write_text(json.dumps({'passed':len(checks),'checks':checks},indent=2),encoding='utf8');(out/'source-hashes.json').write_text(json.dumps({p.relative_to(ROOT).as_posix():sha(p) for p in sorted((ROOT/'prototype').iterdir()) if p.is_file()},indent=2),encoding='utf8');print(f'{len(checks)} integrity checks passed')
