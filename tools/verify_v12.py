"""Verify new v12 art, old resources and exact preservation outside scene integration."""
from pathlib import Path
from PIL import Image
import hashlib,json
ROOT=Path(__file__).resolve().parents[1];checks=[];OUT=ROOT/'revisions/v12'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def read(p):return json.loads((ROOT/p).read_text(encoding='utf8'))
def js(p):return json.loads(p.read_text(encoding='utf8').split('window.KonohaScenes=')[1].strip().rstrip(';'))
def check(name,ok):
 checks.append({'name':name,'passed':bool(ok)})
 if not ok:raise RuntimeError(name)
for name,digest in read('verification/yoru-before.json').items():
 if name!='verification/preview-stderr.log':check('Yoru static '+name,sha(ROOT.parent/'AnimeLauncher'/name)==digest)
for name,digest in read('revisions/v11/verification/source-hashes.json').items():check('exact v11 backup '+name,sha(OUT/'before'/name)==digest)
for version in ['v6','v7','v8','v9','v10','v11']:
 for row in read('revisions/'+version+'/asset-manifest.json'):
  check(version+' source '+row['source'],sha(ROOT/row['source'])==row['source_sha256']);check(version+' runtime '+row['file'],sha(ROOT/row['file'])==row['sha256'])
for row in read('research/asset-manifest.json'):
 check('initial source '+row['source'],sha(ROOT/row['source'])==row['source_sha256']);check('initial runtime '+row['runtime'],sha(ROOT/row['runtime'])==row['runtime_sha256'])
for row in read('revisions/v5/audio-manifest.json'):check('audio '+row['file'],sha(ROOT/row['file'])==row['sha256'])
rows=read('revisions/v12/asset-manifest.json');ids={'gaara','jiraiya','itachi','tsunade','minato','sai'};check('six distinct wallpapers',len(rows)==6 and {r['id'] for r in rows}==ids)
for row in rows:
 check('new original '+row['id'],sha(ROOT/row['source'])==row['source_sha256']);check('new runtime '+row['id'],sha(ROOT/row['file'])==row['sha256']);im=Image.open(ROOT/row['file']);im.load();check('Full HD decodes '+row['id'],im.size==(1920,1080))
scenes=js(ROOT/'prototype/scenes.js');old=js(OUT/'before/prototype/scenes.js');check('17 unique scene IDs',len(scenes)==17 and len({s['id'] for s in scenes})==17);check('14 hero scenes',sum(s['hero'] for s in scenes)==14)
for s in old:
 if s['id']!='gaara':check('old scene retained '+s['id'],s==next(n for n in scenes if n['id']==s['id']))
for row in rows:
 s=next(s for s in scenes if s['id']==row['id']);check('versioned scene key '+row['id'],s['image']=='v12/scene-'+row['id']+'.jpg?v='+row['sha256'][:12])
for p in (ROOT/'prototype').iterdir():
 if p.is_file() and p.name not in ['scenes.js','app.js','index.html']:check('unrelated code exact '+p.name,sha(p)==sha(OUT/'before/prototype'/p.name))
before=(OUT/'before/prototype/app.js').read_text(encoding='utf8');expected=before.replace('Три настрої села й дев’ять шпалер героїв.',"Три настрої Наруто й '+scenes.filter(s=>s.hero).length+' шпалер героїв.")
check('only scene count text changed in app.js',(ROOT/'prototype/app.js').read_text(encoding='utf8')==expected)
prompts=read('revisions/v12/generation-manifest.json');check('six separate built-in generations',len(prompts)==6 and all(p['tool']=='built-in image_gen' and p['skill']=='anime-tv-cel-art' and p['ref']==p['id']+'-board.jpg' for p in prompts))
for r in read('revisions/v12/references/sources.json'):check('actual anime reference hash '+r['file'],sha(OUT/'references'/r['file'])==r['sha256'])
(OUT/'verification').mkdir(exist_ok=True);(OUT/'verification/offline.json').write_text(json.dumps({'passed':len(checks),'checks':checks},indent=2),encoding='utf8')
(OUT/'verification/source-hashes.json').write_text(json.dumps({p.relative_to(ROOT).as_posix():sha(p) for p in sorted((ROOT/'prototype').iterdir()) if p.is_file()},indent=2),encoding='utf8')
print(f'{len(checks)} v12 integrity checks passed')
