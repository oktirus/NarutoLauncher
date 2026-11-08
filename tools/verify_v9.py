"""Focused v9 integrity and preservation evidence, never rewriting prior reports."""
from pathlib import Path
from PIL import Image
import hashlib, json
ROOT=Path(__file__).resolve().parents[1];checks=[]
def read(p):return json.loads((ROOT/p).read_text(encoding='utf-8-sig'))
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def check(name,ok):
    checks.append({'name':name,'passed':bool(ok)})
    if not ok:raise RuntimeError(name)
def js(p,name):return json.loads((ROOT/p).read_text(encoding='utf-8').split('window.'+name+'=')[1].strip().rstrip(';'))
for name,digest in read('verification/yoru-before.json').items():
    if name!='verification/preview-stderr.log':check('Yoru static '+name,sha(ROOT.parent/'AnimeLauncher'/name)==digest)
for name,digest in read('revisions/v8/verification/source-hashes.json').items():check('exact v8 backup '+name,sha(ROOT/'revisions/v9/before'/name)==digest)
for version in ['v6','v7','v8']:
    for row in read('revisions/'+version+'/asset-manifest.json'):
        check(version+' source preserved '+row['source'],sha(ROOT/row['source'])==row['source_sha256']);check(version+' runtime preserved '+row['file'],sha(ROOT/row['file'])==row['sha256'])
for row in read('revisions/v5/audio-manifest.json'):check('audio preserved '+row['file'],sha(ROOT/row['file'])==row['sha256'])
rows=read('revisions/v9/asset-manifest.json');check('six generated assets',len(rows)==6)
for row in rows:
    check('new source '+row['source'],sha(ROOT/row['source'])==row['source_sha256']);check('new runtime '+row['file'],sha(ROOT/row['file'])==row['sha256']);im=Image.open(ROOT/row['file']);im.load();check('image decodes '+row['file'],list(im.size)==row['size'])
    if row['kind']=='slide':
        check('slide alpha exterior',im.mode=='RGBA' and im.getchannel('A').getextrema()[0]==0);hashes=[]
        for f in row['frames']:
            k=f['frame'];cell=im.crop((k%2*512,k//2*512,k%2*512+512,k//2*512+512));hashes.append(hashlib.sha256(cell.tobytes()).hexdigest());check('slide baseline '+str(k),f['x']>=20 and f['x']+f['width']<=492 and f['y']+f['height']==498);check('slide art clears shuriken '+str(k),f['height']*row['draw_size']/512<=62.01)
        check('four distinct slide phases',len(set(hashes))==4)
old=js('revisions/v9/before/prototype/scenes.js','KonohaScenes');new=js('prototype/scenes.js','KonohaScenes');check('same twelve IDs',len(new)==12 and [s['id'] for s in new]==[s['id'] for s in old]);check('first seven scenes unchanged',new[:7]==old[:7])
for a,b in zip(old[7:],new[7:]):
    check('only image changed '+a['id'],{k:v for k,v in a.items() if k!='image'}=={k:v for k,v in b.items() if k!='image'});row=next(r for r in rows if r.get('id')==b['id']);check('new scene cache key '+b['id'],b['image']=='v9/'+b['id']+'.jpg?v='+row['sha256'][:12])
old=js('revisions/v9/before/prototype/runner-art.js','KonohaRunnerSources');new=js('prototype/runner-art.js','KonohaRunnerSources');check('Naruto atlas and slide exact',old['naruto']==new['naruto']);check('Gamabunta run exact',{k:v for k,v in new['gamabunta'].items() if k!='slide'}==old['gamabunta']);check('Gamabunta new dedicated slide',new['gamabunta']['slide']['url'].startswith('../assets/game/v9/'))
old=(ROOT/'revisions/v9/before/prototype/runner.js').read_text(encoding='utf8');new=(ROOT/'prototype/runner.js').read_text(encoding='utf8');check('physics Run class unchanged',old.split(' class Run{')[1].split(' let session=null;')[0]==new.split(' class Run{')[1].split(' let session=null;')[0])
for p in (ROOT/'prototype').iterdir():
    if p.is_file() and p.name not in ['runner.js','runner-art.js','scenes.js','index.html']:check('unrelated prototype exact '+p.name,sha(p)==sha(ROOT/'revisions/v9/before/prototype'/p.name))
prompts=read('revisions/v9/generation-manifest.json');check('six built-in generation prompts',len(prompts)==6 and all(p['prompt'] and p['tool']=='built-in image_gen' for p in prompts))
out=ROOT/'revisions/v9/verification';(out/'offline.json').write_text(json.dumps({'passed':len(checks),'checks':checks},indent=2),encoding='utf8');(out/'source-hashes.json').write_text(json.dumps({p.relative_to(ROOT).as_posix():sha(p) for p in sorted((ROOT/'prototype').iterdir()) if p.is_file()},indent=2),encoding='utf8');print(f'{len(checks)} v9 integrity checks passed')
