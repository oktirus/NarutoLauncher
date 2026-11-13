"""v17 lineage and resource guards, relative to the frozen v16 product."""
from pathlib import Path
from PIL import Image
import json,hashlib,subprocess
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v17';checks=[]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def check(name,ok):
 checks.append(dict(name=name,passed=bool(ok)));assert ok,name
def registry(p,name):return json.loads(p.read_text(encoding='utf8').split('window.'+name+'=')[1].strip().rstrip(';'))
allowed={'prototype/app.js','prototype/index.html','prototype/scenes.js'}
for file,h in json.loads((OUT/'before-hashes.json').read_text()).items():
 if file not in allowed:check('unchanged '+file,sha(ROOT/file)==h)
for file,h in json.loads((ROOT/'verification/yoru-before.json').read_text()).items():
 if file!='verification/preview-stderr.log':check('Yoru '+file,sha(ROOT.parent/'AnimeLauncher'/file)==h)
scenes=registry(ROOT/'prototype/scenes.js','KonohaScenes');before=registry(OUT/'before/prototype/scenes.js','KonohaScenes');changed={'night','rocklee','sai'}
check('17 stable ids',[s['id'] for s in scenes]==[s['id'] for s in before] and len(scenes)==17)
for a,b in zip(before,scenes):
 if a['id'] not in changed:check('exact scene '+a['id'],a==b)
 else:check('new version '+a['id'],b['image'].startswith('v17/scene-'))
rows=json.loads((OUT/'asset-manifest.json').read_text());check('three selected scenes',{r['id'] for r in rows}==changed and len(rows)==3)
for row in rows:
 check('source '+row['id'],sha(ROOT/row['source'])==row['source_sha256']);check('runtime '+row['id'],sha(ROOT/row['file'])==row['sha256']);check('dimensions '+row['id'],Image.open(ROOT/row['file']).size==(1920,1080));check('image cache '+row['id'],row['sha256'][:12] in next(s for s in scenes if s['id']==row['id'])['image'])
html=(ROOT/'prototype/index.html').read_text(encoding='utf8');app=(ROOT/'prototype/app.js').read_text(encoding='utf8')
for token in ['hero-kicker','quote-source']:check('removed '+token,token not in html and token not in app)
check('new application heading','<h2 id="apps-title">Сувої розваг</h2>' in html)
for name in ['app.js','scenes.js','quotes.js','revision16.css','revision17.css','technique-art.js']:check('cache '+name,name+'?v='+sha(ROOT/'prototype'/name)[:12] in html)
for p in [ROOT/'prototype/app.js',ROOT/'prototype/scenes.js',ROOT/'tools/browser_v17_order_checks.js']:check('syntax '+p.name,subprocess.run(['node','--check',str(p)],capture_output=True).returncode==0)
(OUT/'verification/offline.json').write_text(json.dumps(checks,indent=2),encoding='utf8');print(f'{len(checks)}/{len(checks)} passed')
