"""Preservation, lineage, selected assets and duplicate UI removal guards."""
from pathlib import Path
from PIL import Image
import hashlib,json,re,subprocess
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v16';checks=[]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def check(name,ok):
    checks.append(dict(name=name,passed=bool(ok)))
    assert ok,name
def registry(p,name):return json.loads(p.read_text(encoding='utf8').split('window.'+name+'=')[1].strip().rstrip(';'))
for file,h in json.loads((ROOT/'verification/yoru-before.json').read_text()).items():
    if file!='verification/preview-stderr.log':check('Yoru '+file,sha(ROOT.parent/'AnimeLauncher'/file)==h)
allowed={'prototype/index.html','prototype/app.js','prototype/scenes.js','prototype/technique-art.js'}
for file,h in json.loads((OUT/'before-hashes.json').read_text()).items():
    if file not in allowed:check('preserved '+file,sha(ROOT/file)==h)
scenes=registry(ROOT/'prototype/scenes.js','KonohaScenes');before=registry(OUT/'before/prototype/scenes.js','KonohaScenes')
changed={'dusk','night','sakura','gaara','rocklee','jiraiya','minato','sai'}
check('17 ids stable',[s['id'] for s in scenes]==[s['id'] for s in before] and len(scenes)==17)
for a,b in zip(before,scenes):
    if a['id'] not in changed:check('exact other scene '+a['id'],a==b)
    else:check('v16 '+a['id'],b['image'].startswith('v16/scene-'))
check('boat replaced by training',next(s for s in scenes if s['id']=='night')['name']=='Тренування Расенсюрикена')
rows=json.loads((OUT/'asset-manifest.json').read_text());check('eight wallpapers plus logo',len(rows)==9 and {r['id'] for r in rows}==changed|{'logo'})
for row in rows:
    check('original '+row['id'],sha(ROOT/row['source'])==row['source_sha256'])
    check('runtime '+row['id'],sha(ROOT/row['file'])==row['sha256'])
    if row['id']!='logo':check('cache key '+row['id'],row['sha256'][:12] in next(s for s in scenes if s['id']==row['id'])['image'])
logo=Image.open(ROOT/'assets/runtime/v16/naruto-logo.webp');check('logo real alpha',logo.mode=='RGBA' and logo.getchannel('A').getextrema()==(0,255))
quotes=registry(ROOT/'prototype/quotes.js','KonohaQuotes');check('quote for every scene',set(quotes)=={s['id'] for s in scenes})
for id,row in quotes.items():check('sourced own translation '+id,row['source'].startswith('https://') and bool(row['original']) and 'Власний' in row['translation'])
html=(ROOT/'prototype/index.html').read_text(encoding='utf8')
for token in ['nav-tab','scene-caption','audio-invitation','hero-actions','scene-strip','footer-quote']:check('removed '+token,token not in html)
check('game icon replaces tab','id="gamepad"' in html and 'game-shortcut' in html)
check('drawn header logo','class="naruto-logo"' in html and '>KONOHA<' not in html)
art=(ROOT/'prototype/technique-art.js').read_text();check('Chidori full procedural','if(id===\'chidori\'){c.save();c.translate(x,y);electricity' in art and "api.proceduralEffects=['chidori']" in art)
index=html
for filename in ['app.js','scenes.js','technique-art.js','quotes.js','revision16.css']:check('content cache '+filename,filename+'?v='+sha(ROOT/'prototype'/filename)[:12] in index)
commit=subprocess.check_output(['git','show','-s','--format=%s','e68d521'],cwd=ROOT,encoding='utf8').strip();check('baseline commit exists','preserve v15' in commit)
(OUT/'verification/offline.json').write_text(json.dumps(checks,indent=2),encoding='utf8');print(f'{len(checks)}/{len(checks)} passed')
