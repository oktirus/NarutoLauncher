from pathlib import Path
from PIL import Image
import hashlib,json,subprocess
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v18';checks=[]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def check(name,ok):
 checks.append(dict(name=name,passed=bool(ok)));assert ok,name
def registry(p,name):return json.loads(p.read_text(encoding='utf8').split('window.'+name+'=')[1].strip().rstrip(';'))
allowed={'prototype/app.js','prototype/index.html','prototype/scenes.js','prototype/quotes.js'}
for file,h in json.loads((OUT/'before-hashes.json').read_text()).items():
 if file not in allowed:check('preserved '+file,sha(ROOT/file)==h)
for file,h in json.loads((ROOT/'verification/yoru-before.json').read_text()).items():
 if file!='verification/preview-stderr.log':check('Yoru '+file,sha(ROOT.parent/'AnimeLauncher'/file)==h)
scenes=registry(ROOT/'prototype/scenes.js','KonohaScenes');before=registry(OUT/'before/prototype/scenes.js','KonohaScenes')
check('18 scenes and stable old order',len(scenes)==18 and [s['id'] for s in scenes if s['id']!='ramen']==[s['id'] for s in before])
for a in before:
 b=next(s for s in scenes if s['id']==a['id'])
 if a['id'] not in {'night','minato'}:check('exact scene '+a['id'],a==b)
 else:check('v18 image '+a['id'],b['image'].startswith('v18/scene-'))
quotes=registry(ROOT/'prototype/quotes.js','KonohaQuotes');old=registry(OUT/'before/prototype/quotes.js','KonohaQuotes');check('18 quote mappings',set(quotes)=={s['id'] for s in scenes});check('old quotes preserved',all(quotes[k]==v for k,v in old.items()));check('ramen same sourced Naruto quote',quotes['ramen']==quotes['dusk'])
rows=json.loads((OUT/'asset-manifest.json').read_text());check('three selected assets',len(rows)==3 and {r['id'] for r in rows}=={'night','minato','ramen'})
for row in rows:
 check('source '+row['id'],sha(ROOT/row['source'])==row['source_sha256']);check('runtime '+row['id'],sha(ROOT/row['file'])==row['sha256']);check('dimensions '+row['id'],Image.open(ROOT/row['file']).size==(1920,1080));check('image cache '+row['id'],row['sha256'][:12] in next(s for s in scenes if s['id']==row['id'])['image'])
html=(ROOT/'prototype/index.html').read_text(encoding='utf8');app=(ROOT/'prototype/app.js').read_text(encoding='utf8');oldapp=(OUT/'before/prototype/app.js').read_text(encoding='utf8')
check('new wallpaper glyph',html.count('id="wallpapers"')==1 and 'href="#wallpapers"' in app and 'scene-choice-name' in app)
check('order manager preserved',app.split('function orderedFavorites()')[1].split('function settingsPanel()')[0]==oldapp.split('function orderedFavorites()')[1].split('function settingsPanel()')[0])
for name in ['app.js','scenes.js','quotes.js','revision18.css','revision17.css','revision16.css','technique-art.js']:check('cache '+name,name+'?v='+sha(ROOT/'prototype'/name)[:12] in html)
for name in ['app.js','scenes.js','quotes.js']:check('syntax '+name,subprocess.run(['node','--check',str(ROOT/'prototype'/name)],capture_output=True).returncode==0)
(OUT/'verification/offline.json').write_text(json.dumps(checks,indent=2),encoding='utf8');print(f'{len(checks)}/{len(checks)} passed')
