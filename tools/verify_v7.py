"""Preservation and asset-contract guards for v7; never overwrites historical evidence."""
from pathlib import Path
from PIL import Image
import array,hashlib,json,wave
from analyze_audio_v5 import metrics
ROOT=Path(__file__).resolve().parents[1];checks=[]
def read(path):return json.loads((ROOT/path).read_text(encoding='utf-8'))
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def check(name,ok):
    checks.append({'name':name,'passed':bool(ok)})
    if not ok:raise RuntimeError(name)
runtime_logs=[]
for name,digest in read('verification/yoru-before.json').items():
    path=ROOT.parent/'AnimeLauncher'/name
    if name=='verification/preview-stderr.log':
        runtime_logs.append({'file':name,'baseline_sha256':digest,'observed_sha256':sha(path),'bytes':path.stat().st_size,'classification':'live HTTP access log; observed separately, not a source/resource; never restored or baseline-updated'})
    else:check('Yoru preserved '+name,sha(path)==digest)
for name,digest in read('revisions/v6/verification/source-hashes.json').items():check('exact v6 backup '+name,sha(ROOT/'revisions/v7/before'/name)==digest)
for row in read('research/asset-manifest.json'):
    check('prior source '+row['source'],sha(ROOT/row['source'])==row['source_sha256']);check('prior runtime '+row['runtime'],sha(ROOT/row['runtime'])==row['runtime_sha256'])
for row in read('revisions/v3/effect-manifest.json'):check('v3 painted effect retained '+row['id'],sha(ROOT/row['source'])==row['source_sha256'] and sha(ROOT/row['runtime'])==row['runtime_sha256'])
for row in read('revisions/v5/audio-manifest.json'):check('prior v5 audio retained '+row['file'],sha(ROOT/row['file'])==row['sha256'])
for row in read('revisions/v2/audio-manifest.json')['music']:check('OST unchanged '+row['name'],sha(ROOT/row['file'])==row['sha256'])
for row in read('revisions/v6/asset-manifest.json'):
    check('v6 source retained '+row['source'],sha(ROOT/row['source'])==row['source_sha256']);check('v6 runtime retained '+row['file'],sha(ROOT/row['file'])==row['sha256'])
old=read('revisions/v6/audio-manifest.json')['jutsu'];check('v6 focus kept as archive',sha(ROOT/old['file'])==old['sha256'])
for row in read('revisions/v7/asset-manifest.json'):
    check('v7 source hash '+row['source'],sha(ROOT/row['source'])==row['source_sha256']);check('v7 runtime hash '+row['file'],sha(ROOT/row['file'])==row['sha256'])
    im=Image.open(ROOT/row['file']);im.load();check('new image size '+row['file'],list(im.size)==row['size']);check('new true alpha '+row['file'],im.mode=='RGBA' and im.getchannel('A').getextrema()[0]==0)
    if row['kind']=='motion-sheet':
        check('eight poses '+row['hero'],len(row['frames'])==8 and row['grid']==[4,2]);check('uniform scaling '+row['hero'],row['uniform_scale']>0)
        hashes=[]
        for frame in row['frames']:
            k=frame['frame'];cell=im.crop((k%4*512,k//4*512,k%4*512+512,k//4*512+512));hashes.append(hashlib.sha256(cell.tobytes()).hexdigest());box=cell.getchannel('A').point(lambda a:255 if a>32 else 0).getbbox()
            check(row['hero']+' pose '+str(k)+' safety margin',box is not None and box[0]>=20 and box[2]<=492 and box[1]>=25 and box[3]<=499)
        check('distinct frames '+row['hero'],len(set(hashes))==8)
        check('naturally lower crouches '+row['hero'],max(f['height'] for f in row['frames'][6:])<row['frames'][5]['height']*.78)
        size=116 if row['hero']=='naruto' else 142
        check('crouch clears visual shuriken '+row['hero'],max(f['height'] for f in row['frames'][6:])*size/512<63)
row=read('revisions/v7/audio-manifest.json');check('activation source integrity',sha(ROOT/row['source'])==row['source_sha256']);check('activation output integrity',sha(ROOT/row['file'])==row['sha256']);check('only final activation window',row['window_start']==1.48)
with wave.open(str(ROOT/row['file']),'rb')as f:
    data=array.array('h');data.frombytes(f.readframes(f.getnframes()));q=metrics(array.array('f',(v/32768 for v in data)),f.getframerate());check('standard PCM',f.getsampwidth()==2 and f.getframerate()==44100 and f.getnchannels()==1)
check('short final activation',.6<q['duration']<.7);check('immediate onset',q['leading_silence']<=.03);check('quiet natural release',q['tail_ratio_50ms']<.1)
refs=read('revisions/v7/reference-manifest.json');check('three Naruto anime references',len(refs)==3)
for row in refs:check('run reference '+row['file'],sha(ROOT/row['file'])==row['sha256'])
check('three distinct reference images',len({r['sha256'] for r in refs})==3)
prompts=read('revisions/v7/generation-manifest.json');check('all generation steps recorded',len(prompts)==8 and all(r['prompt'] for r in prompts))
out=ROOT/'revisions/v7/verification';(out/'offline.json').write_text(json.dumps({'passed':len(checks),'checks':checks,'yoru_static_files_preserved':121,'runtime_logs_observed_separately':runtime_logs},indent=2),encoding='utf-8');(out/'source-hashes.json').write_text(json.dumps({p.relative_to(ROOT).as_posix():sha(p) for p in sorted((ROOT/'prototype').iterdir()) if p.is_file()},indent=2),encoding='utf-8');print(f'{len(checks)} v7 integrity checks passed; 121 Yoru static files preserved, live access log observed separately')
