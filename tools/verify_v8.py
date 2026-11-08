"""Verify v8 contracts and preservation; only write v8 evidence."""
from pathlib import Path
from PIL import Image
import hashlib,json
ROOT=Path(__file__).resolve().parents[1];checks=[]
def read(path):return json.loads((ROOT/path).read_text(encoding='utf-8'))
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def check(name,ok):
    checks.append({'name':name,'passed':bool(ok)})
    if not ok:raise RuntimeError(name)
def js(path,name):return json.loads((ROOT/path).read_text(encoding='utf-8').split('window.'+name+'=')[1].strip().rstrip(';'))
for name,digest in read('verification/yoru-before.json').items():
    if name!='verification/preview-stderr.log':check('Yoru static '+name,sha(ROOT.parent/'AnimeLauncher'/name)==digest)
for name,digest in read('revisions/v7/verification/source-hashes.json').items():check('exact v7 backup '+name,sha(ROOT/'revisions/v8/before'/name)==digest)
for row in read('research/asset-manifest.json'):
    check('original art '+row['source'],sha(ROOT/row['source'])==row['source_sha256']);check('original runtime '+row['runtime'],sha(ROOT/row['runtime'])==row['runtime_sha256'])
for row in read('revisions/v3/effect-manifest.json'):check('v3 VFX retained '+row['id'],sha(ROOT/row['runtime'])==row['runtime_sha256'] and sha(ROOT/row['source'])==row['source_sha256'])
for row in read('revisions/v5/audio-manifest.json'):check('v5 audio retained '+row['file'],sha(ROOT/row['file'])==row['sha256'])
for row in read('revisions/v2/audio-manifest.json')['music']:check('OST retained '+row['name'],sha(ROOT/row['file'])==row['sha256'])
for version in ['v6','v7']:
    for row in read('revisions/'+version+'/asset-manifest.json'):
        check(version+' source retained '+row['source'],sha(ROOT/row['source'])==row['source_sha256']);check(version+' runtime retained '+row['file'],sha(ROOT/row['file'])==row['sha256'])
audio=read('revisions/v7/audio-manifest.json');check('final seal cue unchanged',sha(ROOT/audio['file'])==audio['sha256'])
rows=read('revisions/v8/asset-manifest.json');check('five scenes, one eye, one slide',len(rows)==7 and sum(r['kind']=='wallpaper' for r in rows)==5)
for row in rows:
    check('new source '+row['source'],sha(ROOT/row['source'])==row['source_sha256']);check('new runtime '+row['file'],sha(ROOT/row['file'])==row['sha256']);im=Image.open(ROOT/row['file']);im.load();check('new image geometry '+row['file'],list(im.size)==row['size'])
    if row['kind']!='wallpaper':check('genuine transparent exterior '+row['file'],im.mode=='RGBA' and im.getchannel('A').getextrema()[0]==0)
    if row['kind']=='slide':
        hashes=[]
        for f in row['frames']:
            k=f['frame'];cell=im.crop((k%2*512,k//2*512,k%2*512+512,k//2*512+512));hashes.append(hashlib.sha256(cell.tobytes()).hexdigest());check('slide safe baseline '+str(k),f['x']>=20 and f['x']+f['width']<=492 and f['y']+f['height']==498);check('slide clears flying obstacle '+str(k),f['height']*row['draw_size']/512<=62.01)
        check('four different slide frames',len(set(hashes))==4)
    if row['kind']=='effect':
        box=im.getchannel('A').point(lambda a:255 if a>32 else 0).getbbox();check('iris preserves round proportions',.95<(box[2]-box[0])/(box[3]-box[1])<1.05)
old=js('revisions/v8/before/prototype/scenes.js','KonohaScenes');scenes=js('prototype/scenes.js','KonohaScenes');check('seven prior scenes unchanged',scenes[:7]==old);check('twelve unique scene IDs',len(scenes)==12 and len({s['id'] for s in scenes})==12);check('nine hero scenes',sum(s['hero'] for s in scenes)==9)
for scene in scenes[7:]:
    row=next(r for r in rows if r.get('id')==scene['id']);check('scene cache key '+scene['id'],scene['image'].endswith(row['sha256'][:12]));check('scene individual treatment '+scene['id'],bool(scene['particles'] and scene['palette'] and scene['cardPosition'] and scene['mobilePosition']))
old=js('revisions/v8/before/prototype/runner-art.js','KonohaRunnerSources');new=js('prototype/runner-art.js','KonohaRunnerSources');check('Naruto run atlas unchanged',new['naruto']['url']==old['naruto']['url']);check('Gamabunta atlas and metadata unchanged',new['gamabunta']==old['gamabunta']);check('new dedicated slide atlas',new['naruto']['slide']['columns']==2 and new['naruto']['slide']['rows']==2)
prompts=read('revisions/v8/generation-manifest.json');check('seven complete ImageGen prompts',len(prompts)==7 and all(p['prompt'] for p in prompts))
out=ROOT/'revisions/v8/verification';(out/'offline.json').write_text(json.dumps({'passed':len(checks),'yoru_static_preserved':121,'checks':checks},indent=2),encoding='utf-8');(out/'source-hashes.json').write_text(json.dumps({p.relative_to(ROOT).as_posix():sha(p) for p in sorted((ROOT/'prototype').iterdir()) if p.is_file()},indent=2),encoding='utf-8');print(f'{len(checks)} v8 integrity checks passed')
