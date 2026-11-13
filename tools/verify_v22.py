from pathlib import Path
from PIL import Image
import hashlib,json,re,subprocess
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v22';checks=[]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def check(name,ok):
    checks.append(dict(name=name,passed=bool(ok)))
    assert ok,name
def scenes(p):return json.loads(p.read_text(encoding='utf8').split('window.KonohaScenes=')[1].strip().rstrip(';'))
for path,hash in json.loads((OUT/'before-hashes.json').read_text()).items():
    if path not in {'prototype/index.html','prototype/scenes.js'}:check('preserved '+path,sha(ROOT/path)==hash)
before=scenes(OUT/'before/prototype/scenes.js');after=scenes(ROOT/'prototype/scenes.js')
check('18 unchanged scene ids',[s['id'] for s in before]==[s['id'] for s in after] and len(after)==18)
allowed={'image','mood','description','mobilePosition','cardPosition'}
for old,new in zip(before,after):
    if old['id'] in {'night','sakura'}:
        check('only allowed art fields '+old['id'],{k:v for k,v in old.items() if k not in allowed}=={k:v for k,v in new.items() if k not in allowed})
    else:check('exact scene '+old['id'],old==new)
for row in json.loads((OUT/'asset-manifest.json').read_text()):
    check('source hash '+row['id'],sha(ROOT/row['source'])==row['source_sha256'])
    check('runtime hash '+row['id'],sha(ROOT/row['file'])==row['sha256'])
    check('1920x1080 '+row['id'],Image.open(ROOT/row['file']).size==(1920,1080))
    check('cache key '+row['id'],next(s for s in after if s['id']==row['id'])['image']==f"v22/scene-{row['id']}.jpg?v="+row['sha256'][:12])
old=(OUT/'before/prototype/index.html').read_text(encoding='utf8');new=(ROOT/'prototype/index.html').read_text(encoding='utf8')
check('HTML only scenes cache changed',new==re.sub(r'scenes.js\?v=[a-zA-Z0-9]+','scenes.js?v='+sha(ROOT/'prototype/scenes.js')[:12],old))
check('scenes syntax',subprocess.run(['node','--check',str(ROOT/'prototype/scenes.js')],capture_output=True).returncode==0)
(OUT/'verification/offline.json').write_text(json.dumps(checks,indent=2),encoding='utf8')
print(f'{len(checks)}/{len(checks)} passed')
