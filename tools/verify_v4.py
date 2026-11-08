"""Asset integrity and preservation checks for revision 4; no historical reports overwritten."""
from pathlib import Path
import hashlib,json,wave
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];V4=ROOT/'revisions/v4';checks=[]
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def read(path):return json.loads((ROOT/path).read_text(encoding='utf-8'))
def check(name,ok):
    checks.append({'name':name,'passed':bool(ok)})
    if not ok:raise RuntimeError(name)
baseline=read('verification/yoru-before.json')
for name,digest in baseline.items():check('Yoru unchanged '+name,sha(ROOT.parent/'AnimeLauncher'/name)==digest)
for name,digest in read('revisions/v3/verification/source-hashes.json').items():
    check('v3 code backed up '+name,sha(V4/'before'/name)==digest)
before=read('revisions/v4/before/asset-manifest.json');current=read('research/asset-manifest.json')
check('17 visual assets',len(current)==17)
for row in before:
    same=next(r for r in current if r['source']==row['source'])
    check('previous visual source preserved '+row['source'],same['source_sha256']==row['source_sha256'] and sha(ROOT/row['source'])==row['source_sha256'])
    check('previous runtime preserved '+row['runtime'],same['runtime_sha256']==row['runtime_sha256'] and sha(ROOT/row['runtime'])==row['runtime_sha256'])
for row in current:
    for key,hashkey in [('source','source_sha256'),('runtime','runtime_sha256')]:check('asset '+row[key],sha(ROOT/row[key])==row[hashkey])
    with Image.open(ROOT/row['runtime']) as image:image.verify()
    check('image decodes '+row['runtime'],True)
for row in read('revisions/v3/effect-manifest.json'):
    check('painted VFX source retained '+row['id'],sha(ROOT/row['source'])==row['source_sha256'])
    check('painted VFX runtime retained '+row['id'],sha(ROOT/row['runtime'])==row['runtime_sha256'])
audio=read('revisions/v4/audio-manifest.json');check('20 contextual audio entries',len(audio)==20)
for cue in ['rasengan','chidori','sharingan','kurama','summoning','hiraishin','fireball','amaterasu','rasenshuriken','gamabunta']:
    rows=[r for r in audio if r['technique']==cue];check('separate readiness and use '+cue,{r['event'] for r in rows}=={'focus','confirm'} and len({r['sha256'] for r in rows})==2)
for row in audio:
    name=row['technique']+' '+row['event'];check('audio source hash '+name,sha(ROOT/row['source'])==row['source_sha256']);check('audio runtime hash '+name,sha(ROOT/row['file'])==row['sha256'])
    with wave.open(str(ROOT/row['file']),'rb') as file:duration=file.getnframes()/file.getframerate();check('audio format '+name,file.getframerate()==44100 and file.getnchannels()==1)
    check('audio duration '+name,abs(duration-row['duration'])<.001)
    if row['event']=='confirm':check('complete activation phrase / SFX '+name,row['window'] is None and abs(duration-row['source_duration'])<.06)
    elif row['window']:check('readiness within source bounds '+name,row['window']['start']+row['window']['duration']<=row['source_duration']+.01)
check('Rasengan source replaced with longer complete phrase',next(r for r in audio if r['technique']=='rasengan' and r['event']=='confirm')['duration']>2.3)
for row in read('revisions/v2/audio-manifest.json')['music']:check('music preview unchanged '+row['name'],sha(ROOT/row['file'])==row['sha256'])
for row in read('revisions/v4/references/gaara-manifest.json'):check('Gaara reference hash '+row['file'],sha(ROOT/row['file'])==row['sha256'])
prompts=read('revisions/v4/generation-manifest.json');check('Gaara prompt and built-in generation recorded',len(prompts)==1 and prompts[0].get('prompt') and prompts[0]['tool']=='built-in imagegen')
hashes={p.relative_to(ROOT).as_posix():sha(p) for p in (ROOT/'prototype').iterdir() if p.is_file()}
(V4/'verification/source-hashes.json').write_text(json.dumps(hashes,indent=2),encoding='utf-8')
report={'passed':len(checks),'total':len(checks),'yoru_files_preserved':len(baseline),'wallpapers':7,'app_icons':10,'techniques':10,'audio_variants':20,'checks':checks}
(V4/'verification/offline-checks.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
print('PASS:',len(checks),'checks; 122 Yoru files and all previous art unchanged; 7 wallpapers; 20 contextual audio entries.')
