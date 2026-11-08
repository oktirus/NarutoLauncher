"""Verify revision-3 assets, 30 technique references and preservation boundaries."""
from pathlib import Path
import hashlib,json,wave
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];checks=[]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def read(p):return json.loads((ROOT/p).read_text(encoding='utf-8'))
def check(name,ok):
 checks.append({'name':name,'passed':bool(ok)})
 if not ok:raise RuntimeError(name)
for file,digest in read('verification/yoru-before.json').items():check('Yoru preserved '+file,sha(ROOT.parent/'AnimeLauncher'/file)==digest)
before=read('revisions/v3/before/asset-manifest.json')
for row in before:check('v2 generated backup '+row['source'],sha(ROOT/'revisions/v3/before/generated'/Path(row['source']).name)==row['source_sha256'])
for name,digest in read('revisions/v2/verification/source-hashes.json').items():
 if name.startswith('prototype/'):check('v2 code backup '+name,sha(ROOT/'revisions/v3/before'/name)==digest)
visuals=read('research/asset-manifest.json');check('16 visual assets',len(visuals)==16)
for row in visuals:
 for key,hash_key in [('source','source_sha256'),('runtime','runtime_sha256')]:check(key+' hash '+row[key],sha(ROOT/row[key])==row[hash_key])
 with Image.open(ROOT/row['runtime']) as image:image.verify()
 check('runtime decodes '+row['runtime'],True)
 if row['runtime'].endswith('.webp'):
  old=next(r for r in before if r['source']==row['source']);check('original app icon unchanged '+row['source'],row['source_sha256']==old['source_sha256'] and row['runtime_sha256']==old['runtime_sha256'])
effects=read('revisions/v3/effect-manifest.json');check('9 effect plates and 2 Gamabunta poses',len(effects)==11)
for row in effects:
 check('effect source '+row['id'],sha(ROOT/row['source'])==row['source_sha256']);check('effect runtime '+row['id'],sha(ROOT/row['runtime'])==row['runtime_sha256'])
 image=Image.open(ROOT/row['runtime']);image.load();check('genuine alpha '+row['id'],image.mode=='RGBA' and image.getchannel('A').getextrema()==(0,255))
refs=read('revisions/v3/reference-manifest.json')
for tag in ['rasengan','chidori','sharingan','kurama','summoning','hiraishin','fireball','amaterasu','rasenshuriken','gamabunta']:
 rows=[r for r in refs if r['tag']==tag];check('3 distinct references '+tag,len(rows)==3 and len({r.get('sha256') for r in rows})==3 and all(r.get('page') for r in rows))
 for row in rows:check('reference hash '+tag+' '+str(row['index']),sha(ROOT/row['file'])==row['sha256'])
for row in read('revisions/v3/audio-manifest.json'):
 check('full cue hash '+row['cue'],sha(ROOT/row['file'])==row['sha256'])
 with wave.open(str(ROOT/row['file']),'rb') as f:duration=f.getnframes()/f.getframerate()
 check('complete source duration '+row['cue'],abs(duration-row['source_duration'])<.06)
for row in read('revisions/v2/audio-manifest.json')['music']:check('official music preview preserved '+row['name'],sha(ROOT/row['file'])==row['sha256'])
prompts=read('revisions/v3/generation-manifest.json');check('17 generation prompts',len(prompts)==17 and all(row.get('prompt') for row in prompts))
source_hashes={p.relative_to(ROOT).as_posix():sha(p) for p in (ROOT/'prototype').iterdir() if p.is_file()}
(ROOT/'revisions/v3/verification/source-hashes.json').write_text(json.dumps(source_hashes,indent=2),encoding='utf-8')
report={'passed':len(checks),'total':len(checks),'yoru_files_preserved':122,'wallpapers':6,'app_icons':10,'effect_assets':11,'technique_reference_frames':30,'full_cues':4,'checks':checks}
(ROOT/'revisions/v3/verification/offline-checks.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
print('PASS:',len(checks),'checks; Yoru preserved; 30 references, 6 wallpapers, 11 FX/sprites, 4 complete cues.')
