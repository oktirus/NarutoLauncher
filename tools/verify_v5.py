"""Integrity and signal-level regression guards for the v4 review fixes."""
from pathlib import Path
import array,hashlib,json,wave
from PIL import Image
from analyze_audio_v5 import decode,metrics,envelope
ROOT=Path(__file__).resolve().parents[1];V5=ROOT/'revisions/v5';checks=[]
def read(path):return json.loads((ROOT/path).read_text(encoding='utf-8'))
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def check(name,ok):
    checks.append({'name':name,'passed':bool(ok)})
    if not ok:raise RuntimeError(name)
def samples(path):
    with wave.open(str(path),'rb') as file:
        data=array.array('h');data.frombytes(file.readframes(file.getnframes()));rate=file.getframerate()
    return array.array('f',(v/32768 for v in data)),rate
def renewed_onset(values,rate):
    values=envelope(values,rate,.05);peak=max(values);seen=False;quiet=0;armed=False
    for value in values:
        if armed and value>peak*.30:return True
        if value>peak*.20:seen=True
        quiet=quiet+1 if seen and value<peak*.03 else 0
        if quiet>=3:armed=True
    return False
for name,digest in read('verification/yoru-before.json').items():check('Yoru unchanged '+name,sha(ROOT.parent/'AnimeLauncher'/name)==digest)
for name,digest in read('revisions/v4/verification/source-hashes.json').items():check('v4 code backup '+name,sha(V5/'before'/name)==digest)
for row in read('research/asset-manifest.json'):
    check('previous art source '+row['source'],sha(ROOT/row['source'])==row['source_sha256']);check('previous art runtime '+row['runtime'],sha(ROOT/row['runtime'])==row['runtime_sha256'])
    with Image.open(ROOT/row['runtime']) as image:image.verify()
    check('image decodes '+row['runtime'],True)
for row in read('revisions/v3/effect-manifest.json'):check('original painted VFX '+row['id'],sha(ROOT/row['source'])==row['source_sha256'] and sha(ROOT/row['runtime'])==row['runtime_sha256'])
old=read('revisions/v4/audio-manifest.json')
for row in old:check('v4 audio retained '+row['technique']+' '+row['event'],sha(ROOT/row['file'])==row['sha256'])
for row in read('revisions/v2/audio-manifest.json')['music']:check('music unchanged '+row['name'],sha(ROOT/row['file'])==row['sha256'])
rows=read('revisions/v5/audio-manifest.json');check('20 technique variants plus neutral service cue',len(rows)==21)
quality=[]
for row in rows:
    name=row['technique']+' '+row['event'];path=ROOT/row['file'];data,rate=samples(path);actual=metrics(data,rate)
    check('source hash '+name,sha(ROOT/row['source'])==row['source_sha256']);check('delivered hash '+name,sha(path)==row['sha256'])
    check('duration '+name,abs(len(data)/rate-row['duration'])<.001);check('rate '+name,rate==44100)
    check('prompt onset '+name,actual['leading_silence']<=.08);check('quiet 50ms tail '+name,actual['tail_ratio_50ms']<=.10)
    check('release envelope '+name,row['release_seconds']>=.15)
    quality.append({'technique':row['technique'],'event':row['event'],**actual})
before_failures=[]
for row in old:
    data,rate=samples(ROOT/row['file']);q=metrics(data,rate)
    if q['leading_silence']>.08 or q['tail_ratio_50ms']>.1:before_failures.append({'technique':row['technique'],'event':row['event'],**q})
for cue in ['gamabunta','rasenshuriken']:check('new onset guard rejects v4 '+cue+' focus',any(r['technique']==cue and r['event']=='focus' for r in before_failures))
onsets=[]
for cue in ['kurama','summoning']:
    before=next(r for r in old if r['technique']==cue and r['event']=='confirm');after=next(r for r in rows if r['technique']==cue and r['event']=='confirm');a,ar=samples(ROOT/before['file']);b,br=samples(ROOT/after['file']);bad=renewed_onset(a,ar);good=not renewed_onset(b,br)
    check('v4 unwanted renewed onset detected '+cue,bad);check('unwanted renewed onset removed '+cue,good);onsets.append({'technique':cue,'before':bad,'after':not good})
check('Gamabunta activation differs from plain summon',next(r['sha256'] for r in rows if r['technique']=='gamabunta' and r['event']=='confirm')!=next(r['sha256'] for r in rows if r['technique']=='summoning' and r['event']=='confirm'))
check('neutral feedback differs from summon focus',next(r['sha256'] for r in rows if r['technique']=='ui')!=next(r['sha256'] for r in rows if r['technique']=='summoning' and r['event']=='focus'))
alpha=read('revisions/v5/verification/plate-alpha.json');check('alpha regression fails all old modes',alpha['before']['passed']==0 and alpha['before']['total']==9);check('alpha regression passes all fixed modes',alpha['after']['passed']==9 and all(r['spread']<=3 for r in alpha['after']['results']))
(V5/'verification/audio-quality.json').write_text(json.dumps({'after':quality,'before_guard_failures':before_failures,'onset_regression':onsets},indent=2),encoding='utf-8')
(V5/'verification/source-hashes.json').write_text(json.dumps({p.relative_to(ROOT).as_posix():sha(p) for p in (ROOT/'prototype').iterdir() if p.is_file()},indent=2),encoding='utf-8')
report={'passed':len(checks),'total':len(checks),'yoru_files_preserved':122,'audio_variants':21,'checks':checks}
(V5/'verification/offline-checks.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
print('PASS:',len(checks),'checks; original art/music/Yoru and v4 audio preserved; all new cue heads/tails pass; old seam and signal guards demonstrably fail.')
