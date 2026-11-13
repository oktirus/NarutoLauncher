from pathlib import Path
from PIL import Image
import json,hashlib,subprocess
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v20';checks=[]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def check(name,ok):checks.append(dict(name=name,passed=bool(ok)));assert ok,name
def registry(p):return json.loads(p.read_text(encoding='utf8').split('window.KonohaScenes=')[1].strip().rstrip(';'))
for file,h in json.loads((OUT/'before-hashes.json').read_text()).items():
 if file not in {'prototype/index.html','prototype/scenes.js'}:check('preserved '+file,sha(ROOT/file)==h)
before=registry(OUT/'before/prototype/scenes.js');after=registry(ROOT/'prototype/scenes.js');check('18 stable ids',len(after)==18 and [s['id'] for s in before]==[s['id'] for s in after])
for a,b in zip(before,after):
 if a['id']!='sai':check('exact scene '+a['id'],a==b)
row=json.loads((OUT/'asset-manifest.json').read_text())[0];s=next(s for s in after if s['id']=='sai');check('selected original',sha(ROOT/row['source'])==row['source_sha256']);check('runtime',sha(ROOT/row['file'])==row['sha256']);check('runtime dimensions',Image.open(ROOT/row['file']).size==(1920,1080));check('image cache key',s['image']=='v20/scene-sai.jpg?v='+row['sha256'][:12])
html=(ROOT/'prototype/index.html').read_text(encoding='utf8');old=(OUT/'before/prototype/index.html').read_text(encoding='utf8');import re
check('only scene cache changed in HTML',html==re.sub(r'scenes.js\?v=[a-zA-Z0-9]+','scenes.js?v='+sha(ROOT/'prototype/scenes.js')[:12],old))
check('scenes syntax',subprocess.run(['node','--check',str(ROOT/'prototype/scenes.js')],capture_output=True).returncode==0)
(OUT/'verification/offline.json').write_text(json.dumps(checks,indent=2),encoding='utf8');print(f'{len(checks)}/{len(checks)} passed')
