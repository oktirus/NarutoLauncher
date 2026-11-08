"""Verify one artwork repair and the exact scope of v14 UI fixes."""
from pathlib import Path
from PIL import Image
import json,hashlib
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v14';checks=[]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def read(p):return json.loads((ROOT/p).read_text(encoding='utf8'))
def js(p):return json.loads(p.read_text(encoding='utf8').split('window.KonohaScenes=')[1].strip().rstrip(';'))
def check(n,ok):
 checks.append({'name':n,'passed':bool(ok)})
 if not ok:raise RuntimeError(n)
for n,h in read('verification/yoru-before.json').items():
 if n!='verification/preview-stderr.log':check('Yoru static '+n,sha(ROOT.parent/'AnimeLauncher'/n)==h)
for n,h in read('revisions/v13/verification/source-hashes.json').items():check('exact v13 backup '+n,sha(OUT/'before'/n)==h)
for v in ['v6','v7','v8','v9','v10','v11','v12','v13']:
 for r in read('revisions/'+v+'/asset-manifest.json'):
  check(v+' original '+r['source'],sha(ROOT/r['source'])==r['source_sha256']);check(v+' runtime '+r['file'],sha(ROOT/r['file'])==r['sha256'])
for r in read('research/asset-manifest.json'):
 check('initial original '+r['source'],sha(ROOT/r['source'])==r['source_sha256']);check('initial runtime '+r['runtime'],sha(ROOT/r['runtime'])==r['runtime_sha256'])
for r in read('revisions/v5/audio-manifest.json'):check('audio retained '+r['file'],sha(ROOT/r['file'])==r['sha256'])
rows=read('revisions/v14/asset-manifest.json');check('only Sai art edited',len(rows)==1 and rows[0]['id']=='sai')
for r in rows:
 check('new original',sha(ROOT/r['source'])==r['source_sha256']);check('new runtime',sha(ROOT/r['file'])==r['sha256']);im=Image.open(ROOT/r['file']);im.load();check('Full HD decode',im.size==(1920,1080))
a=Image.open(ROOT/'assets/generated/v13/wallpaper-sai.png').convert('RGB');b=Image.open(ROOT/rows[0]['source']).convert('RGB')
for p in [(1250,430),(1240,500)]:check('former wall patch is dark sleeve at '+str(p),min(a.getpixel(p))>170 and max(b.getpixel(p))<120)
scenes=js(ROOT/'prototype/scenes.js');old=js(OUT/'before/prototype/scenes.js');check('same 17 scene IDs',[s['id'] for s in scenes]==[s['id'] for s in old])
for a,b in zip(old,scenes):
 if b['id']!='sai':check('other scene exact '+b['id'],a==b)
 else:
  check('Sai metadata retained',{k:v for k,v in a.items() if k!='image'}=={k:v for k,v in b.items() if k!='image'});check('Sai versioned path',b['image']=='v14/scene-sai.jpg?v='+rows[0]['sha256'][:12])
for p in (ROOT/'prototype').iterdir():
 if p.is_file() and p.name not in ['scenes.js','app.js','index.html','revision13.css']:check('unrelated code exact '+p.name,sha(p)==sha(OUT/'before/prototype'/p.name))
old_app=(OUT/'before/prototype/app.js').read_text(encoding='utf8');expected=old_app.replace(" $('.audio-invitation').setAttribute('aria-pressed',String(enabled));"," const invitation=$('.audio-invitation');\n invitation.setAttribute('aria-pressed',String(enabled));\n invitation.setAttribute('aria-label',$('#audio-label').textContent);")
check('only dynamic audio accessible label added',(ROOT/'prototype/app.js').read_text(encoding='utf8')==expected)
expected=(OUT/'before/prototype/index.html').read_text(encoding='utf8').replace('scenes.js?v=16','scenes.js?v=17').replace('app.js?v=15','app.js?v=17').replace('revision13.css?v=16','revision13.css?v=17');check('only changed resource cache keys in index',(ROOT/'prototype/index.html').read_text(encoding='utf8')==expected)
check('anime board exact copy',sha(OUT/'references/sai-board.jpg')==sha(ROOT/'revisions/v13/references/sai-board.jpg'))
p=read('revisions/v14/generation-manifest.json');check('one targeted built-in edit',len(p)==1 and p[0]['tool']=='built-in image_gen' and p[0]['skill']=='anime-tv-cel-art')
(OUT/'verification').mkdir(exist_ok=True);(OUT/'verification/offline.json').write_text(json.dumps({'passed':len(checks),'checks':checks},indent=2),encoding='utf8');(OUT/'verification/source-hashes.json').write_text(json.dumps({p.relative_to(ROOT).as_posix():sha(p) for p in sorted((ROOT/'prototype').iterdir()) if p.is_file()},indent=2),encoding='utf8');print(f'{len(checks)} v14 integrity checks passed')
