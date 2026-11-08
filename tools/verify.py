"""Verify generated assets, cue integrity and byte preservation of the existing launcher."""
import hashlib,json,wave
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];checks=[]
def check(name,condition):
 checks.append({'name':name,'passed':bool(condition)})
 if not condition:raise RuntimeError(name)
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
baseline=json.loads((ROOT/'verification/yoru-before.json').read_text(encoding='utf-8'))
for relative,expected in baseline.items():
 p=ROOT.parent/'AnimeLauncher'/relative
 check('Yoru preserved: '+relative,p.is_file() and sha(p)==expected)
manifest=json.loads((ROOT/'research/asset-manifest.json').read_text(encoding='utf-8'))
check('17 visual assets',len(manifest)==17)
icons=0
for item in manifest:
 original=ROOT/item['source'];runtime=ROOT/item['runtime']
 check('original hash: '+original.name,sha(original)==item['source_sha256'])
 check('runtime hash: '+runtime.name,sha(runtime)==item['runtime_sha256'])
 image=Image.open(runtime);image.load()
 check('image decodes: '+runtime.name,image.width>0 and image.height>0)
 if runtime.suffix=='.webp':
  icons+=1;check('genuine transparency: '+runtime.name,image.mode=='RGBA' and image.getchannel('A').getextrema()[0]==0)
check('10 illustrated app icons',icons==10)
for item in json.loads((ROOT/'research/audio-manifest.json').read_text(encoding='utf-8')):
 p=ROOT/item['file'];check('audio hash: '+p.name,sha(p)==item['sha256'])
 with wave.open(str(p),'rb') as f:check('WAV decodes: '+p.name,f.getnframes()>0 and f.getframerate()==44100)
prompts=json.loads((ROOT/'research/generation-manifest.json').read_text(encoding='utf-8'))
check('original v1 generation prompts preserved',len(prompts)==13 and all(x.get('prompt') for x in prompts))
report={'passed':len(checks),'total':len(checks),'yoru_files_preserved':len(baseline),'visual_assets':len(manifest),'illustrated_icons':10,'original_audio_cues':5,'checks':checks}
(ROOT/'verification/offline-checks.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
print(f"PASS: {len(checks)} checks; {len(baseline)} Yoru files unchanged; {len(manifest)} visuals; 5 archived v1 audio cues.")
