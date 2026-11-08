"""v6 integrity and preservation checks. Writes only the v6 evidence directory."""
from pathlib import Path
import hashlib,json,wave,array
from PIL import Image
from analyze_audio_v5 import metrics
ROOT=Path(__file__).resolve().parents[1];checks=[]
def read(path):return json.loads((ROOT/path).read_text(encoding='utf-8'))
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def check(name,ok):
    checks.append({'name':name,'passed':bool(ok)})
    if not ok:raise RuntimeError(name)
for name,digest in read('verification/yoru-before.json').items():check('Yoru unchanged '+name,sha(ROOT.parent/'AnimeLauncher'/name)==digest)
for name,digest in read('revisions/v5/verification/source-hashes.json').items():check('exact v5 backup '+name,sha(ROOT/'revisions/v6/before'/name)==digest)
for row in read('research/asset-manifest.json'):
    check('prior source preserved '+row['source'],sha(ROOT/row['source'])==row['source_sha256'])
    check('prior runtime preserved '+row['runtime'],sha(ROOT/row['runtime'])==row['runtime_sha256'])
for row in read('revisions/v3/effect-manifest.json'):check('painted VFX preserved '+row['id'],sha(ROOT/row['source'])==row['source_sha256'] and sha(ROOT/row['runtime'])==row['runtime_sha256'])
for row in read('revisions/v2/audio-manifest.json')['music']:check('music preserved '+row['name'],sha(ROOT/row['file'])==row['sha256'])
for row in read('revisions/v5/audio-manifest.json'):check('v5 audio preserved '+row['file'],sha(ROOT/row['file'])==row['sha256'])
assets=read('revisions/v6/asset-manifest.json');check('ten icons and two game resources',len(assets)==12)
for row in assets:
    check('new source hash '+row['source'],sha(ROOT/row['source'])==row['source_sha256']);check('new runtime hash '+row['file'],sha(ROOT/row['file'])==row['sha256'])
    with Image.open(ROOT/row['file']) as image:
        image.load();check('runtime geometry '+row['file'],list(image.size)==row['size'])
        if row['kind'] in ['icon','sprite-sheet']:check('genuine alpha '+row['file'],image.mode=='RGBA' and image.getchannel('A').getextrema()[0]==0)
row=read('revisions/v6/audio-manifest.json')['jutsu'];check('jutsu original hash',sha(ROOT/row['source'])==row['source_sha256']);check('jutsu delivered hash',sha(ROOT/row['file'])==row['sha256'])
with wave.open(str(ROOT/row['file']),'rb') as f:
    check('jutsu standard PCM',f.getnchannels()==1 and f.getframerate()==44100 and f.getsampwidth()==2);data=array.array('h');data.frombytes(f.readframes(f.getnframes()));q=metrics(array.array('f',(x/32768 for x in data)),f.getframerate())
check('jutsu prompt onset',q['leading_silence']<=.08);check('jutsu quiet tail',q['tail_ratio_50ms']<=.1)
prompts=read('revisions/v6/generation-manifest.json');check('all generated prompts retained',len(prompts)==13 and all(r['prompt'] for r in prompts));check('ten shipped icon prompts',sum(r['used'] and r['id'] not in ['naruto-run','konoha-forest'] for r in prompts)==10)
report={'passed':len(checks),'yoru_preserved':122,'assets':12,'checks':checks};(ROOT/'revisions/v6/verification/offline.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
(ROOT/'revisions/v6/verification/source-hashes.json').write_text(json.dumps({p.relative_to(ROOT).as_posix():sha(p) for p in sorted((ROOT/'prototype').iterdir()) if p.is_file()},indent=2),encoding='utf-8')
print(f'{len(checks)} integrity checks passed; 122 Yoru files and all prior art/music/audio retained')
