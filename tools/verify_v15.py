"""Scope, asset alpha and exact v14 preservation checks for two redraws."""
from pathlib import Path
from PIL import Image
import hashlib,json,re
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v15';results=[]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def check(name,ok):
    results.append(dict(name=name,passed=bool(ok)))
    assert ok,name
for file,hash_ in json.loads((ROOT/'verification/yoru-before.json').read_text()).items():
    if file!='verification/preview-stderr.log':check('Yoru preserved '+file,sha(ROOT.parent/'AnimeLauncher'/file)==hash_)
for file,hash_ in json.loads((OUT/'before-hashes.json').read_text()).items():
    if file.startswith('assets/'):
        check('preserved '+file,sha(ROOT/file)==hash_)
changed={'scenes.js','effects.js','technique-art.js','index.html'}
for p in (OUT/'before/prototype').iterdir():
    if p.is_file() and p.name not in changed:check('unchanged code '+p.name,sha(p)==sha(ROOT/'prototype'/p.name))
def registry(path,name):return json.loads(path.read_text(encoding='utf8').split('window.'+name+'=')[1].strip().rstrip(';'))
before=registry(OUT/'before/prototype/scenes.js','KonohaScenes');after=registry(ROOT/'prototype/scenes.js','KonohaScenes')
check('same 17 scene ids',[s['id'] for s in before]==[s['id'] for s in after] and len(after)==17)
for a,b in zip(before,after):
    if a['id']!='sakura':check('exact scene '+a['id'],a==b)
    else:check('Sakura text unchanged',{k:v for k,v in a.items() if k!='image'}=={k:v for k,v in b.items() if k not in ('image','mobilePosition','cardPosition')})
effectsBefore=registry(OUT/'before/prototype/effects.js','KonohaEffectSources');effectsAfter=registry(ROOT/'prototype/effects.js','KonohaEffectSources')
check('only Chidori effect path changed',{k:v for k,v in effectsBefore.items() if k!='chidori'}=={k:v for k,v in effectsAfter.items() if k!='chidori'})
art=(OUT/'before/prototype/technique-art.js').read_text().replace("'fireball','amaterasu']);","'fireball','amaterasu','chidori']);").replace("id==='chidori'?.68+Math.sin(t*17)*.1","id==='chidori'?.88+Math.sin(t*17)*.07").replace('if(!completePaintings.has(id))accents','if(!completePaintings.has(id)||id===\'chidori\')accents')
check('only Chidori render conditions changed',art==(ROOT/'prototype/technique-art.js').read_text())
for row in json.loads((OUT/'asset-manifest.json').read_text()):
    check(row['id']+' runtime hash',sha(ROOT/row['file'])==row['sha256'])
    check(row['id']+' original hash',sha(ROOT/row['source'])==row['source_sha256'])
im=Image.open(ROOT/'assets/effects/v15/chidori.webp').convert('RGBA');alpha=im.getchannel('A')
check('real generated transparency',alpha.getextrema()==(0,255))
check('center fully painted',alpha.crop((300,300,468,468)).getextrema()[0]>250)
# Generator retains a few alpha=1 pixels (0.39% opacity), including one corner.
check('four corners <=1/255 opacity',all(alpha.getpixel(p)<=1 for p in ((0,0),(767,0),(0,767),(767,767))))
for row in json.loads((OUT/'references/sources.json').read_text(encoding='utf8')):check('reference '+row['file'],sha(ROOT/row['file'])==row['sha256'])
(OUT/'verification/offline.json').write_text(json.dumps(results,indent=2),encoding='utf8')
print(f'{len(results)}/{len(results)} passed: old assets, unrelated code, other scenes/effects, new sources and complete alpha core')
